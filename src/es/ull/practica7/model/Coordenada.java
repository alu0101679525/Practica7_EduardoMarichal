package es.ull.practica7.model;

import java.util.Objects;

/**
 * Representa una ubicación geográfica inmutable en grados decimales.
 */
public final class Coordenada {
    private final double latitud;
    private final double longitud;

    public Coordenada(double latitud, double longitud) {
        if (latitud < -90.0 || latitud > 90.0) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90 grados.");
        }
        if (longitud < -180.0 || longitud > 180.0) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180 grados.");
        }
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    @Override
    public String toString() {
        String hemisferioLat = latitud >= 0 ? "N" : "S";
        String hemisferioLon = longitud >= 0 ? "E" : "O";
        return String.format("%.4f° %s, %.4f° %s",
                Math.abs(latitud), hemisferioLat,
                Math.abs(longitud), hemisferioLon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordenada)) return false;
        Coordenada that = (Coordenada) o;
        return Double.compare(that.latitud, latitud) == 0
                && Double.compare(that.longitud, longitud) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitud, longitud);
    }
}