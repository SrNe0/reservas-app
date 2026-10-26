package com.reservas.api.recurso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// PUT reemplaza el recurso completo: activo es obligatorio para no reactivar por omisión
public record RecursoActualizarRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull Boolean activo
) {
}
