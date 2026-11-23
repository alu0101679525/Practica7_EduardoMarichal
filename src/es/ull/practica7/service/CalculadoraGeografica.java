package es.ull.practica7.service;

import es.ull.practica7.model.Coordenada;

/**
 * Utilidad matemática para cálculos geodésicos sobre la esfera terrestre.
 */
public final class CalculadoraGeografica {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private CalculadoraGeografica() {
        // Clase de utilidad no instanciable
    }

    /**
     * Calcula la distancia ortodrómica en kilómetros entre dos coordenadas
     * utilizando la fórmula del semiverseno (Haversine).
     */
    public static double calcularDistanciaKm(Coordenada origen, Coordenada destino) {
        double dLat = Math.toRadians(destino.getLatitud() - origen.getLatitud());
        double dLon = Math.toRadians(destino.getLongitud() - origen.getLongitud());

        double lat1 = Math.toRadians(origen.getLatitud());
        double lat2 = Math.toRadians(destino.getLatitud());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}