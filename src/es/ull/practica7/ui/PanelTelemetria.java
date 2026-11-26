package es.ull.practica7.ui;

import es.ull.practica7.model.Coordenada;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Observador Concreto 1: Renderiza el mapamundi equirectangular oscuro (proporción fija 2:1),
 * la trayectoria orbital de la ISS, el punto objetivo con su radio de alerta circular
 * y las coordenadas en tiempo real.
 */
public class PanelTelemetria extends JPanel implements Observador {

    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);
    private static final String RUTA_RECURSO_MAPA = "/mapa_mundo.png";

    private final BufferedImage imagenMapa;
    private final List<Coordenada> historialTrayectoria = new ArrayList<>();
    private Coordenada posicionActualISS = new Coordenada(28.4818, -16.3201);
    private Coordenada puntoObjetivo = new Coordenada(28.4818, -16.3201);
    private double radioAlertaKm = 1000.0;
    private String horaActualizacion = "--:--:--";

    private Consumer<Coordenada> onMapaClickListener;

    public PanelTelemetria() {
        setBackground(FONDO_PANEL);
        setLayout(new BorderLayout());

        TitledBorder titulo = BorderFactory.createTitledBorder(
                new LineBorder(BORDE_PANEL, 1, true),
                " PanelTelemetria (Observador) ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                Color.WHITE
        );
        setBorder(new CompoundBorder(titulo, new EmptyBorder(8, 8, 8, 8)));

        this.imagenMapa = cargarMapaOscuroDesdeRecursos();

        LienzoMapa lienzo = new LienzoMapa();
        add(lienzo, BorderLayout.CENTER);
    }

    public void setOnMapaClickListener(Consumer<Coordenada> listener) {
        this.onMapaClickListener = listener;
    }

    @Override
    public void actualizar(Sujeto sujeto) {
        if (sujeto instanceof CentroControlOrbital) {
            CentroControlOrbital control = (CentroControlOrbital) sujeto;
            Coordenada nuevaPos = control.getPosicionISS();

            SwingUtilities.invokeLater(() -> {
                if (historialTrayectoria.isEmpty()
                        || !historialTrayectoria.get(historialTrayectoria.size() - 1).equals(nuevaPos)) {
                    historialTrayectoria.add(nuevaPos);
                    if (historialTrayectoria.size() > 150) {
                        historialTrayectoria.remove(0);
                    }
                }
                this.posicionActualISS = nuevaPos;
                this.puntoObjetivo = control.getPuntoObjetivo();
                this.radioAlertaKm = control.getRadioAlertaKm();
                this.horaActualizacion = control.getHoraUltimaActualizacion();
                repaint();
            });
        }
    }

    private BufferedImage cargarMapaOscuroDesdeRecursos() {
        try (InputStream is = Objects.requireNonNull(
                getClass().getResourceAsStream(RUTA_RECURSO_MAPA),
                "No se encontró el archivo " + RUTA_RECURSO_MAPA + " en resources")) {

            BufferedImage imgOriginal = ImageIO.read(is);
            BufferedImage mapaOscuro = new BufferedImage(
                    imgOriginal.getWidth(), imgOriginal.getHeight(), BufferedImage.TYPE_INT_RGB);

            // Transforma la imagen equirectangular a tonos oscuros azulados/grisáceos de alto contraste
            for (int y = 0; y < imgOriginal.getHeight(); y++) {
                for (int x = 0; x < imgOriginal.getWidth(); x++) {
                    Color c = new Color(imgOriginal.getRGB(x, y));
                    // Detectamos si es océano (predominio de azul) o tierra
                    boolean esOceano = (c.getBlue() > c.getRed() + 12) && (c.getBlue() > c.getGreen());
                    if (esOceano) {
                        mapaOscuro.setRGB(x, y, new Color(18, 22, 29).getRGB());
                    } else {
                        int luminancia = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                        int r = Math.min(255, 44 + (luminancia / 9));
                        int g = Math.min(255, 50 + (luminancia / 9));
                        int b = Math.min(255, 62 + (luminancia / 8));
                        mapaOscuro.setRGB(x, y, new Color(r, g, b).getRGB());
                    }
                }
            }
            return mapaOscuro;
        } catch (Exception e) {
            throw new RuntimeException("Error cargando el recurso del mapa mundial: " + e.getMessage(), e);
        }
    }

    private class LienzoMapa extends JPanel {

        public LienzoMapa() {
            setBackground(new Color(14, 17, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Rectangle areaMapa = calcularAreaMapaFija();
                    if (!areaMapa.contains(e.getPoint())) return;

                    double relX = (e.getX() - areaMapa.x) / (double) areaMapa.width;
                    double relY = (e.getY() - areaMapa.y) / (double) areaMapa.height;

                    double lon = (relX * 360.0) - 180.0;
                    double lat = 90.0 - (relY * 180.0);

                    lat = Math.max(-90.0, Math.min(90.0, lat));
                    lon = Math.max(-180.0, Math.min(180.0, lon));

                    if (onMapaClickListener != null) {
                        onMapaClickListener.accept(new Coordenada(lat, lon));
                    }
                }
            });
        }

        /**
         * Calcula el rectángulo centrado con proporción exacta 2:1 (360° x 180°)
         * para que el mapa nunca se estire ni deforme al cambiar el tamaño de la ventana.
         */
        private Rectangle calcularAreaMapaFija() {
            int panelW = getWidth();
            int panelH = getHeight();
            if (panelW <= 0 || panelH <= 0) return new Rectangle(0, 0, 1, 1);

            double aspectoObjetivo = 2.0; // 360 grados / 180 grados
            int mapW = panelW;
            int mapH = (int) Math.round(panelW / aspectoObjetivo);

            if (mapH > panelH) {
                mapH = panelH;
                mapW = (int) Math.round(panelH * aspectoObjetivo);
            }

            int offsetX = (panelW - mapW) / 2;
            int offsetY = (panelH - mapH) / 2;
            return new Rectangle(offsetX, offsetY, mapW, mapH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            Rectangle area = calcularAreaMapaFija();

            // 1. Imagen del mapamundi equirectangular sin deformación
            g2.drawImage(imagenMapa, area.x, area.y, area.width, area.height, null);
            dibujarCuadriculaCoordenadas(g2, area);

            // Borde sutil alrededor del área exacta del mapa
            g2.setColor(new Color(65, 72, 85));
            g2.drawRect(area.x, area.y, area.width - 1, area.height - 1);

            // 2. Círculo perfecto de Radio de Alerta alrededor del punto objetivo
            Point2D.Double pObj = coordenadaAPixel(puntoObjetivo, area);
            double gradosAprox = radioAlertaKm / 111.32;
            double radioPx = (gradosAprox / 360.0) * area.width;

            g2.setColor(new Color(220, 53, 69, 45));
            g2.fill(new Ellipse2D.Double(pObj.x - radioPx, pObj.y - radioPx, radioPx * 2, radioPx * 2));
            g2.setColor(new Color(220, 53, 69, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Double(pObj.x - radioPx, pObj.y - radioPx, radioPx * 2, radioPx * 2));

            // 3. Trayectoria orbital amarilla punteada
            if (historialTrayectoria.size() > 1) {
                g2.setColor(new Color(255, 212, 59, 220));
                float[] patronPunteado = {6f, 6f};
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, patronPunteado, 0f));

                for (int i = 1; i < historialTrayectoria.size(); i++) {
                    Coordenada c1 = historialTrayectoria.get(i - 1);
                    Coordenada c2 = historialTrayectoria.get(i);
                    if (Math.abs(c1.getLongitud() - c2.getLongitud()) < 180.0) {
                        Point2D.Double p1 = coordenadaAPixel(c1, area);
                        Point2D.Double p2 = coordenadaAPixel(c2, area);
                        g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    }
                }
            }

            // 4. Pin rojo del punto objetivo
            dibujarPinObjetivo(g2, (int) pObj.x, (int) pObj.y);

            // 5. Icono de la Estación Espacial Internacional (ISS)
            Point2D.Double pISS = coordenadaAPixel(posicionActualISS, area);
            dibujarIconoISS(g2, (int) pISS.x, (int) pISS.y);

            // 6. Recuadro superpuesto inferior izquierdo con telemetría numérica
            dibujarOverlayTelemetria(g2, getHeight());

            g2.dispose();
        }

        private Point2D.Double coordenadaAPixel(Coordenada c, Rectangle area) {
            double x = area.x + ((c.getLongitud() + 180.0) / 360.0) * area.width;
            double y = area.y + ((90.0 - c.getLatitud()) / 180.0) * area.height;
            return new Point2D.Double(x, y);
        }

        private void dibujarCuadriculaCoordenadas(Graphics2D g2, Rectangle area) {
            g2.setColor(new Color(255, 255, 255, 18));
            g2.setStroke(new BasicStroke(1f));
            for (int lon = -150; lon <= 150; lon += 30) {
                int x = area.x + (int) (((lon + 180.0) / 360.0) * area.width);
                g2.drawLine(x, area.y, x, area.y + area.height);
            }
            for (int lat = -60; lat <= 60; lat += 30) {
                int y = area.y + (int) (((90.0 - lat) / 180.0) * area.height);
                g2.drawLine(area.x, y, area.x + area.width, y);
            }
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawLine(area.x, area.y + area.height / 2, area.x + area.width, area.y + area.height / 2);
            g2.drawLine(area.x + area.width / 2, area.y, area.x + area.width / 2, area.y + area.height);
        }

        private void dibujarPinObjetivo(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(235, 64, 52));
            g2.fillOval(x - 7, y - 18, 14, 14);
            int[] xs = {x - 6, x + 6, x};
            int[] ys = {y - 8, y - 8, y};
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 3, y - 14, 6, 6);
        }

        private void dibujarIconoISS(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(255, 212, 59, 70));
            g2.fillOval(x - 16, y - 16, 32, 32);

            g2.setColor(new Color(100, 181, 246));
            g2.fillRect(x - 14, y - 7, 5, 14);
            g2.fillRect(x + 9, y - 7, 5, 14);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x - 14, y, x + 14, y);
            g2.fillRect(x - 3, y - 4, 6, 8);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(255, 212, 59));
            g2.drawString("ISS", x + 16, y + 4);
        }

        private void dibujarOverlayTelemetria(Graphics2D g2, int panelH) {
            int boxX = 14;
            int boxY = panelH - 88;
            int boxW = 245;
            int boxH = 74;

            g2.setColor(new Color(22, 26, 33, 220));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
            g2.setColor(new Color(80, 88, 102));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

            String hemLat = posicionActualISS.getLatitud() >= 0 ? "N" : "S";
            String hemLon = posicionActualISS.getLongitud() >= 0 ? "E" : "O";

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString(String.format("Latitud:  %.4f %s", Math.abs(posicionActualISS.getLatitud()), hemLat), boxX + 12, boxY + 22);
            g2.drawString(String.format("Longitud: %.4f %s", Math.abs(posicionActualISS.getLongitud()), hemLon), boxX + 12, boxY + 42);

            g2.setColor(new Color(165, 175, 190));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString("Clic en mapa para fijar objetivo (" + horaActualizacion + ")", boxX + 12, boxY + 62);
        }
    }
}