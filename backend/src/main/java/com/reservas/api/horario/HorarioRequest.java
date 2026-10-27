package com.reservas.api.horario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioRequest(
        @NotNull @Min(1) @Max(7) Short diaSemana,
        @NotNull LocalTime horaApertura,
        @NotNull LocalTime horaCierre
) {
}
