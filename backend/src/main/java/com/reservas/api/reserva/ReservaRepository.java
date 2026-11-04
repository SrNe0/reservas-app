package com.reservas.api.reserva;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Reservas confirmadas del recurso que tocan el intervalo [desde, hasta); ignorarReservaId permite
    // no contar una reserva (la que se está reprogramando) como si ocupara su propio turno
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.recursoId = :recursoId
              AND r.estado = com.reservas.api.reserva.EstadoReserva.CONFIRMADA
              AND r.inicio < :hasta
              AND r.fin > :desde
              AND (:ignorarReservaId IS NULL OR r.id <> :ignorarReservaId)
            ORDER BY r.inicio
            """)
    List<Reserva> buscarConfirmadas(Long recursoId, Instant desde, Instant hasta, Long ignorarReservaId);

    Optional<Reserva> findByIdAndNegocioId(Long id, Long negocioId);

    // SELECT ... FOR UPDATE: cancelar y reprogramar la misma reserva no se pisan entre sí
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id AND r.negocioId = :negocioId")
    Optional<Reserva> buscarBloqueando(Long id, Long negocioId);

    List<Reserva> findByNegocioIdOrderByInicio(Long negocioId);

    List<Reserva> findByNegocioIdAndRecursoIdOrderByInicio(Long negocioId, Long recursoId);

}
