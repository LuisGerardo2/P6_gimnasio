package mx.tecnm.orizaba.tp.gimnasio.funcional;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;

/**
 * Fábrica genérica acotada, diseñada para recibir <strong>referencias a constructor</strong>:
 * {@code FabricaElemento<Actividad.ClaseGrupal, String> f = Actividad.ClaseGrupal::new;}
 * (véase {@code java_5_lambdas/c_referencias_metodos/Referencias4}).
 *
 * @param <E> subtipo concreto de Actividad
 * @param <A> tipo del atributo específico del subtipo
 */
@FunctionalInterface
public interface FabricaElemento<E extends Actividad, A> {

    E crear(String nombre, double precioBase, A atributo);
}
