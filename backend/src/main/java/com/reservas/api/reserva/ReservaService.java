package com.reservas.api.reserva;

import com.reservas.api.disponibilidad.DisponibilidadService;
import com.reservas.api.disponibilidad.Tramo;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
import com.reservas.api.recurso.Recurso;
import com.reservas.api.recurso.RecursoRepository;
import com.reservas.api.servicio.Servicio;
import com.reservas.api.servicio.ServicioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReservaService {

    // Nombre de la restricción EXCLUDE creada en la migración V5
    private static final String RESTRICCION_SOLAPE = "ex_reserva_recurso_solape";

    // Cancelar o reprogramar exige al menos esta antelación respecto al inicio de la reserva
    static final Duration ANTICIPACION_MINIMA = Duration.ofHours(2);

    private final ReservaRepository repositorio;
    private final NegocioRepository negocios;
    private final RecursoRepository recursos;
    private final ServicioRepository servicios;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public ReservaService(ReservaRepository repositorio, NegocioRepository negocios,
                          RecursoRepository recursos, ServicioRepository servicios,
                          DisponibilidadService disponibilidad, Clock reloj) {
        this.repositorio = repositorio;
        this.negocios = negocios;
        this.recursos = recursos;
        this.servicios = servicios;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    @Transactional
    public ReservaResponse crear(Long negocioId, ReservaRequest datos) {
        Negocio negocio = negocios.findById(negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId));
        Recurso recurso = recursos.buscarBloqueandoParaReservar(datos.recursoId(), negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el recurso " + datos.recursoId() + " en el negocio " + negocioId));
        Servicio servicio = servicios.findByIdAndNegocioId(datos.servicioId(), negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el servicio " + datos.servicioId() + " en el negocio " + negocioId));

        if (!recurso.isActivo() || !servicio.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El recurso o el servicio no están activos");
        }

        Instant fin = datos.inicio().plus(Duration.ofMinutes(servicio.getDuracionMin()));
        verificarTurno(negocio, recurso, servicio, new Tramo(datos.inicio(), fin), null);

        Reserva reserva = new Reserva(negocioId, servicio.getId(), recurso.getId(),
                datos.inicio(), fin, servicio.getPrecio());
        try {
            return ReservaResponse.de(repositorio.save(reserva));
        } catch (DataIntegrityViolationException e) {
            if (!violaLaRestriccionDeSolape(e)) {
                throw e;
            }
            throw turnoOcupado();
        }
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(Long negocioId, Long recursoId) {
        if (!negocios.existsById(negocioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId);
        }
        List<Reserva> reservas = recursoId == null
                ? repositorio.findByNegocioIdOrderByInicio(negocioId)
                : repositorio.findByNegocioIdAndRecursoIdOrderByInicio(negocioId, recursoId);
        return reservas.stream().map(ReservaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long negocioId, Long id) {
        return ReservaResponse.de(repositorio.findByIdAndNegocioId(id, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva " + id + " en el negocio " + negocioId)));
    }

    @Transactional
    public ReservaResponse cancelar(Long negocioId, Long id) {
        Reserva reserva = buscarReservaModificable(negocioId, id);
        reserva.cancelar();
        return ReservaResponse.de(reserva);
    }

    @Transactional
    public ReservaResponse reprogramar(Long negocioId, Long id, ReprogramarRequest datos) {
        Negocio negocio = negocios.findById(negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId));
        Reserva reserva = buscarReservaModificable(negocioId, id);

        Long recursoDestinoId = datos.recursoId() != null ? datos.recursoId() : reserva.getRecursoId();
        Recurso recurso = recursos.buscarBloqueandoParaReservar(recursoDestinoId, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el recurso " + recursoDestinoId + " en el negocio " + negocioId));
        Servicio servicio = servicios.findByIdAndNegocioId(reserva.getServicioId(), negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el servicio " + reserva.getServicioId() + " en el negocio " + negocioId));

        if (!recurso.isActivo() || !servicio.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El recurso o el servicio no están activos");
        }

        Instant nuevoFin = datos.inicio().plus(Duration.ofMinutes(servicio.getDuracionMin()));
        verificarTurno(negocio, recurso, servicio, new Tramo(datos.inicio(), nuevoFin), reserva.getId());

        reserva.reprogramar(recurso.getId(), datos.inicio(), nuevoFin);
        try {
            // Forzar el UPDATE aquí para que cualquier violación de ex_reserva_recurso_solape ocurra dentro del try
            repositorio.flush();
            return ReservaResponse.de(reserva);
        } catch (DataIntegrityViolationException e) {
            if (!violaLaRestriccionDeSolape(e)) {
                throw e;
            }
            throw turnoOcupado();
        }
    }

    private Reserva buscarReservaModificable(Long negocioId, Long id) {
        Reserva reserva = repositorio.buscarBloqueando(id, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva " + id + " en el negocio " + negocioId));
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya está cancelada");
        }
        if (reserva.getInicio().isBefore(reloj.instant().plus(ANTICIPACION_MINIMA))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Se requieren al menos 2 horas de antelación para modificar la reserva");
        }
        return reserva;
    }

    private boolean violaLaRestriccionDeSolape(DataIntegrityViolationException e) {
        return e.getMessage() != null && e.getMessage().contains(RESTRICCION_SOLAPE);
    }

    private ResponseStatusException turnoOcupado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya está ocupado por otra reserva");
    }

    private void verificarTurno(Negocio negocio, Recurso recurso, Servicio servicio, Tramo candidato,
                                Long ignorarReservaId) {
        LocalDate fecha = candidato.inicio().atZone(ZoneId.of(negocio.getZonaHoraria())).toLocalDate();
        if (!disponibilidad.turnos(negocio, recurso, servicio, fecha, false).contains(candidato)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El inicio no corresponde a un turno válido del horario de atención");
        }
        if (!disponibilidad.turnos(negocio, recurso, servicio, fecha, true, ignorarReservaId).contains(candidato)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno ya está ocupado por otra reserva");
        }
    }

}
