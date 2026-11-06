package com.reservas.api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lee {@code Authorization: Bearer <token>} y, si el token es bueno, deja al usuario identificado
 * en el contexto de seguridad. Si falta o no vale, no hace nada: decidir qué hacer con una petición
 * anónima le toca a la configuración de cada endpoint, no al filtro.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        extraerToken(request.getHeader(HttpHeaders.AUTHORIZATION)).ifPresent(token -> {
            try {
                DatosDelToken datos = jwt.validar(token);
                var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                        datos, null, List.of(new SimpleGrantedAuthority("ROLE_" + datos.rol().name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (TokenInvalidoException e) {
                // Token malo: la petición sigue como anónima
                SecurityContextHolder.clearContext();
            }
        });
        cadena.doFilter(request, response);
    }

    static Optional<String> extraerToken(String cabecera) {
        if (cabecera == null || !cabecera.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            return Optional.empty();
        }
        String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
        if (token.isEmpty() || token.indexOf(' ') >= 0) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

}
