package com.reservas.api.auth;

/**
 * El token no se puede aceptar: caducado, manipulado o mal formado.
 * Es propia para que el resto de la API no dependa de la librería de JWT.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

}
