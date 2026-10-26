package com.reservas.api.recurso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecursoRequest(

    @NotBlank @Size(max = 120) String nombre,
    Boolean activo
) {
}
