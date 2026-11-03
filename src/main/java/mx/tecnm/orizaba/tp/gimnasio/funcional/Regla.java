package mx.tecnm.orizaba.tp.gimnasio.funcional;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Interfaz funcional propia (un solo método abstracto) con métodos por defecto de composición
 * (véase {@code java_5_lambdas/b_interfaces_funcionales} y {@code d_orden_superior/Superior2}).
 *
 * @param <T> tipo evaluado
 */
@FunctionalInterface
public interface Regla<T> {

    boolean evaluar(T t);

    default Regla<T> y(Regla<? super T> otra) {
        Objects.requireNonNull(otra,"Regla no puede ser nula(Regla 18)");
        return x->evaluar(x) && otra.evaluar(x);
        // HECHO TODO E11: Devolver la lambda t -> evaluar(t) && otra.evaluar(t) (validar otra no nula).

    }

    default Regla<T> o(Regla<? super T> otra) {
        return x->evaluar(x) || otra.evaluar(x);
        // HECHO TODO E11: Devolver la lambda t -> evaluar(t) || otra.evaluar(t).

    }

    default Regla<T> negar() {
        return x->!evaluar(x);
        // TODO E11: Devolver la lambda t -> !evaluar(t).

    }

    /** Adaptador hacia {@link Predicate} mediante referencia a método de instancia particular. */
    default Predicate<T> comoPredicado() {
        return this::evaluar;
    }

    static <T> Regla<T> todas() {
        return t -> true;
    }

    /** Adaptador desde {@link Predicate}. */
    static <T> Regla<T> de(Predicate<? super T> p) {
        return p::test;
    }
}
