package es.ull.practica7.model;

import java.util.Objects;

/**
 * Representa un tripulante actualmente en órbita con su fotografía, rol, nave y código de bandera.
 */
public final class Astronauta {
    private final String nombre;
    private final String nave;
    private final String rol;
    private final String pais;
    private final String urlImagen;
    private final String codigoBandera;

    public Astronauta(String nombre, String nave, String rol, String pais, String urlImagen, String codigoBandera) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre no puede ser nulo");
        this.nave = Objects.requireNonNull(nave, "La nave no puede ser nula");
        this.rol = (rol == null || rol.isBlank()) ? "Tripulante" : rol;
        this.pais = (pais == null || pais.isBlank()) ? "" : pais;
        this.urlImagen = urlImagen;
        this.codigoBandera = (codigoBandera == null) ? "" : codigoBandera.trim().toLowerCase();
    }

    public String getNombre() {
        return nombre;
    }

    public String getNave() {
        return nave;
    }

    public String getRol() {
        return rol;
    }

    public String getPais() {
        return pais;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public String getCodigoBandera() {
        return codigoBandera;
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