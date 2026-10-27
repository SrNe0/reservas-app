package com.reservas.api.disponibilidad;

import com.reservas.api.horario.Horario;
import com.reservas.api.horario.HorarioRepository;
import com.reservas.api.negocio.Negocio;
import com.reservas.api.negocio.NegocioRepository;
import com.reservas.api.recurso.Recurso;
import com.reservas.api.recurso.RecursoRepository;
import com.reservas.api.reserva.ReservaRepository;
import com.reservas.api.servicio.Servicio;
import com.reservas.api.servicio.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DisponibilidadService {

    static final Duration REJILLA = Duration.ofMinutes(15);
    static final int DIAS_MAXIMOS = 60;

    private final NegocioRepository negocios;
    private final RecursoRepository recursos;
    private final ServicioRepository servicios;
    private final HorarioRepository horarios;
    private final ReservaRepository reservas;
    private final Clock reloj;

    public DisponibilidadService(NegocioRepository negocios, RecursoRepository recursos,
                                 ServicioRepository servicios, HorarioRepository horarios,
                                 ReservaRepository reservas, Clock reloj) {
        this.negocios = negocios;
        this.recursos = recursos;
        this.servicios = servicios;
        this.horarios = horarios;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(Long negocioId, Long recursoId, Long servicioId, LocalDate fecha) {
        Negocio negocio = negocios.findById(negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el negocio " + negocioId));
        Recurso recurso = recursos.findByIdAndNegocioId(recursoId, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el recurso " + recursoId + " en el negocio " + negocioId));
        Servicio servicio = servicios.findByIdAndNegocioId(servicioId, negocioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el servicio " + servicioId + " en el negocio " + negocioId));

        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        validarFecha(fecha, zona);

        if (!recurso.isActivo() || !servicio.isActivo()) {
            return respuesta(fecha, zona, servicio, List.of());
        }

        List<Tramo> atencion = tramosDeAtencion(negocioId, fecha, zona);
        if (atencion.isEmpty()) {
            return respuesta(fecha, zona, servicio, List.of());
        }

        // Los tramos no se solapan y van ordenados: el primero abre y el último cierra el día
        Instant desde = atencion.getFirst().inicio();
        Instant hasta = atencion.getLast().fin();
        List<Tramo> ocupados = reservas.buscarConfirmadas(recursoId, desde, hasta).stream()
                .map(r -> new Tramo(r.getInicio(), r.getFin()))
                .toList();

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(atencion, ocupados,
                Duration.ofMinutes(servicio.getDuracionMin()), REJILLA, reloj.instant());
        return respuesta(fecha, zona, servicio, turnos);
    }

    private List<Tramo> tramosDeAtencion(Long negocioId, LocalDate fecha, ZoneId zona) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        List<Horario> delDia = horarios.findByNegocioId(negocioId).stream()
                .filter(h -> h.getDiaSemana() == diaSemana)
                .sorted(Comparator.comparing(Horario::getHoraApertura))
                .toList();

        List<Tramo> tramos = new ArrayList<>();
        for (Horario horario : delDia) {
            Instant inicio = instante(fecha, horario.getHoraApertura(), zona);
            Instant fin = instante(fecha, horario.getHoraCierre(), zona);
            // En el día en que el reloj se adelanta, un horario como 02:00-03:00 no dura nada:
            // ambas horas caen en el mismo instante y el tramo se descarta
            if (fin.isAfter(inicio)) {
                tramos.add(new Tramo(inicio, fin));
            }
        }
        return tramos;
    }

    // Una hora local solo es un instante dentro de una zona; ZonedDateTime resuelve los cambios de hora
    private Instant instante(LocalDate fecha, LocalTime hora, ZoneId zona) {
        return ZonedDateTime.of(fecha, hora, zona).toInstant();
    }

    private void validarFecha(LocalDate fecha, ZoneId zona) {
        LocalDate hoy = LocalDate.now(reloj.withZone(zona));
        if (fecha.isBefore(hoy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha no puede estar en el pasado");
        }
        if (fecha.isAfter(hoy.plusDays(DIAS_MAXIMOS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se consulta hasta " + DIAS_MAXIMOS + " días hacia adelante");
        }
    }

    private DisponibilidadResponse respuesta(LocalDate fecha, ZoneId zona, Servicio servicio, List<Tramo> turnos) {
        return new DisponibilidadResponse(fecha, zona.getId(), servicio.getDuracionMin(), turnos);
    }

}
