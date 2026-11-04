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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hoy, para estas pruebas, es domingo 2026-11-01 a las 11:00 en Bogotá (16:00 UTC).
// El lunes 2026-11-02 a las 09:00 locales equivale a 2026-11-02T14:00:00Z.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ReservaCancelacionTests.RelojFijo.class})
@Transactional
class ReservaCancelacionTests {

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

    @Test
    void cancelarConMasDeDosHorasDeAntelacionDevuelve200YLiberaElTurno() throws Exception {
        Escenario e = prepararEscenario();
        Reserva reserva = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(cancelar(e.negocio, reserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reserva.getId()))
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(post("/negocios/" + e.negocio.getId() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "servicioId": %d,
                                    "recursoId": %d,
                                    "inicio": "2026-11-02T14:00:00Z"
                                }
                                """.formatted(e.servicio.getId(), e.recurso.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelarEnLaFronteraDeLasDosHorasPermiteExactoYRechazaUnSegundoAntes() throws Exception {
        Escenario e = prepararEscenario();
        // Reloj en 16:00:00Z -> 18:00:00Z son exactamente 2 h; 17:59:59Z es 1 segundo menos
        Reserva exacta = guardarReserva(e, "2026-11-01T18:00:00Z", "2026-11-01T19:00:00Z");

        mockMvc.perform(cancelar(e.negocio, exacta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        reservas.flush();

        Reserva unSegundoAntes = guardarReserva(e, "2026-11-01T17:59:59Z", "2026-11-01T18:59:59Z");
        mockMvc.perform(cancelar(e.negocio, unSegundoAntes))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarUnaReservaQueYaEmpezoOPasoDevuelve409() throws Exception {
        Escenario e = prepararEscenario();
        Reserva enCurso = guardarReserva(e, "2026-11-01T16:00:00Z", "2026-11-01T17:00:00Z");
        Reserva pasada = guardarReserva(e, "2026-11-01T14:00:00Z", "2026-11-01T15:00:00Z");

        mockMvc.perform(cancelar(e.negocio, enCurso))
                .andExpect(status().isConflict());
        mockMvc.perform(cancelar(e.negocio, pasada))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarUnaReservaYaCanceladaDevuelve409() throws Exception {
        Escenario e = prepararEscenario();
        Reserva reserva = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(cancelar(e.negocio, reserva))
                .andExpect(status().isOk());
        mockMvc.perform(cancelar(e.negocio, reserva))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarUnaReservaDeOtroNegocioDevuelve404() throws Exception {
        Escenario a = prepararEscenario();
        Negocio b = negocios.save(new Negocio("Cancha B", null, "America/Bogota"));
        Reserva reservaDeA = guardarReserva(a, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(cancelar(b, reservaDeA))
                .andExpect(status().isNotFound());
    }

    @Test
    void reprogramarAOtroTurnoLibreDelMismoRecursoActualizaInicioYFin() throws Exception {
        Escenario e = prepararEscenario();
        Reserva reserva = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(reprogramar(e.negocio, reserva, null, "2026-11-02T16:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reserva.getId()))
                .andExpect(jsonPath("$.recursoId").value(e.recurso.getId()))
                .andExpect(jsonPath("$.inicio").value("2026-11-02T16:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-11-02T17:00:00Z"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void reprogramarTreintaMinutosNoChocaConsigoMisma() throws Exception {
        Escenario e = prepararEscenario();
        Reserva reserva = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(reprogramar(e.negocio, reserva, null, "2026-11-02T14:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-11-02T14:30:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-11-02T15:30:00Z"));
    }

    @Test
    void reprogramarAOtroRecursoCambiaElRecursoId() throws Exception {
        Escenario e = prepararEscenario();
        Recurso otroRecurso = recursos.save(new Recurso(e.negocio.getId(), "Sillón 2", true));
        Reserva reserva = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(reprogramar(e.negocio, reserva, otroRecurso.getId(), "2026-11-02T14:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursoId").value(otroRecurso.getId()))
                .andExpect(jsonPath("$.inicio").value("2026-11-02T14:00:00Z"));
    }

    @Test
    void reprogramarATurnoOcupadoDevuelve409YATurnoInvalidoDevuelve400() throws Exception {
        Escenario e = prepararEscenario();
        Reserva primera = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");
        guardarReserva(e, "2026-11-02T15:00:00Z", "2026-11-02T16:00:00Z");

        mockMvc.perform(reprogramar(e.negocio, primera, null, "2026-11-02T15:00:00Z"))
                .andExpect(status().isConflict());
        mockMvc.perform(reprogramar(e.negocio, primera, null, "2026-11-02T14:07:00Z"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(reprogramar(e.negocio, primera, null, "2026-11-02T20:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reprogramarReservaAMenosDeDosHorasOYaCanceladaDevuelve409() throws Exception {
        Escenario e = prepararEscenario();
        Reserva proxima = guardarReserva(e, "2026-11-01T17:30:00Z", "2026-11-01T18:30:00Z");
        Reserva cancelada = guardarReserva(e, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");
        cancelada.cancelar();
        reservas.saveAndFlush(cancelada);

        mockMvc.perform(reprogramar(e.negocio, proxima, null, "2026-11-02T15:00:00Z"))
                .andExpect(status().isConflict());
        mockMvc.perform(reprogramar(e.negocio, cancelada, null, "2026-11-02T15:00:00Z"))
                .andExpect(status().isConflict());
    }

    @Test
    void reprogramarARecursoDeOtroNegocioDevuelve404() throws Exception {
        Escenario a = prepararEscenario();
        Negocio b = negocios.save(new Negocio("Cancha B", null, "America/Bogota"));
        Recurso recursoDeB = recursos.save(new Recurso(b.getId(), "Cancha 1", true));
        Reserva reservaDeA = guardarReserva(a, "2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z");

        mockMvc.perform(reprogramar(a.negocio, reservaDeA, recursoDeB.getId(), "2026-11-02T15:00:00Z"))
                .andExpect(status().isNotFound());
    }

    private Escenario prepararEscenario() {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "America/Bogota"));
        Recurso recurso = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.saveAndFlush(new Servicio(
                negocio.getId(), "Corte", 60, new BigDecimal("25.00"), true));
        horarios.save(new Horario(negocio.getId(), LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        return new Escenario(negocio, recurso, servicio);
    }

    private Reserva guardarReserva(Escenario e, String inicio, String fin) {
        return reservas.saveAndFlush(new Reserva(
                e.negocio.getId(), e.servicio.getId(), e.recurso.getId(),
                Instant.parse(inicio), Instant.parse(fin), e.servicio.getPrecio()));
    }

    private MockHttpServletRequestBuilder cancelar(Negocio negocio, Reserva reserva) {
        return post("/negocios/" + negocio.getId() + "/reservas/" + reserva.getId() + "/cancelar");
    }

    private MockHttpServletRequestBuilder reprogramar(
            Negocio negocio, Reserva reserva, Long recursoId, String inicio) {
        String cuerpo = recursoId == null
                ? """
                {
                    "inicio": "%s"
                }
                """.formatted(inicio)
                : """
                {
                    "recursoId": %d,
                    "inicio": "%s"
                }
                """.formatted(recursoId, inicio);
        return post("/negocios/" + negocio.getId() + "/reservas/" + reserva.getId() + "/reprogramar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private record Escenario(Negocio negocio, Recurso recurso, Servicio servicio) {
    }

}
