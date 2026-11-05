package com.reservas.api.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt solo usa los primeros 72 bytes de la contraseña: más largas no aportan seguridad
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String nombre,
        // Si no se envía, la cuenta es de CLIENTE
        TipoDeCuenta tipoDeCuenta
) {

    public RegistroRequest {
        if (email != null) {
            email = email.trim();
        }
    }

}
