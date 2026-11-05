package com.reservas.api.usuario;

import java.time.Instant;

// Nunca incluye la contraseña ni su hash
public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        Rol rol,
        Instant creadoEn
) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getCreadoEn());
    }

}
