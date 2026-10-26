package com.reservas.api.servicio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByNegocioId(Long negocioId);

    // Buscar siempre por id y negocio: un servicio de otro negocio se comporta como si no existiera
    Optional<Servicio> findByIdAndNegocioId(Long id, Long negocioId);

}
