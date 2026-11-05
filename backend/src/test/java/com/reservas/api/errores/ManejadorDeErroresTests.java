package com.reservas.api.errores;

import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.horario.Horario;
import com.reservas.api.horario.HorarioRepository;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ManejadorDeErroresTests.ControladorDeFalloConfig.class})
@Transactional
class ManejadorDeErroresTests {

    @TestConfiguration
    static class ControladorDeFalloConfig {

        @Bean
        ControladorDeFallo controladorDeFallo() {
            return new ControladorDeFallo();
        }

    }

    @RestController
    static class ControladorDeFallo {

        @GetMapping("/pruebas/fallo-interno")
        void fallar() {
            throw new IllegalStateException("Error SQL: password=secreto en jdbc:postgresql://db:5432");
        }

    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private HorarioRepository horarios;

    @Test
    void noEncontradoDevuelveProblemDetailConElMensajeDelServicio() throws Exception {
        mockMvc.perform(get("/negocios/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value(containsString("No existe el negocio 999999")));
    }

    @Test
    void conflictoDevuelveProblemDetailConElMotivo() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "America/Bogota"));
        horarios.save(new Horario(negocio.getId(), (short) 1, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "diaSemana": 1,
                                    "horaApertura": "10:00",
                                    "horaCierre": "14:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("solapa")));
    }

    @Test
    void validacionConUnCampoInvalidoDevuelve400YListaDeErrores() throws Exception {
        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nombre": "",
                                    "zonaHoraria": "America/Bogota"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.length()").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
    }

    @Test
    void validacionConDosCamposInvalidosIncluyeAmbos() throws Exception {
        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nombre": "",
                                    "zonaHoraria": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("zonaHoraria")));
    }

    @Test
    void unCampoQueIncumpleDosReglasDevuelveUnElementoPorCadaRegla() throws Exception {
        String masDeCientoVeinteEspacios = " ".repeat(130);

        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nombre": "%s",
                                    "zonaHoraria": "America/Bogota"
                                }
                                """.formatted(masDeCientoVeinteEspacios)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[1].campo").value("nombre"));
    }

    @Test
    void excepcionNoControladaDevuelve500GenericoSinFiltrarDetallesSensibles() throws Exception {
        mockMvc.perform(get("/pruebas/fallo-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail").value("Ha ocurrido un error inesperado."))
                .andExpect(content().string(not(containsString("password=secreto"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void metodoHttpNoPermitidoSigueDevolviendo405YNo500() throws Exception {
        mockMvc.perform(put("/negocios"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

}
