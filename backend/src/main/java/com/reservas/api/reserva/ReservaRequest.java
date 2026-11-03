package com.reservas.api.reserva;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReservaRequest(
        @NotNull Long servicioId,
        @NotNull Long recursoId,
        // Instante exacto en que empieza el turno, por ejemplo 2026-11-09T14:00:00Z
        @NotNull Instant inicio
) {
}
