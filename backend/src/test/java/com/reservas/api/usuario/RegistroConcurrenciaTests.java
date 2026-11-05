package com.reservas.api.usuario;

import com.reservas.api.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Sin @Transactional en la clase: cada hilo necesita su propia transacción real.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistroConcurrenciaTests {

    private static final int HILOS = 10;
    private static final String EMAIL_CONCURRENTE = "concurrente@ejemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM usuario WHERE lower(email) = ?", EMAIL_CONCURRENTE);
    }

    @Test
    void diezRegistrosSimultaneosDelMismoEmailCreanUnSoloUsuario() throws Exception {
        String cuerpo = """
                {
                    "email": "%s",
                    "password": "passwordSegura123",
                    "nombre": "Ana Concurrente"
                }
                """.formatted(EMAIL_CONCURRENTE);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>(HILOS);

        try {
            for (int i = 0; i < HILOS; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return mockMvc.perform(post("/auth/registro")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(cuerpo))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            listos.await();
            salida.countDown();

            List<Integer> codigos = new ArrayList<>(HILOS);
            for (Future<Integer> futuro : futuros) {
                codigos.add(futuro.get());
            }

            long creados = codigos.stream().filter(c -> c == 201).count();
            long conflictos = codigos.stream().filter(c -> c == 409).count();

            Integer filasEnBase = jdbc.queryForObject(
                    "SELECT count(*) FROM usuario WHERE lower(email) = ?",
                    Integer.class, EMAIL_CONCURRENTE);

            assertThat(filasEnBase).isEqualTo(1);
            assertThat(creados).isEqualTo(1);
            assertThat(conflictos).isEqualTo(HILOS - 1);
        } finally {
            pool.shutdown();
        }
    }

}
