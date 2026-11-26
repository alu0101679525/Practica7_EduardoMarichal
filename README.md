# OrbitWatch

Aplicación de escritorio en Java para la monitorización en tiempo real de la Estación Espacial Internacional (ISS) y de la tripulación humana actualmente activa en el espacio.

## Descripción General

**OrbitWatch** actúa como un centro de control orbital que consulta telemetría en vivo cada 5 segundos y actualiza automáticamente sus distintos módulos visuales de forma desacoplada:

* **Telemetría y Mapa Orbital:** Mapamundi equirectangular interactivo de alta resolución (con soporte para *zoom* con la rueda del ratón, desplazamiento arrastrando y selección de coordenadas objetivo mediante clic). Muestra la posición actual de la ISS, su trayectoria reciente, su velocidad y su altitud.
* **Radar de Proximidad:** Calcula geodésicamente (mediante la fórmula de Haversine) la distancia entre la ISS y el punto objetivo vigilado, alertando visualmente cuando la estación entra dentro del radio de avistamiento configurado.
* **Tripulación en Órbita:** Presenta tarjetas de cada astronauta activo en el espacio, incluyendo su fotografía oficial, bandera de nacionalidad, cargo y nave asignada.

---

## Arquitectura y Patrón de Diseño

El núcleo de la aplicación implementa el **Patrón de Diseño Observador (Observer)**:

```text
src/es/ull/practica7/
├── observer/       # Abstracciones del patrón (Sujeto y Observador)
├── subject/        # Sujeto concreto (CentroControlOrbital)
├── model/          # Modelos de dominio inmutables (Coordenada, TelemetriaISS, Astronauta)
├── service/        # Acceso a APIs externas y utilidades geodésicas
└── ui/             # Observadores concretos visuales y ventana principal en Java Swing
```

### Funcionamiento del Patrón Observador (Modelo *Pull*)
1. **`Sujeto` y `Observador` (`observer`):** Definen el contrato genérico de suscripción (`agregarObservador`, `eliminarObservador`) y notificación (`notificar`, `actualizar(Sujeto)`), completamente desacoplado de la lógica espacial o de la interfaz gráfica.
2. **`CentroControlOrbital` (`subject`):** Actúa como *Sujeto concreto*. Mantiene el estado centralizado (telemetría de la ISS, punto objetivo, radio de alerta y lista de tripulantes) y notifica a sus observadores cada vez que llegan nuevos datos de las APIs o el usuario modifica el objetivo.
3. **Observadores concretos (`ui`):** `PanelTelemetria`, `RadarProximidad` y `MonitorTripulacion` implementan la interfaz `Observador`. Al recibir una notificación, cada panel consulta únicamente los datos que necesita del sujeto y actualiza su vista de manera independiente.

---

## Fuentes de Datos (APIs)

* **Telemetría orbital de la ISS:** [Where The ISS At? API](https://api.wheretheiss.at/v1/satellites/25544) (coordenadas, velocidad y altitud en tiempo real).
* **Tripulación activa en órbita:** [Corquaid People in Space API](https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json) (perfiles, naves, cargos y nacionalidades).

---

## Requisitos y Ejecución

* **Java Development Kit (JDK) 17** o superior (sin dependencias externas; utiliza `java.net.http.HttpClient` y `javax.swing`).
* Asegúrate de que la carpeta `resources/` (con `mapa_mundo.png`) esté marcada como *Resources Root* en tu IDE.
* Ejecuta la clase principal `es.ull.practica7.Main`.
