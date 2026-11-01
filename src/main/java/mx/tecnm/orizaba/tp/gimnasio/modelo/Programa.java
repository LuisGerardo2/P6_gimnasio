package mx.tecnm.orizaba.tp.gimnasio.modelo;

import mx.tecnm.orizaba.tp.gimnasio.funcional.Regla;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Programa: <strong>agregación</strong> de actividades (las partes existen con independencia del
 * todo; véase {@code java_4_modelo/t4_jerarquia/t2_agregacion/Jerarquia02}).
 * Reúne en un solo lugar tres variantes de tipos internos:
 * clase interna privada ({@link Iterador}), clase anónima ({@link #porTarifa(int)}) y clase local
 * ({@link #resumen()}).
 */
public class Programa implements Iterable<Actividad>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<CodigoActividad, Actividad> elementos = new LinkedHashMap<>();

    /** Constructor con número variable de argumentos. */
    public Programa(Actividad... iniciales) {
        agregar(iniciales);
    }

    public final void agregar(Actividad... nuevos) {
        // TODO E09: Recorrer los argumentos; rechazar nulos y claves duplicadas (ReglaNegocioException); usar putIfAbsent.
        throw new UnsupportedOperationException("TODO E09");
    }

    public Optional<Actividad> buscar(CodigoActividad clave) {
        return Optional.ofNullable(elementos.get(clave));
    }

    public boolean retirar(CodigoActividad clave) {
        return elementos.remove(clave) != null;
    }

    public int tamanio() {
        return elementos.size();
    }

    /** Filtrado con la interfaz funcional propia {@link Regla}. */
    public List<Actividad> filtrar(Regla<? super Actividad> regla) {
        // TODO E09: Stream de los valores filtrado con regla::evaluar y recolectado con toList().
        throw new UnsupportedOperationException("TODO E09");
    }

    /** Filtrado por subtipo con genéricos acotados: {@code deTipo(Actividad.ClaseGrupal.class)}. */
    public <E extends Actividad> List<E> deTipo(Class<E> tipo) {
        // TODO E09: Usar filter(tipo::isInstance) y map(tipo::cast).
        throw new UnsupportedOperationException("TODO E09");
    }

    public List<Actividad> ordenados(Comparator<? super Actividad> criterio) {
        return elementos.values().stream().sorted(criterio).toList();
    }

    /**
     * Comparador implementado con una <strong>clase anónima</strong> que captura la variable
     * {@code unidades} (efectivamente final; véase {@code Parcel09}).
     */
    public static Comparator<Actividad> porTarifa(final int unidades) {
        // TODO E10: Devolver new Comparator<>() { compare(a, b) → Double.compare(a.tarifa(unidades), b.tarifa(unidades)) }.
        throw new UnsupportedOperationException("TODO E10");
    }

    /** Resumen calculado con una <strong>clase local</strong> (véase {@code Parcel05}). */
    public String resumen() {
        // TODO E10: Declarar dentro del método la clase local Acumulador (cuenta, suma, mínimo, máximo) y recorrer el catálogo.
        throw new UnsupportedOperationException("TODO E10");
    }

    @Override
    public Iterator<Actividad> iterator() {
        return new Iterador();
    }

    /**
     * Clase interna privada que implementa una interfaz pública: el cliente sólo conoce
     * {@code Iterator<Actividad>} y la implementación queda oculta (véase {@code Parcel04}).
     * Itera sobre una instantánea, de modo que el catálogo no puede modificarse a través de ella.
     */
    private class Iterador implements Iterator<Actividad> {
        private final List<Actividad> instantanea = new ArrayList<>(elementos.values());
        private int cursor;

        @Override
        public boolean hasNext() {
            return cursor < instantanea.size();
        }

        @Override
        public Actividad next() {
            // TODO E10: Lanzar NoSuchElementException si no hay siguiente; devolver el elemento y avanzar el cursor.
            throw new UnsupportedOperationException("TODO E10");
        }
    }
}
