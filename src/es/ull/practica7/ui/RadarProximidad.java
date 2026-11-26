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
 * Calcula la distancia entre la ISS y el punto objetivo vigilado,
 * alertando visualmente cuando la estación entra dentro del radio configurado.
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
                " Radar de Proximidad ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                Color.WHITE
        );
        setBorder(new CompoundBorder(titulo, new EmptyBorder(6, 14, 8, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        add(crearEtiqueta("Distancia al objetivo:", Font.PLAIN, 13, new Color(200, 205, 215)), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        lblDistancia = crearEtiqueta("-- km", Font.BOLD, 14, Color.WHITE);
        lblDistancia.setHorizontalAlignment(SwingConstants.RIGHT);
        add(lblDistancia, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(crearEtiqueta("Radio de alerta:", Font.PLAIN, 13, new Color(200, 205, 215)), gbc);

        gbc.gridx = 1;
        lblRadio = crearEtiqueta("1.000 km", Font.BOLD, 14, Color.WHITE);
        lblRadio.setHorizontalAlignment(SwingConstants.RIGHT);
        add(lblRadio, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 4, 2, 4);
        lblEstado = new JLabel("SINCRONIZANDO RADAR...", SwingConstants.CENTER);
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblEstado.setForeground(Color.WHITE);
        lblEstado.setOpaque(true);
        lblEstado.setBackground(new Color(60, 65, 75));
        lblEstado.setBorder(new EmptyBorder(6, 10, 6, 10));
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
                    lblEstado.setText("¡AVISTAMIENTO POSIBLE EN ZONA!");
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