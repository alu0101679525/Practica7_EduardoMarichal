package es.ull.practica7.subject;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.Coordenada;
import es.ull.practica7.model.TelemetriaISS;
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
 * Centraliza la telemetría en vivo de la ISS, el punto objetivo vigilado
 * y la tripulación activa, notificando automáticamente ante cualquier cambio.
 */
public class CentroControlOrbital extends Sujeto {

    private final ProveedorDatosOrbitales proveedorDatos;

    private TelemetriaISS telemetriaISS;
    private Coordenada puntoObjetivo;
    private double radioAlertaKm;
    private List<Astronauta> astronautasEnOrbita;
    private String horaUltimaActualizacion;

    public CentroControlOrbital(ProveedorDatosOrbitales proveedorDatos) {
        this.proveedorDatos = Objects.requireNonNull(proveedorDatos);
        Coordenada tenerife = new Coordenada(28.4818, -16.3201);
        this.telemetriaISS = new TelemetriaISS(tenerife, 27600.0, 408.0);
        this.puntoObjetivo = tenerife;
        this.radioAlertaKm = 1000.0;
        this.astronautasEnOrbita = new ArrayList<>();
        this.horaUltimaActualizacion = "--:--:--";
    }

    /**
     * Consulta las APIs externas, actualiza el estado interno y notifica a los observadores.
     */
    public void actualizarDesdeAPIs() {
        try {
            this.telemetriaISS = proveedorDatos.obtenerTelemetriaISS();
            this.astronautasEnOrbita = proveedorDatos.obtenerTripulacionEnOrbita();
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
     * Centra el punto objetivo sobre la posición actual de la estación espacial.
     */
    public void centrarObjetivoEnISS() {
        this.puntoObjetivo = this.telemetriaISS.getCoordenada();
        notificar();
    }

    public TelemetriaISS getTelemetriaISS() {
        return telemetriaISS;
    }

    public Coordenada getPosicionISS() {
        return telemetriaISS.getCoordenada();
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