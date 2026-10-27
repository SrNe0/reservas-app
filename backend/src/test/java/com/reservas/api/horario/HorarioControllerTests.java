package com.reservas.api.horario;

import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class HorarioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private HorarioRepository horarios;

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "13:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/horarios/\\d+")))
                .andExpect(jsonPath("$.negocioId").value(negocio.getId()))
                .andExpect(jsonPath("$.diaSemana").value(1))
                .andExpect(jsonPath("$.horaApertura").value("09:00:00"))
                .andExpect(jsonPath("$.horaCierre").value("13:00:00"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());
    }

    @Test
    void crearEnUnNegocioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/negocios/999999/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "13:00"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearConCierreAnteriorOIgualAAperturaDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "13:00", "horaCierre": "09:00"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "09:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConDiaSemanaInvalidoDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 8, "horaApertura": "09:00", "horaCierre": "13:00"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarDevuelveSoloLosHorariosDelNegocio() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "UTC"));
        Negocio b = negocios.save(new Negocio("Cancha B", null, "UTC"));
        horarios.save(nuevo(a, 1, "09:00", "13:00"));
        horarios.save(nuevo(a, 2, "09:00", "13:00"));
        horarios.save(nuevo(b, 1, "09:00", "13:00"));

        mockMvc.perform(get("/negocios/" + a.getId() + "/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void permiteHorarioPartidoYTurnosAdyacentesElMismoDia() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        horarios.save(nuevo(negocio, 1, "09:00", "13:00"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "15:00", "horaCierre": "19:00"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "13:00", "horaCierre": "15:00"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void crearHorarioSolapadoEnElMismoDiaDevuelve409() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        horarios.save(nuevo(negocio, 1, "09:00", "13:00"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "12:00", "horaCierre": "16:00"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarUnHorarioNoChocaConsigoMismoPeroSiConOtro() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        Horario manana = horarios.save(nuevo(negocio, 1, "09:00", "13:00"));
        horarios.save(nuevo(negocio, 1, "15:00", "19:00"));

        mockMvc.perform(put("/negocios/" + negocio.getId() + "/horarios/" + manana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "14:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaCierre").value("14:00:00"));

        mockMvc.perform(put("/negocios/" + negocio.getId() + "/horarios/" + manana.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "09:00", "horaCierre": "16:00"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unHorarioDeOtroNegocioSeComportaComoSiNoExistiera() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "UTC"));
        Negocio b = negocios.save(new Negocio("Cancha B", null, "UTC"));
        Horario deB = horarios.save(nuevo(b, 1, "09:00", "13:00"));

        mockMvc.perform(get("/negocios/" + a.getId() + "/horarios/" + deB.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/negocios/" + a.getId() + "/horarios/" + deB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diaSemana": 1, "horaApertura": "10:00", "horaCierre": "12:00"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/negocios/" + a.getId() + "/horarios/" + deB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        Horario horario = horarios.save(nuevo(negocio, 1, "09:00", "13:00"));

        mockMvc.perform(delete("/negocios/" + negocio.getId() + "/horarios/" + horario.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/negocios/" + negocio.getId() + "/horarios/" + horario.getId()))
                .andExpect(status().isNotFound());
    }

    private Horario nuevo(Negocio negocio, int dia, String apertura, String cierre) {
        return new Horario(negocio.getId(), (short) dia, LocalTime.parse(apertura), LocalTime.parse(cierre));
    }

}
