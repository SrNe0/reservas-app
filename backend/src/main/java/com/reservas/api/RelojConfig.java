package com.reservas.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    // Inyectar el reloj (en vez de llamar a Instant.now()) permite fijar la hora en las pruebas
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

}
