package com.reservas.api.recurso;

import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
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
class RecursoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void crearDevuelve201ConLocationYQuedaActivoPorDefecto() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/recursos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nombre": "Sillón 1"}
            """)).andExpect(status().isCreated()).andExpect(header().string("Location", matchesPattern(".*/recursos/\\d+"))).andExpect(jsonPath("$.negocioId").value(negocio.getId())).andExpect(jsonPath("$.nombre").value("Sillón 1")).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void unRecursoDeOtroNegocioSeComportaComoSiNoExistiera() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "UTC"));
        Negocio b = negocios.save(new Negocio("Cancha B", null, "UTC"));
        Recurso deB = recursos.save(new Recurso(b.getId(), "Cancha Norte", true));

        // Intentamos leer, editar y borrar el recurso de B usando la URL del negocio A -> todos deben dar 404
        mockMvc.perform(get("/negocios/" + a.getId() + "/recursos/" + deB.getId())).andExpect(status().isNotFound());
        mockMvc.perform(put("/negocios/" + a.getId() + "/recursos/" + deB.getId()).contentType(MediaType.APPLICATION_JSON).content("""
            {"nombre": "Invadido"}
            """)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/negocios/" + a.getId() + "/recursos/" + deB.getId())).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnRecursoConReservasDevuelve409() throws Exception {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "UTC"));
        Recurso recurso = recursos.saveAndFlush(new Recurso(negocio.getId(), "Sillón 1", true));
        Long servicioId = jdbc.queryForObject("INSERT INTO servicio (negocio_id, nombre, duracion_min, precio) VALUES (?, 'Corte', 30, 10.00) RETURNING id", Long.class, negocio.getId());
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio) " + "VALUES (?, ?, ?, '2030-01-01 10:00+00', '2030-01-01 10:30+00', 10.00)", negocio.getId(), servicioId, recurso.getId());

        mockMvc.perform(delete("/negocios/" + negocio.getId() + "/recursos/" + recurso.getId())).andExpect(status().isConflict());
    }

    @Test
    void crearConNombreEnBlancoDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/recursos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nombre": " "}
            """)).andExpect(status().isBadRequest());
    }

    @Test
    void crearEnUnNegocioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/negocios/999999/recursos").contentType(MediaType.APPLICATION_JSON).content("""
            {"nombre": "Sillón 1"}
            """)).andExpect(status().isNotFound());
    }

}
