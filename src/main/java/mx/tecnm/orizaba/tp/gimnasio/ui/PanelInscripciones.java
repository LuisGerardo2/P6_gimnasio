package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Inscripciones: lista maestra y detalle con líneas, totales y ciclo de vida (iniciar, cerrar, cancelar). */
final class PanelInscripciones extends Vista {

    private static final String TODAS = "Todos los estados";

    private final Gimnasio g;
    private final Runnable alCambiar;

    // ---- lista
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Folio", "Miembro", "Fecha", "Estado", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0 -> Integer.class;
                case 2 -> java.time.LocalDate.class;
                case 3 -> Estado.class;
                case 4 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JComboBox<Object> filtro = Tema.combo(filtros());

    // ---- detalle
    private final JLabel titulo = Tema.etiqueta("Selecciona una inscripción", Tema.F_SUBTITULO, Tema.TEXTO);
    private final JLabel subtitulo = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
    private final JPanel zonaEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final DefaultTableModel modeloLineas = new DefaultTableModel(
            new String[]{"Actividad", "Sesiones", "Subtotal", "Importe"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? String.class : c == 1 ? Integer.class : Double.class;
        }
    };
    private final JTable tablaLineas = new JTable(modeloLineas);
    private final JComboBox<Actividad> comboActividad = Tema.combo(new Actividad[0]);
    private final JSpinner sesiones = Tema.spinner(new SpinnerNumberModel(1, 1, 200, 1));
    private final JButton agregar = Tema.botonSecundario("Agregar");
    private final JButton quitar = Tema.botonSecundario("Quitar línea");
    private final JButton iniciar = Tema.boton("Iniciar", Tema.EXITO);
    private final JButton cerrar = Tema.boton("Concluir", Tema.PRIMARIO);
    private final JButton cancelar = Tema.boton("Cancelar", Tema.PELIGRO);
    private final JLabel lSubtotal = Tema.etiqueta("—", Tema.F_BASE, Tema.TEXTO);
    private final JLabel lDescuento = Tema.etiqueta("—", Tema.F_BASE, Tema.EXITO);
    private final JLabel lTotal = Tema.etiqueta("—", Tema.fuente(Font.BOLD, 20), Tema.TEXTO);

    private List<Inscripcion.Linea> lineasActuales = List.of();
    private boolean cargandoCombo;

    PanelInscripciones(Gimnasio g, Runnable alCambiar) {
        super("Inscripciones", "Flujo completo: abrir, agregar actividades, iniciar y concluir");
        this.g = g;
        this.alCambiar = alCambiar;

        JButton nueva = Tema.boton("＋  Nueva inscripción", Tema.PRIMARIO);
        nueva.addActionListener(e -> nueva());
        agregarAccion(nueva);

        add(construirCentro(), BorderLayout.CENTER);
        refrescar();
    }

    private static Object[] filtros() {
        List<Object> l = new ArrayList<>();
        l.add(TODAS);
        l.addAll(List.of(Estado.values()));
        return l.toArray();
    }

    private JComponent construirCentro() {
        // ---------- izquierda: lista
        Tema.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {55, 140, 95, 110, 90};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getColumnModel().getColumn(3).setCellRenderer(PILDORA);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrar();
            }
        });
        filtro.addActionListener(e -> refrescar());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.setOpaque(false);
        barra.add(Tema.etiqueta("Estado", Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        barra.add(filtro);

        Tema.Tarjeta izq = new Tema.Tarjeta(new BorderLayout(0, 12));
        izq.setBorder(Tema.margen(14, 14));
        izq.add(barra, BorderLayout.NORTH);
        izq.add(Tema.scroll(tabla), BorderLayout.CENTER);

        // ---------- derecha: detalle
        zonaEstado.setOpaque(false);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(subtitulo);
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(textos, BorderLayout.CENTER);
        cab.add(zonaEstado, BorderLayout.EAST);

        Tema.estilizarTabla(tablaLineas);
        tablaLineas.setRowHeight(32);
        tablaLineas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] aw = {190, 80, 90, 90};
        for (int i = 0; i < aw.length; i++) {
            tablaLineas.getColumnModel().getColumn(i).setPreferredWidth(aw[i]);
        }
        tablaLineas.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        JScrollPane spLineas = Tema.scroll(tablaLineas);
        spLineas.setBorder(new Tema.BordeRedondeado(10, Tema.BORDE));

        comboActividad.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Actividad a) {
                    setText(a.clave() + "  ·  " + a.nombre());
                }
                return this;
            }
        });

        JPanel agregarFila = new JPanel(new BorderLayout(8, 0));
        agregarFila.setOpaque(false);
        agregarFila.add(comboActividad, BorderLayout.CENTER);
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        der.add(sesiones);
        der.add(agregar);
        agregarFila.add(der, BorderLayout.EAST);

        JPanel totales = new JPanel(new GridBagLayout());
        totales.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        totales.add(Tema.etiqueta("Subtotal", Tema.F_BASE, Tema.TEXTO_SUAVE), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        totales.add(lSubtotal, c);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        totales.add(Tema.etiqueta("Descuento por nivel", Tema.F_BASE, Tema.TEXTO_SUAVE), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        totales.add(lDescuento, c);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 0, 0, 0);
        totales.add(Tema.etiqueta("Total", Tema.F_SUBTITULO, Tema.TEXTO), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        totales.add(lTotal, c);

        JPanel botonesLinea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botonesLinea.setOpaque(false);
        botonesLinea.add(quitar);
        JPanel flujo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        flujo.setOpaque(false);
        flujo.add(cancelar);
        flujo.add(iniciar);
        flujo.add(cerrar);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(botonesLinea, BorderLayout.WEST);
        pie.add(flujo, BorderLayout.EAST);

        JPanel medio = new JPanel(new BorderLayout(0, 10));
        medio.setOpaque(false);
        medio.add(agregarFila, BorderLayout.NORTH);
        medio.add(spLineas, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, 14));
        abajo.setOpaque(false);
        abajo.add(totales, BorderLayout.NORTH);
        abajo.add(pie, BorderLayout.SOUTH);

        Tema.Tarjeta detalle = new Tema.Tarjeta(new BorderLayout(0, 14));
        detalle.add(cab, BorderLayout.NORTH);
        detalle.add(medio, BorderLayout.CENTER);
        detalle.add(abajo, BorderLayout.SOUTH);
        detalle.setPreferredSize(new Dimension(470, 100));

        agregar.addActionListener(e -> agregarLinea());
        quitar.addActionListener(e -> quitarLinea());
        iniciar.addActionListener(e -> transicion("iniciar", () -> g.iniciar(actual().id())));
        cerrar.addActionListener(e -> transicion("concluir", () -> g.cerrar(actual().id())));
        cancelar.addActionListener(e -> {
            if (Util.confirmar(this, "¿Cancelar la inscripción #" + actual().id() + "?")) {
                transicion("cancelar", () -> g.cancelar(actual().id()));
            }
        });

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(izq, BorderLayout.CENTER);
        centro.add(detalle, BorderLayout.EAST);
        return centro;
    }

    private static final TableCellRenderer PILDORA = new RenderPildora();

    /** Dibuja el estado como una píldora de color sobre el fondo de la fila. */
    private static final class RenderPildora extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private Color fondo = Tema.SUPERFICIE;
        private Color acento = Tema.INFO;

        RenderPildora() {
            setOpaque(false);
            setFont(Tema.fuente(Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
            Estado e = (Estado) v;
            fondo = sel ? Tema.SELECCION : fila % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA;
            acento = Util.colorEstado(e);
            setText(e.etiqueta());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(getText()) + 20;
            int alto = fm.getHeight() + 6;
            int x = 8, y = (getHeight() - alto) / 2;
            g2.setColor(new Color(acento.getRed(), acento.getGreen(), acento.getBlue(), 30));
            g2.fillRoundRect(x, y, ancho, alto, alto, alto);
            g2.setColor(acento);
            g2.drawString(getText(), x + 10, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ datos

    @Override
    void refrescar() {
        Integer idSel = actual() != null ? actual().id() : null;
        List<Inscripcion> lista = filtro.getSelectedItem() instanceof Estado e ? g.registrosEn(e) : g.registros();
        modelo.setRowCount(0);
        for (Inscripcion i : lista) {
            modelo.addRow(new Object[]{i.id(), i.miembro().nombre(), i.fecha(), i.estado(), i.total()});
        }
        if (idSel != null) {
            for (int r = 0; r < tabla.getRowCount(); r++) {
                if (idSel.equals(tabla.getValueAt(r, 0))) {
                    tabla.setRowSelectionInterval(r, r);
                    break;
                }
            }
        }
        mostrar();
    }

    private Inscripcion actual() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        int id = (Integer) tabla.getValueAt(fila, 0);
        return g.registros().stream().filter(i -> i.id() == id).findFirst().orElse(null);
    }

    private void mostrar() {
        Inscripcion i = actual();
        modeloLineas.setRowCount(0);
        zonaEstado.removeAll();
        if (i == null) {
            titulo.setText("Selecciona una inscripción");
            subtitulo.setText("Elige un folio de la lista o crea una nueva");
            lSubtotal.setText("—");
            lDescuento.setText("—");
            lTotal.setText("—");
            lineasActuales = List.of();
            cargarActividades(null);
        } else {
            titulo.setText("Inscripción #" + i.id() + " · " + i.miembro().nombre());
            subtitulo.setText("Abierta el " + Util.FECHA.format(i.fecha()) + " · nivel " + i.miembro().nivel().etiqueta());
            zonaEstado.add(Tema.pildora(i.estado().etiqueta(), Util.colorEstado(i.estado())));
            lineasActuales = i.lineas();
            for (Inscripcion.Linea l : lineasActuales) {
                modeloLineas.addRow(new Object[]{l.elemento().clave() + " · " + l.elemento().nombre(),
                        l.unidades(), l.subtotal(), l.importe()});
            }
            double sub = i.subtotal();
            double tot = i.total();
            lSubtotal.setText(Util.dinero(sub));
            lDescuento.setText(sub - tot > 0.004 ? "− " + Util.dinero(sub - tot) : Util.dinero(0));
            lTotal.setText(Util.dinero(tot));
            cargarActividades(i);
        }
        zonaEstado.revalidate();
        zonaEstado.repaint();
        actualizarBotones();
    }

    /** El combo ofrece sólo actividades que todavía no están en la inscripción. */
    private void cargarActividades(Inscripcion i) {
        cargandoCombo = true;
        comboActividad.removeAllItems();
        if (i != null) {
            List<String> usadas = i.lineas().stream().map(l -> l.elemento().clave().toString()).toList();
            g.catalogo().ordenados(java.util.Comparator.comparing(Actividad::clave)).stream()
                    .filter(a -> !usadas.contains(a.clave().toString()))
                    .forEach(comboActividad::addItem);
        }
        cargandoCombo = false;
    }

    private void actualizarBotones() {
        Inscripcion i = actual();
        Estado e = i == null ? null : i.estado();
        boolean abierta = e == Estado.ABIERTA;
        agregar.setEnabled(abierta && comboActividad.getItemCount() > 0);
        comboActividad.setEnabled(abierta);
        sesiones.setEnabled(abierta);
        quitar.setEnabled(abierta && tablaLineas.getSelectedRow() >= 0);
        iniciar.setEnabled(abierta);
        cerrar.setEnabled(e == Estado.EN_CURSO);
        cancelar.setEnabled(e != null && !e.esTerminal());
    }

    // ------------------------------------------------------------------ operaciones

    private void nueva() {
        List<Miembro> miembros = g.miembros();
        if (miembros.isEmpty()) {
            Util.info(this, "Primero registra al menos un miembro.");
            return;
        }
        JComboBox<Miembro> combo = Tema.combo(miembros.toArray(new Miembro[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Miembro m) {
                    setText("#" + m.id() + "  ·  " + m.nombre() + "  (" + m.nivel().etiqueta() + ")");
                }
                return this;
            }
        });
        JPanel f = new JPanel(new GridLayout(0, 1, 0, 10));
        f.add(Util.campo("Miembro", combo));
        f.add(Tema.etiqueta("La inscripción se crea abierta; después podrás agregar actividades.",
                Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        final Inscripcion[] creada = {null};
        boolean ok = Util.dialogo(this, "Nueva inscripción", f, () -> {
            creada[0] = Util.obtener(this, () -> g.abrir(((Miembro) combo.getSelectedItem()).id()));
            return creada[0] != null;
        });
        if (ok) {
            filtro.setSelectedIndex(0);
            alCambiar.run();
            seleccionarFolio(creada[0].id());
        }
    }

    private void seleccionarFolio(int folio) {
        for (int r = 0; r < tabla.getRowCount(); r++) {
            if (Integer.valueOf(folio).equals(tabla.getValueAt(r, 0))) {
                tabla.setRowSelectionInterval(r, r);
                tabla.scrollRectToVisible(tabla.getCellRect(r, 0, true));
                return;
            }
        }
    }

    private void agregarLinea() {
        Inscripcion i = actual();
        Actividad a = (Actividad) comboActividad.getSelectedItem();
        if (i == null || a == null || cargandoCombo) {
            return;
        }
        int n = (Integer) sesiones.getValue();
        if (Util.intentar(this, () -> g.agregarLinea(i.id(), a.clave().toString(), n))) {
            sesiones.setValue(1);
            alCambiar.run();
        }
    }

    private void quitarLinea() {
        Inscripcion i = actual();
        int fila = tablaLineas.getSelectedRow();
        if (i == null || fila < 0) {
            return;
        }
        Inscripcion.Linea l = lineasActuales.get(tablaLineas.convertRowIndexToModel(fila));
        if (Util.intentar(this, () -> g.quitarLinea(i.id(), l.elemento().clave().toString()))) {
            alCambiar.run();
        }
    }

    private void transicion(String verbo, Runnable accion) {
        if (actual() != null && Util.intentar(this, accion)) {
            alCambiar.run();
        }
    }
}
