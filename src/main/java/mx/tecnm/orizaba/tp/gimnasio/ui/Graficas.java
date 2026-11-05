package mx.tecnm.orizaba.tp.gimnasio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gráficas dibujadas con Java2D (sin librerías externas). */
final class Graficas {

    private Graficas() {
    }

    private static Graphics2D preparar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Barras verticales con valores. */
    static final class Barras extends JComponent {
        private Map<String, Double> datos = new LinkedHashMap<>();

        Barras() {
            setPreferredSize(new Dimension(300, 220));
        }

        void datos(Map<String, Double> d) {
            this.datos = new LinkedHashMap<>(d);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            int w = getWidth(), h = getHeight();
            if (datos.isEmpty()) {
                vacio(g2, w, h, "Sin ingresos todavía (se cuentan inscripciones concluidas)");
                g2.dispose();
                return;
            }
            double max = datos.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (max <= 0) {
                max = 1;
            }
            int izq = 12, der = 12, arriba = 24, abajo = 34;
            int util = h - arriba - abajo;
            g2.setColor(Tema.BORDE);
            for (int i = 0; i <= 4; i++) {
                int y = arriba + util - util * i / 4;
                g2.drawLine(izq, y, w - der, y);
            }
            int n = datos.size();
            int ancho = (w - izq - der) / n;
            int barra = Math.min(70, ancho * 6 / 10);
            int i = 0;
            for (Map.Entry<String, Double> e : datos.entrySet()) {
                int alto = (int) (util * (e.getValue() / max));
                int x = izq + i * ancho + (ancho - barra) / 2;
                int y = arriba + util - alto;
                g2.setColor(Tema.SERIE[i % Tema.SERIE.length]);
                g2.fill(new RoundRectangle2D.Float(x, y, barra, Math.max(alto, 2), 10, 10));
                g2.setFont(Tema.fuente(Font.BOLD, 12));
                g2.setColor(Tema.TEXTO);
                centrado(g2, Util.dinero(e.getValue()), x + barra / 2, y - 6);
                g2.setFont(Tema.F_PEQUENA);
                g2.setColor(Tema.TEXTO_SUAVE);
                centrado(g2, e.getKey(), x + barra / 2, h - 12);
                i++;
            }
            g2.dispose();
        }
    }

    /** Dona con leyenda; los colores se asignan por posición. */
    static final class Dona extends JComponent {
        private Map<String, Long> datos = new LinkedHashMap<>();
        private Map<String, Color> colores = new LinkedHashMap<>();

        Dona() {
            setPreferredSize(new Dimension(300, 220));
        }

        void datos(Map<String, Long> d, Map<String, Color> c) {
            this.datos = new LinkedHashMap<>(d);
            this.colores = new LinkedHashMap<>(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            int w = getWidth(), h = getHeight();
            long total = datos.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                vacio(g2, w, h, "Sin inscripciones registradas");
                g2.dispose();
                return;
            }
            int d = Math.min(h - 16, w / 2);
            int x0 = 12, y0 = (h - d) / 2;
            double inicio = 90;
            for (Map.Entry<String, Long> e : datos.entrySet()) {
                double barrido = -360.0 * e.getValue() / total;
                g2.setColor(colores.getOrDefault(e.getKey(), Tema.PRIMARIO));
                g2.fill(new Arc2D.Double(x0, y0, d, d, inicio, barrido, Arc2D.PIE));
                inicio += barrido;
            }
            int hueco = d * 6 / 10;
            g2.setColor(Tema.SUPERFICIE);
            g2.fillOval(x0 + (d - hueco) / 2, y0 + (d - hueco) / 2, hueco, hueco);
            g2.setColor(Tema.TEXTO);
            g2.setFont(Tema.fuente(Font.BOLD, 24));
            centrado(g2, String.valueOf(total), x0 + d / 2, y0 + d / 2 + 4);
            g2.setFont(Tema.F_PEQUENA);
            g2.setColor(Tema.TEXTO_SUAVE);
            centrado(g2, "inscripciones", x0 + d / 2, y0 + d / 2 + 22);

            int lx = x0 + d + 24, ly = Math.max(20, h / 2 - datos.size() * 14);
            for (Map.Entry<String, Long> e : datos.entrySet()) {
                g2.setColor(colores.getOrDefault(e.getKey(), Tema.PRIMARIO));
                g2.fill(new RoundRectangle2D.Float(lx, ly - 10, 12, 12, 4, 4));
                g2.setColor(Tema.TEXTO);
                g2.setFont(Tema.F_BASE);
                g2.drawString(e.getKey() + "  " + e.getValue(), lx + 20, ly);
                ly += 28;
            }
            g2.dispose();
        }
    }

    private static void centrado(Graphics2D g2, String s, int cx, int y) {
        g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
    }

    private static void vacio(Graphics2D g2, int w, int h, String texto) {
        g2.setFont(Tema.F_BASE);
        g2.setColor(Tema.TEXTO_SUAVE);
        centrado(g2, texto, w / 2, h / 2);
    }
}
