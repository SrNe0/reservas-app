package com.reservas.api.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraerTokenTests {

    @Test
    void cabeceraBearerValidaDevuelveElTokenTalCual() {
        assertThat(JwtAuthenticationFilter.extraerToken("Bearer abc.def.ghi"))
                .contains("abc.def.ghi");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void cabeceraNulaOVaciaDevuelveVacio(String cabecera) {
        assertThat(JwtAuthenticationFilter.extraerToken(cabecera))
                .isEmpty();
    }

    @Test
    void esquemaDistintoDeBearerDevuelveVacio() {
        assertThat(JwtAuthenticationFilter.extraerToken("Basic abc123"))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer   "})
    void bearerSinTokenDevuelveVacio(String cabecera) {
        assertThat(JwtAuthenticationFilter.extraerToken(cabecera))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"bearer x", "BEARER x", "bEaReR x"})
    void elEsquemaNoDistingueMayusculasSegunRfc7235(String cabecera) {
        assertThat(JwtAuthenticationFilter.extraerToken(cabecera))
                .contains("x");
    }

    @Test
    void conservaLasMayusculasYMinusculasDelToken() {
        assertThat(JwtAuthenticationFilter.extraerToken("Bearer AbC"))
                .contains("AbC");
    }

    @Test
    void bearerPegadoAlTokenSinEspacioDevuelveVacio() {
        assertThat(JwtAuthenticationFilter.extraerToken("Bearerxyz"))
                .isEmpty();
    }

    @Test
    void rechazaTokenConEspaciosInternosPorqueBase64UrlNoAdmiteEspacios() {
        assertThat(JwtAuthenticationFilter.extraerToken("Bearer abc def"))
                .isEmpty();
    }

}
