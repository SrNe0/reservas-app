package com.reservas.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        // Mismo tope que en el registro: BCrypt no lee más de 72 bytes
        @NotBlank @Size(max = 72) String password
) {

    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }

}
