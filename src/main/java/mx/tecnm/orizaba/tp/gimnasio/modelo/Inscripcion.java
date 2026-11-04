package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inscripcion: agregado con ciclo de vida ({@link Estado}) compuesto por líneas.
 * <ul>
 *   <li>Composición: {@link Linea} es una clase <strong>interna</strong> (no estática) con constructor
 *       privado; sólo existe dentro de un(a) inscripción y accede a su envolvente con
 *       {@code Inscripcion.this} (véase {@code java_3_tipos_internos/t1_fundamentos/Parcel01..04}).</li>
 *   <li>Concurrencia: los métodos que cambian estado están sincronizados porque el monitor
 *       los consulta desde otro hilo.</li>
 *   <li>Persistencia: serializar un objeto de clase interna exige que la envolvente sea serializable
 *       (véase {@code SerializaClasesInternas_08}).</li>
 * </ul>
 */
public class Inscripcion implements Iterable<Inscripcion.Linea>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final int id;
    private final Miembro miembro;
    private final LocalDate fecha;
    private Estado estado;
    private final List<Linea> lineas = new ArrayList<>();

    {
        estado = Estado.ABIERTA;
    }

    /** Nuevo(a) inscripción con identificador consecutivo y fecha de hoy. */
    public Inscripcion(Miembro miembro) {
        this(SECUENCIA.incrementAndGet(), miembro, LocalDate.now());
    }

    /** Constructor completo (también usado al reconstruir desde la base de datos). */
    public Inscripcion(int id, Miembro miembro, LocalDate fecha) {
        if(id<=0){ throw new ReglaNegocioException("El Id debe ser mayor a 1");}else{
            this.id=id;
            this.miembro=Objects.requireNonNull(miembro);
            this.fecha=Objects.requireNonNull(fecha);
            SECUENCIA.accumulateAndGet(id(),Math::max);
        }

        // TODO E07: Validar id > 0, actor y fecha no nulos; asignar y sincronizar SECUENCIA con accumulateAndGet(id, Math::max).

    }

    public int id() {
        return id;
    }

    public Miembro miembro() {
        return miembro;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public synchronized Estado estado() {
        return estado;
    }

    /** Agrega una línea; sólo es posible mientras está ABIERTA y sin repetir actividad. */
    public synchronized Linea agregar(Actividad elemento, int unidades) {
        if (estado==Estado.ABIERTA) {

            Objects.requireNonNull(elemento,"elemento");
           if( lineas.stream().anyMatch(x->x.elemento.equals(elemento))){
               throw new ReglaNegocioException("Esta cuenta ya esta inscrita en esta actividad");

           }
           else{
               Linea l=new Linea(elemento,unidades);
               lineas.add(l);
               return l;
           }
        }else {throw new ReglaNegocioException("El estado debe estar abierto");}
        // HECHO TODO E07: Exigir estado ABIERTA, rechazar elementos repetidos (stream anyMatch), crear la Linea interna y añadirla.

    }

    /** Quita la línea del actividad indicado (sólo en estado ABIERTA). */
    public synchronized boolean quitar(CodigoActividad clave) {
        if (estado != Estado.ABIERTA) {
            throw new ReglaNegocioException("Se necesita estado Abierta(Inscripcion 99)");
        }
        return lineas.removeIf(x->x.elemento.clave().equals(clave));

        //HECHO TODO E07: Exigir estado ABIERTA y eliminar con removeIf la línea cuyo elemento tenga esa clave.

    }

    public synchronized void cambiarEstado(Estado destino) {
        if (!estado.permiteTransicionA(destino)){
            throw new ReglaNegocioException("Transición inválida: " + estado.etiqueta() + " → " + destino.etiqueta());
        }

        if (destino == Estado.EN_CURSO && lineas.isEmpty()) {
            throw new ReglaNegocioException("Se requiere al menos una línea para activar la inscripción");
        }
        estado = destino;


        //HECHO TODO E07: Validar la transición con permiteTransicionA y exigir al menos una línea para pasar a EN_CURSO.

    }

    public void iniciar() {
        cambiarEstado(Estado.EN_CURSO);
    }

    public void cerrar() {
        cambiarEstado(Estado.CERRADA);
    }

    public void cancelar() {
        cambiarEstado(Estado.CANCELADA);
    }

    /** Uso exclusivo de la capa de persistencia al reconstruir el objeto. */
    public synchronized void restaurarEstado(Estado estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public synchronized List<Linea> lineas() {
        return Collections.unmodifiableList(new ArrayList<>(lineas));
    }

    /** Suma de subtotales sin descuento. */
    public synchronized double subtotal() {
       return lineas.stream().mapToDouble(Linea::subtotal).sum();

        // HECHO TODO E08: Sumar los subtotales de las líneas con mapToDouble y redondear a 2 decimales.

    }

    /** Total con el descuento del nivel del miembro. */
    public synchronized double total() {
        return lineas.stream().mapToDouble(Linea::importe).sum();
        // HECHO TODO E08: Sumar los importes de las líneas (ya con descuento) y redondear a 2 decimales.

    }

    public long diasDesde(LocalDate hoy) {
        return ChronoUnit.DAYS.between(fecha, hoy);
    }

    @Override
    public Iterator<Linea> iterator() {
        return lineas().iterator();
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return String.format("Inscripcion #%d [%s] %s, %d línea(s), total $%.2f",
                id, estado(), miembro.nombre(), lineas().size(), total());
    }

    /**
     * Línea de inscripción: clase interna (composición). Su constructor es privado, de modo que
     * sólo la envolvente puede crearla; accede al estado de la envolvente con {@code Inscripcion.this}.
     */
    public final class Linea implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Actividad elemento;
        private final int unidades;

        private Linea(Actividad elemento, int unidades) {
            if (unidades <= 0) {
                throw new ReglaNegocioException("Las sesiones deben ser mayores que cero");
            }
            this.elemento = Objects.requireNonNull(elemento, "El elemento es nulo(Inscipcion linea 190)");
            this.unidades = unidades;
        }

        public Actividad elemento() {
            return elemento;
        }

        public int unidades() {
            return unidades;
        }

        public double subtotal() {
            return elemento.tarifa(unidades);
        }

        /** Importe con el descuento del nivel del miembro de la envolvente. */
        public double importe() {
            // HECHO TODO E08: Usar Inscripcion.this.miembro.nivel().aplicar(subtotal()).
            return Inscripcion.this.miembro.nivel().aplicar(subtotal());
        }

        public Inscripcion contenedor() {
            return Inscripcion.this;
        }

        @Override
        public String toString() {
            return String.format("%s x %d sesiones = $%.2f", elemento.nombre(), unidades, importe());
        }
    }
}
