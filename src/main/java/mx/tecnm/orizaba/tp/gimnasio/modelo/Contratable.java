package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * Abstracción raíz del dominio: todo lo que puede contratarse expone una clave,
 * un nombre, un precio base y una tarifa por unidad (sesiones).
 * <p>Abstracción (Booch): define el <em>qué</em>, no el <em>cómo</em>
 * (véase {@code java_4_modelo/t1_abstraccion/Abstraccion03_interfaces}).</p>
 */
public interface Contratable {

    /** Constante de interfaz: implícitamente {@code public static final}. */
    double TASA_IMPUESTO = 0.16;

    CodigoActividad clave();

    String nombre();

    double precioBase();

    /** Tarifa (sin impuestos) para la cantidad de sesiones indicada. */
    double tarifa(int unidades);

    /** Método por defecto (extensión de la interfaz sin romper implementaciones). */
    default double tarifaConImpuesto(int unidades) {
        BigDecimal bd= new BigDecimal(Double.toString(tarifa(unidades)*(1+TASA_IMPUESTO)));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();

        // TODO E02: Aplicar TASA_IMPUESTO a tarifa(unidades) y redondear a 2 decimales.

    }

    /** Método estático de interfaz: comparador por nombre (referencia a método de objeto arbitrario). */
    static Comparator<Contratable> porNombre() {

        return Comparator.comparing(
                Contratable::nombre,
                String.CASE_INSENSITIVE_ORDER
        );
        // TODO E02: Usar Comparator.comparing(Contratable::nombre, String.CASE_INSENSITIVE_ORDER).

    }
}
