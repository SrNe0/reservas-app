package com.reservas.api.servicio;

import java.math.BigDecimal;
import java.time.Instant;

public record ServicioResponse(
        Long id,
        Long negocioId,
        String nombre,
        int duracionMin,
        BigDecimal precio,
        boolean activo,
        Instant creadoEn
) {

    static ServicioResponse de(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getNegocioId(),
                servicio.getNombre(),
                servicio.getDuracionMin(),
                servicio.getPrecio(),
                servicio.isActivo(),
                servicio.getCreadoEn());
    }

}
