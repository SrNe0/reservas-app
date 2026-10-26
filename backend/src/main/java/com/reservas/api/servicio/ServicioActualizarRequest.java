package com.reservas.api.servicio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// PUT reemplaza el recurso completo: activo es obligatorio para no reactivar por omisión
public record ServicioActualizarRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Positive int duracionMin,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @NotNull Boolean activo
) {
}
