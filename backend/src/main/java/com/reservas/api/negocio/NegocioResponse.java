package com.reservas.api.negocio;

import java.time.Instant;

public record NegocioResponse(
        Long id,
        String nombre,
        String descripcion,
        String zonaHoraria,
        Instant creadoEn
) {

    static NegocioResponse de(Negocio negocio) {
        return new NegocioResponse(
                negocio.getId(),
                negocio.getNombre(),
                negocio.getDescripcion(),
                negocio.getZonaHoraria(),
                negocio.getCreadoEn());
    }

}
