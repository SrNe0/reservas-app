package com.reservas.api.usuario;

// Roles que se pueden elegir al registrarse. ADMIN no está aquí a propósito: desde fuera nadie
// puede pedirlo, ni siquiera mandando "ADMIN" en el cuerpo (daría 400 al leer el JSON).
public enum TipoDeCuenta {
    CLIENTE(Rol.CLIENTE),
    NEGOCIO(Rol.NEGOCIO);

    private final Rol rol;

    TipoDeCuenta(Rol rol) {
        this.rol = rol;
    }

    public Rol rol() {
        return rol;
    }
}
