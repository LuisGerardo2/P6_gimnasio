package mx.tecnm.orizaba.tp.gimnasio.funcional;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;

/**
 * Biblioteca de reglas reutilizables (clase utilitaria no instanciable).
 */
public final class Reglas {

    private Reglas() {
        throw new AssertionError("No instanciable");
    }

    public static Regla<Actividad> precioHasta(double maximo) {
        return x->x.precioBase()<=maximo;
        // HECHO TODO E11: Lambda que acepta los elementos cuyo precioBase() sea menor o igual que maximo.

    }

    public static Regla<Actividad> nombreContiene(String texto) {
        // HECHO TODO E11: Lambda insensible a mayúsculas/minúsculas sobre nombre().
        return x->x.nombre().toLowerCase().contains(texto);
    }

    /** Elementos que implementan una capacidad (p. ej. {@code ConInstructor.class}). */
    public static Regla<Actividad> conCapacidad(Class<?> capacidad) {
        return capacidad::isInstance;
        // HECHO TODO E11: Referencia a método de instancia particular: capacidad::isInstance.

    }
}
