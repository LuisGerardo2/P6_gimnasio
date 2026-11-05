package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.funcional.Regla;
import mx.tecnm.orizaba.tp.gimnasio.funcional.Reglas;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Catálogo de actividades: búsqueda con reglas componibles, alta, edición, retiro y cotizador. */
final class PanelActividades extends Vista {

    private static final String TODOS = "Todos los tipos";
    private static final String[] TIPOS = {TODOS, "Clase grupal", "Entrenamiento", "Acceso libre"};

    private final Gimnasio g;
    private final Runnable alCambiar;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Clave", "Tipo", "Nombre", "Precio", "Característica"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 3 ? Double.class : String.class;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextField buscar = Tema.campo(14, "Buscar por nombre…");
    private final JComboBox<String> tipo = Tema.combo(TIPOS);
    private final JSpinner precioMax = Tema.spinner(new SpinnerNumberModel(0.0, 0.0, 100000.0, 50.0));
    private final JLabel contador = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
    private final JTextArea descripcion = new JTextArea(6, 20);
    private final JSpinner sesiones = Tema.spinner(new SpinnerNumberModel(1, 1, 200, 1));
    private final JLabel cotSin = Tema.etiqueta("—", Tema.F_SUBTITULO, Tema.TEXTO);
    private final JLabel cotCon = Tema.etiqueta("—", Tema.F_SUBTITULO, Tema.PRIMARIO);
    private final JButton editar = Tema.botonSecundario("Editar");
    private final JButton retirar = Tema.boton("Retirar", Tema.PELIGRO);
    private List<Actividad> visibles = List.of();

    PanelActividades(Gimnasio g, Runnable alCambiar) {
        super("Actividades", "Catálogo de clases, entrenamientos y accesos disponibles");
        this.g = g;
        this.alCambiar = alCambiar;

        JButton nueva = Tema.boton("＋  Nueva actividad", Tema.PRIMARIO);
        nueva.addActionListener(e -> nueva());
        agregarAccion(nueva);

        // ---- filtros
        precioMax.setToolTipText("Precio máximo por sesión (0 = sin límite)");
        buscar.setToolTipText("Buscar por nombre");

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);
        filtros.add(buscar);
        filtros.add(tipo);
        filtros.add(Tema.etiqueta("Precio máx.", Tema.F_PEQUENA, Tema.TEXTO_SUAVE));
        filtros.add(precioMax);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(filtros, BorderLayout.WEST);
        barra.add(contador, BorderLayout.EAST);

        buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refrescar(); }
            public void removeUpdate(DocumentEvent e) { refrescar(); }
            public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        tipo.addActionListener(e -> refrescar());
        precioMax.addChangeListener(e -> refrescar());

        // ---- tabla
        Tema.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {100, 120, 220, 100, 190};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMinWidth(100);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                detalle();
            }
        });
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editar();
                }
            }
        });
        Tema.Tarjeta tarjetaTabla = new Tema.Tarjeta(new BorderLayout(0, 12));
        tarjetaTabla.setBorder(Tema.margen(14, 14));
        tarjetaTabla.add(barra, BorderLayout.NORTH);
        tarjetaTabla.add(Tema.scroll(tabla), BorderLayout.CENTER);

        // ---- panel lateral de detalle + cotizador
        descripcion.setEditable(false);
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);
        descripcion.setOpaque(false);
        descripcion.setFont(Tema.F_BASE);
        descripcion.setForeground(Tema.TEXTO);
        descripcion.setText("Selecciona una actividad para ver su detalle.");

        sesiones.addChangeListener(e -> cotizar());

        editar.addActionListener(e -> editar());
        retirar.addActionListener(e -> retirar());
        JPanel acciones = new JPanel(new GridLayout(1, 2, 8, 0));
        acciones.setOpaque(false);
        acciones.add(editar);
        acciones.add(retirar);

        JPanel lateral = new JPanel();
        lateral.setOpaque(false);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.add(Tema.etiqueta("Detalle", Tema.F_SUBTITULO, Tema.TEXTO));
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(alineado(descripcion));
        lateral.add(Box.createVerticalStrut(18));
        lateral.add(Tema.etiqueta("Cotizador", Tema.F_SUBTITULO, Tema.TEXTO));
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(alineado(Util.campo("Número de sesiones", sesiones)));
        lateral.add(Box.createVerticalStrut(12));
        lateral.add(alineado(Tema.etiqueta("Tarifa sin impuestos", Tema.F_PEQUENA, Tema.TEXTO_SUAVE)));
        lateral.add(alineado(cotSin));
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(alineado(Tema.etiqueta("Tarifa con IVA (16 %)", Tema.F_PEQUENA, Tema.TEXTO_SUAVE)));
        lateral.add(alineado(cotCon));
        lateral.add(Box.createVerticalStrut(16));
        lateral.add(alineado(acciones));

        Tema.Tarjeta tarjetaLateral = new Tema.Tarjeta(new BorderLayout());
        tarjetaLateral.add(lateral, BorderLayout.CENTER);
        tarjetaLateral.setPreferredSize(new Dimension(270, 100));

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(tarjetaTabla, BorderLayout.CENTER);
        centro.add(tarjetaLateral, BorderLayout.EAST);
        add(centro, BorderLayout.CENTER);

        refrescar();
    }

    private static JComponent alineado(JComponent c) {
        return Tema.acotar(c);
    }

    private static String caracteristica(Actividad a) {
        return switch (a) {
            case Actividad.ClaseGrupal c -> "Disciplina: " + c.disciplina();
            case Actividad.Entrenamiento e -> "Intensidad " + e.intensidad() + " de 5";
            case Actividad.AccesoLibre l -> "Turno " + l.turno();
        };
    }

    private static String nombreTipo(Actividad a) {
        return switch (a) {
            case Actividad.ClaseGrupal c -> "Clase grupal";
            case Actividad.Entrenamiento e -> "Entrenamiento";
            case Actividad.AccesoLibre l -> "Acceso libre";
        };
    }

    private Regla<Actividad> regla() {
        Regla<Actividad> r = Regla.todas();
        String texto = buscar.getText().trim();
        if (!texto.isEmpty()) {
            r = r.y(Reglas.nombreContiene(texto));
        }
        double max = ((Number) precioMax.getValue()).doubleValue();
        if (max > 0) {
            r = r.y(Reglas.precioHasta(max));
        }
        Class<? extends Actividad> clase = switch (tipo.getSelectedIndex()) {
            case 1 -> Actividad.ClaseGrupal.class;
            case 2 -> Actividad.Entrenamiento.class;
            case 3 -> Actividad.AccesoLibre.class;
            default -> null;
        };
        if (clase != null) {
            r = r.y(Regla.de(clase::isInstance));
        }
        return r;
    }

    @Override
    void refrescar() {
        String claveSel = seleccionada() != null ? seleccionada().clave().toString() : null;
        visibles = g.buscar(regla()).stream()
                .sorted(java.util.Comparator.comparing(Actividad::clave)).toList();
        modelo.setRowCount(0);
        for (Actividad a : visibles) {
            modelo.addRow(new Object[]{a.clave().toString(), nombreTipo(a), a.nombre(), a.precioBase(), caracteristica(a)});
        }
        contador.setText(visibles.size() + " de " + g.catalogo().tamanio() + " actividades");
        if (claveSel != null) {
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (claveSel.equals(tabla.getValueAt(i, 0))) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        detalle();
    }

    private Actividad seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        String clave = (String) tabla.getValueAt(fila, 0);
        return visibles.stream().filter(a -> a.clave().toString().equals(clave)).findFirst().orElse(null);
    }

    private void detalle() {
        Actividad a = seleccionada();
        editar.setEnabled(a != null);
        retirar.setEnabled(a != null);
        if (a == null) {
            descripcion.setText("Selecciona una actividad para ver su detalle.");
            cotSin.setText("—");
            cotCon.setText("—");
            return;
        }
        descripcion.setText(g.describir(a));
        cotizar();
    }

    private void cotizar() {
        Actividad a = seleccionada();
        if (a == null) {
            return;
        }
        int n = (Integer) sesiones.getValue();
        cotSin.setText(Util.dinero(a.tarifa(n)));
        cotCon.setText(Util.dinero(a.tarifaConImpuesto(n)));
    }

    // ------------------------------------------------------------------ operaciones

    private void nueva() {
        JComboBox<String> t = Tema.combo(new String[]{"Clase grupal", "Entrenamiento", "Acceso libre"});
        JTextField nombre = Tema.campo(22);
        JTextField precio = Tema.campo(10);
        JTextField texto = Tema.campo(12);
        JSpinner intensidad = Tema.spinner(new SpinnerNumberModel(3, 1, 5, 1));
        JComboBox<String> turno = Tema.combo(new String[]{"Matutino", "Vespertino"});

        CardLayout cartas = new CardLayout();
        JPanel atributo = new JPanel(cartas);
        atributo.setOpaque(false);
        atributo.add(Util.campo("Disciplina", texto), "0");
        atributo.add(Util.campo("Intensidad (1 a 5)", intensidad), "1");
        atributo.add(Util.campo("Turno", turno), "2");
        t.addActionListener(e -> cartas.show(atributo, String.valueOf(t.getSelectedIndex())));

        JPanel f = new JPanel(new GridLayout(0, 1, 0, 10));
        f.add(Util.campo("Tipo de actividad", t));
        f.add(Util.campo("Nombre", nombre));
        f.add(Util.campo("Precio por sesión (MXN)", precio));
        f.add(atributo);

        boolean ok = Util.dialogo(this, "Nueva actividad", f, () -> {
            Double p = leerPrecio(precio.getText());
            if (nombre.getText().isBlank() || p == null) {
                Util.info(this, "Captura un nombre y un precio válido (número mayor o igual a 0).");
                return false;
            }
            String nom = nombre.getText().trim();
            return Util.intentar(this, () -> {
                switch (t.getSelectedIndex()) {
                    case 0 -> g.registrar(Actividad.ClaseGrupal::new, nom, p, texto.getText().trim());
                    case 1 -> g.registrar(Actividad.Entrenamiento::new, nom, p, (Integer) intensidad.getValue());
                    default -> g.registrar(Actividad.AccesoLibre::new, nom, p, (String) turno.getSelectedItem());
                }
            });
        });
        if (ok) {
            alCambiar.run();
        }
    }

    private void editar() {
        Actividad a = seleccionada();
        if (a == null) {
            return;
        }
        JTextField nombre = Tema.campo(22);
        nombre.setText(a.nombre());
        JTextField precio = Tema.campo(10);
        precio.setText(String.valueOf(a.precioBase()));
        JPanel f = new JPanel(new GridLayout(0, 1, 0, 10));
        f.add(Util.campo("Clave (no editable)", Tema.etiqueta(a.clave() + " · " + nombreTipo(a), Tema.F_NEGRITA, Tema.TEXTO)));
        f.add(Util.campo("Nombre", nombre));
        f.add(Util.campo("Precio por sesión (MXN)", precio));
        boolean ok = Util.dialogo(this, "Editar actividad", f, () -> {
            Double p = leerPrecio(precio.getText());
            if (nombre.getText().isBlank() || p == null) {
                Util.info(this, "Captura un nombre y un precio válido (número mayor o igual a 0).");
                return false;
            }
            return Util.intentar(this, () -> g.actualizar(a, nombre.getText().trim(), p));
        });
        if (ok) {
            alCambiar.run();
        }
    }

    private void retirar() {
        Actividad a = seleccionada();
        if (a == null || !Util.confirmar(this, "¿Retirar «" + a.nombre() + "» del catálogo?")) {
            return;
        }
        if (Util.intentar(this, () -> g.retirar(a.clave()))) {
            alCambiar.run();
        }
    }

    private static Double leerPrecio(String s) {
        try {
            double v = Double.parseDouble(s.trim().replace(",", "."));
            return Double.isFinite(v) && v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
