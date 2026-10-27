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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El domingo 2027-03-14 Nueva York adelanta el reloj: a las 02:00 pasan a ser las 03:00
// (las 02:00-02:59 no existen). Antes del cambio rige UTC-5 y después UTC-4.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DisponibilidadCambioDeHoraTests.RelojFijo.class})
@Transactional
class DisponibilidadCambioDeHoraTests {

    private static final short DOMINGO = 7;
    private static final String DIA_DEL_CAMBIO = "2027-03-14";

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse("2027-03-10T12:00:00Z"), ZoneOffset.UTC);
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

    @Test
    void unHorarioQueDesapareceEnElSaltoDeHoraSeIgnoraYElRestoSigueFuncionando() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería NY", null, "America/New_York"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(
                new Servicio(negocio.getId(), "Corte", 60, new BigDecimal("10.00"), true));
        // 02:00-03:00 no existe ese día (queda de duración cero); 09:00-10:00 es normal
        horarios.save(new Horario(negocio.getId(), DOMINGO, LocalTime.of(2, 0), LocalTime.of(3, 0)));
        horarios.save(new Horario(negocio.getId(), DOMINGO, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        // 09:00 en UTC-4 son las 13:00 UTC
        mockMvc.perform(consulta(negocio, recurso, servicio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(1))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2027-03-14T13:00:00Z"));
    }

    @Test
    void unHorarioQueAtraviesaElSaltoDuraUnaHoraMenos() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería NY", null, "America/New_York"));
        Recurso recurso = recursos.save(new Recurso(negocio.getId(), "Sillón 1", true));
        Servicio servicio = servicios.save(
                new Servicio(negocio.getId(), "Corte", 60, new BigDecimal("10.00"), true));
        // De 01:00 (UTC-5, 06:00 UTC) a 04:00 (UTC-4, 08:00 UTC) pasan solo 2 horas reales
        horarios.save(new Horario(negocio.getId(), DOMINGO, LocalTime.of(1, 0), LocalTime.of(4, 0)));

        // Inicios posibles cada 15 min: de 06:00 a 07:00 UTC, es decir 5 turnos
        mockMvc.perform(consulta(negocio, recurso, servicio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos.length()").value(5))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2027-03-14T06:00:00Z"))
                .andExpect(jsonPath("$.turnos[4].fin").value("2027-03-14T08:00:00Z"));
    }

    private MockHttpServletRequestBuilder consulta(
            Negocio negocio, Recurso recurso, Servicio servicio) {
        return get("/negocios/" + negocio.getId() + "/recursos/" + recurso.getId() + "/disponibilidad")
                .param("servicioId", servicio.getId().toString())
                .param("fecha", DIA_DEL_CAMBIO);
    }

}
