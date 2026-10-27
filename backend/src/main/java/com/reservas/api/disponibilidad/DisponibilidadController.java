package com.reservas.api.disponibilidad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/negocios/{negocioId}/recursos/{recursoId}/disponibilidad")
@Tag(name = "Disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService servicio;

    public DisponibilidadController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Turnos libres de un recurso para un servicio en una fecha",
            description = "La fecha se interpreta en la zona horaria del negocio y los turnos se devuelven en UTC. "
                    + "Es una consulta informativa: no reserva nada. La garantía contra dobles reservas "
                    + "se aplica al crear la reserva.")
    public DisponibilidadResponse consultar(@PathVariable Long negocioId, @PathVariable Long recursoId,
                                            @RequestParam Long servicioId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.consultar(negocioId, recursoId, servicioId, fecha);
    }

}
