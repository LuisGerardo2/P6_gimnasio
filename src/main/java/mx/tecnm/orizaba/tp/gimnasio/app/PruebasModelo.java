package mx.tecnm.orizaba.tp.gimnasio.app;

import mx.tecnm.orizaba.tp.gimnasio.eventos.Evento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.TipoEvento;
import mx.tecnm.orizaba.tp.gimnasio.funcional.FabricaElemento;
import mx.tecnm.orizaba.tp.gimnasio.funcional.Regla;
import mx.tecnm.orizaba.tp.gimnasio.funcional.Reglas;
import mx.tecnm.orizaba.tp.gimnasio.modelo.*;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.Respaldo;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pruebas de aceptación del modelo (sin dependencias externas). Deben pasar al 100 %
 * antes de comenzar la UI. Ejecutar: {@code java ... mx.tecnm.orizaba.tp.gimnasio.app.PruebasModelo}
 */
public final class PruebasModelo {

    private static int aprobadas;
    private static final List<String> fallidas = new ArrayList<>();

    @FunctionalInterface
    private interface Caso {
        void ejecutar() throws Exception;
    }

    private static void prueba(String nombre, Caso caso) {
        try {
            caso.ejecutar();
            aprobadas++;
            System.out.println("  [OK]    " + nombre);
        } catch (Throwable t) {
            fallidas.add(nombre);
            System.out.println("  [FALLA] " + nombre + " -> " + t);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void cerca(double esperado, double real) {
        verificar(Math.abs(esperado - real) < 0.01, "se esperaba " + esperado + " y se obtuvo " + real);
    }

    private static void lanza(Class<? extends Throwable> tipo, Caso caso) {
        try {
            caso.ejecutar();
        } catch (Throwable t) {
            verificar(tipo.isInstance(t), "se esperaba " + tipo.getSimpleName() + " y se obtuvo " + t);
            return;
        }
        throw new AssertionError("se esperaba " + tipo.getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("pruebas-gimnasio");
        System.out.println("Pruebas del modelo – Gimnasio");

        prueba("T01 CodigoActividad: formato, parse y validación", () -> {
            CodigoActividad c = CodigoActividad.parse("ABC-0042");
            verificar(c.toString().equals("ABC-0042"), "toString");
            verificar(c.compareTo(new CodigoActividad("ABC", 43)) < 0, "orden");
            lanza(IllegalArgumentException.class, () -> new CodigoActividad("ab", 1));
            lanza(IllegalArgumentException.class, () -> CodigoActividad.parse("ABC_1"));
        });

        prueba("T02 Invariantes de Actividad (encapsulamiento)", () -> {
            lanza(ReglaNegocioException.class, () -> new Actividad.ClaseGrupal("x", -1, "Spinning"));
            lanza(ReglaNegocioException.class, () -> new Actividad.ClaseGrupal(" ", 10, "Spinning"));
            lanza(ReglaNegocioException.class, () -> new Actividad.Entrenamiento("x", 10, 6));
            lanza(ReglaNegocioException.class, () -> new Actividad.ClaseGrupal("x", 10, "Spinning").tarifa(0));
        });

        prueba("T03 Polimorfismo: tarifas por subtipo (Template Method)", () -> {
            Contratable[] v = {
                new Actividad.ClaseGrupal("a", 120.0, "Spinning"),
                new Actividad.Entrenamiento("b", 250.0, 4),
                new Actividad.AccesoLibre("c", 60.0, "Matutino") };
            cerca(960.0, v[0].tarifa(8));
            cerca(1200.0, v[1].tarifa(4));
            cerca(720.0, v[2].tarifa(20));
            cerca(Math.round(960.0 * 1.16 * 100.0) / 100.0, v[0].tarifaConImpuesto(8));
        });

        prueba("T04 Clasificación múltiple y colisión de métodos default", () -> {
            Object s1 = new Actividad.ClaseGrupal("a", 1, "Spinning");
            Object s2 = new Actividad.Entrenamiento("b", 1, 4);
            Actividad.AccesoLibre s3 = new Actividad.AccesoLibre("c", 1, "Matutino");
            verificar(s1 instanceof ConInstructor && !(s1 instanceof ConCupo), "ClaseGrupal sólo ConInstructor");
            verificar(s2 instanceof ConCupo && !(s2 instanceof ConInstructor), "Entrenamiento sólo ConCupo");
            verificar(s3 instanceof ConInstructor && s3 instanceof ConCupo, "AccesoLibre ambas");
            String d = s3.descripcionCorta();
            verificar(d.contains("Instructor") && d.contains("Cupo máximo"), "colisión resuelta: " + d);
        });

        prueba("T05 Retorno covariante en copiar()", () -> {
            Actividad.ClaseGrupal a = new Actividad.ClaseGrupal("a", 5, "Spinning");
            Actividad.ClaseGrupal b = a.copiar();   // sin conversión: retorno covariante
            verificar(!a.equals(b) && a.nombre().equals(b.nombre()), "copia con clave nueva");
        });

        prueba("T06 Máquina de estados de Inscripcion", () -> {
            Miembro x = new Miembro(900, "Prueba", "prueba@ito.mx");
            Inscripcion t = new Inscripcion(900, x, LocalDate.now());
            lanza(ReglaNegocioException.class, t::iniciar);
            t.agregar(new Actividad.ClaseGrupal("a", 10, "Spinning"), 1);
            t.iniciar();
            lanza(ReglaNegocioException.class, () -> t.agregar(new Actividad.ClaseGrupal("b", 1, "Spinning"), 1));
            t.cerrar();
            lanza(ReglaNegocioException.class, t::cancelar);
            verificar(t.estado() == Estado.CERRADA, "estado final");
        });

        prueba("T07 Composición: la línea usa el nivel de la envolvente", () -> {
            Nivel n = Nivel.values()[Nivel.values().length - 1];
            Miembro x = new Miembro(901, "Prueba", "p2@ito.mx", n);
            Inscripcion t = new Inscripcion(901, x, LocalDate.now());
            Inscripcion.Linea l = t.agregar(new Actividad.ClaseGrupal("a", 120.0, "Spinning"), 8);
            cerca(n.aplicar(960.0), l.importe());
            verificar(l.contenedor() == t, "Inscripcion.this");
            lanza(ReglaNegocioException.class, () -> t.agregar(l.elemento(), 2));
        });

        prueba("T08 Agregación, iterador oculto, clase anónima y clase local", () -> {
            Actividad a = new Actividad.ClaseGrupal("Alfa", 300, "Spinning");
            Actividad b = new Actividad.Entrenamiento("Beta", 100, 4);
            Programa cat = new Programa(a, b);
            lanza(ReglaNegocioException.class, () -> cat.agregar(a));
            Iterator<Actividad> it = cat.iterator();
            verificar(!it.getClass().getSimpleName().isEmpty() && it.hasNext(), "iterador");
            lanza(UnsupportedOperationException.class, it::remove);
            verificar(cat.ordenados(Programa.porTarifa(1)).get(0) == b, "comparador anónimo");
            verificar(cat.resumen().startsWith("2 "), "clase local: " + cat.resumen());
            verificar(cat.deTipo(Actividad.ClaseGrupal.class).size() == 1, "genéricos acotados");
        });

        prueba("T09 Interfaz funcional propia y composición de reglas", () -> {
            Actividad a = new Actividad.ClaseGrupal("Alfa", 300, "Spinning");
            Actividad b = new Actividad.AccesoLibre("Gamma", 100, "Matutino");
            Programa cat = new Programa(a, b);
            Regla<Actividad> r = Reglas.precioHasta(200).y(Reglas.conCapacidad(ConCupo.class));
            verificar(cat.filtrar(r).equals(List.of(b)), "y");
            verificar(cat.filtrar(r.negar()).equals(List.of(a)), "negar");
            verificar(cat.filtrar(Reglas.nombreContiene("ALF").o(r)).size() == 2, "o");
            FabricaElemento<Actividad.ClaseGrupal, String> f = Actividad.ClaseGrupal::new;
            verificar(f.crear("Delta", 1, "Spinning").tipo().equals("ClaseGrupal"), "referencia a constructor");
        });

        prueba("T10 Serialización: transient y Externalizable", () -> {
            Actividad a = new Actividad.ClaseGrupal("Alfa", 10, "Spinning");
            a.tarifa(1);
            Miembro x = new Miembro(7, "Ada", "ada@ito.mx", Nivel.values()[1]);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
                oos.writeObject(a);
                oos.writeObject(x);
            }
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                Actividad a2 = (Actividad) ois.readObject();
                Miembro x2 = (Miembro) ois.readObject();
                verificar(a2.equals(a) && a2.cotizaciones() == 0 && a.cotizaciones() == 1, "transient");
                verificar(x2.correo().equals("ada@ito.mx") && x2.nivel() == Nivel.values()[1], "externalizable");
            }
        });

        Path bd = tmp.resolve("pruebas.db");
        AtomicReference<LocalDate> hoy = new AtomicReference<>(LocalDate.now());
        int[] ids = new int[2];

        prueba("T11 Servicio + SQLite: alta, flujo y reportes", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Gimnasio g = new Gimnasio(cx, hoy::get);
                List<Evento> eventos = new CopyOnWriteArrayList<>();
                g.suscribir(eventos::add);
                Actividad e1 = g.registrar(Actividad.ClaseGrupal::new, "Uno", 120.0, "Spinning");
                Actividad e3 = g.registrar(Actividad.AccesoLibre::new, "Tres", 60.0, "Matutino");
                Miembro a = g.registrarMiembro("Grace Hopper", "grace@ito.mx", Nivel.values()[0]);
                lanza(ReglaNegocioException.class, () -> g.registrarMiembro("Otra", "GRACE@ito.mx", Nivel.values()[0]));
                Inscripcion t = g.abrir(a.id());
                g.agregarLinea(t.id(), e1.clave().toString(), 8);
                g.agregarLinea(t.id(), e3.clave().toString(), 20);
                g.iniciar(t.id());
                g.cerrar(t.id());
                ids[0] = t.id();
                cerca(960.0 + 720.0, g.estadisticas().getSum());
                verificar(g.ingresosPorTipo().size() == 2, "ingresos por tipo");
                verificar(g.mejores(1).equals(List.of(a)), "mejores");
                verificar(g.masDemandado().isPresent(), "más demandado");
                verificar(g.describir(e1).contains("ClaseGrupal"), "describir");
                verificar(g.proyectar(Actividad::nombre).containsAll(List.of("Uno", "Tres")), "proyectar");
                verificar(eventos.stream().anyMatch(ev -> ev.tipo() == TipoEvento.ALTA), "eventos");
                lanza(ReglaNegocioException.class, () -> g.retirar(e1.clave()));
                ids[1] = g.abrir(a.id()).id();
            }
        });

        prueba("T12 Persistencia: la información sobrevive al reinicio", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Gimnasio g = new Gimnasio(cx, hoy::get);
                verificar(g.catalogo().tamanio() == 2, "catálogo");
                verificar(g.miembros().size() == 1, "miembros");
                Inscripcion t = g.registro(ids[0]);
                verificar(t.estado() == Estado.CERRADA && t.lineas().size() == 2, "registro reconstruido");
                cerca(960.0 + 720.0, t.total());
            }
        });

        prueba("T13 Monitor: reglas, hilo y detención", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Gimnasio g = new Gimnasio(cx, hoy::get);
                List<Evento> eventos = new CopyOnWriteArrayList<>();
                g.suscribir(eventos::add);
                Inscripcion t = g.abrir(g.miembros().get(0).id());
                g.agregarLinea(t.id(), g.catalogo().iterator().next().clave().toString(), 1);
                g.iniciar(t.id());
                hoy.set(hoy.get().plusDays(Gimnasio.PLAZO_DIAS + 1));
                verificar(g.revisar() == 2, "cancelación + alerta");
                verificar(g.registro(ids[1]).estado() == Estado.CANCELADA, "abierta sin líneas cancelada");
                verificar(g.revisar() == 0, "la alerta no se repite");
                g.iniciarMonitor(20);
                Thread.sleep(120);
                verificar(g.monitorActivo(), "hilo activo");
                g.detenerMonitor();
                verificar(!g.monitorActivo(), "hilo detenido");
                long alertas = eventos.stream().filter(ev -> ev.tipo() == TipoEvento.ALERTA).count();
                verificar(alertas == 1, "alertas: " + alertas);
            }
        });

        prueba("T14 Respaldo binario (exportar/importar)", () -> {
            Path archivo = tmp.resolve("respaldo.bin");
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Gimnasio g = new Gimnasio(cx, hoy::get);
                g.exportarRespaldo(archivo);
                Respaldo.Contenido c = Respaldo.importar(archivo);
                verificar(c.catalogo().tamanio() == g.catalogo().tamanio(), "catálogo");
                verificar(c.registros().size() == g.registros().size(), "registros");
                Inscripcion.Linea l = c.registros().get(0).lineas().get(0);
                verificar(l.contenedor() == c.registros().get(0), "clase interna serializada con su envolvente");
            }
        });

        System.out.printf("%nResultado: %d aprobadas, %d fallidas%n", aprobadas, fallidas.size());
        if (!fallidas.isEmpty()) {
            System.exit(1);
        }
    }
}
