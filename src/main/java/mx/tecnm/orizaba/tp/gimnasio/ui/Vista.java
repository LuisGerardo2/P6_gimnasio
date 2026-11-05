package mx.tecnm.orizaba.tp.gimnasio.ui;

import javax.swing.*;
import java.awt.*;

/** Base de cada pantalla: encabezado (título + subtítulo + acciones) y método {@link #refrescar()}. */
abstract class Vista extends JPanel {

    private final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    Vista(String titulo, String subtitulo) {
        super(new BorderLayout(0, 18));
        setBackground(Tema.FONDO);
        setBorder(Tema.margen(26, 30));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(Tema.etiqueta(titulo, Tema.F_TITULO, Tema.TEXTO));
        textos.add(Box.createVerticalStrut(2));
        textos.add(Tema.etiqueta(subtitulo, Tema.F_BASE, Tema.TEXTO_SUAVE));

        acciones.setOpaque(false);
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(acciones, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);
    }

    void agregarAccion(JComponent c) {
        acciones.add(c);
    }

    /** Vuelve a leer los datos del servicio y repinta. */
    abstract void refrescar();
}
