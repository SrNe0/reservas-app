package com.reservas.api.horario;

import com.reservas.api.negocio.NegocioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HorarioService {

    private final HorarioRepository repositorio;
    private final NegocioRepository negocios;

    public HorarioService(HorarioRepository repositorio, NegocioRepository negocios) {
        this.repositorio = repositorio;
        this.negocios = negocios;
    }

    @Transactional
    public HorarioResponse crear(Long negocioId, HorarioRequest datos) {
        verificarNegocio(negocioId);
        validarRangoDeHoras(datos);
        verificarSolape(negocioId, datos, null);
        Horario horario = new Horario(negocioId, datos.diaSemana(), datos.horaApertura(), datos.horaCierre());
        return HorarioResponse.de(repositorio.save(horario));
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listar(Long negocioId) {
        verificarNegocio(negocioId);
        return repositorio.findByNegocioId(negocioId).stream().map(HorarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public HorarioResponse obtener(Long negocioId, Long id) {
        return HorarioResponse.de(buscar(negocioId, id));
    }

    @Transactional
    public HorarioResponse actualizar(Long negocioId, Long id, HorarioRequest datos) {
        Horario horario = buscar(negocioId, id);
        validarRangoDeHoras(datos);
        verificarSolape(negocioId, datos, id);
        horario.actualizar(datos.diaSemana(), datos.horaApertura(), datos.horaCierre());
        return HorarioResponse.de(horario);
    }

    @Transactional
    public void eliminar(Long negocioId, Long id) {
        Horario horario = buscar(negocioId, id);
        repositorio.delete(horario);
    }

    private Horario buscar(Long negocioId, Long id) {
        return repositorio.findByIdAndNegocioId(id, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el horario " + id + " en el negocio " + negocioId));
    }

    private void verificarNegocio(Long negocioId) {
        if (!negocios.existsById(negocioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId);
        }
    }

    private void validarRangoDeHoras(HorarioRequest datos) {
        if (!datos.horaCierre().isAfter(datos.horaApertura())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de cierre debe ser posterior a la hora de apertura");
        }
    }

    private void verificarSolape(Long negocioId, HorarioRequest datos, Long idExcluir) {
        boolean solapa = repositorio.haySolape(
                negocioId, datos.diaSemana(), datos.horaApertura(), datos.horaCierre(), idExcluir);
        if (solapa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El horario se solapa con otro turno existente en el mismo día");
        }
    }

}
