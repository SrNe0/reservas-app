package com.reservas.api.negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "negocio")
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    // Acepta nulos hasta la Etapa 3, cuando existan los usuarios
    @Column(name = "propietario_id")
    private Long propietarioId;

    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    // JPA necesita un constructor sin argumentos
    protected Negocio() {
    }

    public Negocio(String nombre, String descripcion, String zonaHoraria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.zonaHoraria = zonaHoraria;
    }

    public void actualizar(String nombre, String descripcion, String zonaHoraria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.zonaHoraria = zonaHoraria;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Long getPropietarioId() {
        return propietarioId;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

}
