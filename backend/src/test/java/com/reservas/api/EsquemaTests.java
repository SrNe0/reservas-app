package com.reservas.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que las restricciones del esquema rechazan datos inválidos.
 * Cada prueba corre en una transacción que se revierte al terminar. Como un error SQL
 * deja la transacción abortada, la sentencia que debe fallar va siempre al final.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EsquemaTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void reservaConServicioDeOtroNegocioEsRechazada() {
        long negocioA = crearNegocio("Barbería A");
        long negocioB = crearNegocio("Consultorio B");
        long servicioA = crearServicio(negocioA);
        long servicioB = crearServicio(negocioB);
        long recursoA = crearRecurso(negocioA);

        // Control: con un servicio del mismo negocio la reserva se acepta
        insertarReserva(negocioA, servicioA, recursoA, "2030-01-01 10:00+00", "2030-01-01 11:00+00");

        assertThatThrownBy(() ->
                insertarReserva(negocioA, servicioB, recursoA, "2030-01-01 12:00+00", "2030-01-01 13:00+00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_reserva_servicio");
    }

    @Test
    void reservaQueTerminaAntesDeEmpezarEsRechazada() {
        long negocio = crearNegocio("Cancha");
        long servicio = crearServicio(negocio);
        long recurso = crearRecurso(negocio);

        assertThatThrownBy(() ->
                insertarReserva(negocio, servicio, recurso, "2030-01-01 11:00+00", "2030-01-01 10:00+00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_reserva_fechas");
    }

    @Test
    void emailDuplicadoSinDistinguirMayusculasEsRechazado() {
        crearUsuario("Ana@Ejemplo.com");

        assertThatThrownBy(() -> crearUsuario("ana@ejemplo.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_usuario_email");
    }

    @Test
    void horarioAceptaDosTurnosElMismoDia() {
        long negocio = crearNegocio("Barbería");

        insertarHorario(negocio, 1, "09:00", "13:00");
        insertarHorario(negocio, 1, "15:00", "19:00");

        Integer turnos = jdbc.queryForObject(
                "SELECT count(*) FROM horario WHERE negocio_id = ? AND dia_semana = 1", Integer.class, negocio);
        assertThat(turnos).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"18:00,09:00", "09:00,09:00"})
    void horarioConCierreIgualOAnteriorALaAperturaEsRechazado(String apertura, String cierre) {
        long negocio = crearNegocio("Barbería");

        assertThatThrownBy(() -> insertarHorario(negocio, 1, apertura, cierre))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_horario_horas");
    }

    @Test
    void dosReservasConfirmadasQueSeSolapanEnElMismoRecursoSonRechazadas() {
        long negocio = crearNegocio("Barbería");
        long servicio = crearServicio(negocio);
        long recurso = crearRecurso(negocio);
        insertarReserva(negocio, servicio, recurso, "2030-01-01 10:00+00", "2030-01-01 11:00+00");

        assertThatThrownBy(() ->
                insertarReserva(negocio, servicio, recurso, "2030-01-01 10:30+00", "2030-01-01 11:30+00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_reserva_recurso_solape");
    }

    @Test
    void reservasAdyacentesOEnRecursosDistintosSeAceptan() {
        long negocio = crearNegocio("Barbería");
        long servicio = crearServicio(negocio);
        long recurso = crearRecurso(negocio);
        long otroRecurso = crearRecurso(negocio);
        insertarReserva(negocio, servicio, recurso, "2030-01-01 10:00+00", "2030-01-01 11:00+00");

        // Empieza justo cuando termina la anterior: el rango es [inicio, fin)
        insertarReserva(negocio, servicio, recurso, "2030-01-01 11:00+00", "2030-01-01 12:00+00");
        // Mismo horario pero otro recurso
        insertarReserva(negocio, servicio, otroRecurso, "2030-01-01 10:00+00", "2030-01-01 11:00+00");

        Integer total = jdbc.queryForObject("SELECT count(*) FROM reserva WHERE negocio_id = ?",
                Integer.class, negocio);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void unaReservaCanceladaNoBloqueaElTurno() {
        long negocio = crearNegocio("Barbería");
        long servicio = crearServicio(negocio);
        long recurso = crearRecurso(negocio);
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio, estado) "
                        + "VALUES (?, ?, ?, '2030-01-01 10:00+00', '2030-01-01 11:00+00', 10.00, 'CANCELADA')",
                negocio, servicio, recurso);

        insertarReserva(negocio, servicio, recurso, "2030-01-01 10:00+00", "2030-01-01 11:00+00");

        Integer total = jdbc.queryForObject("SELECT count(*) FROM reserva WHERE negocio_id = ?",
                Integer.class, negocio);
        assertThat(total).isEqualTo(2);
    }

    private long crearNegocio(String nombre) {
        return insertarYObtenerId(
                "INSERT INTO negocio (nombre, zona_horaria) VALUES (?, 'UTC') RETURNING id", nombre);
    }

    private long crearServicio(long negocioId) {
        return insertarYObtenerId(
                "INSERT INTO servicio (negocio_id, nombre, duracion_min, precio) VALUES (?, 'Corte', 30, 10.00) RETURNING id",
                negocioId);
    }

    private long crearRecurso(long negocioId) {
        return insertarYObtenerId(
                "INSERT INTO recurso (negocio_id, nombre) VALUES (?, 'Sillón 1') RETURNING id", negocioId);
    }

    private long insertarYObtenerId(String sql, Object... args) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, args));
    }

    private void crearUsuario(String email) {
        jdbc.update("INSERT INTO usuario (email, password_hash, nombre, rol) VALUES (?, 'hash', 'Ana', 'CLIENTE')",
                email);
    }

    private void insertarHorario(long negocioId, int diaSemana, String apertura, String cierre) {
        jdbc.update("INSERT INTO horario (negocio_id, dia_semana, hora_apertura, hora_cierre) "
                        + "VALUES (?, ?, ?::time, ?::time)",
                negocioId, diaSemana, apertura, cierre);
    }

    private void insertarReserva(long negocioId, long servicioId, long recursoId, String inicio, String fin) {
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio) "
                        + "VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, 10.00)",
                negocioId, servicioId, recursoId, inicio, fin);
    }

}
