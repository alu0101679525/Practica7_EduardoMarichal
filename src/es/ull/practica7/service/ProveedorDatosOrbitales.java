package es.ull.practica7.service;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.Coordenada;

import java.util.List;

/**
 * Abstracción para obtener telemetría orbital.
 */
public interface ProveedorDatosOrbitales {
    Coordenada obtenerPosicionISS() throws Exception;
    List<Astronauta> obtenerTripulacionEnOrbita() throws Exception;
}