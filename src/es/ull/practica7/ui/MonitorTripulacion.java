package es.ull.practica7.ui;

import es.ull.practica7.model.Astronauta;
import es.ull.practica7.observer.Observador;
import es.ull.practica7.observer.Sujeto;
import es.ull.practica7.subject.CentroControlOrbital;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra tarjetas detalladas de cada tripulante actualmente en órbita:
 * fotografía circular, nombre con bandera de su país, cargo y nave asignada.
 */
public class MonitorTripulacion extends JPanel implements Observador {

    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color FONDO_TARJETA = new Color(25, 29, 35);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);
    private static final int DIAMETRO_AVATAR = 60;

    private final JLabel lblContador;
    private final JPanel contenedorTarjetas;
    private final HttpClient httpImagenes;
    private final ExecutorService colaDescargas;
    private final Map<String, ImageIcon> cacheAvatares = new ConcurrentHashMap<>();
    private final Map<String, ImageIcon> cacheBanderas = new ConcurrentHashMap<>();
    private List<Astronauta> ultimaTripulacionRenderizada = List.of();

    public MonitorTripulacion() {
        setBackground(FONDO_PANEL);
        setLayout(new BorderLayout(0, 8));

        this.httpImagenes = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Cola secuencial para evitar bloqueo por exceso de peticiones paralelas en Wikimedia
        this.colaDescargas = Executors.newSingleThreadExecutor();

        TitledBorder titulo = BorderFactory.createTitledBorder(
                new LineBorder(BORDE_PANEL, 1, true),
                " Tripulación en Órbita ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                Color.WHITE
        );
        setBorder(new CompoundBorder(titulo, new EmptyBorder(8, 12, 10, 12)));

        lblContador = new JLabel("Cargando tripulación...");
        lblContador.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblContador.setForeground(new Color(210, 220, 235));
        add(lblContador, BorderLayout.NORTH);

        contenedorTarjetas = new JPanel();
        contenedorTarjetas.setLayout(new BoxLayout(contenedorTarjetas, BoxLayout.Y_AXIS));
        contenedorTarjetas.setBackground(new Color(20, 23, 28));
        contenedorTarjetas.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(contenedorTarjetas);
        scroll.setBorder(new LineBorder(new Color(55, 60, 70), 1));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void actualizar(Sujeto sujeto) {
        if (sujeto instanceof CentroControlOrbital) {
            CentroControlOrbital control = (CentroControlOrbital) sujeto;
            List<Astronauta> tripulacion = control.getAstronautasEnOrbita();

            SwingUtilities.invokeLater(() -> {
                lblContador.setText(tripulacion.size() + " Astronautas activos en el espacio");

                if (!tripulacion.equals(ultimaTripulacionRenderizada)) {
                    ultimaTripulacionRenderizada = List.copyOf(tripulacion);
                    reconstruirTarjetas(tripulacion);
                }
            });
        }
    }

    private void reconstruirTarjetas(List<Astronauta> tripulacion) {
        contenedorTarjetas.removeAll();

        for (Astronauta astro : tripulacion) {
            contenedorTarjetas.add(crearTarjetaAstronauta(astro));
            contenedorTarjetas.add(Box.createVerticalStrut(8));
        }

        contenedorTarjetas.revalidate();
        contenedorTarjetas.repaint();
    }

    private JPanel crearTarjetaAstronauta(Astronauta astro) {
        JPanel tarjeta = new JPanel(new BorderLayout(12, 0));
        tarjeta.setBackground(FONDO_TARJETA);
        tarjeta.setPreferredSize(new Dimension(0, 82));
        tarjeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        tarjeta.setBorder(new CompoundBorder(
                new LineBorder(new Color(52, 58, 70), 1, true),
                new EmptyBorder(9, 10, 9, 12)
        ));

        // Foto circular a la izquierda
        JLabel lblFoto = new JLabel();
        lblFoto.setPreferredSize(new Dimension(DIAMETRO_AVATAR, DIAMETRO_AVATAR));
        lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblFoto.setIcon(crearAvatarPorDefecto(astro.getNombre()));

        if (astro.getUrlImagen() != null && !astro.getUrlImagen().isBlank()) {
            cargarAvatarEnCola(astro.getUrlImagen(), lblFoto);
        }
        tarjeta.add(lblFoto, BorderLayout.WEST);

        // Bloque de 3 líneas a la derecha
        JPanel panelTextos = new JPanel(new GridLayout(3, 1, 0, 2));
        panelTextos.setOpaque(false);

        // Línea 1: Nombre + Bandera del país
        JPanel filaNombreBandera = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaNombreBandera.setOpaque(false);

        JLabel lblNombre = new JLabel(astro.getNombre() + "  ");
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblNombre.setForeground(Color.WHITE);
        filaNombreBandera.add(lblNombre);

        JLabel lblBandera = new JLabel();
        lblBandera.setToolTipText(astro.getPais());
        filaNombreBandera.add(lblBandera);

        if (!astro.getCodigoBandera().isEmpty()) {
            cargarBanderaEnCola(astro.getCodigoBandera(), lblBandera);
        }

        // Línea 2: Cargo / Rol
        JLabel lblRol = new JLabel(astro.getRol());
        lblRol.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblRol.setForeground(new Color(125, 185, 245));

        // Línea 3: Nave espacial
        JLabel lblNave = new JLabel(astro.getNave());
        lblNave.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblNave.setForeground(new Color(165, 175, 190));

        panelTextos.add(filaNombreBandera);
        panelTextos.add(lblRol);
        panelTextos.add(lblNave);

        tarjeta.add(panelTextos, BorderLayout.CENTER);
        return tarjeta;
    }

    private void cargarAvatarEnCola(String url, JLabel destino) {
        if (cacheAvatares.containsKey(url)) {
            destino.setIcon(cacheAvatares.get(url));
            return;
        }

        colaDescargas.submit(() -> {
            try {
                // Si ya se descargó mientras esperaba en cola, reutilizamos caché
                if (cacheAvatares.containsKey(url)) {
                    ImageIcon cached = cacheAvatares.get(url);
                    SwingUtilities.invokeLater(() -> destino.setIcon(cached));
                    return;
                }

                BufferedImage original = descargarImagenConReintento(url);
                if (original != null) {
                    ImageIcon circular = recortarEnCirculo(original, DIAMETRO_AVATAR);
                    cacheAvatares.put(url, circular);
                    SwingUtilities.invokeLater(() -> destino.setIcon(circular));
                }
                Thread.sleep(150);
            } catch (Exception ignored) {
            }
        });
    }

    private void cargarBanderaEnCola(String codigoPais, JLabel destino) {
        if (cacheBanderas.containsKey(codigoPais)) {
            destino.setIcon(cacheBanderas.get(codigoPais));
            return;
        }

        String urlBandera = "https://flagcdn.com/w40/" + codigoPais + ".png";
        colaDescargas.submit(() -> {
            try {
                if (cacheBanderas.containsKey(codigoPais)) {
                    ImageIcon cached = cacheBanderas.get(codigoPais);
                    SwingUtilities.invokeLater(() -> destino.setIcon(cached));
                    return;
                }

                BufferedImage original = descargarImagenConReintento(urlBandera);
                if (original != null) {
                    Image escalada = original.getScaledInstance(22, 15, Image.SCALE_SMOOTH);
                    ImageIcon iconoBandera = new ImageIcon(escalada);
                    cacheBanderas.put(codigoPais, iconoBandera);
                    SwingUtilities.invokeLater(() -> destino.setIcon(iconoBandera));
                }
            } catch (Exception ignored) {
            }
        });
    }

    private BufferedImage descargarImagenConReintento(String url) throws Exception {
        String urlSegura = url.replace(" ", "%20");
        for (int intento = 0; intento < 2; intento++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlSegura))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) OrbitWatch/1.0")
                    .header("Accept", "image/png,image/jpeg,image/*;q=0.8")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = httpImagenes.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) {
                return ImageIO.read(new ByteArrayInputStream(resp.body()));
            }
            if (resp.statusCode() == 429) {
                Thread.sleep(600);
            }
        }
        return null;
    }

    private ImageIcon recortarEnCirculo(BufferedImage src, int diametro) {
        BufferedImage salida = new BufferedImage(diametro, diametro, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = salida.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setClip(new Ellipse2D.Double(0, 0, diametro, diametro));

        int lado = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - lado) / 2;
        int y = Math.max(0, (src.getHeight() - lado) / 5);
        g2.drawImage(src, 0, 0, diametro, diametro, x, y, x + lado, y + lado, null);

        g2.setClip(null);
        g2.setColor(new Color(95, 108, 130));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(0.5, 0.5, diametro - 1, diametro - 1));
        g2.dispose();

        return new ImageIcon(salida);
    }

    private ImageIcon crearAvatarPorDefecto(String nombre) {
        BufferedImage img = new BufferedImage(DIAMETRO_AVATAR, DIAMETRO_AVATAR, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(52, 62, 78));
        g2.fillOval(0, 0, DIAMETRO_AVATAR, DIAMETRO_AVATAR);

        String[] partes = nombre.trim().split("\\s+");
        String iniciales = partes.length > 1
                ? "" + partes[0].charAt(0) + partes[partes.length - 1].charAt(0)
                : nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (DIAMETRO_AVATAR - fm.stringWidth(iniciales)) / 2;
        int ty = ((DIAMETRO_AVATAR - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(iniciales, tx, ty);
        g2.dispose();

        return new ImageIcon(img);
    }
}