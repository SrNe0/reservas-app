package com.reservas.api.auth;

import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.usuario.Rol;
import com.reservas.api.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El filtro con la cadena de seguridad real: lo que ve un controlador según la cabecera que llegue. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FiltroJwtTests.ControladorDePruebaConfig.class})
class FiltroJwtTests {

    @TestConfiguration
    static class ControladorDePruebaConfig {

        @Bean
        QuienSoy quienSoy() {
            return new QuienSoy();
        }

    }

    @RestController
    static class QuienSoy {

        @GetMapping("/pruebas/quien-soy")
        String quienSoy() {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a == null || !(a.getPrincipal() instanceof DatosDelToken datos)) {
                return "anonimo";
            }
            String roles = a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
            return datos.usuarioId() + ":" + roles;
        }

    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwt;

    @Value("${app.jwt.secret}")
    private String secreto;

    @Test
    void conUnTokenValidoElControladorVeAlUsuarioYSuRol() throws Exception {
        String token = jwt.emitir(usuario(7L, Rol.NEGOCIO));

        mockMvc.perform(get("/pruebas/quien-soy").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("7:ROLE_NEGOCIO"));
    }

    @Test
    void sinCabeceraLaPeticionEsAnonima() throws Exception {
        mockMvc.perform(get("/pruebas/quien-soy"))
                .andExpect(status().isOk())
                .andExpect(content().string("anonimo"));
    }

    @Test
    void conUnTokenManipuladoLaPeticionSigueComoAnonima() throws Exception {
        String token = jwt.emitir(usuario(7L, Rol.CLIENTE));

        mockMvc.perform(get("/pruebas/quien-soy").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isOk())
                .andExpect(content().string("anonimo"));
    }

    @Test
    void conUnTokenCaducadoLaPeticionSigueComoAnonima() throws Exception {
        Clock haceUnaHora = Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC);
        String caducado = new JwtService(secreto, 15, haceUnaHora).emitir(usuario(7L, Rol.ADMIN));

        mockMvc.perform(get("/pruebas/quien-soy").header("Authorization", "Bearer " + caducado))
                .andExpect(status().isOk())
                .andExpect(content().string("anonimo"));
    }

    @Test
    void unEsquemaDistintoDeBearerSeIgnora() throws Exception {
        String token = jwt.emitir(usuario(7L, Rol.CLIENTE));

        mockMvc.perform(get("/pruebas/quien-soy").header("Authorization", "Basic " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("anonimo"));
    }

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario("ana@ejemplo.com", "hash", "Ana", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

}
