package com.reservas.api.servicio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Positive int duracionMin,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal precio,
        // Si no se envía, el servicio queda activo
        Boolean activo
) {
}
