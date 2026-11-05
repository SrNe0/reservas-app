package com.reservas.api.auth;

import com.reservas.api.usuario.Rol;
import com.reservas.api.usuario.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba unitaria pura: sin Spring ni base de datos, con reloj fijo. */
class JwtServiceTests {

    private static final String SECRETO = "una-clave-de-prueba-de-mas-de-treinta-y-dos-bytes-1234";
    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final JwtService servicio = new JwtService(SECRETO, 15, reloj);

    @Test
    void elTokenEmitidoTieneTresPartesYLosDatosDelUsuarioEnElPayload() {
        String token = servicio.emitir(usuario(7L, Rol.NEGOCIO));

        String[] partes = token.split("[.]");
        assertThat(partes).hasSize(3);

        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertThat(payload)
                .contains("\"sub\":\"7\"")
                .contains("\"rol\":\"NEGOCIO\"")
                .contains("\"iat\":" + AHORA.getEpochSecond())
                .contains("\"exp\":" + AHORA.plusSeconds(15 * 60).getEpochSecond())
                .doesNotContain("password");
    }

    @Test
    void unaClaveCortaImpideArrancar() {
        assertThatThrownBy(() -> new JwtService("corta", 15, reloj))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void tokenValidoDevuelveElMismoIdYRolConLosQueSeEmitio() {
        String token = servicio.emitir(usuario(42L, Rol.NEGOCIO));

        DatosDelToken datos = servicio.validar(token);

        assertThat(datos).isEqualTo(new DatosDelToken(42L, Rol.NEGOCIO));
    }

    @Test
    void tokenCaducadoLanzaTokenInvalidoExceptionPorExpiracion() {
        String token = servicio.emitir(usuario(7L, Rol.CLIENTE));
        Clock relojDieciseisMinutosDespues = Clock.fixed(AHORA.plus(Duration.ofMinutes(16)), ZoneOffset.UTC);
        JwtService servicioEnElFuturo = new JwtService(SECRETO, 15, relojDieciseisMinutosDespues);

        assertThatThrownBy(() -> servicioEnElFuturo.validar(token))
                .isInstanceOf(TokenInvalidoException.class)
                .hasCauseInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenConPayloadManipuladoLanzaTokenInvalidoExceptionPorFirma() {
        String token = servicio.emitir(usuario(7L, Rol.CLIENTE));
        String[] partes = token.split("[.]");
        String payloadOriginal = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadManipulado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payloadOriginal.replace("CLIENTE", "ADMIN").getBytes(StandardCharsets.UTF_8));
        String tokenManipulado = partes[0] + "." + payloadManipulado + "." + partes[2];

        assertThatThrownBy(() -> servicio.validar(tokenManipulado))
                .isInstanceOf(TokenInvalidoException.class)
                .hasCauseInstanceOf(SignatureException.class);
    }

    @Test
    void tokenFirmadoConOtraClaveLanzaTokenInvalidoException() {
        String otraClave = "otra-clave-distinta-de-mas-de-treinta-y-dos-bytes-9999";
        JwtService otroServicio = new JwtService(otraClave, 15, reloj);
        String tokenDeOtro = otroServicio.emitir(usuario(7L, Rol.CLIENTE));

        assertThatThrownBy(() -> servicio.validar(tokenDeOtro))
                .isInstanceOf(TokenInvalidoException.class)
                .hasCauseInstanceOf(SignatureException.class);
    }

    @Test
    void tokenMalFormadoVacioONuloLanzaTokenInvalidoException() {
        assertThatThrownBy(() -> servicio.validar("no.es.un.jwt"))
                .isInstanceOf(TokenInvalidoException.class);
        assertThatThrownBy(() -> servicio.validar("basura"))
                .isInstanceOf(TokenInvalidoException.class);
        assertThatThrownBy(() -> servicio.validar(""))
                .isInstanceOf(TokenInvalidoException.class);
        assertThatThrownBy(() -> servicio.validar("   "))
                .isInstanceOf(TokenInvalidoException.class);
        assertThatThrownBy(() -> servicio.validar(null))
                .isInstanceOf(TokenInvalidoException.class);
    }

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario("ana@ejemplo.com", "hash", "Ana", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

}
