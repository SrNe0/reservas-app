package com.reservas.api.reserva;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;

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

    // Copia del precio del servicio al reservar, para conservar el histórico
    @Column(nullable = false, updatable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Generated(event = EventType.INSERT)
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;

    protected Reserva() {
    }

    public Reserva(Long negocioId, Long servicioId, Long recursoId, Instant inicio, Instant fin,
                   BigDecimal precio) {
        this.negocioId = negocioId;
        this.servicioId = servicioId;
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fin = fin;
        this.precio = precio;
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public void reprogramar(Long recursoId, Instant inicio, Instant fin) {
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() {
        return id;
    }

    public Long getNegocioId() {
        return negocioId;
    }

    public Long getServicioId() {
        return servicioId;
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

    public BigDecimal getPrecio() {
        return precio;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

}
