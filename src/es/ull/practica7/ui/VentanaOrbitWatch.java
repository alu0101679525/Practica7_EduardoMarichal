package es.ull.practica7.ui;

import es.ull.practica7.model.Coordenada;
import es.ull.practica7.subject.CentroControlOrbital;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal de la aplicación OrbitWatch.
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

        setTitle("OrbitWatch");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 760);
        setMinimumSize(new Dimension(1040, 660));
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO_APP);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(12, 14, 14, 14));

        // Instanciar y registrar los observadores visuales
        PanelTelemetria panelTelemetria = new PanelTelemetria();
        RadarProximidad radarProximidad = new RadarProximidad();
        MonitorTripulacion monitorTripulacion = new MonitorTripulacion();

        centroControl.agregarObservador(panelTelemetria);
        centroControl.agregarObservador(radarProximidad);
        centroControl.agregarObservador(monitorTripulacion);

        // Cabecera superior
        add(crearCabecera(), BorderLayout.NORTH);

        // Cuerpo principal
        JPanel cuerpo = new JPanel(new BorderLayout(12, 12));
        cuerpo.setOpaque(false);

        txtLatitud = crearCampoTexto("28.4818", 8);
        txtLongitud = crearCampoTexto("-16.3201", 8);
        txtRadio = crearCampoTexto("1000", 6);

        cuerpo.add(crearBarraControles(), BorderLayout.NORTH);

        // Clic directo en el mapa actualiza los campos y el objetivo
        panelTelemetria.setOnMapaClickListener(coord -> {
            txtLatitud.setText(String.format(java.util.Locale.US, "%.4f", coord.getLatitud()));
            txtLongitud.setText(String.format(java.util.Locale.US, "%.4f", coord.getLongitud()));
            aplicarObjetivoDesdeCampos();
        });

        // Cuadrícula inferior: Izquierda 64% Mapa | Derecha 36% Radar (arriba) + Tripulación (abajo)
        JPanel gridPrincipal = new JPanel(new GridBagLayout());
        gridPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.64;
        gbc.insets = new Insets(0, 0, 0, 12);
        gridPrincipal.add(panelTelemetria, gbc);

        // Columna derecha equilibrada (26% RadarProximidad, 74% MonitorTripulacion)
        JPanel columnaDerecha = new JPanel(new GridBagLayout());
        columnaDerecha.setOpaque(false);
        GridBagConstraints gbcDer = new GridBagConstraints();
        gbcDer.gridx = 0;
        gbcDer.fill = GridBagConstraints.BOTH;
        gbcDer.weightx = 1.0;

        gbcDer.gridy = 0; gbcDer.weighty = 0.26;
        gbcDer.insets = new Insets(0, 0, 12, 0);
        columnaDerecha.add(radarProximidad, gbcDer);

        gbcDer.gridy = 1; gbcDer.weighty = 0.74;
        gbcDer.insets = new Insets(0, 0, 0, 0);
        columnaDerecha.add(monitorTripulacion, gbcDer);

        gbc.gridx = 1; gbc.weightx = 0.36;
        gbc.insets = new Insets(0, 0, 0, 0);
        gridPrincipal.add(columnaDerecha, gbc);

        cuerpo.add(gridPrincipal, BorderLayout.CENTER);
        add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel crearCabecera() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titulo = new JLabel("OrbitWatch");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);
        header.add(titulo, BorderLayout.WEST);

        JPanel panelDerecho = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        panelDerecho.setOpaque(false);

        JLabel lblReloj = new JLabel();
        lblReloj.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblReloj.setForeground(new Color(200, 210, 225));
        actualizarTextoReloj(lblReloj);

        Timer timerReloj = new Timer(1000, e -> actualizarTextoReloj(lblReloj));
        timerReloj.start();

        JLabel badgeEnVivo = new JLabel(" ● EN VIVO ");
        badgeEnVivo.setFont(new Font("SansSerif", Font.BOLD, 13));
        badgeEnVivo.setForeground(new Color(110, 231, 135));
        badgeEnVivo.setOpaque(true);
        badgeEnVivo.setBackground(new Color(28, 68, 42));
        badgeEnVivo.setBorder(new CompoundBorder(
                new LineBorder(new Color(46, 160, 67), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        panelDerecho.add(lblReloj);
        panelDerecho.add(badgeEnVivo);
        header.add(panelDerecho, BorderLayout.EAST);

        return header;
    }

    private void actualizarTextoReloj(JLabel lblReloj) {
        ZonedDateTime ahoraLocal = ZonedDateTime.now();
        ZonedDateTime ahoraUTC = ahoraLocal.withZoneSameInstant(ZoneOffset.UTC);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        lblReloj.setText("Local " + ahoraLocal.format(fmt) + "  |  UTC " + ahoraUTC.format(fmt));
    }

    private JPanel crearBarraControles() {
        JPanel panelControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        panelControl.setBackground(FONDO_PANEL);
        panelControl.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(BORDE_PANEL, 1, true),
                        " Control de Objetivo ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 13), Color.WHITE),
                new EmptyBorder(4, 10, 6, 10)
        ));

        panelControl.add(crearLabel("Latitud:"));
        panelControl.add(txtLatitud);

        panelControl.add(crearLabel("Longitud:"));
        panelControl.add(txtLongitud);

        panelControl.add(crearLabel("Radio de alerta (km):"));
        panelControl.add(txtRadio);

        JButton btnFijar = crearBoton("Fijar objetivo");
        btnFijar.addActionListener(e -> aplicarObjetivoDesdeCampos());
        panelControl.add(btnFijar);

        JButton btnCentrarISS = crearBoton("Centrar en ISS");
        btnCentrarISS.addActionListener(e -> {
            centroControl.centrarObjetivoEnISS();
            Coordenada pos = centroControl.getPuntoObjetivo();
            txtLatitud.setText(String.format(java.util.Locale.US, "%.4f", pos.getLatitud()));
            txtLongitud.setText(String.format(java.util.Locale.US, "%.4f", pos.getLongitud()));
        });
        panelControl.add(btnCentrarISS);

        return panelControl;
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
                new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}