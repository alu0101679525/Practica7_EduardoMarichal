package es.ull.practica7.model;

import java.util.Objects;

/**
 * Representa un tripulante actualmente en órbita y la nave en la que se encuentra.
 */
public final class Astronauta {
    private final String nombre;
    private final String nave;

    public Astronauta(String nombre, String nave) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre no puede ser nulo");
        this.nave = Objects.requireNonNull(nave, "La nave no puede ser nula");
    }

    public String getNombre() {
        return nombre;
    }

    public String getNave() {
        return nave;
    }

    @Override
    public String toString() {
        return nombre + " (" + nave + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Astronauta)) return false;
        Astronauta that = (Astronauta) o;
        return nombre.equals(that.nombre) && nave.equals(that.nave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, nave);
    }
}