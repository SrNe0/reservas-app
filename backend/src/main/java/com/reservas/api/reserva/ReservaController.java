package com.reservas.api.reserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/negocios/{negocioId}/reservas")
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaService servicio;

    public ReservaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crea una reserva",
            description = "El inicio debe ser uno de los turnos que ofrece la disponibilidad. "
                    + "400 si no es un turno posible (pasado, fuera de horario, fuera de la rejilla), "
                    + "409 si el recurso ya lo tiene ocupado o el recurso o el servicio están inactivos.")
    public ResponseEntity<ReservaResponse> crear(@PathVariable Long negocioId,
                                                 @Valid @RequestBody ReservaRequest datos) {
        ReservaResponse creada = servicio.crear(negocioId, datos);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela una reserva",
            description = "Solo con al menos 2 horas de antelación respecto al inicio. "
                    + "409 si ya estaba cancelada o falta menos de ese tiempo.")
    public ReservaResponse cancelar(@PathVariable Long negocioId, @PathVariable Long id) {
        return servicio.cancelar(negocioId, id);
    }

    @PostMapping("/{id}/reprogramar")
    @Operation(summary = "Mueve una reserva a otro turno",
            description = "Del mismo recurso o de otro (recursoId opcional). El nuevo inicio debe ser un turno "
                    + "disponible y la reserva actual debe empezar en al menos 2 horas. "
                    + "400 si no es un turno posible; 409 si está ocupado o no se puede mover.")
    public ReservaResponse reprogramar(@PathVariable Long negocioId, @PathVariable Long id,
                                       @Valid @RequestBody ReprogramarRequest datos) {
        return servicio.reprogramar(negocioId, id, datos);
    }

    @GetMapping
    @Operation(summary = "Lista las reservas del negocio ordenadas por inicio; opcionalmente de un recurso")
    public List<ReservaResponse> listar(@PathVariable Long negocioId,
                                        @RequestParam(required = false) Long recursoId) {
        return servicio.listar(negocioId, recursoId);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long negocioId, @PathVariable Long id) {
        return servicio.obtener(negocioId, id);
    }

}
