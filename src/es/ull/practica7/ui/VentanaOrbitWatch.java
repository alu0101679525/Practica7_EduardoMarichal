package es.ull.practica7.ui;

import es.ull.practica7.model.Coordenada;
import es.ull.practica7.subject.CentroControlOrbital;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Ventana principal de la aplicación OrbitWatch.
 * Conecta los controles del usuario con el sujeto concreto (CentroControlOrbital)
 * y organiza visualmente los tres Observadores Concretos.
 */
public class VentanaOrbitWatch extends JFrame {

    private static final Color FONDO_APP = new Color(24, 27, 32);
    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);

    private final CentroControlOrbital centroControl;
    private final JTextField txtLatitud;
    private final JTextField txtLongitud;
    private final JTextField txtRadio;

    public VentanaOrbitWatch(CentroControlOrbital centroControl) {
        this.centroControl = centroControl;

        setTitle("OrbitWatch - Monitorización Orbital en Tiempo Real");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 740);
        setMinimumSize(new Dimension(1020, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO_APP);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(12, 14, 14, 14));

        // Instanciar los 3 Observadores Concretos
        PanelTelemetria panelTelemetria = new PanelTelemetria();
        RadarProximidad radarProximidad = new RadarProximidad();
        MonitorTripulacion monitorTripulacion = new MonitorTripulacion();

        // Registrar observadores iniciales en el Sujeto
        centroControl.agregarObservador(panelTelemetria);
        centroControl.agregarObservador(radarProximidad);
        centroControl.agregarObservador(monitorTripulacion);

        // 1. Cabecera superior
        add(crearCabecera(), BorderLayout.NORTH);

        // 2. Cuerpo principal: Panel de control arriba + Cuadrícula de observadores abajo
        JPanel cuerpo = new JPanel(new BorderLayout(12, 12));
        cuerpo.setOpaque(false);

        txtLatitud = crearCampoTexto("28.4818", 8);
        txtLongitud = crearCampoTexto("-16.3201", 8);
        txtRadio = crearCampoTexto("1000", 6);

        cuerpo.add(crearBarraControles(panelTelemetria, radarProximidad, monitorTripulacion), BorderLayout.NORTH);

        // Configurar clic interactivo en el mapa
        panelTelemetria.setOnMapaClickListener(coord -> {
            txtLatitud.setText(String.format(java.util.Locale.US, "%.4f", coord.getLatitud()));
            txtLongitud.setText(String.format(java.util.Locale.US, "%.4f", coord.getLongitud()));
            aplicarObjetivoDesdeCampos();
        });

        // Panel central dividido (Izquierda 66%: Mapa | Derecha 34%: Radar + Tripulación)
        JPanel gridObservadores = new JPanel(new GridBagLayout());
        gridObservadores.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.66;
        gbc.insets = new Insets(0, 0, 0, 12);
        gridObservadores.add(panelTelemetria, gbc);

        JPanel columnaDerecha = new JPanel(new GridLayout(2, 1, 0, 12));
        columnaDerecha.setOpaque(false);
        columnaDerecha.add(radarProximidad);
        columnaDerecha.add(monitorTripulacion);

        gbc.gridx = 1; gbc.weightx = 0.34;
        gbc.insets = new Insets(0, 0, 0, 0);
        gridObservadores.add(columnaDerecha, gbc);

        cuerpo.add(gridObservadores, BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel crearCabecera() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titulo = new JLabel("OrbitWatch - Monitorización Orbital en Tiempo Real");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);
        header.add(titulo, BorderLayout.WEST);

        JLabel badgeEnVivo = new JLabel(" ● EN VIVO ");
        badgeEnVivo.setFont(new Font("SansSerif", Font.BOLD, 13));
        badgeEnVivo.setForeground(new Color(110, 231, 135));
        badgeEnVivo.setOpaque(true);
        badgeEnVivo.setBackground(new Color(28, 68, 42));
        badgeEnVivo.setBorder(new CompoundBorder(
                new LineBorder(new Color(46, 160, 67), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        header.add(badgeEnVivo, BorderLayout.EAST);

        return header;
    }

    private JPanel crearBarraControles(PanelTelemetria obs1, RadarProximidad obs2, MonitorTripulacion obs3) {
        JPanel contenedor = new JPanel(new GridBagLayout());
        contenedor.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Izquierda: Controles del Sujeto
        JPanel panelSujeto = new JPanel(new GridBagLayout());
        panelSujeto.setBackground(FONDO_PANEL);
        panelSujeto.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(BORDE_PANEL, 1, true),
                        " CentroControlOrbital (Sujeto) ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), Color.WHITE),
                new EmptyBorder(6, 12, 8, 12)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: Inputs
        c.gridy = 0;
        c.gridx = 0; panelSujeto.add(crearLabel("Latitud"), c);
        c.gridx = 1; panelSujeto.add(txtLatitud, c);
        c.gridx = 2; panelSujeto.add(crearLabel("Longitud"), c);
        c.gridx = 3; panelSujeto.add(txtLongitud, c);
        c.gridx = 4; panelSujeto.add(crearLabel("Radio (km)"), c);
        c.gridx = 5; panelSujeto.add(txtRadio, c);

        // Fila 2: Botones de acción
        JButton btnFijar = crearBoton("Fijar Objetivo");
        btnFijar.addActionListener(e -> aplicarObjetivoDesdeCampos());

        JButton btnISSActual = crearBoton("Fijar en ISS Actual");
        btnISSActual.addActionListener(e -> {
            centroControl.fijarObjetivoEnISSActual();
            Coordenada pos = centroControl.getPuntoObjetivo();
            txtLatitud.setText(String.format(java.util.Locale.US, "%.4f", pos.getLatitud()));
            txtLongitud.setText(String.format(java.util.Locale.US, "%.4f", pos.getLongitud()));
        });

        JButton btnSimularAstro = crearBoton("Simular Nuevo Astronauta");
        btnSimularAstro.addActionListener(e -> centroControl.simularNuevoAstronauta());

        c.gridy = 1;
        c.gridx = 0; c.gridwidth = 2; panelSujeto.add(btnFijar, c);
        c.gridx = 2; c.gridwidth = 2; panelSujeto.add(btnISSActual, c);
        c.gridx = 4; c.gridwidth = 2; panelSujeto.add(btnSimularAstro, c);

        // Derecha: Checkboxes de Observadores Activos
        JPanel panelSuscripciones = new JPanel(new GridLayout(3, 1, 0, 2));
        panelSuscripciones.setBackground(FONDO_PANEL);
        panelSuscripciones.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(BORDE_PANEL, 1, true),
                        " Observadores Activos ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), Color.WHITE),
                new EmptyBorder(4, 12, 6, 12)
        ));

        panelSuscripciones.add(crearCheckObservador("PanelTelemetria", obs1));
        panelSuscripciones.add(crearCheckObservador("RadarProximidad", obs2));
        panelSuscripciones.add(crearCheckObservador("MonitorTripulacion", obs3));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.72;
        gbc.insets = new Insets(0, 0, 0, 12);
        contenedor.add(panelSujeto, gbc);

        gbc.gridx = 1; gbc.weightx = 0.28;
        gbc.insets = new Insets(0, 0, 0, 0);
        contenedor.add(panelSuscripciones, gbc);

        return contenedor;
    }

    private void aplicarObjetivoDesdeCampos() {
        try {
            double lat = Double.parseDouble(txtLatitud.getText().trim().replace(",", "."));
            double lon = Double.parseDouble(txtLongitud.getText().trim().replace(",", "."));
            double radio = Double.parseDouble(txtRadio.getText().trim().replace(",", "."));

            centroControl.fijarObjetivo(new Coordenada(lat, lon), radio);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Valores no válidos. Revisa latitud (-90 a 90), longitud (-180 a 180) y radio (> 0).",
                    "Error de entrada", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JCheckBox crearCheckObservador(String nombre, es.ull.practica7.observer.Observador observador) {
        JCheckBox check = new JCheckBox(nombre, true);
        check.setOpaque(false);
        check.setForeground(Color.WHITE);
        check.setFont(new Font("SansSerif", Font.PLAIN, 13));
        check.addActionListener(e -> {
            if (check.isSelected()) {
                centroControl.agregarObservador(observador);
                // Notificamos inmediatamente al re-suscribir para que se sincronice
                observador.actualizar(centroControl);
            } else {
                centroControl.eliminarObservador(observador);
            }
        });
        return check;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(210, 215, 225));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private JTextField crearCampoTexto(String valorInicial, int columnas) {
        JTextField field = new JTextField(valorInicial, columnas);
        field.setBackground(new Color(45, 50, 60));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(90, 100, 115), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        return field;
    }

    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(55, 62, 74));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setBorder(new CompoundBorder(
                new LineBorder(new Color(95, 105, 120), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
