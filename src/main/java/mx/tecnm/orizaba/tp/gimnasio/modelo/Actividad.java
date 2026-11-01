package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Clase abstracta <strong>sellada</strong> que factoriza el estado y el comportamiento común de todo
 * actividad. Sus tres subtipos se declaran como clases anidadas estáticas finales
 * (véase {@code java_4_modelo/t1_abstraccion/Abstraccion04_anidadas}).
 * <ul>
 *   <li>Abstracción + Jerarquía: patrón <em>Template Method</em> ({@link #tarifa(int)} es final y delega en
 *       {@link #calcularTarifa(int)}).</li>
 *   <li>Proceso: bloque estático, bloque de instancia y encadenamiento {@code this(...)}/{@code super(...)}
 *       (véase {@code java_2_proceso/Proceso03}, {@code Proceso09}).</li>
 *   <li>Persistencia: {@code Serializable} con un campo {@code transient}
 *       (véase {@code java_4_modelo/t7_persistencia/a_serializacion/NoSerializar_09}).</li>
 * </ul>
 */
public abstract sealed class Actividad implements Contratable, Serializable
        permits Actividad.ClaseGrupal, Actividad.Entrenamiento, Actividad.AccesoLibre {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger INSTANCIAS_CREADAS;

    static {
        INSTANCIAS_CREADAS = new AtomicInteger();
    }

    private final CodigoActividad clave;
    private String nombre;
    private double precioBase;
    private final LocalDateTime creadoEn;
    /** No se serializa: tras recuperar un respaldo vuelve a 0. */
    private transient int cotizaciones;

    {
        // Bloque de inicialización de instancia: se ejecuta antes del cuerpo de cualquier constructor.
        creadoEn = LocalDateTime.now();
        INSTANCIAS_CREADAS.incrementAndGet();
    }

    /** Constructor para objetos nuevos: genera la clave y encadena con {@code this(...)}. */
    protected Actividad(String prefijo, String nombre, double precioBase) {
        this(CodigoActividad.siguiente(prefijo), nombre, precioBase);
    }

    /** Constructor para objetos reconstruidos (persistencia) con clave conocida. */
    protected Actividad(CodigoActividad clave, String nombre, double precioBase) {
        this.clave=Objects.requireNonNull(clave,"clave");
        CodigoActividad.registrar(clave);
        setNombre(nombre);
        setPrecioBase(precioBase);
        




        // TODO E04: Asignar la clave (no nula), registrarla en CodigoActividad.registrar y validar nombre y precio mediante los setters.

    }

    public static int instanciasCreadas() {
        return INSTANCIAS_CREADAS.get();
    }

    @Override
    public final CodigoActividad clave() {
        return clave;
    }

    @Override
    public final String nombre() {
        return nombre;
    }

    @Override
    public final double precioBase() {
        return precioBase;
    }

    public final LocalDateTime creadoEn() {
        return creadoEn;
    }

    public final int cotizaciones() {
        return cotizaciones;
    }

    public final void setNombre(String nombre) {
        // TODO E04: Rechazar nombres nulos o en blanco (ReglaNegocioException); guardar el nombre sin espacios extremos.
        if(nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre esta en blanco");


        }else{
            this.nombre=nombre;

       }
    }

    public final void setPrecioBase(double precioBase) {
        // TODO E04: Rechazar precios negativos, NaN o infinitos (ReglaNegocioException).
        if (Double.isNaN(precioBase) || Double.isInfinite(precioBase) || precioBase<0){
            throw new ReglaNegocioException("El precio debe ser un numero positivo");
        }
        this.precioBase=precioBase;
    }

    /** Método plantilla: valida, contabiliza y redondea; la variación la aporta cada subtipo. */
    @Override
    public final double tarifa(int unidades) {
        // TODO E04: Validar unidades > 0 (ReglaNegocioException), incrementar cotizaciones y devolver calcularTarifa redondeada a 2 decimales.
        if (unidades<=0){ throw new ReglaNegocioException("Las unidades deben ser mayores a cero");}
        cotizaciones++;

        double tcal=calcularTarifa(unidades);

        return BigDecimal.valueOf(tcal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }


    /** Punto de variación del Template Method. */
    protected abstract double calcularTarifa(int unidades);

    /** Copia con clave nueva; los subtipos la redefinen con <strong>retorno covariante</strong>. */
    public abstract Actividad copiar();

    /** Nombre corto del subtipo (útil para la UI y la persistencia). */
    public final String tipo() {
        return getClass().getSimpleName();
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Actividad otro && clave.equals(otro.clave));
    }

    @Override
    public final int hashCode() {
        return clave.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, $%.2f]", tipo(), clave, nombre, precioBase);
    }

    /**
     * Clase grupal: precio por sesión.
     * <p>Subtipo de Actividad declarado como clase anidada estática final; implementa ConInstructor.</p>
     */
    public static final class ClaseGrupal extends Actividad implements ConInstructor {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "CLG";

        private final String disciplina;

        public ClaseGrupal(String nombre, double precioBase, String disciplina) {
            super(PREFIJO, nombre, precioBase);
            this.disciplina = validar(disciplina);
        }

        public ClaseGrupal(CodigoActividad clave, String nombre, double precioBase, String disciplina) {
            super(clave, nombre, precioBase);
            this.disciplina = validar(disciplina);
        }

        private static String validar(String disciplina) {
            // TODO E05: Validar: disciplina != null && !disciplina.isBlank() (si no se cumple: ReglaNegocioException "La disciplina es obligatoria").
            if(disciplina!=null && !disciplina.isBlank()){
                return disciplina;
            }
            else{
                throw new ReglaNegocioException("La disciplina es obligatoria");
            }
        }

        public String disciplina() {
            return disciplina;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            return precioBase()*unidades;
            // TODO E05: Clase grupal: precio por sesión. Fórmula: precioBase() * unidades

        }

        /** Retorno covariante: devuelve ClaseGrupal, no Actividad. */
        @Override
        public ClaseGrupal copiar() {
            // TODO E05: Crear un nuevo ClaseGrupal con el mismo nombre, precio y atributo (clave nueva).

            return new ClaseGrupal(nombre(),precioBase(),disciplina);
        }

        @Override
        public String instructor() {
            return "Instructor certificado de " + disciplina;
        }

        @Override
        public String toString() {
            return super.toString() + " {disciplina=" + disciplina + "}";
        }
    }

    /**
     * Entrenamiento: 5 % adicional por nivel de intensidad.
     * <p>Subtipo de Actividad declarado como clase anidada estática final; implementa ConCupo.</p>
     */
    public static final class Entrenamiento extends Actividad implements ConCupo {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "ENT";

        private final int intensidad;

        public Entrenamiento(String nombre, double precioBase, int intensidad) {
            super(PREFIJO, nombre, precioBase);
            this.intensidad = validar(intensidad);
        }

        public Entrenamiento(CodigoActividad clave, String nombre, double precioBase, int intensidad) {
            super(clave, nombre, precioBase);
            this.intensidad = validar(intensidad);
        }

        private static int validar(int intensidad) {
            // TODO E05: Validar: intensidad >= 1 && intensidad <= 5 (si no se cumple: ReglaNegocioException "La intensidad debe estar entre 1 y 5").
            if(intensidad>=1 && intensidad<=5){
                return intensidad;
            }else{throw new ReglaNegocioException("La intensidad debe estar entre 1 y 5");}
        }

        public int intensidad() {

            return intensidad;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            // TODO E05: Entrenamiento: 5 % adicional por nivel de intensidad. Fórmula: precioBase() * unidades * (1 + 0.05 * intensidad)

            return precioBase()*unidades*(1+0.05*intensidad());
        }

        /** Retorno covariante: devuelve Entrenamiento, no Actividad. */
        @Override
        public Entrenamiento copiar() {
            // TODO E05: Crear un nuevo Entrenamiento con el mismo nombre, precio y atributo (clave nueva).
            return new Entrenamiento(nombre(),precioBase(),intensidad());
        }

        @Override
        public int cupoMaximo() {
            return intensidad > 3 ? 1 : 2;
        }

        @Override
        public String toString() {
            return super.toString() + " {intensidad=" + intensidad + "}";
        }
    }

    /**
     * Acceso libre: tope de 12 sesiones cobradas.
     * <p>Subtipo de Actividad declarado como clase anidada estática final; implementa ConInstructor, ConCupo.</p>
     */
    public static final class AccesoLibre extends Actividad implements ConInstructor, ConCupo {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "ACL";

        private final String turno;

        public AccesoLibre(String nombre, double precioBase, String turno) {
            super(PREFIJO, nombre, precioBase);
            this.turno = validar(turno);
        }

        public AccesoLibre(CodigoActividad clave, String nombre, double precioBase, String turno) {
            super(clave, nombre, precioBase);
            this.turno = validar(turno);
        }

        private static String validar(String turno) {
            // TODO E05: Validar: turno != null && !turno.isBlank() (si no se cumple: ReglaNegocioException "El turno es obligatorio").
            if(turno!=null && !turno.isBlank()){
                return turno;
            }else{throw new ReglaNegocioException("El turno es obligatorio");}
        }

        public String turno() {
            return turno;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            return precioBase()*Math.min(unidades,12);
            // TODO E05: Acceso libre: tope de 12 sesiones cobradas. Fórmula: precioBase() * Math.min(unidades, 12)

        }

        /** Retorno covariante: devuelve AccesoLibre, no Actividad. */
        @Override
        public AccesoLibre copiar() {
            return new AccesoLibre(nombre(),precioBase(),turno());
            // TODO E05: Crear un nuevo AccesoLibre con el mismo nombre, precio y atributo (clave nueva).

        }

        @Override
        public String instructor() {
            return "Personal de piso";
        }

        @Override
        public int cupoMaximo() {
            return "Vespertino".equalsIgnoreCase(turno) ? 40 : 80;
        }

        /** Resolución explícita de la colisión entre métodos por defecto. */
        @Override
        public String descripcionCorta() {
            // TODO E05: Combinar ConInstructor.super.descripcionCorta() y ConCupo.super.descripcionCorta() separados por " | ".
            return ConInstructor.super.descripcionCorta() + " | " + ConCupo.super.descripcionCorta();
        }

        @Override
        public String toString() {
            return super.toString() + " {turno=" + turno + "}";
        }
    }
}
