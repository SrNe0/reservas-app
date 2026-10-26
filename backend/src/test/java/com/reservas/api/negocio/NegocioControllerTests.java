package com.reservas.api.negocio;

import com.reservas.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
class NegocioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void crearDevuelve201ConLocationYElNegocio() throws Exception {
        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Barbería Central", "descripcion": "Cortes", "zonaHoraria": "America/Bogota"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/negocios/\\d+")))
                .andExpect(jsonPath("$.nombre").value("Barbería Central"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Bogota"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void crearConNombreEnBlancoDevuelve400() throws Exception {
        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": " ", "zonaHoraria": "UTC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConZonaHorariaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Cancha", "zonaHoraria": "UTC+3"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarDevuelveLosNegociosCreados() throws Exception {
        repositorio.save(new Negocio("Barbería", null, "UTC"));
        repositorio.save(new Negocio("Consultorio", null, "UTC"));

        mockMvc.perform(get("/negocios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void obtenerUnNegocioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/negocios/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarCambiaLosDatos() throws Exception {
        Negocio negocio = repositorio.save(new Negocio("Nombre viejo", null, "UTC"));

        mockMvc.perform(put("/negocios/" + negocio.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Nombre nuevo", "zonaHoraria": "America/Lima"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Lima"));
    }

    @Test
    void eliminarDevuelve204YDespuesElNegocioNoExiste() throws Exception {
        Negocio negocio = repositorio.save(new Negocio("Temporal", null, "UTC"));

        mockMvc.perform(delete("/negocios/" + negocio.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/negocios/" + negocio.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnNegocioConServiciosDevuelve409() throws Exception {
        Negocio negocio = repositorio.saveAndFlush(new Negocio("Con servicios", null, "UTC"));
        jdbc.update("INSERT INTO servicio (negocio_id, nombre, duracion_min, precio) VALUES (?, 'Corte', 30, 10.00)",
                negocio.getId());

        mockMvc.perform(delete("/negocios/" + negocio.getId()))
                .andExpect(status().isConflict());
    }

}
