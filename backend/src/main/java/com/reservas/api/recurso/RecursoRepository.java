package com.reservas.api.recurso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByNegocioId(Long negocioId);

    Optional<Recurso> findByIdAndNegocioId(Long id, Long negocioId);
}
