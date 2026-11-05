package com.reservas.api.usuario;

import com.reservas.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class RegistroControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registroValidoDevuelve201SinExponerPasswordNiHash() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "miPasswordSegura123",
                                    "nombre": "Ana Pérez"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ana@ejemplo.com"))
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("miPasswordSegura123"))))
                .andExpect(content().string(not(containsString("$2"))));
    }

    @Test
    void guardaLaContrasenaHasheadaConBCryptYNoEnClaro() throws Exception {
        String password = "miPasswordSegura123";

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "%s",
                                    "nombre": "Ana Pérez"
                                }
                                """.formatted(password)))
                .andExpect(status().isCreated());

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE email = ?",
                String.class, "ana@ejemplo.com");

        assertThat(hash)
                .isNotNull()
                .isNotEqualTo(password)
                .startsWith("$2");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
    }

    @Test
    void dosUsuariosConLaMismaContrasenaTienenHashesDistintosPorLaSal() throws Exception {
        String password = "mismaPassword123";

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "uno@ejemplo.com",
                                    "password": "%s",
                                    "nombre": "Usuario Uno"
                                }
                                """.formatted(password)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "dos@ejemplo.com",
                                    "password": "%s",
                                    "nombre": "Usuario Dos"
                                }
                                """.formatted(password)))
                .andExpect(status().isCreated());

        String hashUno = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE email = 'uno@ejemplo.com'", String.class);
        String hashDos = jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE email = 'dos@ejemplo.com'", String.class);

        assertThat(hashUno).isNotEqualTo(hashDos);
        assertThat(passwordEncoder.matches(password, hashUno)).isTrue();
        assertThat(passwordEncoder.matches(password, hashDos)).isTrue();
    }

    @Test
    void sinTipoDeCuentaAsignaClienteYConNegocioAsignaNegocio() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "cliente@ejemplo.com",
                                    "password": "password123",
                                    "nombre": "Cliente"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "dueno@ejemplo.com",
                                    "password": "password123",
                                    "nombre": "Dueño",
                                    "tipoDeCuenta": "NEGOCIO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("NEGOCIO"));
    }

    @Test
    void pedirRolAdminEnElRegistroDevuelve400YNoCreaUsuario() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@ejemplo.com",
                                    "password": "password123",
                                    "nombre": "Intento Admin",
                                    "tipoDeCuenta": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(usuarios.count()).isZero();
    }

    @Test
    void normalizaElEmailEnMinusculasYSinEspacios() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "  Ana@Correo.COM  ",
                                    "password": "password123",
                                    "nombre": "Ana"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@correo.com"));

        String emailEnBase = jdbc.queryForObject(
                "SELECT email FROM usuario", String.class);
        assertThat(emailEnBase).isEqualTo("ana@correo.com");
    }

    @Test
    void emailRepetidoInclusoConDistintasMayusculasDevuelve409() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "password123",
                                    "nombre": "Ana"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ANA@Ejemplo.COM",
                                    "password": "otraPassword123",
                                    "nombre": "Otra Ana"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void validacionRechazaDatosInvalidosSinFiltrarLaContrasena() throws Exception {
        String passwordCorta = "Clave7!";
        String passwordLarga = "A".repeat(73);

        // Email mal formado
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "no-es-un-email",
                                    "password": "passwordValida123",
                                    "nombre": "Ana"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Contraseña menor de 8 caracteres
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "%s",
                                    "nombre": "Ana"
                                }
                                """.formatted(passwordCorta)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(passwordCorta))));

        // Contraseña mayor de 72 caracteres
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "%s",
                                    "nombre": "Ana"
                                }
                                """.formatted(passwordLarga)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(passwordLarga))));

        // Nombre vacío
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ana@ejemplo.com",
                                    "password": "passwordValida123",
                                    "nombre": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

}
