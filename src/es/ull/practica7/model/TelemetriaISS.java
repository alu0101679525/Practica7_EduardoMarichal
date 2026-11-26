package es.ull.practica7.model;

import java.util.Objects;

/**
 * Representa el paquete completo de telemetría instantánea de la ISS.
 */
public final class TelemetriaISS {
    private final Coordenada coordenada;
    private final double velocidadKmh;
    private final double altitudKm;

    public TelemetriaISS(Coordenada coordenada, double velocidadKmh, double altitudKm) {
        this.coordenada = Objects.requireNonNull(coordenada, "La coordenada no puede ser nula");
        this.velocidadKmh = velocidadKmh;
        this.altitudKm = altitudKm;
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    public double getVelocidadKmh() {
        return velocidadKmh;
    }

    public double getAltitudKm() {
        return altitudKm;
    }
}