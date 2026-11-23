package es.ull.practica7.observer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase base abstracta que gestiona la suscripción y notificación
 * de objetos Observador de forma desacoplada.
 */
public abstract class Sujeto {

    private final List<Observador> observadores;

    protected Sujeto() {
        this.observadores = new CopyOnWriteArrayList<>();
    }

    /**
     * Registra un nuevo observador si no estaba ya suscrito.
     *
     * @param observador Instancia que implementa la interfaz Observador.
     */
    public void agregarObservador(Observador observador) {
        Objects.requireNonNull(observador, "El observador no puede ser nulo");
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    /**
     * Elimina un observador de la lista de notificaciones.
     *
     * @param observador Instancia a desvincular.
     */
    public void eliminarObservador(Observador observador) {
        observadores.remove(observador);
    }

    /**
     * Comprueba si un observador concreto está actualmente suscrito.
     *
     * @param observador Instancia a comprobar.
     * @return true si está suscrito, false en caso contrario.
     */
    public boolean contieneObservador(Observador observador) {
        return observadores.contains(observador);
    }

    /**
     * Notifica a todos los observadores registrados que ha ocurrido
     * un cambio relevante en el estado del sujeto.
     */
    protected void notificar() {
        for (Observador observador : observadores) {
            observador.actualizar(this);
        }
    }
}