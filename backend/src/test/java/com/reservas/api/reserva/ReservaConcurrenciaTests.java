package com.reservas.api.reserva;

import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.horario.Horario;
import com.reservas.api.horario.HorarioRepository;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
import com.reservas.api.recurso.Recurso;
import com.reservas.api.recurso.RecursoRepository;
import com.reservas.api.servicio.Servicio;
import com.reservas.api.servicio.ServicioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Sin @Transactional en la clase: cada hilo necesita su propia transacción real y ver los datos confirmados.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ReservaConcurrenciaTests.RelojFijo.class})
class ReservaConcurrenciaTests {

    private static final int HILOS = 50;
    private static final int HILOS_REPROGRAMAR = 10;
    private static final short LUNES = 1;

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse("2026-11-01T16:00:00Z"), ZoneOffset.UTC);
        }

    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private ServicioRepository servicios;

    @Autowired
    private HorarioRepository horarios;

    @Autowired
    private ReservaRepository reservas;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM reserva");
        jdbc.update("DELETE FROM horario");
        jdbc.update("DELETE FROM servicio");
        jdbc.update("DELETE FROM recurso");
        jdbc.update("DELETE FROM negocio");
    }

    @Test
    void cincuentaSolicitudesSimultaneasAlMismoTurnoCreanUnaSolaReserva() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(new Servicio(
                negocio.getId(), "Corte", 60, new BigDecimal("25.00"), true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        String cuerpo = """
                {
                    "servicioId": %d,
                    "recursoId": %d,
                    "inicio": "2026-11-02T14:00:00Z"
                }
                """.formatted(servicio.getId(), recurso.getId());
        String ruta = "/negocios/" + negocio.getId() + "/reservas";

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>(HILOS);

        try {
            for (int i = 0; i < HILOS; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return mockMvc.perform(post(ruta)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(cuerpo))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            listos.await();
            salida.countDown();

            List<Integer> codigos = new ArrayList<>(HILOS);
            for (Future<Integer> futuro : futuros) {
                codigos.add(futuro.get());
            }

            long creadas = codigos.stream().filter(c -> c == 201).count();
            long conflictos = codigos.stream().filter(c -> c == 409).count();

            Integer confirmadasEnBase = jdbc.queryForObject(
                    "SELECT count(*) FROM reserva WHERE recurso_id = ? AND estado = 'CONFIRMADA'",
                    Integer.class, recurso.getId());

            assertThat(confirmadasEnBase).isEqualTo(1);
            assertThat(creadas).isEqualTo(1);
            assertThat(conflictos).isEqualTo(HILOS - 1);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void diezReprogramacionesSimultaneasAlMismoTurnoMuevenUnaSolaReserva() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(new Servicio(
                negocio.getId(), "Corte", 60, new BigDecimal("25.00"), true));
        // Horario de 08:00 a 20:00 (13:00Z a 01:00Z) para alojar las 10 reservas iniciales y el turno destino
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(8, 0), LocalTime.of(20, 0)));

        Instant base = Instant.parse("2026-11-02T13:00:00Z");
        List<Reserva> creadas = new ArrayList<>(HILOS_REPROGRAMAR);
        for (int i = 0; i < HILOS_REPROGRAMAR; i++) {
            Instant inicio = base.plus(Duration.ofHours(i));
            creadas.add(reservas.save(new Reserva(
                    negocio.getId(), servicio.getId(), recurso.getId(),
                    inicio, inicio.plus(Duration.ofMinutes(60)), servicio.getPrecio())));
        }

        // Turno destino libre: lunes a las 19:00 locales (2026-11-03T00:00:00Z)
        Instant inicioDestino = Instant.parse("2026-11-03T00:00:00Z");
        String cuerpo = """
                {
                    "inicio": "%s"
                }
                """.formatted(inicioDestino);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS_REPROGRAMAR);
        CountDownLatch listos = new CountDownLatch(HILOS_REPROGRAMAR);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>(HILOS_REPROGRAMAR);

        try {
            for (Reserva reserva : creadas) {
                String ruta = "/negocios/" + negocio.getId() + "/reservas/" + reserva.getId() + "/reprogramar";
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return mockMvc.perform(post(ruta)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(cuerpo))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            listos.await();
            salida.countDown();

            List<Integer> codigos = new ArrayList<>(HILOS_REPROGRAMAR);
            for (Future<Integer> futuro : futuros) {
                codigos.add(futuro.get());
            }

            long reprogramadas = codigos.stream().filter(c -> c == 200).count();
            long conflictos = codigos.stream().filter(c -> c == 409).count();

            Integer enTurnoDestino = jdbc.queryForObject(
                    "SELECT count(*) FROM reserva WHERE recurso_id = ? AND inicio = ? AND estado = 'CONFIRMADA'",
                    Integer.class, recurso.getId(), java.time.OffsetDateTime.ofInstant(inicioDestino, ZoneOffset.UTC));

            assertThat(enTurnoDestino).isEqualTo(1);
            assertThat(reprogramadas).isEqualTo(1);
            assertThat(conflictos).isEqualTo(HILOS_REPROGRAMAR - 1);
        } finally {
            pool.shutdown();
        }
    }

}
