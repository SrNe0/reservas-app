package com.reservas.api.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación")
public class RegistroController {

    private final UsuarioService servicio;

    public RegistroController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario",
            description = "La cuenta es de CLIENTE salvo que se pida NEGOCIO; ADMIN no se puede elegir. "
                    + "La contraseña (de 8 a 72 caracteres) se guarda con BCrypt y nunca se devuelve. "
                    + "409 si el email ya está registrado, sin distinguir mayúsculas.")
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest datos) {
        return servicio.registrar(datos);
    }

}
