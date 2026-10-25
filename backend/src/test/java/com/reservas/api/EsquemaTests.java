package com.reservas.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reservaQueTerminaAntesDeEmpezarEsRechazada() {
        long negocio = crearNegocio("Cancha");
        long servicio = crearServicio(negocio);
        long recurso = crearRecurso(negocio);

        assertThatThrownBy(() ->
                insertarReserva(negocio, servicio, recurso, "2030-01-01 11:00+00", "2030-01-01 10:00+00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDuplicadoSinDistinguirMayusculasEsRechazado() {
        crearUsuario("Ana@Ejemplo.com");

        assertThatThrownBy(() -> crearUsuario("ana@ejemplo.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long crearNegocio(String nombre) {
        return jdbc.queryForObject(
                "INSERT INTO negocio (nombre, zona_horaria) VALUES (?, 'UTC') RETURNING id",
                Long.class, nombre);
    }

    private long crearServicio(long negocioId) {
        return jdbc.queryForObject(
                "INSERT INTO servicio (negocio_id, nombre, duracion_min, precio) VALUES (?, 'Corte', 30, 10.00) RETURNING id",
                Long.class, negocioId);
    }

    private long crearRecurso(long negocioId) {
        return jdbc.queryForObject(
                "INSERT INTO recurso (negocio_id, nombre) VALUES (?, 'Sillón 1') RETURNING id",
                Long.class, negocioId);
    }

    private void crearUsuario(String email) {
        jdbc.update("INSERT INTO usuario (email, password_hash, nombre, rol) VALUES (?, 'hash', 'Ana', 'CLIENTE')",
                email);
    }

    private void insertarReserva(long negocioId, long servicioId, long recursoId, String inicio, String fin) {
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio) "
                        + "VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, 10.00)",
                negocioId, servicioId, recursoId, inicio, fin);
    }

}
