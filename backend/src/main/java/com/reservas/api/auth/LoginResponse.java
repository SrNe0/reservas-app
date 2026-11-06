package com.reservas.api.auth;

/** Respuesta del login. {@code expiraEn} son los segundos de vida del access token. */
public record LoginResponse(String accessToken, String tipo, long expiraEn) {

    public static LoginResponse bearer(String accessToken, long expiraEn) {
        return new LoginResponse(accessToken, "Bearer", expiraEn);
    }

}
