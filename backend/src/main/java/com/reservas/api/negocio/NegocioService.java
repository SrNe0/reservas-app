package com.reservas.api.negocio;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.List;

@Service
public class NegocioService {

    private final NegocioRepository repositorio;

    public NegocioService(NegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public NegocioResponse crear(NegocioRequest datos) {
        validarZonaHoraria(datos.zonaHoraria());
        Negocio negocio = new Negocio(datos.nombre(), datos.descripcion(), datos.zonaHoraria());
        return NegocioResponse.de(repositorio.save(negocio));
    }

    @Transactional(readOnly = true)
    public List<NegocioResponse> listar() {
        return repositorio.findAll().stream().map(NegocioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public NegocioResponse obtener(Long id) {
        return NegocioResponse.de(buscar(id));
    }

    @Transactional
    public NegocioResponse actualizar(Long id, NegocioRequest datos) {
        validarZonaHoraria(datos.zonaHoraria());
        Negocio negocio = buscar(id);
        negocio.actualizar(datos.nombre(), datos.descripcion(), datos.zonaHoraria());
        return NegocioResponse.de(negocio);
    }

    @Transactional
    public void eliminar(Long id) {
        Negocio negocio = buscar(id);
        try {
            repositorio.delete(negocio);
            // flush fuerza el DELETE ahora para que la base rechace si tiene datos asociados
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El negocio tiene servicios, recursos, horarios o reservas asociados");
        }
    }

    private Negocio buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + id));
    }

    // Solo nombres de la base de zonas horarias (America/Bogota, UTC), no desfases como "UTC+3"
    private void validarZonaHoraria(String zonaHoraria) {
        if (!ZoneId.getAvailableZoneIds().contains(zonaHoraria)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Zona horaria no válida: " + zonaHoraria);
        }
    }

}
