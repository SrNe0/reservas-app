package com.reservas.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión",
            description = "Devuelve un access token JWT de vida corta para enviar en la cabecera "
                    + "Authorization: Bearer. Con un email inexistente o una contraseña incorrecta la "
                    + "respuesta es la misma (401), para no revelar qué cuentas existen.")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return servicio.login(datos);
    }

}
