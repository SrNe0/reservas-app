package com.reservas.api.disponibilidad;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraDeTurnosTests {

    private static final Duration REJILLA_15 = Duration.ofMinutes(15);
    private static final Duration DURACION_60 = Duration.ofMinutes(60);
    private static final Instant AHORA = Instant.parse("2026-11-01T08:00:00Z");

    @Test
    void tramoRechazaFinAnteriorOIgualAlInicio() {
        Instant t = Instant.parse("2026-11-01T10:00:00Z");

        assertThatThrownBy(() -> new Tramo(t, t))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tramo(t, t.minusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tramosAdyacentesNoSeSolapanPeroCruzadosYContenidosSi() {
        Tramo primero = tramo("09:00", "10:00");
        Tramo adyacente = tramo("10:00", "11:00");
        Tramo solapado = tramo("09:30", "10:30");
        Tramo contenedor = tramo("09:00", "12:00");
        Tramo contenido = tramo("10:00", "11:00");

        assertThat(primero.solapaCon(adyacente)).isFalse();
        assertThat(adyacente.solapaCon(primero)).isFalse();
        assertThat(primero.solapaCon(solapado)).isTrue();
        assertThat(contenedor.solapaCon(contenido)).isTrue();
        assertThat(contenido.solapaCon(contenedor)).isTrue();
    }

    @Test
    void generaTurnosEnRejillaDentroDelHorario() {
        List<Tramo> atencion = List.of(tramo("09:00", "10:30"));

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                atencion, List.of(), DURACION_60, REJILLA_15, AHORA);

        assertThat(turnos).containsExactly(
                tramo("09:00", "10:00"),
                tramo("09:15", "10:15"),
                tramo("09:30", "10:30"));
    }

    @Test
    void recorreTodosLosTramosCuandoHayHorarioPartido() {
        List<Tramo> atencion = List.of(
                tramo("09:00", "10:00"),
                tramo("15:00", "16:00"));

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                atencion, List.of(), DURACION_60, REJILLA_15, AHORA);

        assertThat(turnos).containsExactly(
                tramo("09:00", "10:00"),
                tramo("15:00", "16:00"));
    }

    @Test
    void excluyeTurnosQueSolapanConReservaYPuedenPegarseEnLosBordes() {
        List<Tramo> atencion = List.of(tramo("09:00", "12:00"));
        List<Tramo> ocupados = List.of(tramo("10:00", "11:00"));

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                atencion, ocupados, DURACION_60, REJILLA_15, AHORA);

        assertThat(turnos).containsExactly(
                tramo("09:00", "10:00"),
                tramo("11:00", "12:00"));
    }

    @Test
    void excluyeTurnosQueEmpiezanAntesDeAhoraPeroPermiteElDeAhoraExacto() {
        List<Tramo> atencion = List.of(tramo("09:00", "11:00"));
        Instant ahora = Instant.parse("2026-11-01T09:30:00Z");

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                atencion, List.of(), DURACION_60, REJILLA_15, ahora);

        assertThat(turnos).containsExactly(
                tramo("09:30", "10:30"),
                tramo("09:45", "10:45"),
                tramo("10:00", "11:00"));
    }

    @Test
    void siElServicioDuraMasQueElTramoDevuelveListaVacia() {
        List<Tramo> atencion = List.of(tramo("09:00", "09:45"));

        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                atencion, List.of(), DURACION_60, REJILLA_15, AHORA);

        assertThat(turnos).isEmpty();
    }

    @Test
    void sinTramosDeAtencionDevuelveListaVacia() {
        List<Tramo> turnos = CalculadoraDeTurnos.calcular(
                List.of(), List.of(), DURACION_60, REJILLA_15, AHORA);

        assertThat(turnos).isEmpty();
    }

    @Test
    void rechazaDuracionORejillaNoPositiva() {
        List<Tramo> atencion = List.of(tramo("09:00", "10:00"));

        assertThatThrownBy(() -> CalculadoraDeTurnos.calcular(
                atencion, List.of(), Duration.ZERO, REJILLA_15, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraDeTurnos.calcular(
                atencion, List.of(), DURACION_60, Duration.ZERO, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculadoraDeTurnos.calcular(
                atencion, List.of(), Duration.ofMinutes(-15), REJILLA_15, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Tramo tramo(String horaInicio, String horaFin) {
        return new Tramo(
                Instant.parse("2026-11-01T" + horaInicio + ":00Z"),
                Instant.parse("2026-11-01T" + horaFin + ":00Z"));
    }

}
