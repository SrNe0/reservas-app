package com.reservas.api.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Reservas confirmadas del recurso que tocan el intervalo [desde, hasta)
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.recursoId = :recursoId
              AND r.estado = com.reservas.api.reserva.EstadoReserva.CONFIRMADA
              AND r.inicio < :hasta
              AND r.fin > :desde
            ORDER BY r.inicio
            """)
    List<Reserva> buscarConfirmadas(Long recursoId, Instant desde, Instant hasta);

}
