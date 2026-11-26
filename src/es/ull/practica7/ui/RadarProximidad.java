package es.ull.practica7.ui;

import es.ull.practica7.model.Coordenada;
import es.ull.practica7.observer.Observador;
import es.ull.practica7.observer.Sujeto;
import es.ull.practica7.service.CalculadoraGeografica;
import es.ull.practica7.subject.CentroControlOrbital;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Observador Concreto 2: Calcula la distancia entre la ISS y el punto objetivo,
 * cambiando visualmente su estado cuando entra dentro del radio de alerta.
 */
public class RadarProximidad extends JPanel implements Observador {

    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);
    private static final Color VERDE_SEGURO = new Color(46, 160, 67);
    private static final Color ROJO_ALERTA = new Color(218, 54, 51);

    private final JLabel lblDistancia;
    private final JLabel lblRadio;
    private final JLabel lblEstado;

    public RadarProximidad() {
        setBackground(FONDO_PANEL);
        setLayout(new GridBagLayout());

        TitledBorder titulo = BorderFactory.createTitledBorder(
                new LineBorder(BORDE_PANEL, 1, true),
                " RadarProximidad (Observador) ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                Color.WHITE
        );
        setBorder(new CompoundBorder(titulo, new EmptyBorder(10, 15, 10, 15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: Distancia
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        JLabel txtDist = crearEtiqueta("Distancia:", Font.PLAIN, 14, new Color(200, 205, 215));
        add(txtDist, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        lblDistancia = crearEtiqueta("-- km", Font.BOLD, 15, Color.WHITE);
        lblDistancia.setHorizontalAlignment(SwingConstants.RIGHT);
        add(lblDistancia, gbc);

        // Fila 2: Radio de alerta
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel txtRadio = crearEtiqueta("Radio de Alerta:", Font.PLAIN, 14, new Color(200, 205, 215));
        add(txtRadio, gbc);

        gbc.gridx = 1;
        lblRadio = crearEtiqueta("1.000 km", Font.BOLD, 15, Color.WHITE);
        lblRadio.setHorizontalAlignment(SwingConstants.RIGHT);
        add(lblRadio, gbc);

        // Fila 3: Badge de estado
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 5, 5, 5);
        lblEstado = new JLabel("ESPERANDO TELEMETRÍA", SwingConstants.CENTER);
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblEstado.setForeground(Color.WHITE);
        lblEstado.setOpaque(true);
        lblEstado.setBackground(new Color(60, 65, 75));
        lblEstado.setBorder(new EmptyBorder(8, 12, 8, 12));
        add(lblEstado, gbc);
    }

    @Override
    public void actualizar(Sujeto sujeto) {
        if (sujeto instanceof CentroControlOrbital) {
            CentroControlOrbital control = (CentroControlOrbital) sujeto;
            Coordenada iss = control.getPosicionISS();
            Coordenada objetivo = control.getPuntoObjetivo();
            double radioKm = control.getRadioAlertaKm();

            double distanciaKm = CalculadoraGeografica.calcularDistanciaKm(iss, objetivo);
            boolean enRango = distanciaKm <= radioKm;

            SwingUtilities.invokeLater(() -> {
                lblDistancia.setText(String.format("%,.0f km", distanciaKm));
                lblRadio.setText(String.format("%,.0f km", radioKm));

                if (enRango) {
                    lblEstado.setText("¡AVISTAMIENTO POSIBLE!");
                    lblEstado.setBackground(ROJO_ALERTA);
                } else {
                    lblEstado.setText("FUERA DE RANGO");
                    lblEstado.setBackground(VERDE_SEGURO);
                }
            });
        }
    }

    private JLabel crearEtiqueta(String texto, int estilo, int tamano, Color color) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", estilo, tamano));
        label.setForeground(color);
        return label;
    }
}