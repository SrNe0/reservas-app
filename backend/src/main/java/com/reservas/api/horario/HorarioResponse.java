package com.reservas.api.horario;

import java.time.Instant;
import java.time.LocalTime;

public record HorarioResponse(
        Long id,
        Long negocioId,
        short diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        Instant creadoEn
) {

    static HorarioResponse de(Horario horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getNegocioId(),
                horario.getDiaSemana(),
                horario.getHoraApertura(),
                horario.getHoraCierre(),
                horario.getCreadoEn());
    }

}
