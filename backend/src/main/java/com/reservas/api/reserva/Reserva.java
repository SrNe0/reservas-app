package com.reservas.api.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Por ahora solo se lee (disponibilidad); el resto de columnas se mapea al crear reservas
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private Long negocioId;

    @Column(name = "servicio_id", nullable = false, updatable = false)
    private Long servicioId;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    protected Reserva() {
    }

    public Long getId() {
        return id;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

}
