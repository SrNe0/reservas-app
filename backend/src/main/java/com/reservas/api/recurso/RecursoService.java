package com.reservas.api.recurso;

import com.reservas.api.negocio.NegocioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecursoService {

    private final RecursoRepository repositorio;
    private final NegocioRepository negocios;

    public RecursoService(RecursoRepository repositorio, NegocioRepository negocios) {
        this.repositorio = repositorio;
        this.negocios = negocios;
    }

    @Transactional
    public RecursoResponse crear(Long negocioId, RecursoRequest datos) {
        verificarNegocio(negocioId);
        Recurso recurso = new Recurso(negocioId, datos.nombre(), estaActivo(datos));
        return RecursoResponse.de(repositorio.save(recurso));
    }

    @Transactional(readOnly = true)
    public List<RecursoResponse> listar(Long negocioId) {
        verificarNegocio(negocioId);
        return repositorio.findByNegocioId(negocioId).stream().map(RecursoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public RecursoResponse obtener(Long negocioId, Long id) {
        return RecursoResponse.de(buscar(negocioId, id));
    }

    @Transactional
    public RecursoResponse actualizar(Long negocioId, Long id, RecursoActualizarRequest datos) {
        Recurso recurso = buscar(negocioId, id);
        recurso.actualizar(datos.nombre(), datos.activo());
        return RecursoResponse.de(recurso);
    }

    @Transactional
    public void eliminar(Long negocioId, Long id) {
        Recurso recurso = buscar(negocioId, id);
        try {
            repositorio.delete(recurso);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El recurso tiene reservas asociadas");
        }
    }

    private Recurso buscar(Long negocioId, Long id) {
        return repositorio.findByIdAndNegocioId(id, negocioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el recurso " + id + " en el negocio " + negocioId));
    }

    private void verificarNegocio(Long negocioId) {
        if (!negocios.existsById(negocioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId);
        }
    }

    private boolean estaActivo(RecursoRequest datos) {
        return datos.activo() == null || datos.activo();
    }
}
