package com.reservas.api.reserva;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReprogramarRequest(
        // Si no se envía, la reserva sigue en el mismo recurso
        Long recursoId,
        @NotNull Instant inicio
) {
}
