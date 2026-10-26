package com.reservas.api.servicio;

import com.reservas.api.negocio.NegocioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository repositorio;
    private final NegocioRepository negocios;

    public ServicioService(ServicioRepository repositorio, NegocioRepository negocios) {
        this.repositorio = repositorio;
        this.negocios = negocios;
    }

    @Transactional
    public ServicioResponse crear(Long negocioId, ServicioRequest datos) {
        verificarNegocio(negocioId);
        Servicio servicio = new Servicio(negocioId, datos.nombre(), datos.duracionMin(),
                datos.precio(), estaActivo(datos));
        return ServicioResponse.de(repositorio.save(servicio));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long negocioId) {
        verificarNegocio(negocioId);
        return repositorio.findByNegocioId(negocioId).stream().map(ServicioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(Long negocioId, Long id) {
        return ServicioResponse.de(buscar(negocioId, id));
    }

    @Transactional
    public ServicioResponse actualizar(Long negocioId, Long id, ServicioActualizarRequest datos) {
        Servicio servicio = buscar(negocioId, id);
        servicio.actualizar(datos.nombre(), datos.duracionMin(), datos.precio(), datos.activo());
        return ServicioResponse.de(servicio);
    }

    @Transactional
    public void eliminar(Long negocioId, Long id) {
        Servicio servicio = buscar(negocioId, id);
        try {
            repositorio.delete(servicio);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio tiene reservas asociadas");
        }
    }

    private Servicio buscar(Long negocioId, Long id) {
        return repositorio.findByIdAndNegocioId(id, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el servicio " + id + " en el negocio " + negocioId));
    }

    private void verificarNegocio(Long negocioId) {
        if (!negocios.existsById(negocioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId);
        }
    }

    private boolean estaActivo(ServicioRequest datos) {
        return datos.activo() == null || datos.activo();
    }

}
