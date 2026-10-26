package com.reservas.api.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "servicio")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Se guarda el id y no una relación @ManyToOne: no hay cargas perezosas y encaja con la clave compuesta
    @Column(name = "negocio_id", nullable = false, updatable = false)
    private Long negocioId;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    protected Servicio() {
    }

    public Servicio(Long negocioId, String nombre, int duracionMin, BigDecimal precio, boolean activo) {
        this.negocioId = negocioId;
        this.nombre = nombre;
        this.duracionMin = duracionMin;
        this.precio = precio;
        this.activo = activo;
    }

    public void actualizar(String nombre, int duracionMin, BigDecimal precio, boolean activo) {
        this.nombre = nombre;
        this.duracionMin = duracionMin;
        this.precio = precio;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public Long getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDuracionMin() {
        return duracionMin;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

}
