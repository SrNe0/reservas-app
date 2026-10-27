package com.reservas.api.disponibilidad;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hoy, para estas pruebas, es domingo 2026-11-01 a las 11:00 en Bogotá (16:00 UTC).
// Bogotá es UTC-5 todo el año, así que 09:00 locales son las 14:00 UTC.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DisponibilidadControllerTests.RelojFijo.class})
@Transactional
class DisponibilidadControllerTests {

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
    private JdbcTemplate jdbc;

    @Test
    void horarioPartidoDevuelveTurnosDeAmbosTramos() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(15, 0), LocalTime.of(19, 0)));

        // De 09:00 a 13:00 con turnos de 60 min cada 15 min hay 13 inicios posibles (09:00 ... 12:00)
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaHoraria").value("America/Bogota"))
                .andExpect(jsonPath("$.duracionMin").value(60))
                .andExpect(jsonPath("$.turnos.length()").value(26))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-11-02T14:00:00Z"))
                .andExpect(jsonPath("$.turnos[0].fin").value("2026-11-02T15:00:00Z"))
                .andExpect(jsonPath("$.turnos[13].inicio").value("2026-11-02T20:00:00Z"));
    }

    @Test
    void usaLaZonaHorariaDelNegocio() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Peluquería", null, "Europe/Madrid"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Silla 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        // Madrid es UTC+1 en noviembre: las 09:00 locales son las 08:00 UTC
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(1))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-11-02T08:00:00Z"));
    }

    @Test
    void unaReservaConfirmadaOcupaSusTurnos() throws Exception {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.saveAndFlush(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        // Reserva de 10:00 a 11:00 locales (15:00 a 16:00 UTC)
        insertarReserva(negocio, servicio, recurso, "2026-11-02T15:00:00Z", "2026-11-02T16:00:00Z", "CONFIRMADA");

        // Quedan 09:00 (termina justo cuando empieza la reserva) y de 11:00 a 12:00 cada 15 min
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(6))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-11-02T14:00:00Z"))
                .andExpect(jsonPath("$.turnos[1].inicio").value("2026-11-02T16:00:00Z"));
    }

    @Test
    void unaReservaCanceladaNoOcupaTurnos() throws Exception {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.saveAndFlush(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        insertarReserva(negocio, servicio, recurso, "2026-11-02T15:00:00Z", "2026-11-02T16:00:00Z", "CANCELADA");

        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(13));
    }

    @Test
    void laReservaDeOtroRecursoNoAfecta() throws Exception {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "America/Bogota"));
        Recurso uno = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Recurso dos = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 2", true));
        Servicio servicio = servicios.saveAndFlush(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        insertarReserva(negocio, servicio, uno, "2026-11-02T15:00:00Z", "2026-11-02T16:00:00Z", "CONFIRMADA");

        mockMvc.perform(consulta(negocio, dos, servicio, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(13));
    }

    @Test
    void noOfreceTurnosQueYaEmpezaron() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), DOMINGO, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        // Ahora son las 11:00 locales: el primer turno posible es justo las 11:00 y el último las 12:00
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(5))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-11-01T16:00:00Z"));
    }

    @Test
    void unDiaSinHorarioDevuelveListaVacia() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        // 2026-11-03 es martes y solo hay horario los lunes
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(0));
    }

    @Test
    void unRecursoOUnServicioInactivoDevuelveListaVacia() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso inactivo = recursos.save(new Recurso(negocio.getId(), "Sillón roto", false));
        Recurso activo = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicioActivo = servicios.save(servicio(negocio, 60, true));
        Servicio servicioInactivo = servicios.save(servicio(negocio, 60, false));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        mockMvc.perform(consulta(negocio, inactivo, servicioActivo, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(0));
        mockMvc.perform(consulta(negocio, activo, servicioInactivo, "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(0));
    }

    @Test
    void unRecursoOUnServicioDeOtroNegocioDevuelve404() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "America/Bogota"));
        Negocio b = negocios.save(new Negocio("Cancha B", null, "America/Bogota"));
        Recurso recursoDeA = recursos.save(new Recurso(a.getId(), "Sillón 1", true));
        Servicio servicioDeA = servicios.save(servicio(a, 60, true));
        Recurso recursoDeB = recursos.save(new Recurso(b.getId(), "Cancha Norte", true));
        Servicio servicioDeB = servicios.save(servicio(b, 60, true));

        mockMvc.perform(get("/negocios/" + a.getId() + "/recursos/" + recursoDeB.getId() + "/disponibilidad")
                        .param("servicioId", servicioDeA.getId().toString())
                        .param("fecha", "2026-11-02"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/negocios/" + a.getId() + "/recursos/" + recursoDeA.getId() + "/disponibilidad")
                        .param("servicioId", servicioDeB.getId().toString())
                        .param("fecha", "2026-11-02"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/negocios/999999/recursos/" + recursoDeA.getId() + "/disponibilidad")
                        .param("servicioId", servicioDeA.getId().toString())
                        .param("fecha", "2026-11-02"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaFechasPasadasYDemasiadoLejanas() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));

        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-10-31"))
                .andExpect(status().isBadRequest());
        // Hoy es 2026-11-01: 60 días después (2026-12-31) vale y 61 no
        mockMvc.perform(consulta(negocio, recurso, servicio, "2026-12-31"))
                .andExpect(status().isOk());
        mockMvc.perform(consulta(negocio, recurso, servicio, "2027-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faltarUnParametroOEnviarUnaFechaMalFormadaDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(servicio(negocio, 60, true));

        mockMvc.perform(get("/negocios/" + negocio.getId() + "/recursos/" + recurso.getId() + "/disponibilidad")
                        .param("fecha", "2026-11-02"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(consulta(negocio, recurso, servicio, "02/11/2026"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder consulta(
            Negocio negocio, Recurso recurso, Servicio servicio, String fecha) {
        return get("/negocios/" + negocio.getId() + "/recursos/" + recurso.getId() + "/disponibilidad")
                .param("servicioId", servicio.getId().toString())
                .param("fecha", fecha);
    }

    private Servicio servicio(Negocio negocio, int duracionMin, boolean activo) {
        return new Servicio(negocio.getId(), "Servicio de " + duracionMin + " min", duracionMin,
                new BigDecimal("10.00"), activo);
    }

    private void insertarReserva(Negocio negocio, Servicio servicio, Recurso recurso,
                                 String inicio, String fin, String estado) {
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio, estado) "
                        + "VALUES (?, ?, ?, ?, ?, 10.00, ?)",
                negocio.getId(), servicio.getId(), recurso.getId(),
                OffsetDateTime.parse(inicio), OffsetDateTime.parse(fin), estado);
    }

}
