package es.ull.practica7.service;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.model.Coordenada;
import es.ull.practica7.model.TelemetriaISS;

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
 * Cliente HTTP que consulta telemetría orbital en tiempo real desde WhereTheISSAt
 * y los perfiles completos de la tripulación desde Corquaid API.
 */
public class ClienteAPIsOrbitales implements ProveedorDatosOrbitales {

    private static final String URL_ISS_NOW = "https://api.wheretheiss.at/v1/satellites/25544";
    private static final String URL_ASTROS = "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json";

    private static final Pattern PATRON_LAT = Pattern.compile("\"latitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PATRON_LON = Pattern.compile("\"longitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PATRON_ALT = Pattern.compile("\"altitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PATRON_VEL = Pattern.compile("\"velocity\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private static final Pattern PATRON_OBJETO_PERSONA = Pattern.compile("\\{[^{}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}");
    private static final Pattern PATRON_NAVE = Pattern.compile("\"(?:spacecraft|craft)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_ROL = Pattern.compile("\"position\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_PAIS = Pattern.compile("\"country\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_IMAGEN = Pattern.compile("\"image\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PATRON_FLAG_CODE = Pattern.compile("\"flag_code\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;

    public ClienteAPIsOrbitales() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public TelemetriaISS obtenerTelemetriaISS() throws Exception {
        String json = realizarPeticionGet(URL_ISS_NOW);

        double lat = extraerDouble(PATRON_LAT, json, "latitud");
        double lon = extraerDouble(PATRON_LON, json, "longitud");
        double alt = extraerDouble(PATRON_ALT, json, "altitud");
        double vel = extraerDouble(PATRON_VEL, json, "velocidad");

        return new TelemetriaISS(new Coordenada(lat, lon), vel, alt);
    }

    @Override
    public List<Astronauta> obtenerTripulacionEnOrbita() throws Exception {
        String json = realizarPeticionGet(URL_ASTROS);
        List<Astronauta> tripulacion = new ArrayList<>();

        Matcher mPersona = PATRON_OBJETO_PERSONA.matcher(json);
        while (mPersona.find()) {
            String bloque = mPersona.group(0);
            String nombre = mPersona.group(1);

            String nave = extraerCampoOpcional(PATRON_NAVE, bloque, "ISS");
            String rol = extraerCampoOpcional(PATRON_ROL, bloque, "Astronauta");
            String pais = extraerCampoOpcional(PATRON_PAIS, bloque, "");
            String imagen = extraerCampoOpcional(PATRON_IMAGEN, bloque, null);
            String codigoBandera = extraerCampoOpcional(PATRON_FLAG_CODE, bloque, "");

            tripulacion.add(new Astronauta(nombre, nave, rol, pais, imagen, codigoBandera));
        }

        return tripulacion;
    }

    private double extraerDouble(Pattern patron, String texto, String campo) {
        Matcher m = patron.matcher(texto);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        throw new IllegalStateException("No se pudo extraer el campo '" + campo + "' de la telemetría.");
    }

    private String extraerCampoOpcional(Pattern patron, String bloque, String valorDefecto) {
        Matcher m = patron.matcher(bloque);
        return m.find() ? m.group(1) : valorDefecto;
    }

    private String realizarPeticionGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "OrbitWatchApp/1.0")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error HTTP " + response.statusCode() + " al consultar " + url);
        }
        return response.body();
    }
}