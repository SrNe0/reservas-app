package com.reservas.api.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByNegocioId(Long negocioId);

    Optional<Horario> findByIdAndNegocioId(Long id, Long negocioId);

    @Query("""
            SELECT COUNT(h) > 0 FROM Horario h
            WHERE h.negocioId = :negocioId
              AND h.diaSemana = :diaSemana
              AND h.horaApertura < :horaCierre
              AND h.horaCierre > :horaApertura
              AND (:idExcluir IS NULL OR h.id <> :idExcluir)
            """)
    boolean haySolape(Long negocioId, short diaSemana, LocalTime horaApertura, LocalTime horaCierre, Long idExcluir);

}
