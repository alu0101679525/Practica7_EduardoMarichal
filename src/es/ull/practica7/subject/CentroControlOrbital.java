package es.ull.practica7.subject;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.Coordenada;
import es.ull.practica7.observer.Sujeto;
import es.ull.practica7.service.ProveedorDatosOrbitales;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sujeto Concreto del patrón Observador.
 * Centraliza el estado orbital de la ISS, el punto objetivo vigilado
 * y la tripulación activa, notificando automáticamente ante cualquier cambio.
 */
public class CentroControlOrbital extends Sujeto {

    private final ProveedorDatosOrbitales proveedorDatos;

    private Coordenada posicionISS;
    private Coordenada puntoObjetivo;
    private double radioAlertaKm;
    private List<Astronauta> astronautasEnOrbita;
    private String horaUltimaActualizacion;

    public CentroControlOrbital(ProveedorDatosOrbitales proveedorDatos) {
        this.proveedorDatos = Objects.requireNonNull(proveedorDatos);
        this.posicionISS = new Coordenada(28.4818, -16.3201);
        this.puntoObjetivo = new Coordenada(28.4818, -16.3201);
        this.radioAlertaKm = 1000.0;
        this.astronautasEnOrbita = new ArrayList<>();
        this.horaUltimaActualizacion = "--:--:--";
    }

    /**
     * Consulta las APIs externas, actualiza el estado interno y dispara la notificación.
     */
    public void actualizarDesdeAPIs() {
        try {
            this.posicionISS = proveedorDatos.obtenerPosicionISS();
            List<Astronauta> nuevaTripulacion = proveedorDatos.obtenerTripulacionEnOrbita();

            // Conserva tripulantes añadidos manualmente mediante el botón de demostración
            List<Astronauta> combinada = new ArrayList<>(nuevaTripulacion);
            for (Astronauta a : astronautasEnOrbita) {
                if (a.getNave().contains("Demo") && !combinada.contains(a)) {
                    combinada.add(a);
                }
            }
            this.astronautasEnOrbita = combinada;
            this.horaUltimaActualizacion = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            notificar();
        } catch (Exception e) {
            System.err.println("Error al consultar las APIs orbitales: " + e.getMessage());
        }
    }

    /**
     * Actualiza las coordenadas objetivo y el radio de alerta definidos por el usuario.
     */
    public void fijarObjetivo(Coordenada nuevoObjetivo, double nuevoRadioKm) {
        if (nuevoRadioKm <= 0) {
            throw new IllegalArgumentException("El radio de alerta debe ser mayor que 0 km.");
        }
        this.puntoObjetivo = Objects.requireNonNull(nuevoObjetivo);
        this.radioAlertaKm = nuevoRadioKm;
        notificar();
    }

    /**
     * Sitúa el punto objetivo sobre la posición actual de la ISS para probar el radar.
     */
    public void fijarObjetivoEnISSActual() {
        this.puntoObjetivo = this.posicionISS;
        notificar();
    }

    /**
     * Añade un astronauta ficticio para comprobar la alerta de cambio de tripulación.
     */
    public void simularNuevoAstronauta() {
        int numero = astronautasEnOrbita.size() + 1;
        List<Astronauta> actualizada = new ArrayList<>(astronautasEnOrbita);
        actualizada.add(new Astronauta("Astronauta Invitado #" + numero, "Crew Dragon Demo"));
        this.astronautasEnOrbita = actualizada;
        this.horaUltimaActualizacion = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        notificar();
    }

    public Coordenada getPosicionISS() {
        return posicionISS;
    }

    public Coordenada getPuntoObjetivo() {
        return puntoObjetivo;
    }

    public double getRadioAlertaKm() {
        return radioAlertaKm;
    }

    public List<Astronauta> getAstronautasEnOrbita() {
        return Collections.unmodifiableList(astronautasEnOrbita);
    }

    public String getHoraUltimaActualizacion() {
        return horaUltimaActualizacion;
    }
}
