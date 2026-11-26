package es.ull.practica7.ui;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.observer.Observador;
import es.ull.practica7.observer.Sujeto;
import es.ull.practica7.subject.CentroControlOrbital;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Observador Concreto 3: Muestra el listado de astronautas activos en el espacio
 * y destaca una alerta visual cuando varía el número de tripulantes.
 */
public class MonitorTripulacion extends JPanel implements Observador {

    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);

    private final JLabel lblContador;
    private final JLabel lblAlertaCambio;
    private final DefaultListModel<String> modeloLista;
    private int totalAnterior = -1;

    public MonitorTripulacion() {
        setBackground(FONDO_PANEL);
        setLayout(new BorderLayout(0, 8));

        TitledBorder titulo = BorderFactory.createTitledBorder(
                new LineBorder(BORDE_PANEL, 1, true),
                " MonitorTripulacion (Observador) ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                Color.WHITE
        );
        setBorder(new CompoundBorder(titulo, new EmptyBorder(10, 15, 10, 15)));

        // Cabecera del panel
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);

        lblContador = new JLabel("0 Astronautas en Órbita");
        lblContador.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblContador.setForeground(Color.WHITE);
        panelSuperior.add(lblContador, BorderLayout.NORTH);

        lblAlertaCambio = new JLabel("Sin cambios en la tripulación");
        lblAlertaCambio.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblAlertaCambio.setForeground(new Color(160, 170, 185));
        lblAlertaCambio.setBorder(new EmptyBorder(4, 0, 2, 0));
        panelSuperior.add(lblAlertaCambio, BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);

        // Lista desplazable de astronautas
        modeloLista = new DefaultListModel<>();
        JList<String> listaVisual = new JList<>(modeloLista);
        listaVisual.setBackground(new Color(26, 29, 34));
        listaVisual.setForeground(new Color(230, 235, 240));
        listaVisual.setFont(new Font("SansSerif", Font.PLAIN, 13));
        listaVisual.setSelectionBackground(new Color(55, 62, 75));
        listaVisual.setBorder(new EmptyBorder(5, 8, 5, 8));

        JScrollPane scroll = new JScrollPane(listaVisual);
        scroll.setBorder(new LineBorder(new Color(55, 60, 70), 1));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void actualizar(Sujeto sujeto) {
        if (sujeto instanceof CentroControlOrbital) {
            CentroControlOrbital control = (CentroControlOrbital) sujeto;
            List<Astronauta> tripulacion = control.getAstronautasEnOrbita();

            SwingUtilities.invokeLater(() -> {
                int totalActual = tripulacion.size();
                lblContador.setText(totalActual + " Astronautas en Órbita");

                if (totalAnterior != -1 && totalActual != totalAnterior) {
                    int diff = totalActual - totalAnterior;
                    String signo = diff > 0 ? "+" + diff : String.valueOf(diff);
                    lblAlertaCambio.setText("¡ALERTA! Cambio de tripulación (" + signo + ")");
                    lblAlertaCambio.setForeground(new Color(255, 193, 7));
                }
                totalAnterior = totalActual;

                modeloLista.clear();
                for (int i = 0; i < tripulacion.size(); i++) {
                    modeloLista.addElement((i + 1) + ". " + tripulacion.get(i).toString());
                }
            });
        }
    }
}