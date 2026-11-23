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
 * Implementación concreta que consulta las APIs públicas de Open Notify
 * utilizando el cliente HTTP nativo de Java.
 */
public class ClienteOpenNotify implements ProveedorDatosOrbitales {

    private static final String URL_ISS_NOW = "http://api.open-notify.org/iss-now.json";
    private static final String URL_ASTROS = "http://api.open-notify.org/astros.json";

    private static final Pattern PATRON_LAT = Pattern.compile("\"latitude\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_LON = Pattern.compile("\"longitude\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_ASTRO = Pattern.compile("\\{\\s*\"craft\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");
    private static final Pattern PATRON_ASTRO_INV = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"craft\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");

    private final HttpClient httpClient;

    public ClienteOpenNotify() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
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
        throw new IllegalStateException("Formato inesperado en iss-now.json");
    }

    @Override
    public List<Astronauta> obtenerTripulacionEnOrbita() throws Exception {
        String json = realizarPeticionGet(URL_ASTROS);
        List<Astronauta> tripulacion = new ArrayList<>();

        Matcher m1 = PATRON_ASTRO.matcher(json);
        while (m1.find()) {
            tripulacion.add(new Astronauta(m1.group(2), m1.group(1)));
        }

        if (tripulacion.isEmpty()) {
            Matcher m2 = PATRON_ASTRO_INV.matcher(json);
            while (m2.find()) {
                tripulacion.add(new Astronauta(m2.group(1), m2.group(2)));
            }
        }

        return tripulacion;
    }

    private String realizarPeticionGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
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
