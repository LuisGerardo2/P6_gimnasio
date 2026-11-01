package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto valor inmutable que identifica a un actividad (formato {@code ABC-0001}).
 * <ul>
 *   <li>Encapsulamiento: {@code record} con constructor compacto que valida invariantes.</li>
 *   <li>Proceso: bloque de inicialización estática (véase {@code java_2_proceso/Proceso03}).</li>
 *   <li>Concurrencia: secuencias atómicas seguras entre hilos.</li>
 * </ul>
 */
public record CodigoActividad(String prefijo, int numero) implements Comparable<CodigoActividad>, Serializable {

    private static final Map<String, AtomicInteger> SECUENCIAS;

    static {
        // Inicialización estática: se ejecuta una sola vez, al primer uso activo de la clase.
        SECUENCIAS = new ConcurrentHashMap<>();
    }

    public CodigoActividad {
        if(prefijo == null || !prefijo.matches("[A-Z]{3}") || numero <= 0){
            throw new IllegalArgumentException("El codigo debe ser con letras mayusculas y tener un numero entero");
        }

        //TODO E03: Validar que el prefijo tenga exactamente 3 letras mayúsculas y que el número sea positivo (IllegalArgumentException).

    }

    /** Genera la siguiente clave del prefijo indicado (operación atómica). */
    public static CodigoActividad siguiente(String prefijo) {
        if (prefijo == null || !prefijo.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "El prefijo debe contener tres letras mayúsculas."
            );
        }

        AtomicInteger secuencia = SECUENCIAS.computeIfAbsent(
                prefijo,
                k -> new AtomicInteger(0)
        );

        int numero = secuencia.incrementAndGet();

        return new CodigoActividad(prefijo, numero);

        // TODO E03: Obtener (o crear) la secuencia del prefijo con computeIfAbsent e incrementarla atómicamente.
    }

    /** Ajusta la secuencia para que nunca genere una clave ya existente (p. ej., tras leer la BD). */
    public static void registrar(CodigoActividad clave) {
        if (clave == null) {
            throw new UnsupportedOperationException("La clave no puede ser nula");
        }
        SECUENCIAS.computeIfAbsent(
                clave.prefijo(),
                k -> new AtomicInteger(0)
        ).accumulateAndGet(clave.numero(), Math::max);


        // TODO E03: Actualizar la secuencia del prefijo con accumulateAndGet(numero, Math::max).

    }

    /** Convierte un texto {@code ABC-0001} en una clave. */
    public static CodigoActividad parse(String texto) {
        if (texto == null || !texto.matches("[A-Z]{3}-\\d{4,}")) {
            throw new UnsupportedOperationException("No tiene el formato establecido");
        }

        String prefijo = texto.substring(0, 3);
        int numero = Integer.parseInt(texto.substring(4));

        return new CodigoActividad(prefijo, numero);

        // TODO E03: Validar el formato con la expresión [A-Z]{3}-\d{4,} y construir la clave.

    }

    @Override
    public int compareTo(CodigoActividad otra) {
        int resultado = this.prefijo.compareTo(otra.prefijo());

        if (resultado != 0) {
            return resultado;
        }

        return Integer.compare(this.numero, otra.numero());
        // TODO E03: Ordenar primero por prefijo y después por número.
    }

    @Override
    public String toString() {
        return String.format("%s-%04d", prefijo, numero);
    }
}
