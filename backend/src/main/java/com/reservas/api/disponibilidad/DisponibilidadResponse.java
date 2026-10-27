package com.reservas.api.disponibilidad;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadResponse(
        LocalDate fecha,
        String zonaHoraria,
        int duracionMin,
        List<Tramo> turnos
) {
}
