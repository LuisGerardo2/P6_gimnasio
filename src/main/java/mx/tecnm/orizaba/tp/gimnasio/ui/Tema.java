package mx.tecnm.orizaba.tp.gimnasio.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Paleta, tipografía y fábrica de componentes con estilo propio (sin dependencias externas). */
public final class Tema {

    private Tema() {
    }

    public static final Color FONDO = new Color(0xF4F6FA);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color BORDE = new Color(0xE2E6EE);
    public static final Color LATERAL = new Color(0x0F172A);
    public static final Color LATERAL_HOVER = new Color(0x1E293B);
    public static final Color PRIMARIO = new Color(0x4F46E5);
    public static final Color PRIMARIO_OSCURO = new Color(0x4338CA);
    public static final Color TEXTO = new Color(0x1E293B);
    public static final Color TEXTO_SUAVE = new Color(0x64748B);
    public static final Color EXITO = new Color(0x16A34A);
    public static final Color AVISO = new Color(0xD97706);
    public static final Color PELIGRO = new Color(0xDC2626);
    public static final Color INFO = new Color(0x0284C7);
    public static final Color SELECCION = new Color(0xE0E7FF);
    public static final Color FILA_ALTERNA = new Color(0xF8FAFC);

    public static final Color[] SERIE = {
            new Color(0x4F46E5), new Color(0x0EA5E9), new Color(0x10B981),
            new Color(0xF59E0B), new Color(0xEF4444), new Color(0x8B5CF6)
    };

    public static final Font F_BASE = fuente(Font.PLAIN, 13);
    public static final Font F_NEGRITA = fuente(Font.BOLD, 13);
    public static final Font F_TITULO = fuente(Font.BOLD, 22);
    public static final Font F_SUBTITULO = fuente(Font.BOLD, 15);
    public static final Font F_PEQUENA = fuente(Font.PLAIN, 12);
    public static final Font F_KPI = fuente(Font.BOLD, 28);
    public static final Font F_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static Font fuente(int estilo, int tam) {
        return new Font("Segoe UI", estilo, tam);
    }

    /** Instala el aspecto global. Debe invocarse antes de crear componentes. */
    public static void instalar() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se conserva el aspecto por omisión
        }
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("OptionPane.messageFont", F_BASE);
        UIManager.put("OptionPane.buttonFont", F_NEGRITA);
        UIManager.put("Label.font", F_BASE);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TextField.font", F_BASE);
        UIManager.put("ComboBox.font", F_BASE);
        UIManager.put("Spinner.font", F_BASE);
        UIManager.put("Table.font", F_BASE);
        UIManager.put("TextArea.font", F_BASE);
        UIManager.put("ToolTip.font", F_PEQUENA);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Button.font", F_NEGRITA);
    }

    // ------------------------------------------------------------------ componentes

    public static Border margen(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static Border borde(int radio) {
        return new BordeRedondeado(radio, BORDE);
    }

    public static JLabel etiqueta(String texto, Font f, Color c) {
        JLabel l = new JLabel(texto);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    public static JTextField campo(int columnas) {
        return campo(columnas, null);
    }

    /** Campo de texto con texto de ayuda visible mientras está vacío. */
    public static JTextField campo(int columnas, String ayuda) {
        JTextField t = new JTextField(columnas) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (ayuda != null && getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0x94A3B8));
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    g2.drawString(ayuda, in.left, in.top + g2.getFontMetrics().getAscent() + 1);
                    g2.dispose();
                }
            }
        };
        estilizar(t);
        return t;
    }

    public static JSpinner spinner(SpinnerModel modelo) {
        JSpinner s = new JSpinner(modelo);
        estilizar(s);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBorder(null);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setFont(F_BASE);
        s.setPreferredSize(new Dimension(Math.max(s.getPreferredSize().width, 90), 36));
        return s;
    }

    /** En un BoxLayout vertical evita que el componente crezca más allá de su altura natural. */
    public static <T extends JComponent> T acotar(T c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public static void estilizar(JComponent c) {
        c.setFont(F_BASE);
        c.setForeground(TEXTO);
        c.setBackground(SUPERFICIE);
        c.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeado(8, BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    public static <T> JComboBox<T> combo(T[] valores) {
        JComboBox<T> cb = new JComboBox<>(valores);
        cb.setFont(F_BASE);
        cb.setBackground(SUPERFICIE);
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width, 34));
        return cb;
    }

    public static JButton boton(String texto, Color color) {
        return new Boton(texto, color, false);
    }

    public static JButton botonSecundario(String texto) {
        return new Boton(texto, PRIMARIO, true);
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(SUPERFICIE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Aplica el estilo de tabla del sistema: filas altas, cabecera plana y filas alternas. */
    public static void estilizarTabla(JTable t) {
        t.setRowHeight(34);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(SELECCION);
        t.setSelectionForeground(TEXTO);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setFont(F_BASE);
        t.setForeground(TEXTO);
        t.setFocusable(true);

        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, f, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!sel) {
                    setBackground(f % 2 == 0 ? SUPERFICIE : FILA_ALTERNA);
                }
                setForeground(TEXTO);
                if (v instanceof Double d) {
                    setText(Util.dinero(d));
                } else if (v instanceof java.time.LocalDate fecha) {
                    setText(Util.FECHA.format(fecha));
                }
                if (v instanceof Number) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, base);
        t.setDefaultRenderer(Number.class, base);
        t.setDefaultRenderer(Integer.class, base);
        t.setDefaultRenderer(Double.class, base);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 38));
        TableCellRenderer rh = (tb, v, s, f, r, c) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setOpaque(true);
            l.setBackground(new Color(0xF1F5F9));
            l.setForeground(TEXTO_SUAVE);
            l.setFont(fuente(Font.BOLD, 12));
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            return l;
        };
        h.setDefaultRenderer(rh);
    }

    /** Etiqueta tipo "píldora" para estados y niveles. */
    public static JLabel pildora(String texto, Color color) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setFont(fuente(Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return l;
    }

    // ------------------------------------------------------------------ clases de apoyo

    /** Borde de esquinas redondeadas. */
    public static final class BordeRedondeado implements Border {
        private final int radio;
        private final Color color;

        public BordeRedondeado(int radio, Color color) {
            this.radio = radio;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x + .5f, y + .5f, w - 1, h - 1, radio, radio));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Botón plano con esquinas redondeadas, relleno o contorneado. */
    private static final class Boton extends JButton {
        private final Color color;
        private final boolean contorno;
        private boolean encima;

        Boton(String texto, Color color, boolean contorno) {
            super(texto);
            this.color = color;
            this.contorno = contorno;
            setFont(F_NEGRITA);
            setForeground(contorno ? color : Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (!isEnabled()) {
                g2.setColor(new Color(0xE2E8F0));
                g2.fill(forma);
                setForeground(new Color(0x94A3B8));
            } else if (contorno) {
                g2.setColor(encima ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 22) : SUPERFICIE);
                g2.fill(forma);
                g2.setColor(color);
                g2.draw(forma);
                setForeground(color);
            } else {
                g2.setColor(encima ? color.darker() : color);
                g2.fill(forma);
                setForeground(Color.WHITE);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel blanco con borde redondeado (tarjeta). */
    public static class Tarjeta extends JPanel {
        public Tarjeta(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SUPERFICIE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
            g2.setColor(BORDE);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
