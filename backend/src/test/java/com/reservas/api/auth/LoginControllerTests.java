package com.reservas.api.auth;

import com.jayway.jsonpath.JsonPath;
import com.reservas.api.TestcontainersConfiguration;
import com.reservas.api.usuario.Rol;
import com.reservas.api.usuario.Usuario;
import com.reservas.api.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class LoginControllerTests {

    private static final String PASSWORD = "miPasswordSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JwtService jwt;

    // Espía sobre el codificador real: deja ver si se llamó a matches sin cambiar su comportamiento
    @MockitoSpyBean
    private PasswordEncoder passwordEncoder;

    private Usuario ana;

    @BeforeEach
    void crearUsuario() {
        ana = usuarios.save(new Usuario("ana@ejemplo.com", passwordEncoder.encode(PASSWORD), "Ana", Rol.NEGOCIO));
    }

    @Test
    void credencialesCorrectasDevuelvenUnTokenQueIdentificaAlUsuario() throws Exception {
        MvcResult resultado = login("ana@ejemplo.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(15 * 60))
                .andExpect(content().string(not(containsString("$2"))))
                .andReturn();

        String token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");

        assertThat(jwt.validar(token)).isEqualTo(new DatosDelToken(ana.getId(), Rol.NEGOCIO));
    }

    @Test
    void elEmailSeAceptaSinImportarMayusculasNiEspacios() throws Exception {
        login("  ANA@Ejemplo.COM ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteDanLaMismaRespuesta401() throws Exception {
        String contrasenaMala = login("ana@ejemplo.com", "otraPassword123")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Credenciales incorrectas"))
                .andReturn().getResponse().getContentAsString();

        String emailInexistente = login("nadie@ejemplo.com", "otraPassword123")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Idénticas byte a byte: nada distingue "no existe" de "clave mala"
        assertThat(emailInexistente).isEqualTo(contrasenaMala);
    }

    @Test
    void conUnEmailInexistenteIgualSeEjecutaBCrypt() throws Exception {
        login("nadie@ejemplo.com", "claveCualquiera123").andExpect(status().isUnauthorized());

        // Si no se llamara a matches, la respuesta sería más rápida y delataría que la cuenta no existe
        verify(passwordEncoder).matches(eq("claveCualquiera123"), anyString());
    }

    @Test
    void camposVaciosDevuelven400() throws Exception {
        login("", PASSWORD).andExpect(status().isBadRequest());
        login("ana@ejemplo.com", "   ").andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

}
