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
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Observador Concreto 1: Renderiza el mapamundi equirectangular en alta resolución,
 * soporta zoom con la rueda del ratón, desplazamiento arrastrando y clic para fijar objetivo.
 */
public class PanelTelemetria extends JPanel implements Observador {

    private static final Color FONDO_PANEL = new Color(33, 37, 43);
    private static final Color BORDE_PANEL = new Color(75, 82, 95);
    private static final String RUTA_RECURSO_MAPA = "/mapa_mundo.png";

    private static final double ZOOM_MIN = 1.0;
    private static final double ZOOM_MAX = 14.0;

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

            BufferedImage imgLeida = ImageIO.read(is);

            // Escalamos 5120x2560
            int w = 5120;
            int h = 2560;
            BufferedImage mapaOscuro = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = mapaOscuro.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(imgLeida, 0, 0, w, h, null);
            g2.dispose();

            int[] pixeles = mapaOscuro.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < pixeles.length; i++) {
                int rgb = pixeles[i];
                int rOrig = (rgb >> 16) & 0xFF;
                int gOrig = (rgb >> 8) & 0xFF;
                int bOrig = rgb & 0xFF;

                boolean esOceano = (bOrig > rOrig + 12) && (bOrig > gOrig);
                if (esOceano) {
                    pixeles[i] = (18 << 16) | (22 << 8) | 29;
                } else {
                    int luminancia = (rOrig + gOrig + bOrig) / 3;
                    int r = Math.min(255, 48 + (luminancia / 8));
                    int g = Math.min(255, 55 + (luminancia / 8));
                    int b = Math.min(255, 68 + (luminancia / 7));
                    pixeles[i] = (r << 16) | (g << 8) | b;
                }
            }
            mapaOscuro.setRGB(0, 0, w, h, pixeles, 0, w);
            return mapaOscuro;
        } catch (Exception e) {
            throw new RuntimeException("Error cargando el recurso del mapa mundial: " + e.getMessage(), e);
        }
    }

    private class LienzoMapa extends JPanel {

        private double zoom = 1.0;
        // Desplazamiento normalizado [0..1] de la esquina superior izquierda visible dentro del mapa
        private double vistaX = 0.0;
        private double vistaY = 0.0;

        private Point puntoPresionado;
        private boolean arrastrando = false;

        public LienzoMapa() {
            setBackground(new Color(14, 17, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    puntoPresionado = e.getPoint();
                    arrastrando = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (puntoPresionado == null || zoom <= 1.0) return;

                    Rectangle area = calcularAreaMapaBase();
                    int dx = e.getX() - puntoPresionado.x;
                    int dy = e.getY() - puntoPresionado.y;

                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                        arrastrando = true;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }

                    if (arrastrando) {
                        vistaX -= dx / (area.width * zoom);
                        vistaY -= dy / (area.height * zoom);
                        limitarVista();
                        puntoPresionado = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    if (!arrastrando) {
                        if (e.getClickCount() == 2) {
                            // Doble clic resetea el zoom a vista global
                            zoom = 1.0;
                            vistaX = 0.0;
                            vistaY = 0.0;
                            repaint();
                        } else {
                            procesarClicObjetivo(e.getPoint());
                        }
                    }
                    arrastrando = false;
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    Rectangle area = calcularAreaMapaBase();
                    if (!area.contains(e.getPoint())) return;

                    // Posición relativa del ratón dentro del área base [0..1]
                    double mouseRelX = (e.getX() - area.x) / (double) area.width;
                    double mouseRelY = (e.getY() - area.y) / (double) area.height;

                    // Coordenada normalizada del mapa bajo el cursor antes del zoom
                    double mapaNormX = vistaX + mouseRelX / zoom;
                    double mapaNormY = vistaY + mouseRelY / zoom;

                    double factor = e.getWheelRotation() < 0 ? 1.25 : 0.8;
                    double nuevoZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * factor));

                    if (Math.abs(nuevoZoom - zoom) > 0.001) {
                        zoom = nuevoZoom;
                        // Recalcular vistaX/vistaY para mantener el punto bajo el cursor en su sitio
                        vistaX = mapaNormX - mouseRelX / zoom;
                        vistaY = mapaNormY - mouseRelY / zoom;
                        limitarVista();
                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
            addMouseWheelListener(mouseHandler);
        }

        private void limitarVista() {
            double maxVista = 1.0 - (1.0 / zoom);
            vistaX = Math.max(0.0, Math.min(maxVista, vistaX));
            vistaY = Math.max(0.0, Math.min(maxVista, vistaY));
        }

        private void procesarClicObjetivo(Point p) {
            Rectangle area = calcularAreaMapaBase();
            if (!area.contains(p)) return;

            double relX = (p.x - area.x) / (double) area.width;
            double relY = (p.y - area.y) / (double) area.height;

            double mapaNormX = vistaX + relX / zoom;
            double mapaNormY = vistaY + relY / zoom;

            double lon = (mapaNormX * 360.0) - 180.0;
            double lat = 90.0 - (mapaNormY * 180.0);

            lat = Math.max(-90.0, Math.min(90.0, lat));
            lon = Math.max(-180.0, Math.min(180.0, lon));

            if (onMapaClickListener != null) {
                onMapaClickListener.accept(new Coordenada(lat, lon));
            }
        }

        /**
         * Calcula el rectángulo centrado con proporción exacta 2:1 (360° x 180°).
         */
        private Rectangle calcularAreaMapaBase() {
            int panelW = getWidth();
            int panelH = getHeight();
            if (panelW <= 0 || panelH <= 0) return new Rectangle(0, 0, 1, 1);

            double aspectoObjetivo = 2.0;
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

            Rectangle area = calcularAreaMapaBase();

            // Recortamos el dibujo exactamente al rectángulo del mapa para que al hacer zoom no se salga
            Shape clipAnterior = g2.getClip();
            g2.setClip(area);

            // 1. Dibujar recorte de alta resolución correspondiente al zoom y desplazamiento actuales
            int imgW = imagenMapa.getWidth();
            int imgH = imagenMapa.getHeight();
            int srcX1 = (int) Math.round(vistaX * imgW);
            int srcY1 = (int) Math.round(vistaY * imgH);
            int srcX2 = (int) Math.round((vistaX + 1.0 / zoom) * imgW);
            int srcY2 = (int) Math.round((vistaY + 1.0 / zoom) * imgH);

            g2.drawImage(imagenMapa,
                    area.x, area.y, area.x + area.width, area.y + area.height,
                    srcX1, srcY1, srcX2, srcY2, null);

            dibujarCuadriculaCoordenadas(g2, area);

            // 2. Círculo perfecto de Radio de Alerta escalado según el zoom
            Point2D.Double pObj = coordenadaAPixel(puntoObjetivo, area);
            double gradosAprox = radioAlertaKm / 111.32;
            double radioPx = (gradosAprox / 360.0) * area.width * zoom;

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

            g2.setClip(clipAnterior);

            // Borde del área del mapa
            g2.setColor(new Color(65, 72, 85));
            g2.drawRect(area.x, area.y, area.width - 1, area.height - 1);

            // 6. Recuadro superpuesto inferior izquierdo con telemetría y nivel de zoom
            dibujarOverlayTelemetria(g2, getHeight());

            g2.dispose();
        }

        private Point2D.Double coordenadaAPixel(Coordenada c, Rectangle area) {
            double normX = (c.getLongitud() + 180.0) / 360.0;
            double normY = (90.0 - c.getLatitud()) / 180.0;

            double x = area.x + ((normX - vistaX) * zoom) * area.width;
            double y = area.y + ((normY - vistaY) * zoom) * area.height;
            return new Point2D.Double(x, y);
        }

        private void dibujarCuadriculaCoordenadas(Graphics2D g2, Rectangle area) {
            g2.setColor(new Color(255, 255, 255, 16));
            g2.setStroke(new BasicStroke(1f));
            for (int lon = -150; lon <= 150; lon += 30) {
                double normX = (lon + 180.0) / 360.0;
                int x = area.x + (int) (((normX - vistaX) * zoom) * area.width);
                g2.drawLine(x, area.y, x, area.y + area.height);
            }
            for (int lat = -60; lat <= 60; lat += 30) {
                double normY = (90.0 - lat) / 180.0;
                int y = area.y + (int) (((normY - vistaY) * zoom) * area.height);
                g2.drawLine(area.x, y, area.x + area.width, y);
            }
            g2.setColor(new Color(255, 255, 255, 35));
            int yEcuador = area.y + (int) (((0.5 - vistaY) * zoom) * area.height);
            int xGreenwich = area.x + (int) (((0.5 - vistaX) * zoom) * area.width);
            g2.drawLine(area.x, yEcuador, area.x + area.width, yEcuador);
            g2.drawLine(xGreenwich, area.y, xGreenwich, area.y + area.height);
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
            int boxY = panelH - 94;
            int boxW = 285;
            int boxH = 80;

            g2.setColor(new Color(22, 26, 33, 225));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
            g2.setColor(new Color(80, 88, 102));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

            String hemLat = posicionActualISS.getLatitud() >= 0 ? "N" : "S";
            String hemLon = posicionActualISS.getLongitud() >= 0 ? "E" : "O";

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString(String.format("Latitud:  %.4f %s", Math.abs(posicionActualISS.getLatitud()), hemLat), boxX + 12, boxY + 22);
            g2.drawString(String.format("Longitud: %.4f %s   (Zoom: %.1fx)", Math.abs(posicionActualISS.getLongitud()), hemLon, zoom), boxX + 12, boxY + 42);

            g2.setColor(new Color(165, 175, 190));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString("Rueda: zoom | Arrastrar: mover | Doble clic: reset", boxX + 12, boxY + 64);
        }
    }
}