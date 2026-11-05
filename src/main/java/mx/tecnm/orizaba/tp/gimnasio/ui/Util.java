package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

/** Utilidades compartidas por los paneles: diálogos, formatos y colores semánticos. */
final class Util {

    private Util() {
    }

    static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(Locale.of("es", "MX"));
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    static String dinero(double v) {
        return MONEDA.format(v);
    }

    static Color colorEstado(Estado e) {
        return switch (e) {
            case ABIERTA -> Tema.INFO;
            case EN_CURSO -> Tema.EXITO;
            case CERRADA -> Tema.TEXTO_SUAVE;
            case CANCELADA -> Tema.PELIGRO;
        };
    }

    static void error(Component padre, Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        JOptionPane.showMessageDialog(padre, msg, "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component padre, String msg) {
        JOptionPane.showMessageDialog(padre, msg, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirmar(Component padre, String msg) {
        return JOptionPane.showConfirmDialog(padre, msg, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Ejecuta una acción del servicio mostrando cualquier error de dominio o persistencia. */
    static boolean intentar(Component padre, Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (RuntimeException ex) {
            error(padre, ex);
            return false;
        }
    }

    /** Evalúa un proveedor que puede fallar; devuelve {@code null} si hubo error (ya notificado). */
    static <T> T obtener(Component padre, Supplier<T> proveedor) {
        try {
            return proveedor.get();
        } catch (RuntimeException ex) {
            error(padre, ex);
            return null;
        }
    }

    /** Fila de formulario: etiqueta arriba y componente debajo. */
    static JPanel campo(String titulo, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(Tema.etiqueta(titulo, Tema.F_PEQUENA, Tema.TEXTO_SUAVE), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Cuadro de diálogo modal con formulario y botones Aceptar/Cancelar. */
    static boolean dialogo(Component padre, String titulo, JPanel formulario, Supplier<Boolean> alAceptar) {
        Window w = SwingUtilities.getWindowAncestor(padre);
        JDialog d = new JDialog(w, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        boolean[] ok = {false};

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(Tema.SUPERFICIE);
        raiz.setBorder(Tema.margen(22, 26));
        raiz.add(Tema.etiqueta(titulo, Tema.F_SUBTITULO, Tema.TEXTO), BorderLayout.NORTH);
        formulario.setOpaque(false);
        raiz.add(formulario, BorderLayout.CENTER);

        JButton aceptar = Tema.boton("Guardar", Tema.PRIMARIO);
        JButton cancelar = Tema.botonSecundario("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(cancelar);
        botones.add(aceptar);
        raiz.add(botones, BorderLayout.SOUTH);

        aceptar.addActionListener(e -> {
            if (alAceptar.get()) {
                ok[0] = true;
                d.dispose();
            }
        });
        cancelar.addActionListener(e -> d.dispose());
        d.getRootPane().setDefaultButton(aceptar);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setContentPane(raiz);
        d.pack();
        d.setMinimumSize(new Dimension(380, d.getHeight()));
        d.setLocationRelativeTo(w);
        d.setVisible(true);
        return ok[0];
    }
}
