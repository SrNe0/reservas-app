package com.reservas.api.reserva;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservaResponse(
        Long id,
        Long negocioId,
        Long servicioId,
        Long recursoId,
        Instant inicio,
        Instant fin,
        BigDecimal precio,
        EstadoReserva estado,
        Instant creadoEn
) {

    static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getNegocioId(),
                reserva.getServicioId(),
                reserva.getRecursoId(),
                reserva.getInicio(),
                reserva.getFin(),
                reserva.getPrecio(),
                reserva.getEstado(),
                reserva.getCreadoEn());
    }

}
