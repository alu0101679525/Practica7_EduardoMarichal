package es.ull.practica7;

import es.ull.practica7.service.ClienteAPIsOrbitales;
import es.ull.practica7.service.ProveedorDatosOrbitales;
import es.ull.practica7.subject.CentroControlOrbital;
import es.ull.practica7.ui.VentanaOrbitWatch;

import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Punto de entrada de la aplicación OrbitWatch.
 */
public class Main {

    private static final int INTERVALO_ACTUALIZACION_SEGUNDOS = 5;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProveedorDatosOrbitales clienteAPI = new ClienteAPIsOrbitales();
            CentroControlOrbital centroControl = new CentroControlOrbital(clienteAPI);

            VentanaOrbitWatch ventana = new VentanaOrbitWatch(centroControl);
            ventana.setVisible(true);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                    centroControl::actualizarDesdeAPIs,
                    0,
                    INTERVALO_ACTUALIZACION_SEGUNDOS,
                    TimeUnit.SECONDS
            );
        });
    }
}