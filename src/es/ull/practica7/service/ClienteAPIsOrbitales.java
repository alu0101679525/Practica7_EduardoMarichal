package es.ull.practica7.service;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.Coordenada;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación concreta que consulta telemetría orbital actualizada en tiempo real
 * desde WhereTheISSAt (posición NORAD) y Corquaid API (tripulación activa).
 */
public class ClienteAPIsOrbitales implements ProveedorDatosOrbitales {

    private static final String URL_ISS_NOW = "https://api.wheretheiss.at/v1/satellites/25544";
    private static final String URL_ASTROS = "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json";

    // En wheretheiss.at los valores vienen como números decimales (ej: "latitude": -51.725749)
    private static final Pattern PATRON_LAT = Pattern.compile("\"latitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PATRON_LON = Pattern.compile("\"longitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    // En corquaid cada objeto de tripulante contiene "name" y "spacecraft" (o "craft")
    private static final Pattern PATRON_OBJETO_PERSONA = Pattern.compile("\\{[^{}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}");
    private static final Pattern PATRON_NAVE = Pattern.compile("\"(?:spacecraft|craft)\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;

    public ClienteAPIsOrbitales() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Coordenada obtenerPosicionISS() throws Exception {
        String json = realizarPeticionGet(URL_ISS_NOW);

        Matcher mLat = PATRON_LAT.matcher(json);
        Matcher mLon = PATRON_LON.matcher(json);

        if (mLat.find() && mLon.find()) {
            double lat = Double.parseDouble(mLat.group(1));
            double lon = Double.parseDouble(mLon.group(1));
            return new Coordenada(lat, lon);
        }
        throw new IllegalStateException("Formato inesperado en respuesta de WhereTheISSAt");
    }

    @Override
    public List<Astronauta> obtenerTripulacionEnOrbita() throws Exception {
        String json = realizarPeticionGet(URL_ASTROS);
        List<Astronauta> tripulacion = new ArrayList<>();

        Matcher mPersona = PATRON_OBJETO_PERSONA.matcher(json);
        while (mPersona.find()) {
            String bloquePersona = mPersona.group(0);
            String nombre = mPersona.group(1);

            Matcher mNave = PATRON_NAVE.matcher(bloquePersona);
            String nave = mNave.find() ? mNave.group(1) : "ISS";

            tripulacion.add(new Astronauta(nombre, nave));
        }

        return tripulacion;
    }

    private String realizarPeticionGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "OrbitWatchApp/1.0")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error HTTP " + response.statusCode() + " al consultar " + url);
        }
        return response.body();
    }
}