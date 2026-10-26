package com.reservas.api.recurso;

import java.time.Instant;

public record RecursoResponse(
    Long id,
    Long negocioId,
    String nombre,
    boolean activo,
    Instant creadoEn
) {
    static RecursoResponse de(Recurso recurso) {
        return new RecursoResponse(
            recurso.getId(),
            recurso.getNegocioId(),
            recurso.getNombre(),
            recurso.isActivo(),
            recurso.getCreadoEn()
        );
    }
}
