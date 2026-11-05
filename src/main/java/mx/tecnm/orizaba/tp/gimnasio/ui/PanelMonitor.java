package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.eventos.Evento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.TipoEvento;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import java.awt.*;

/** Monitor de renovaciones en segundo plano y bitácora de eventos del dominio. */
final class PanelMonitor extends Vista {

    private final Gimnasio g;
    private final RelojSimulado reloj;

    private final DefaultListModel<Evento> bitacora = new DefaultListModel<>();
    private final JList<Evento> lista = new JList<>(bitacora) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bitacora.isEmpty()) {
                g.setFont(Tema.F_BASE);
                g.setColor(Tema.TEXTO_SUAVE);
                String t = "Aún no hay eventos. Realiza operaciones o inicia el monitor.";
                g.drawString(t, (getWidth() - g.getFontMetrics().stringWidth(t)) / 2, 60);
            }
        }
    };
    private final JSpinner periodo = Tema.spinner(new SpinnerNumberModel(1000, 100, 60000, 100));
    private final JButton alternar = Tema.boton("Iniciar monitor", Tema.EXITO);
    private final JLabel estado = Tema.etiqueta(" ", Tema.F_NEGRITA, Tema.TEXTO);
    private final JLabel fechaSim = Tema.etiqueta(" ", Tema.F_SUBTITULO, Tema.TEXTO);

    PanelMonitor(Gimnasio g, RelojSimulado reloj, Runnable alCambiar) {
        super("Monitor y eventos", "Hilo de fondo que cancela inscripciones vacías y avisa renovaciones vencidas");
        this.g = g;
        this.reloj = reloj;

        // ---- control del monitor
        alternar.addActionListener(e -> alternarMonitor());
        JButton revisar = Tema.botonSecundario("Revisar ahora");
        revisar.setToolTipText("Ejecuta un ciclo de revisión inmediatamente");
        revisar.addActionListener(e -> {
            Integer n = Util.obtener(this, g::revisar);
            if (n != null) {
                registrarLocal("Revisión manual: " + n + " acción(es) realizada(s)");
                alCambiar.run();
            }
        });

        JPanel controlMonitor = new JPanel();
        controlMonitor.setOpaque(false);
        controlMonitor.setLayout(new BoxLayout(controlMonitor, BoxLayout.Y_AXIS));
        controlMonitor.add(Tema.etiqueta("Monitor de renovaciones", Tema.F_SUBTITULO, Tema.TEXTO));
        controlMonitor.add(Box.createVerticalStrut(6));
        controlMonitor.add(izq(estado));
        controlMonitor.add(Box.createVerticalStrut(14));
        controlMonitor.add(izq(Util.campo("Periodo de revisión (ms)", periodo)));
        controlMonitor.add(Box.createVerticalStrut(12));
        JPanel fila = new JPanel(new GridLayout(1, 2, 8, 0));
        fila.setOpaque(false);
        fila.add(alternar);
        fila.add(revisar);
        controlMonitor.add(izq(fila));

        // ---- reloj simulado
        JButton mas1 = Tema.botonSecundario("+1 día");
        JButton mas31 = Tema.botonSecundario("+31 días");
        JButton cero = Tema.botonSecundario("Reiniciar");
        mas1.addActionListener(e -> mover(1));
        mas31.addActionListener(e -> mover(Gimnasio.PLAZO_DIAS + 1));
        cero.addActionListener(e -> {
            reloj.reiniciar();
            actualizarFecha();
        });
        JPanel filaReloj = new JPanel(new GridLayout(1, 3, 8, 0));
        filaReloj.setOpaque(false);
        filaReloj.add(mas1);
        filaReloj.add(mas31);
        filaReloj.add(cero);

        JPanel controlReloj = new JPanel();
        controlReloj.setOpaque(false);
        controlReloj.setLayout(new BoxLayout(controlReloj, BoxLayout.Y_AXIS));
        controlReloj.add(Tema.etiqueta("Reloj de simulación", Tema.F_SUBTITULO, Tema.TEXTO));
        controlReloj.add(Box.createVerticalStrut(4));
        controlReloj.add(izq(Tema.etiqueta("<html>Adelanta la fecha del sistema para probar<br>alertas (plazo de "
                + Gimnasio.PLAZO_DIAS + " días) y cancelaciones.</html>", Tema.F_PEQUENA, Tema.TEXTO_SUAVE)));
        controlReloj.add(Box.createVerticalStrut(12));
        controlReloj.add(izq(fechaSim));
        controlReloj.add(Box.createVerticalStrut(12));
        controlReloj.add(izq(filaReloj));

        JPanel lateral = new JPanel();
        lateral.setOpaque(false);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.add(tarjeta(controlMonitor));
        lateral.add(Box.createVerticalStrut(16));
        lateral.add(tarjeta(controlReloj));
        lateral.setPreferredSize(new Dimension(330, 100));

        // ---- bitácora
        lista.setCellRenderer(new RenderEvento());
        lista.setBorder(BorderFactory.createEmptyBorder());
        lista.setFixedCellHeight(42);
        lista.setSelectionBackground(Tema.SELECCION);
        JButton limpiar = Tema.botonSecundario("Limpiar");
        limpiar.addActionListener(e -> bitacora.clear());
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(Tema.etiqueta("Bitácora de eventos", Tema.F_SUBTITULO, Tema.TEXTO), BorderLayout.WEST);
        cab.add(limpiar, BorderLayout.EAST);
        Tema.Tarjeta centro = new Tema.Tarjeta(new BorderLayout(0, 12));
        centro.setBorder(Tema.margen(14, 14));
        centro.add(cab, BorderLayout.NORTH);
        centro.add(Tema.scroll(lista), BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new BorderLayout(16, 0));
        cuerpo.setOpaque(false);
        cuerpo.add(centro, BorderLayout.CENTER);
        cuerpo.add(lateral, BorderLayout.EAST);
        add(cuerpo, BorderLayout.CENTER);

        refrescar();
    }

    private static JComponent izq(JComponent c) {
        return Tema.acotar(c);
    }

    private static JPanel tarjeta(JComponent contenido) {
        Tema.Tarjeta t = new Tema.Tarjeta(new BorderLayout());
        t.add(contenido, BorderLayout.CENTER);
        return Tema.acotar(t);
    }

    private void mover(int dias) {
        reloj.avanzar(dias);
        actualizarFecha();
        registrarLocal("Reloj simulado adelantado " + dias + " día(s)");
    }

    private void actualizarFecha() {
        fechaSim.setText(Util.FECHA.format(reloj.hoy()) + (reloj.dias() == 0 ? "  (hoy)" : "  (+" + reloj.dias() + " d)"));
    }

    private void alternarMonitor() {
        if (g.monitorActivo()) {
            try {
                g.detenerMonitor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            int ms = (Integer) periodo.getValue();
            Util.intentar(this, () -> g.iniciarMonitor(ms));
        }
        refrescar();
    }

    /** Llamado desde el hilo de eventos de Swing cuando el servicio publica un evento. */
    void registrar(Evento e) {
        bitacora.add(0, e);
        while (bitacora.size() > 300) {
            bitacora.removeElementAt(bitacora.size() - 1);
        }
    }

    private void registrarLocal(String mensaje) {
        registrar(Evento.de(TipoEvento.CAMBIO_ESTADO, mensaje));
    }

    @Override
    void refrescar() {
        boolean activo = g.monitorActivo();
        estado.setText(activo ? "● En ejecución" : "● Detenido");
        estado.setForeground(activo ? Tema.EXITO : Tema.TEXTO_SUAVE);
        alternar.setText(activo ? "Detener monitor" : "Iniciar monitor");
        periodo.setEnabled(!activo);
        actualizarFecha();
    }

    static final class RenderEvento extends JPanel implements ListCellRenderer<Evento> {
        private final JLabel icono = new JLabel();
        private final JLabel texto = new JLabel();
        private final JLabel hora = new JLabel();

        RenderEvento() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            icono.setFont(Tema.fuente(Font.BOLD, 11));
            icono.setPreferredSize(new Dimension(190, 20));
            texto.setFont(Tema.F_BASE);
            hora.setFont(Tema.F_MONO);
            hora.setForeground(Tema.TEXTO_SUAVE);
            add(icono, BorderLayout.WEST);
            add(texto, BorderLayout.CENTER);
            add(hora, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Evento> l, Evento e, int i, boolean sel, boolean foco) {
            Color c = switch (e.tipo()) {
                case ALTA -> Tema.EXITO;
                case CAMBIO_ESTADO -> Tema.INFO;
                case ALERTA -> Tema.AVISO;
                case CANCELACION_AUTOMATICA -> Tema.PELIGRO;
            };
            icono.setText("●  " + switch (e.tipo()) {
                case ALTA -> "ALTA";
                case CAMBIO_ESTADO -> "CAMBIO DE ESTADO";
                case ALERTA -> "ALERTA";
                case CANCELACION_AUTOMATICA -> "CANCELACIÓN AUTOMÁTICA";
            });
            icono.setForeground(c);
            texto.setText(e.mensaje());
            texto.setForeground(Tema.TEXTO);
            hora.setText(Util.HORA.format(e.instante()));
            setBackground(sel ? Tema.SELECCION : i % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA);
            return this;
        }
    }
}
