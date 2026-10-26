package com.reservas.api.negocio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NegocioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @NotBlank String zonaHoraria
) {
}
