package com.reservas.api.auth;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declara en Swagger cómo se autentica la API. Con esto aparece el botón «Authorize», donde se pega
 * el accessToken que devuelve /auth/login. Los endpoints que lo exijan lo indicarán con
 * {@code @SecurityRequirement(name = "bearerAuth")} cuando se protejan.
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token de acceso JWT. Se obtiene con POST /auth/login y se envía como "
                + "Authorization: Bearer <token>."
)
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";

}
