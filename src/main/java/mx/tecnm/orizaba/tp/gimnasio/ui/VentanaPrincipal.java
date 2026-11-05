package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.eventos.Evento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.OyenteEvento;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Nivel;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ventana principal: barra lateral de navegación, contenido por tarjetas y barra de estado. */
public final class VentanaPrincipal extends JFrame {

    private final Gimnasio gimnasio;
    private final OyenteEvento oyente = e -> SwingUtilities.invokeLater(() -> alEvento(e));
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private final List<Vista> vistas = new ArrayList<>();
    private final PanelMonitor panelMonitor;
    private final JLabel estado = Tema.etiqueta("Listo", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
    private final ButtonGroup grupo = new ButtonGroup();
    private final JPanel navegacion = new JPanel();

    public VentanaPrincipal(Gimnasio gimnasio, RelojSimulado reloj, String rutaBd, Runnable alCerrar) {
        super("Gimnasio · Sistema de gestión");
        this.gimnasio = gimnasio;

        Runnable refrescarTodo = this::refrescarTodo;
        panelMonitor = new PanelMonitor(gimnasio, reloj, refrescarTodo);
        agregarVista("resumen", "Resumen", new PanelResumen(gimnasio));
        agregarVista("actividades", "Actividades", new PanelActividades(gimnasio, refrescarTodo));
        agregarVista("miembros", "Miembros", new PanelMiembros(gimnasio, refrescarTodo));
        agregarVista("inscripciones", "Inscripciones", new PanelInscripciones(gimnasio, refrescarTodo));
        agregarVista("monitor", "Monitor y eventos", panelMonitor);

        setLayout(new BorderLayout());
        add(construirLateral(rutaBd), BorderLayout.WEST);
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.add(contenido, BorderLayout.CENTER);
        derecha.add(construirEstado(), BorderLayout.SOUTH);
        add(derecha, BorderLayout.CENTER);

        gimnasio.suscribir(oyente);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gimnasio.desuscribir(oyente);
                dispose();
                alCerrar.run();
            }
        });

        setMinimumSize(new Dimension(1180, 740));
        setSize(1320, 820);
        setLocationRelativeTo(null);
        mostrar("resumen");
    }

    // ------------------------------------------------------------------ estructura

    private void agregarVista(String id, String titulo, Vista v) {
        vistas.add(v);
        contenido.add(v, id);
        JToggleButton b = new BotonNavegacion(titulo);
        b.addActionListener(e -> mostrar(id));
        b.setActionCommand(id);
        grupo.add(b);
        navegacion.add(b);
        navegacion.add(Box.createVerticalStrut(4));
    }

    private JComponent construirLateral(String rutaBd) {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(Tema.LATERAL);
        lateral.setPreferredSize(new Dimension(236, 100));
        lateral.setBorder(BorderFactory.createEmptyBorder(24, 16, 18, 16));

        JLabel marca = Tema.etiqueta("GIMNASIO", Tema.fuente(Font.BOLD, 20), Color.WHITE);
        JLabel sub = Tema.etiqueta("Sistema de gestión", Tema.F_PEQUENA, new Color(0x94A3B8));
        JPanel cab = new JPanel();
        cab.setOpaque(false);
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setBorder(BorderFactory.createEmptyBorder(0, 8, 24, 0));
        cab.add(marca);
        cab.add(sub);
        lateral.add(cab, BorderLayout.NORTH);

        navegacion.setOpaque(false);
        navegacion.setLayout(new BoxLayout(navegacion, BoxLayout.Y_AXIS));
        lateral.add(navegacion, BorderLayout.CENTER);

        JButton respaldo = botonLateral("Exportar respaldo…");
        respaldo.addActionListener(e -> exportarRespaldo());
        JButton ejemplo = botonLateral("Cargar datos de ejemplo");
        ejemplo.addActionListener(e -> cargarEjemplo());
        JLabel bd = Tema.etiqueta("<html>BD: " + Path.of(rutaBd).getFileName() + "</html>", Tema.F_PEQUENA, new Color(0x64748B));
        bd.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        pie.add(ejemplo);
        pie.add(Box.createVerticalStrut(6));
        pie.add(respaldo);
        pie.add(bd);
        lateral.add(pie, BorderLayout.SOUTH);
        return lateral;
    }

    private static JButton botonLateral(String texto) {
        JButton b = new JButton(texto);
        b.setFont(Tema.F_BASE);
        b.setForeground(new Color(0xCBD5E1));
        b.setBackground(Tema.LATERAL_HOVER);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        return b;
    }

    private JComponent construirEstado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.SUPERFICIE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), Tema.margen(6, 24)));
        p.add(estado, BorderLayout.WEST);
        return p;
    }

    private void mostrar(String id) {
        cartas.show(contenido, id);
        for (var en = grupo.getElements(); en.hasMoreElements(); ) {
            AbstractButton b = en.nextElement();
            b.setSelected(id.equals(b.getActionCommand()));
        }
        refrescarTodo();
    }

    // ------------------------------------------------------------------ eventos y acciones

    private void refrescarTodo() {
        vistas.forEach(Vista::refrescar);
    }

    /** Se ejecuta en el hilo de Swing; el monitor publica desde otro hilo. */
    private void alEvento(Evento e) {
        panelMonitor.registrar(e);
        estado.setText(Util.HORA.format(e.instante()) + "  ·  " + e);
        refrescarTodo();
    }

    private void exportarRespaldo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar respaldo");
        fc.setSelectedFile(new java.io.File("respaldo-gimnasio.bin"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = fc.getSelectedFile().toPath();
        try {
            gimnasio.exportarRespaldo(destino);
            Util.info(this, "Respaldo guardado en:\n" + destino.toAbsolutePath());
            estado.setText("Respaldo exportado a " + destino.getFileName());
        } catch (IOException | RuntimeException ex) {
            Util.error(this, ex);
        }
    }

    /** Equivale al recorrido de {@code DemoModelo}: útil para explorar la interfaz con datos. */
    private void cargarEjemplo() {
        if (gimnasio.catalogo().tamanio() > 0 || !gimnasio.miembros().isEmpty()) {
            Util.info(this, "Los datos de ejemplo sólo se pueden cargar en una base de datos vacía.");
            return;
        }
        boolean ok = Util.intentar(this, () -> {
            gimnasio.registrar(Actividad.ClaseGrupal::new, "Spinning matutino", 120.0, "Spinning");
            gimnasio.registrar(Actividad.ClaseGrupal::new, "Yoga restaurativo", 110.0, "Yoga");
            gimnasio.registrar(Actividad.Entrenamiento::new, "Entrenamiento funcional", 250.0, 4);
            gimnasio.registrar(Actividad.Entrenamiento::new, "Acondicionamiento básico", 200.0, 2);
            gimnasio.registrar(Actividad.AccesoLibre::new, "Acceso libre matutino", 60.0, "Matutino");
            gimnasio.registrar(Actividad.AccesoLibre::new, "Acceso libre vespertino", 70.0, "Vespertino");
            gimnasio.registrarMiembro("Laura Méndez", "laura.mendez@gmail.com", Nivel.BASICO);
            gimnasio.registrarMiembro("Mario Castro", "mcastro@orizaba.tecnm.mx", Nivel.ESTUDIANTE);
            gimnasio.registrarMiembro("Nora Fuentes", "nora.f@yahoo.com", Nivel.PREMIUM);
            var primera = gimnasio.abrir(1);
            gimnasio.agregarLinea(primera.id(), gimnasio.catalogo().ordenados(java.util.Comparator.comparing(Actividad::clave))
                    .get(0).clave().toString(), 2);
            gimnasio.iniciar(primera.id());
            gimnasio.cerrar(primera.id());
        });
        if (ok) {
            refrescarTodo();
        }
    }

    // ------------------------------------------------------------------ botón de navegación

    private static final class BotonNavegacion extends JToggleButton {
        private boolean encima;

        BotonNavegacion(String texto) {
            super(texto);
            setFont(Tema.fuente(Font.BOLD, 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 12));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
            if (isSelected()) {
                g2.setColor(Tema.PRIMARIO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                setForeground(Color.WHITE);
            } else {
                if (encima) {
                    g2.setColor(Tema.LATERAL_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                setForeground(new Color(0xCBD5E1));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
