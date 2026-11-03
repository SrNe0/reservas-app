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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hoy, para estas pruebas, es domingo 2026-11-01 a las 11:00 en Bogotá (16:00 UTC).
// El lunes 2026-11-02 a las 09:00 locales equivale a 2026-11-02T14:00:00Z.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ReservaControllerTests.RelojFijo.class})
@Transactional
class ReservaControllerTests {

    private static final short LUNES = 1;
    private static final short DOMINGO = 7;

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

    @Test
    void crearEnTurnoLibreDevuelve201ConLocationPrecioYEstadoConfirmada() throws Exception {
        Escenario e = prepararEscenario();

        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        matchesPattern(".*/negocios/" + e.negocio.getId() + "/reservas/\\d+")))
                .andExpect(jsonPath("$.negocioId").value(e.negocio.getId()))
                .andExpect(jsonPath("$.servicioId").value(e.servicio.getId()))
                .andExpect(jsonPath("$.recursoId").value(e.recurso.getId()))
                .andExpect(jsonPath("$.inicio").value("2026-11-02T14:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-11-02T15:00:00Z"))
                .andExpect(jsonPath("$.precio").value(25.00))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());
    }

    @Test
    void inicioFueraDeLaRejillaDevuelve400() throws Exception {
        Escenario e = prepararEscenario();

        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:07:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void turnoFueraDeHorarioDevuelve400() throws Exception {
        Escenario e = prepararEscenario();

        // 20:00Z son las 15:00 locales en Bogotá (el horario cierra a las 13:00)
        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T20:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void turnoEnElPasadoDevuelve400() throws Exception {
        Escenario e = prepararEscenario();
        horarios.save(new Horario(e.negocio.getId(), DOMINGO, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        // El reloj está en 2026-11-01T16:00:00Z (11:00 locales); 14:00Z (09:00 locales) ya pasó
        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-01T14:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservarElMismoTurnoDosVecesDevuelve409() throws Exception {
        Escenario e = prepararEscenario();

        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isCreated());
        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isConflict());
    }

    @Test
    void turnoAdyacenteAUnaReservaExistenteDevuelve201() throws Exception {
        Escenario e = prepararEscenario();

        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isCreated());
        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T15:00:00Z"))
                .andExpect(status().isCreated());
    }

    @Test
    void unaReservaCanceladaNoBloqueaElTurno() throws Exception {
        Escenario e = prepararEscenario();
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio, estado) "
                        + "VALUES (?, ?, ?, ?, ?, 25.00, 'CANCELADA')",
                e.negocio.getId(), e.servicio.getId(), e.recurso.getId(),
                OffsetDateTime.parse("2026-11-02T14:00:00Z"),
                OffsetDateTime.parse("2026-11-02T15:00:00Z"));

        mockMvc.perform(crearReserva(e.negocio, e.servicio, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isCreated());
    }

    @Test
    void recursoOServicioInactivoDevuelve409() throws Exception {
        Escenario e = prepararEscenario();
        Recurso recursoInactivo = recursos.save(new Recurso(e.negocio.getId(), "Sillón 2", false));
        Servicio servicioInactivo = servicios.save(new Servicio(
                e.negocio.getId(), "Barba", 60, new BigDecimal("15.00"), false));

        mockMvc.perform(crearReserva(e.negocio, e.servicio, recursoInactivo, "2026-11-02T14:00:00Z"))
                .andExpect(status().isConflict());
        mockMvc.perform(crearReserva(e.negocio, servicioInactivo, e.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isConflict());
    }

    @Test
    void recursoOServicioDeOtroNegocioONegocioInexistenteDevuelve404() throws Exception {
        Escenario a = prepararEscenario();
        Negocio b = negocios.save(new Negocio("Cancha B", null, "America/Bogota"));
        Recurso recursoDeB = recursos.save(new Recurso(b.getId(), "Cancha 1", true));
        Servicio servicioDeB = servicios.save(new Servicio(
                b.getId(), "Partido", 60, new BigDecimal("50.00"), true));

        mockMvc.perform(crearReserva(a.negocio, a.servicio, recursoDeB, "2026-11-02T14:00:00Z"))
                .andExpect(status().isNotFound());
        mockMvc.perform(crearReserva(a.negocio, servicioDeB, a.recurso, "2026-11-02T14:00:00Z"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/negocios/999999/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "servicioId": %d,
                                    "recursoId": %d,
                                    "inicio": "2026-11-02T14:00:00Z"
                                }
                                """.formatted(a.servicio.getId(), a.recurso.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void faltarUnCampoObligatorioDevuelve400() throws Exception {
        Escenario e = prepararEscenario();

        mockMvc.perform(post("/negocios/" + e.negocio.getId() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "servicioId": %d,
                                    "recursoId": %d
                                }
                                """.formatted(e.servicio.getId(), e.recurso.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerYListarFiltranPorNegocioYPorRecurso() throws Exception {
        Escenario a = prepararEscenario();
        Recurso otroRecursoDeA = recursos.save(new Recurso(a.negocio.getId(), "Sillón 2", true));
        Negocio b = negocios.save(new Negocio("Cancha B", null, "America/Bogota"));
        Recurso recursoDeB = recursos.save(new Recurso(b.getId(), "Cancha 1", true));
        Servicio servicioDeB = servicios.save(new Servicio(
                b.getId(), "Partido", 60, new BigDecimal("50.00"), true));

        Reserva primeraDeA = reservas.save(new Reserva(a.negocio.getId(), a.servicio.getId(), a.recurso.getId(),
                Instant.parse("2026-11-02T15:00:00Z"), Instant.parse("2026-11-02T16:00:00Z"),
                a.servicio.getPrecio()));
        Reserva segundaDeA = reservas.save(new Reserva(a.negocio.getId(), a.servicio.getId(), otroRecursoDeA.getId(),
                Instant.parse("2026-11-02T14:00:00Z"), Instant.parse("2026-11-02T15:00:00Z"),
                a.servicio.getPrecio()));
        Reserva reservaDeB = reservas.save(new Reserva(b.getId(), servicioDeB.getId(), recursoDeB.getId(),
                Instant.parse("2026-11-02T14:00:00Z"), Instant.parse("2026-11-02T15:00:00Z"),
                servicioDeB.getPrecio()));

        mockMvc.perform(get("/negocios/" + a.negocio.getId() + "/reservas/" + primeraDeA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(primeraDeA.getId()));
        mockMvc.perform(get("/negocios/" + a.negocio.getId() + "/reservas/" + reservaDeB.getId()))
                .andExpect(status().isNotFound());

        // Listar devuelve solo las del negocio A ordenadas por inicio (14:00Z antes que 15:00Z)
        mockMvc.perform(get("/negocios/" + a.negocio.getId() + "/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(segundaDeA.getId()))
                .andExpect(jsonPath("$[1].id").value(primeraDeA.getId()));

        // Con ?recursoId= devuelve únicamente las de ese recurso
        mockMvc.perform(get("/negocios/" + a.negocio.getId() + "/reservas")
                        .param("recursoId", a.recurso.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(primeraDeA.getId()));
    }

    private Escenario prepararEscenario() {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.saveAndFlush(new Servicio(
                negocio.getId(), "Corte", 60, new BigDecimal("25.00"), true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        return new Escenario(negocio, recurso, servicio);
    }

    private MockHttpServletRequestBuilder crearReserva(
            Negocio negocio, Servicio servicio, Recurso recurso, String inicio) {
        return post("/negocios/" + negocio.getId() + "/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "servicioId": %d,
                            "recursoId": %d,
                            "inicio": "%s"
                        }
                        """.formatted(servicio.getId(), recurso.getId(), inicio));
    }

    private record Escenario(Negocio negocio, Recurso recurso, Servicio servicio) {
    }

}
