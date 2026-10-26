package com.reservas.api.servicio;

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

import java.math.BigDecimal;

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
class ServicioControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private ServicioRepository servicios;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void crearDevuelve201ConLocationYQuedaActivoPorDefecto() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Corte", "duracionMin": 30, "precio": 12.50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/servicios/\\d+")))
                .andExpect(jsonPath("$.negocioId").value(negocio.getId()))
                .andExpect(jsonPath("$.precio").value(12.50))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());
    }

    @Test
    void crearEnUnNegocioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/negocios/999999/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Corte", "duracionMin": 30, "precio": 12.50}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearConDuracionOPrecioInvalidosDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));

        mockMvc.perform(post("/negocios/" + negocio.getId() + "/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Corte", "duracionMin": 0, "precio": -1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarDevuelveSoloLosServiciosDelNegocio() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "UTC"));
        Negocio b = negocios.save(new Negocio("Consultorio B", null, "UTC"));
        servicios.save(nuevo(a, "Corte"));
        servicios.save(nuevo(a, "Barba"));
        servicios.save(nuevo(b, "Consulta"));

        mockMvc.perform(get("/negocios/" + a.getId() + "/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unServicioDeOtroNegocioSeComportaComoSiNoExistiera() throws Exception {
        Negocio a = negocios.save(new Negocio("Barbería A", null, "UTC"));
        Negocio b = negocios.save(new Negocio("Consultorio B", null, "UTC"));
        Servicio deB = servicios.save(nuevo(b, "Consulta"));

        mockMvc.perform(get("/negocios/" + a.getId() + "/servicios/" + deB.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/negocios/" + a.getId() + "/servicios/" + deB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Hackeado", "duracionMin": 10, "precio": 1, "activo": true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/negocios/" + a.getId() + "/servicios/" + deB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarCambiaLosDatos() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        Servicio servicio = servicios.save(nuevo(negocio, "Corte"));

        mockMvc.perform(put("/negocios/" + negocio.getId() + "/servicios/" + servicio.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Corte y barba", "duracionMin": 45, "precio": 20, "activo": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Corte y barba"))
                .andExpect(jsonPath("$.duracionMin").value(45))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void actualizarSinActivoDevuelve400() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        Servicio servicio = servicios.save(nuevo(negocio, "Corte"));

        mockMvc.perform(put("/negocios/" + negocio.getId() + "/servicios/" + servicio.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Corte", "duracionMin": 30, "precio": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        Negocio negocio = negocios.save(new Negocio("Barbería", null, "UTC"));
        Servicio servicio = servicios.save(nuevo(negocio, "Corte"));

        mockMvc.perform(delete("/negocios/" + negocio.getId() + "/servicios/" + servicio.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/negocios/" + negocio.getId() + "/servicios/" + servicio.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnServicioConReservasDevuelve409() throws Exception {
        Negocio negocio = negocios.saveAndFlush(new Negocio("Barbería", null, "UTC"));
        Servicio servicio = servicios.saveAndFlush(nuevo(negocio, "Corte"));
        Long recursoId = jdbc.queryForObject(
                "INSERT INTO recurso (negocio_id, nombre) VALUES (?, 'Sillón 1') RETURNING id",
                Long.class, negocio.getId());
        jdbc.update("INSERT INTO reserva (negocio_id, servicio_id, recurso_id, inicio, fin, precio) "
                        + "VALUES (?, ?, ?, '2030-01-01 10:00+00', '2030-01-01 10:30+00', 10.00)",
                negocio.getId(), servicio.getId(), recursoId);

        mockMvc.perform(delete("/negocios/" + negocio.getId() + "/servicios/" + servicio.getId()))
                .andExpect(status().isConflict());
    }

    private Servicio nuevo(Negocio negocio, String nombre) {
        return new Servicio(negocio.getId(), nombre, 30, new BigDecimal("10.00"), true);
    }

}
