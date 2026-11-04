package com.reservas.api.recurso;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByNegocioId(Long negocioId);

    Optional<Recurso> findByIdAndNegocioId(Long id, Long negocioId);

    // SELECT ... FOR UPDATE: las reservas del mismo recurso hacen cola hasta que termine la transacción
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :id AND r.negocioId = :negocioId")
    Optional<Recurso> buscarBloqueandoParaReservar(Long id, Long negocioId);
}
