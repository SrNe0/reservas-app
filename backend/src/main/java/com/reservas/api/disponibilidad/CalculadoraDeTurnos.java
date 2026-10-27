package com.reservas.api.disponibilidad;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Lógica pura, sin Spring ni base de datos: se prueba con tests unitarios.
final class CalculadoraDeTurnos {

    private CalculadoraDeTurnos() {
    }

    static List<Tramo> calcular(List<Tramo> atencion, List<Tramo> ocupados,
                                Duration duracion, Duration rejilla, Instant ahora) {
        if (!duracion.isPositive() || !rejilla.isPositive()) {
            throw new IllegalArgumentException("La duración y la rejilla deben ser positivas");
        }
        List<Tramo> turnos = new ArrayList<>();
        for (Tramo horario : atencion) {
            Instant inicio = horario.inicio();
            while (true) {
                Instant fin = inicio.plus(duracion);
                if (fin.isAfter(horario.fin())) {
                    break;
                }
                Tramo candidato = new Tramo(inicio, fin);
                if (!inicio.isBefore(ahora) && estaLibre(candidato, ocupados)) {
                    turnos.add(candidato);
                }
                inicio = inicio.plus(rejilla);
            }
        }
        return turnos;
    }

    private static boolean estaLibre(Tramo candidato, List<Tramo> ocupados) {
        for (Tramo ocupado : ocupados) {
            if (candidato.solapaCon(ocupado)) {
                return false;
            }
        }
        return true;
    }

}
