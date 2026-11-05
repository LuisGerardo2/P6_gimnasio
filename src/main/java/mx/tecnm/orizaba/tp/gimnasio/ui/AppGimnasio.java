package mx.tecnm.orizaba.tp.gimnasio.ui;

import mx.tecnm.orizaba.tp.gimnasio.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import javax.swing.*;

/**
 * Punto de entrada de la interfaz gráfica.
 * Uso: {@code java -cp ...:sqlite-jdbc.jar mx.tecnm.orizaba.tp.gimnasio.ui.AppGimnasio [archivo.db]}
 */
public final class AppGimnasio {

    private AppGimnasio() {
    }

    public static void main(String[] args) {
        String ruta = args.length > 0 ? args[0] : "datos.db";
        SwingUtilities.invokeLater(() -> iniciar(ruta));
    }

    private static void iniciar(String ruta) {
        Tema.instalar();
        ConexionSQLite cx = null;
        Gimnasio gimnasio = null;
        try {
            RelojSimulado reloj = new RelojSimulado();
            cx = new ConexionSQLite(ruta);
            gimnasio = new Gimnasio(cx, reloj::hoy);

            final ConexionSQLite conexion = cx;
            final Gimnasio servicio = gimnasio;
            VentanaPrincipal ventana = new VentanaPrincipal(servicio, reloj, ruta, () -> {
                servicio.close();      // detiene el monitor
                conexion.close();
                System.exit(0);
            });
            ventana.setVisible(true);
        } catch (Throwable t) {
            if (gimnasio != null) {
                gimnasio.close();
            }
            if (cx != null) {
                cx.close();
            }
            String causa = t.getMessage() != null ? t.getMessage() : t.toString();
            JOptionPane.showMessageDialog(null,
                    "No se pudo iniciar la aplicación:\n" + causa
                            + "\n\nVerifica que sqlite-jdbc esté en el classpath.",
                    "Error de inicio", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
