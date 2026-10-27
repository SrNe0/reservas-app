package com.reservas.api.horario;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/negocios/{negocioId}/horarios")
@Tag(name = "Horarios")
public class HorarioController {

    private final HorarioService servicio;

    public HorarioController(HorarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<HorarioResponse> crear(@PathVariable Long negocioId,
                                                 @Valid @RequestBody HorarioRequest datos) {
        HorarioResponse creado = servicio.crear(negocioId, datos);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public List<HorarioResponse> listar(@PathVariable Long negocioId) {
        return servicio.listar(negocioId);
    }

    @GetMapping("/{id}")
    public HorarioResponse obtener(@PathVariable Long negocioId, @PathVariable Long id) {
        return servicio.obtener(negocioId, id);
    }

    @PutMapping("/{id}")
    public HorarioResponse actualizar(@PathVariable Long negocioId, @PathVariable Long id,
                                     @Valid @RequestBody HorarioRequest datos) {
        return servicio.actualizar(negocioId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long negocioId, @PathVariable Long id) {
        servicio.eliminar(negocioId, id);
        return ResponseEntity.noContent().build();
    }

}
