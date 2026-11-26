package es.ull.practica7.service;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.TelemetriaISS;

import java.util.List;

/**
 * Abstracción para obtener telemetría orbital y tripulación activa.
 */
public interface ProveedorDatosOrbitales {
    TelemetriaISS obtenerTelemetriaISS() throws Exception;
    List<Astronauta> obtenerTripulacionEnOrbita() throws Exception;
}