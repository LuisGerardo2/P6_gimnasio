package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Nivel;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Gestión de miembros: alta, edición, baja y consulta de su actividad. */
final class PanelMiembros extends Vista {

    private final Gimnasio gimnasio;
    private final Runnable alCambiar;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Correo", "Nivel", "Inscripciones", "Total concluido"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0, 4 -> Integer.class;
                case 5 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextField buscar = Tema.campo(24, "Buscar por nombre o correo…");
    private final JLabel contador = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
    private final JButton editar = Tema.botonSecundario("Editar");
    private final JButton eliminar = Tema.boton("Eliminar", Tema.PELIGRO);

    PanelMiembros(Gimnasio gimnasio, Runnable alCambiar) {
        super("Miembros", "Altas, cambios de nivel y bajas de socios del gimnasio");
        this.gimnasio = gimnasio;
        this.alCambiar = alCambiar;

        JButton nuevo = Tema.boton("＋  Nuevo miembro", Tema.PRIMARIO);
        nuevo.addActionListener(e -> formulario(null));
        agregarAccion(editar);
        agregarAccion(eliminar);
        agregarAccion(nuevo);
        editar.addActionListener(e -> editarSeleccion());
        eliminar.addActionListener(e -> eliminarSeleccion());

        buscar.setToolTipText("Buscar por nombre o correo");
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refrescar(); }
            public void removeUpdate(DocumentEvent e) { refrescar(); }
            public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);
        izq.add(buscar);
        barra.add(izq, BorderLayout.WEST);
        barra.add(contador, BorderLayout.EAST);

        Tema.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] anchos = {50, 200, 250, 150, 100, 120};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarSeleccion();
                }
            }
        });

        Tema.Tarjeta tarjeta = new Tema.Tarjeta(new BorderLayout(0, 12));
        tarjeta.setBorder(Tema.margen(14, 14));
        tarjeta.add(barra, BorderLayout.NORTH);
        tarjeta.add(Tema.scroll(tabla), BorderLayout.CENTER);
        add(tarjeta, BorderLayout.CENTER);

        refrescar();
    }

    @Override
    void refrescar() {
        Integer idSel = seleccionado() != null ? seleccionado().id() : null;
        String q = buscar.getText().trim().toLowerCase();
        List<Inscripcion> regs = gimnasio.registros();
        List<Miembro> lista = gimnasio.miembros().stream()
                .filter(m -> q.isEmpty() || m.nombre().toLowerCase().contains(q) || m.correo().toLowerCase().contains(q))
                .toList();
        modelo.setRowCount(0);
        for (Miembro m : lista) {
            int cuantas = (int) regs.stream().filter(i -> i.miembro().equals(m)).count();
            double total = regs.stream().filter(i -> i.estado() == Estado.CERRADA && i.miembro().equals(m))
                    .mapToDouble(Inscripcion::total).sum();
            modelo.addRow(new Object[]{m.id(), m.nombre(), m.correo(),
                    m.nivel().etiqueta() + (m.nivel().descuento() > 0
                            ? " (−" + Math.round(m.nivel().descuento() * 100) + " %)" : ""),
                    cuantas, total});
        }
        contador.setText(lista.size() + " de " + gimnasio.miembros().size() + " miembros");
        if (idSel != null) {
            for (int i = 0; i < tabla.getRowCount(); i++) {
                if (idSel.equals(tabla.getValueAt(i, 0))) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hay = seleccionado() != null;
        editar.setEnabled(hay);
        eliminar.setEnabled(hay);
    }

    private Miembro seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        int id = (Integer) tabla.getValueAt(fila, 0);
        return gimnasio.miembros().stream().filter(m -> m.id() == id).findFirst().orElse(null);
    }

    private void editarSeleccion() {
        Miembro m = seleccionado();
        if (m != null) {
            formulario(m);
        }
    }

    private void formulario(Miembro existente) {
        JTextField nombre = Tema.campo(24);
        JTextField correo = Tema.campo(24);
        JComboBox<Nivel> nivel = Tema.combo(Nivel.values());
        if (existente != null) {
            nombre.setText(existente.nombre());
            correo.setText(existente.correo());
            nivel.setSelectedItem(existente.nivel());
        }
        JLabel ayuda = Tema.etiqueta(" ", Tema.F_PEQUENA, Tema.TEXTO_SUAVE);
        Runnable actualizarAyuda = () -> {
            Nivel n = (Nivel) nivel.getSelectedItem();
            ayuda.setText(n.descuento() == 0 ? "Sin descuento en sus inscripciones"
                    : "Descuento del " + Math.round(n.descuento() * 100) + " % en sus inscripciones");
        };
        nivel.addActionListener(e -> actualizarAyuda.run());
        actualizarAyuda.run();

        JPanel f = new JPanel(new GridLayout(0, 1, 0, 10));
        f.add(Util.campo("Nombre completo", nombre));
        f.add(Util.campo("Correo electrónico", correo));
        f.add(Util.campo("Nivel de membresía", nivel));
        f.add(ayuda);

        boolean ok = Util.dialogo(this, existente == null ? "Nuevo miembro" : "Editar miembro #" + existente.id(), f, () -> {
            String nom = nombre.getText().trim();
            String cor = correo.getText().trim();
            Nivel niv = (Nivel) nivel.getSelectedItem();
            if (existente == null) {
                return Util.intentar(this, () -> gimnasio.registrarMiembro(nom, cor, niv));
            }
            boolean repetido = gimnasio.miembros().stream()
                    .anyMatch(o -> o.id() != existente.id() && o.correo().equalsIgnoreCase(cor));
            if (repetido) {
                Util.info(this, "Ya existe otro miembro con ese correo.");
                return false;
            }
            return Util.intentar(this, () -> {
                gimnasio.editarMiembro(existente.id(), nom, cor, niv);
                // editarMiembro reemplaza la instancia del mapa; las inscripciones conservan la anterior,
                // así que se sincroniza para que sus nombres y descuentos se vean actualizados.
                existente.setNombre(nom);
                existente.setCorreo(cor);
                existente.setNivel(niv);
            });
        });
        if (ok) {
            alCambiar.run();
        }
    }

    private void eliminarSeleccion() {
        Miembro m = seleccionado();
        if (m == null) {
            return;
        }
        long inscripciones = gimnasio.registros().stream().filter(i -> i.miembro().equals(m)).count();
        if (inscripciones > 0) {
            Util.info(this, m.nombre() + " tiene " + inscripciones + " inscripción(es) registrada(s).\n"
                    + "Para conservar el historial no se permite eliminar miembros con inscripciones.");
            return;
        }
        if (Util.confirmar(this, "¿Eliminar a " + m.nombre() + "?")
                && Util.intentar(this, () -> gimnasio.eliminarMiembro(m.id()))) {
            alCambiar.run();
        }
    }
}
