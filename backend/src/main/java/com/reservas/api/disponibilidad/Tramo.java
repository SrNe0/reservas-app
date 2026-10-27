package com.reservas.api.disponibilidad;

import java.time.Instant;

// Intervalo de tiempo [inicio, fin): el fin es el primer instante que ya no pertenece al tramo
public record Tramo(Instant inicio, Instant fin) {

    public Tramo {
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }

    boolean solapaCon(Tramo otro) {
        return this.inicio.isBefore(otro.fin) && this.fin.isAfter(otro.inicio);
    }

}
