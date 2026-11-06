package com.reservas.api.auth;

import com.reservas.api.usuario.Rol;

/** Lo que el servidor saca de un token ya verificado. */
public record DatosDelToken(Long usuarioId, Rol rol) {
}
