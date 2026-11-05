package com.reservas.api.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {

    /**
     * Por ahora deja pasar todas las peticiones: el filtro identifica al usuario pero ningún endpoint
     * lo exige todavía. Cada ruta se irá cerrando por rol más adelante (#32).
     */
    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http, JwtService jwt) throws Exception {
        return http
                // Sin sesión en el servidor: cada petición trae su token
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF protege sesiones con cookies; con tokens en una cabecera no aplica
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
                // No es un @Component a propósito: así Spring no lo registra además como filtro suelto
                .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
