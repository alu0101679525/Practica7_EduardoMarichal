package es.ull.practica7.observer;

/**
 * Interfaz que define el contrato para cualquier entidad interesada
 * en recibir notificaciones ante cambios de estado en un Sujeto.
 */
public interface Observador {
    /**
     * Metodo invocado por el Sujeto cuando su estado interno ha cambiado.
     * Recibe la referencia al Sujeto para consultar únicamente los datos que necesite.
     *
     * @param sujeto El objeto observado que originó la notificación.
     */
    void actualizar(Sujeto sujeto);
}