package mx.tecnm.orizaba.tp.gimnasio.app;

import mx.tecnm.orizaba.tp.gimnasio.eventos.OyenteEvento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.Evento;
import mx.tecnm.orizaba.tp.gimnasio.funcional.Reglas;
import mx.tecnm.orizaba.tp.gimnasio.modelo.*;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recorrido de consola por el modelo: sirve como referencia de uso para construir la UI.
 * La UI del estudiante debe ofrecer, como mínimo, lo que aquí se muestra.
 */
public final class DemoModelo {

    public static void main(String[] args) throws Exception {
        Path bd = args.length > 0 ? Path.of(args[0]) : Path.of("datos.db");
        AtomicReference<LocalDate> hoy = new AtomicReference<>(LocalDate.now());

        try (ConexionSQLite cx = new ConexionSQLite(bd.toString());
             Gimnasio g = new Gimnasio(cx, hoy::get)) {

            // Suscripción con clase anónima (la UI usaría Platform.runLater / invokeLater aquí).
            g.suscribir(new OyenteEvento() {
                @Override
                public void notificar(Evento e) {
                    System.out.println("   evento " + e + " [" + Thread.currentThread().getName() + "]");
                }
            });

            System.out.println("== Alta de actividades (referencias a constructor)");
            g.registrar(Actividad.ClaseGrupal::new, "Spinning matutino", 120.0, "Spinning");
            g.registrar(Actividad.ClaseGrupal::new, "Yoga restaurativo", 110.0, "Yoga");
            g.registrar(Actividad.Entrenamiento::new, "Entrenamiento funcional", 250.0, 4);
            g.registrar(Actividad.Entrenamiento::new, "Acondicionamiento básico", 200.0, 2);
            g.registrar(Actividad.AccesoLibre::new, "Acceso libre matutino", 60.0, "Matutino");
            g.registrar(Actividad.AccesoLibre::new, "Acceso libre vespertino", 70.0, "Vespertino");

            System.out.println("== miembros");
            g.registrarMiembro("Laura Méndez", "laura.mendez@gmail.com", Nivel.BASICO);
            g.registrarMiembro("Mario Castro", "mcastro@orizaba.tecnm.mx", Nivel.ESTUDIANTE);
            g.registrarMiembro("Nora Fuentes", "nora.f@yahoo.com", Nivel.PREMIUM);

            System.out.println("== Flujo de inscripción");
            Inscripcion t = g.abrir(1);
            g.agregarLinea(t.id(), "CLG-0001", 2);
            g.agregarLinea(t.id(), "ACL-0001", 3);
            g.iniciar(t.id());
            t.forEach(System.out::println);
            g.cerrar(t.id());
            Inscripcion t2 = g.abrir(2);
            g.agregarLinea(t2.id(), "ENT-0001", 1);
            g.iniciar(t2.id());
            g.abrir(3);

            System.out.println("== Reportes");
            System.out.println(g.catalogo().resumen());
            System.out.println("Ingresos por tipo: " + g.ingresosPorTipo());
            System.out.println("Estadísticas: " + g.estadisticas());
            System.out.println("Conteo por estado: " + g.conteoPorEstado());
            g.mejores(3).forEach(System.out::println);
            g.buscar(Reglas.conCapacidad(ConInstructor.class)).stream().map(g::describir).forEach(System.out::println);

            System.out.println("== Monitor en segundo plano (reloj simulado +" + (Gimnasio.PLAZO_DIAS + 1) + " días)");
            g.iniciarMonitor(100);
            hoy.set(hoy.get().plusDays(Gimnasio.PLAZO_DIAS + 1));
            Thread.sleep(400);
            g.detenerMonitor();
            System.out.println("Base de datos: " + bd.toAbsolutePath());
        }
    }
}
