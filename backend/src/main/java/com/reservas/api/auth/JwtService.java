package com.reservas.api.auth;

import com.reservas.api.usuario.Rol;
import com.reservas.api.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final int BYTES_MINIMOS_CLAVE = 32;

    private final SecretKey clave;
    private final Duration vida;
    private final Clock reloj;
    private final JwtParser parser;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos,
                      Clock reloj) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_CLAVE) {
            // Mejor no arrancar que firmar con una clave débil
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + BYTES_MINIMOS_CLAVE + " bytes");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vida = Duration.ofMinutes(expiracionMinutos);
        this.reloj = reloj;
        // El parser usa nuestro reloj para decidir si el token caducó (así se puede probar con un reloj fijo)
        this.parser = Jwts.parser()
                .verifyWith(clave)
                .clock(() -> Date.from(reloj.instant()))
                .build();
    }

    public long segundosDeVida() {
        return vida.toSeconds();
    }

    public String emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vida)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public DatosDelToken validar(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Long usuarioId = Long.valueOf(claims.getSubject());
            Rol rol = Rol.valueOf(claims.get(CLAIM_ROL, String.class));
            return new DatosDelToken(usuarioId, rol);
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException cubre token nulo/vacío, sub no numérico y rol desconocido
            throw new TokenInvalidoException("Token JWT inválido o caducado", e);
        }
    }

}
