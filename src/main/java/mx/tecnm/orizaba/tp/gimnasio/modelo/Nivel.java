package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nivel del miembro; cada constante encapsula su etiqueta y su descuento.
 * <p>Tipificación fuerte: un {@code enum} es un tipo cerrado con estado y comportamiento.</p>
 */
public enum Nivel {
    BASICO("Básico", 0.0),
    ESTUDIANTE("Estudiante", 0.15),
    PREMIUM("Premium", 0.25);

    private final String etiqueta;
    private final double descuento;

    Nivel(String etiqueta, double descuento) {
        this.etiqueta = etiqueta;
        this.descuento = descuento;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public double descuento() {
        return descuento;
    }

    /** Aplica el descuento del nivel a un monto, redondeando a 2 decimales. */
    public double aplicar(double monto) {
        BigDecimal bd= new BigDecimal(Double.toString(monto-(this.descuento*monto)));
        bd = bd.setScale(2, RoundingMode.HALF_UP);

        return bd.doubleValue();



        // TODO E06: Restar el porcentaje de descuento al monto y redondear a 2 decimales.

    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
