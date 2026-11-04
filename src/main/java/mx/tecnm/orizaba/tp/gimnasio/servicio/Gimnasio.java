package mx.tecnm.orizaba.tp.gimnasio.servicio;

import mx.tecnm.orizaba.tp.gimnasio.concurrencia.TareaPeriodica;
import mx.tecnm.orizaba.tp.gimnasio.eventos.Evento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.OyenteEvento;
import mx.tecnm.orizaba.tp.gimnasio.eventos.TipoEvento;
import mx.tecnm.orizaba.tp.gimnasio.funcional.FabricaElemento;
import mx.tecnm.orizaba.tp.gimnasio.funcional.Regla;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Programa;
import mx.tecnm.orizaba.tp.gimnasio.modelo.CodigoActividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Nivel;
import mx.tecnm.orizaba.tp.gimnasio.modelo.ReglaNegocioException;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.Repositorio;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.RepositorioMiembroSQLite;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.RepositorioActividadSQLite;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.RepositorioInscripcionSQLite;
import mx.tecnm.orizaba.tp.gimnasio.persistencia.Respaldo;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Fachada del dominio (capa de servicio). <strong>La UI sólo debe depender de esta clase</strong>
 * y de los tipos del paquete {@code modelo}/{@code eventos}; nunca de JDBC.
 * <ul>
 *   <li>Concurrencia: {@link MonitorRenovaciones} es una clase interna que <em>es una</em>
 *       {@link TareaPeriodica} y <em>tiene acceso</em> a {@code Gimnasio.this}: herencia múltiple de
 *       implementación mediante clases internas (véase {@code J04_Multiple_De_Implementacion}).</li>
 *   <li>Programación funcional: reportes con streams y funciones de orden superior.</li>
 * </ul>
 */
public class Gimnasio implements AutoCloseable {

    /** Días tras los cuales un(a) inscripción en curso genera una alerta. */
    public static final int PLAZO_DIAS = 30;

    private final Programa catalogo = new Programa();
    private final Map<Integer, Miembro> miembros = new ConcurrentHashMap<>();
    private final Map<Integer, Inscripcion> registros = new ConcurrentHashMap<>();
    private final Set<Integer> alertados = ConcurrentHashMap.newKeySet();
    private final List<OyenteEvento> oyentes = new CopyOnWriteArrayList<>();

    private final Repositorio<Actividad, CodigoActividad> repoElementos;
    private final Repositorio<Miembro, Integer> repoMiembros;
    private final Repositorio<Inscripcion, Integer> repoRegistros;
    private final Supplier<LocalDate> reloj;
    private MonitorRenovaciones monitor;

    public Gimnasio(ConexionSQLite bd) {
        this(bd, LocalDate::now);
    }

    /** @param reloj proveedor de la fecha actual (inyectable para pruebas y simulación) */
    public Gimnasio(ConexionSQLite bd, Supplier<LocalDate> reloj) {
       this.repoElementos=new RepositorioActividadSQLite(bd);
       this.repoMiembros=new RepositorioMiembroSQLite(bd);
       this.repoRegistros=new RepositorioInscripcionSQLite(bd,repoMiembros,repoElementos);
       this.reloj=reloj;
       cargar();
        // HECHO TODO E15: Crear los tres repositorios SQLite, asignar el reloj y cargar los datos existentes en memoria.

    }

    private void cargar() {
        repoElementos.todos().forEach(catalogo::agregar);
        repoMiembros.todos().forEach(x->miembros.put(x.id(),x));
        repoRegistros.todos().forEach(x->registros.put(x.id(),x));

        // HECHO TODO E15: Usar referencias a método: repoElementos.todos().forEach(catalogo::agregar), etc.

    }

    // ------------------------------------------------------------------ eventos

    public void suscribir(OyenteEvento oyente) {
        oyentes.add(oyente);
    }

    public void desuscribir(OyenteEvento oyente) {
        oyentes.remove(oyente);
    }

    private void publicar(TipoEvento tipo, String mensaje) {
        Evento e = Evento.de(tipo, mensaje);
        oyentes.forEach(o -> o.notificar(e));
    }

    // ------------------------------------------------------------------ catálogo

    /**
     * Alta genérica de cualquier subtipo mediante una fábrica (función de orden superior).
     * Ejemplo: {@code registrar(Actividad.ClaseGrupal::new, "...", 100.0, "Spinning")}.
     */
    public <E extends Actividad, A> E registrar(FabricaElemento<E, A> fabrica, String nombre, double precio, A atributo) {

        E elemento = fabrica.crear(nombre, precio, atributo);

        repoElementos.guardar(elemento);
        catalogo.agregar(elemento);

        publicar(TipoEvento.ALTA, "Actividad registrada: " + elemento.clave() + " " + elemento.nombre());
        return elemento;

        //HECHO TODO E15: Crear con la fábrica, persistir, agregar al catálogo, publicar ALTA y devolver el elemento.

    }

    public void actualizar(Actividad elemento, String nombre, double precio) {
        elemento.setNombre(nombre);
        elemento.setPrecioBase(precio);
        repoElementos.guardar(elemento);
    }

    public boolean retirar(CodigoActividad clave) {
        boolean referenciado = registros.values().stream().flatMap(t -> t.lineas().stream()).anyMatch(
                l -> l.elemento().clave().equals(clave));

        if (referenciado) {
            throw new ReglaNegocioException("No se puede retirar " + clave + ": está en una inscripción");
        }

        boolean eliminar = repoElementos.eliminar(clave);
        if(eliminar){
            catalogo.retirar(clave);
        }

        return eliminar ;

        // HECHO TODO E15: Impedir retirar elementos referenciados por algún registro; eliminar de la BD y del catálogo.

    }

    public Programa catalogo() {
        return catalogo;
    }

    public Actividad elemento(String clave) {
        return catalogo.buscar(CodigoActividad.parse(clave))
                .orElseThrow(() -> new ReglaNegocioException("No existe " + clave));
    }

    // ------------------------------------------------------------------ miembros

    public Miembro registrarMiembro(String nombre, String correo, Nivel nivel) {
        boolean repetido = miembros.values().stream().anyMatch(s -> s.correo().equalsIgnoreCase(correo));

        if (repetido) {
            throw new ReglaNegocioException("Ya existe un socio con ese correo");
        }

        int id = miembros.values().stream().mapToInt(Miembro::id).max().orElse(0)+1;
        Miembro miembro = new Miembro(id, nombre,correo,nivel);

        repoMiembros.guardar(miembro);
        miembros.put(id,miembro);
        publicar(TipoEvento.ALTA, "Socio registro" +miembro.nombre());

        return miembro;

        //HECHO TODO E15: Rechazar correos repetidos; id = máximo actual + 1 (stream mapToInt/max); persistir y publicar ALTA.

    }

    public void cambiarNivel(int idMiembro, Nivel nivel) {
        Miembro a = miembro(idMiembro);
        a.setNivel(nivel);
        repoMiembros.guardar(a);
    }

    public Miembro miembro(int id) {
        return Optional.ofNullable(miembros.get(id))
                .orElseThrow(() -> new ReglaNegocioException("No existe el registro " + id));
    }

    public List<Miembro> miembros() {
        return miembros.values().stream().sorted(Comparator.comparingInt(Miembro::id)).toList();
    }

    // ------------------------------------------------------------------ inscripciones

    public Inscripcion abrir(int idMiembro) {
        Miembro miembro = miembro(idMiembro);

        int id = registros.values().stream().mapToInt(Inscripcion::id).max().orElse(0)+1;

        Inscripcion inscripcion = new Inscripcion(id, miembro,reloj.get());

        repoRegistros.guardar(inscripcion);
        registros.put(id,inscripcion);

        publicar(TipoEvento.CAMBIO_ESTADO, "prestamo abierto" + id);

        return inscripcion;

        // HECHO TODO E15: Crear el registro con la fecha del reloj, persistir, guardar en el mapa y publicar CAMBIO_ESTADO.

    }

    public Inscripcion.Linea agregarLinea(int idInscripcion, String clave, int unidades) {
        Inscripcion inscripcion = registro(idInscripcion);

        Actividad actividad = elemento(clave);

        Inscripcion.Linea linea = inscripcion.agregar(actividad, unidades);

        repoRegistros.guardar(inscripcion);

        return linea;

        //HECHO TODO E15: Localizar registro y elemento, agregar la línea y persistir.

    }

    public boolean quitarLinea(int idInscripcion, String clave) {
        Inscripcion t = registro(idInscripcion);
        boolean ok = t.quitar(CodigoActividad.parse(clave));
        repoRegistros.guardar(t);
        return ok;
    }

    public void iniciar(int idInscripcion) {
        transitar(idInscripcion, Inscripcion::iniciar);
    }

    public void cerrar(int idInscripcion) {
        transitar(idInscripcion, Inscripcion::cerrar);
    }

    public void cancelar(int idInscripcion) {
        transitar(idInscripcion, Inscripcion::cancelar);
    }

    /** Orden superior: la transición concreta llega como {@link Consumer} (referencia a método). */
    private void transitar(int id, Consumer<Inscripcion> transicion) {
        Inscripcion inscripcion = registro(id);

        transicion.accept(inscripcion);

        repoRegistros.guardar(inscripcion);

        publicar(TipoEvento.CAMBIO_ESTADO, "Inscripcion" + id +"cambio a " + inscripcion.estado());


        //HECHO TODO E15: Aplicar la transición, persistir y publicar CAMBIO_ESTADO.

    }
    
    public Inscripcion registro(int id) {
        return Optional.ofNullable(registros.get(id))
                .orElseThrow(() -> new ReglaNegocioException("No existe el registro " + id));
    }

    public List<Inscripcion> registros() {
        return registros.values().stream().sorted(Comparator.comparingInt(Inscripcion::id)).toList();
    }

    public List<Inscripcion> registrosEn(Estado estado) {
        return registros().stream().filter(t -> t.estado() == estado).toList();
    }

    // ------------------------------------------------------------------ reportes (streams)

    private Stream<Inscripcion> cerrados() {
        return registros.values().stream().filter(t -> t.estado() == Estado.CERRADA);
    }

    /** Ingresos por subtipo (sólo registros cerrados). */
    public Map<String, Double> ingresosPorTipo() {
        return cerrados().flatMap(p-> p.lineas().stream())
                .collect(Collectors.groupingBy(linea -> linea.elemento().tipo(), TreeMap::new, Collectors.summingDouble(Inscripcion.Linea::importe)));

        //HECHO  TODO E16: flatMap de líneas; groupingBy(tipo, TreeMap::new, summingDouble(importe)).

    }

    /** El actividad con más unidades en registros cerrados. */
    public Optional<Actividad> masDemandado() {
        // HECHO TODO E16: groupingBy(elemento, summingInt(unidades)) y máximo por valor con Map.Entry.comparingByValue().
        return cerrados().flatMap(p->p.lineas().stream())
                .collect(Collectors.groupingBy(Inscripcion.Linea::elemento, Collectors.summingInt(Inscripcion.Linea::unidades)))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);

    }

    /** Los {@code n} miembros con mayor importe acumulado. */
    public List<Miembro> mejores(int n) {
        return cerrados().collect(Collectors.groupingBy(Inscripcion::miembro,Collectors.summingDouble(Inscripcion::total)))
                .entrySet().stream().sorted(Map.Entry.<Miembro,Double> comparingByValue().reversed() )
                .limit(n).map(Map.Entry::getKey).toList();
        //HECHO TODO E16: groupingBy(actor, summingDouble(total)); ordenar descendentemente por valor; limit(n).

    }

    public DoubleSummaryStatistics estadisticas() {
        return cerrados().mapToDouble(Inscripcion::total).summaryStatistics();

        // HECHO TODO E16: mapToDouble(Inscripcion::total).summaryStatistics() sobre los cerrados.

    }

    public Map<Boolean, List<Actividad>> particionarPorPrecio(double umbral) {
        // HECHO TODO E16: partitioningBy(e -> e.precioBase() >= umbral).
        return StreamSupport.stream(catalogo.spliterator(),false)
                .collect(Collectors.partitioningBy(e->e.precioBase() >= umbral));

    }

    public Map<Estado, Long> conteoPorEstado() {
        return registros.values().stream()
                .collect(Collectors.groupingBy(Inscripcion::estado, () -> new TreeMap<>(), Collectors.counting()));
    }

    public List<Actividad> buscar(Regla<? super Actividad> regla) {
        return catalogo.filtrar(regla);
    }

    /** Proyección genérica (orden superior). */
    public <R> List<R> proyectar(Function<? super Actividad, ? extends R> funcion) {
        return catalogo.ordenados(Comparator.comparing(Actividad::clave)).stream().<R>map(funcion).toList();

        // HECHO TODO E16: Recorrer el catálogo ordenado por clave y aplicar map(funcion).

    }

    /** Descripción con <em>pattern matching</em> exhaustivo sobre la jerarquía sellada (sin default). */
    public String describir(Actividad e) {
        return switch (e){

                case Actividad.ClaseGrupal claseGrupal -> "ClaseGrupal %s [%s]: %s, $%.2f por sesión (%s)".formatted(
                        claseGrupal.nombre(), claseGrupal.clave(), claseGrupal.disciplina(), claseGrupal.precioBase(), claseGrupal.descripcionCorta());
                case Actividad.Entrenamiento entrenamiento when entrenamiento.intensidad() >= 4 ->
                        "Entrenamiento INTENSO %s [%s]: nivel %d, $%.2f por sesión (%s)".formatted(
                        entrenamiento.nombre(), entrenamiento.clave(), entrenamiento.intensidad(), entrenamiento.precioBase(), entrenamiento.descripcionCorta());
                case Actividad.Entrenamiento entrenamiento ->
                        "Entrenamiento %s [%s]: nivel %d, $%.2f por sesión (%s)".formatted(
                        entrenamiento.nombre(), entrenamiento.clave(), entrenamiento.intensidad(), entrenamiento.precioBase(), entrenamiento.descripcionCorta());
                case Actividad.AccesoLibre accesoLibre ->
                        "AccesoLibre %s [%s]: turno %s, $%.2f por sesión, tope 12 (%s)".formatted(
                        accesoLibre.nombre(), accesoLibre.clave(), accesoLibre.turno(), accesoLibre.precioBase(), accesoLibre.descripcionCorta());
            };
        //HECHO TODO E16: switch (e) con un caso por subtipo y al menos un caso con guarda "when".

    }

    // ------------------------------------------------------------------ monitor

    /**
     * Un ciclo de revisión (síncrono y probado de forma determinista):
     * cancela registros ABIERTOS sin líneas con al menos un día de antigüedad y emite una
     * ALERTA (una sola vez) por cada registro EN_CURSO con más de {@link #PLAZO_DIAS} días.
     *
     * @return número de acciones realizadas
     */
    public synchronized int revisar() {
        int acciones = 0;
        LocalDate hoy = reloj.get();

        for (Inscripcion inscripcion : registros.values()) {

            long dias = inscripcion.diasDesde(hoy);
            switch (inscripcion.estado()) {
                case ABIERTA -> {
                    if (inscripcion.lineas().isEmpty() && dias >= 1) {
                        inscripcion.cancelar();
                        repoRegistros.guardar(inscripcion);
                        publicar(TipoEvento.CANCELACION_AUTOMATICA,
                                "Inscripción sin actividades cancelada: #" + inscripcion.id());
                        acciones++;
                    }
                }
                case EN_CURSO -> {
                    if (dias > PLAZO_DIAS && alertados.add(inscripcion.id())) {
                        publicar(TipoEvento.ALERTA, "Renovación requerida para la inscripción #" + inscripcion.id()
                                + " de " + inscripcion.miembro().nombre());
                        acciones++;
                    }
                }
                case CERRADA, CANCELADA -> {
                    // estados terminales: nada que revisar
                }
            }
        }
        return acciones;
        //HECHO TODO E12: Recorrer los registros y aplicar las dos reglas con un switch sobre el estado.

    }

    public synchronized void iniciarMonitor(long periodoMs) {
        this.monitor = new MonitorRenovaciones(periodoMs);
        this.monitor.iniciar("Monitor renovaciones");


        // HECHO TODO E12: Crear el monitor interno (new MonitorRenovaciones(periodoMs)) e iniciarlo con un nombre de hilo descriptivo.

    }

    public void detenerMonitor() throws InterruptedException {
        MonitorRenovaciones m;
        synchronized (this) {
            m = monitor;
        }
        if (m != null) {
            m.detener();
        }
    }

    public synchronized boolean monitorActivo() {
        return monitor != null && monitor.activa();
    }

    /**
     * Clase interna: ES UNA {@link TareaPeriodica} y TIENE ACCESO a la envolvente.
     */
    public final class MonitorRenovaciones extends TareaPeriodica {

        private MonitorRenovaciones(long periodoMs) {
            super(periodoMs);
        }

        @Override
        protected void ejecutarCiclo() {
            Gimnasio.this.revisar();
            // TODO E12: Delegar en la envolvente: Gimnasio.this.revisar().

        }
    }

    // ------------------------------------------------------------------ respaldo

    public void exportarRespaldo(Path archivo) throws IOException {
        Respaldo.exportar(archivo, catalogo, miembros(), registros());
    }

    /** Detiene el monitor (si está activo). Invocar al cerrar la aplicación. */
    @Override
    public void close() {
        try {
            detenerMonitor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
