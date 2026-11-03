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
        for(Actividad a:nuevos){
            if (elementos.putIfAbsent(a.clave(), a) != null) {
                throw new ReglaNegocioException("Ya existe la actividad " + a.clave());
            }
        }

        //HECHO  TODO E09: Recorrer los argumentos; rechazar nulos y claves duplicadas (ReglaNegocioException); usar putIfAbsent.

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
    return elementos.values().stream().filter(regla::evaluar).toList();

    // HECHO TODO E09: Stream de los valores filtrado con regla::evaluar y recolectado con toList().

    }

    /** Filtrado por subtipo con genéricos acotados: {@code deTipo(Actividad.ClaseGrupal.class)}. */
    public <E extends Actividad> List<E> deTipo(Class<E> tipo) {
        return elementos.values().stream().filter(tipo::isInstance).map(tipo::cast).toList();

        // HECHO TODO E09: Usar filter(tipo::isInstance) y map(tipo::cast).

    }

    public List<Actividad> ordenados(Comparator<? super Actividad> criterio) {
        return elementos.values().stream().sorted(criterio).toList();
    }

    /**
     * Comparador implementado con una <strong>clase anónima</strong> que captura la variable
     * {@code unidades} (efectivamente final; véase {@code Parcel09}).
     */
    public static Comparator<Actividad> porTarifa(final int unidades) {
        return new Comparator<Actividad>() {
            @Override
            public int compare(Actividad o1, Actividad o2) {
                return Double.compare(o1.tarifa(unidades),o2.tarifa(unidades));
            }
        };

        // HECHO TODO E10: Devolver new Comparator<>() { compare(a, b) → Double.compare(a.tarifa(unidades), b.tarifa(unidades)) }.

    }

    /** Resumen calculado con una <strong>clase local</strong> (véase {@code Parcel05}). */
    public String resumen() {
        class Acumulador{
           private int cuenta;
            private double suma;
            private double minimo ;
            private double maximo;

            void agregar(Actividad actividad){
                double precio=actividad.precioBase();
                cuenta++;
                suma+=precio;
                minimo=Math.min(minimo,precio);
                maximo=Math.max(maximo,precio);
            }
        }
        Acumulador ac=new Acumulador();
        for (Actividad a: elementos.values()){
            ac.agregar(a);
        }
        double minimo= ac.cuenta==0 ? 0.0: ac.minimo;
        double maximo= ac.cuenta==0?0.0:ac.maximo;
        return "%d elementos, suma=%.2f, mínimo=%.2f, máximo=%.2f"
                .formatted(
                        ac.cuenta,
                        ac.suma,
                        minimo,
                        maximo
                );

        //HECHO TODO E10: Declarar dentro del método la clase local Acumulador (cuenta, suma, mínimo, máximo) y recorrer el catálogo.

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
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return instantanea.get(cursor++);
            //HECHO  TODO E10: Lanzar NoSuchElementException si no hay siguiente; devolver el elemento y avanzar el cursor.

        }
    }
}
