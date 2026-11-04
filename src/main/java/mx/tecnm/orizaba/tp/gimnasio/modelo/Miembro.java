package mx.tecnm.orizaba.tp.gimnasio.modelo;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Miembro del sistema. Implementa {@link Externalizable}: el propio objeto decide qué y cómo se
 * escribe (véase {@code java_4_modelo/t7_persistencia/b_externalizacion/Cosas2}); por ello requiere un
 * constructor público sin argumentos.
 */
public class Miembro implements Externalizable {

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private int id;
    private String nombre;
    private String correo;
    private Nivel nivel;

    /** Requerido por la externalización: se invoca antes de readExternal. */
    public Miembro() {
    }

    public Miembro(int id, String nombre, String correo, Nivel nivel) {
        if(id>0){ this.id=id;
            setNombre(nombre);
            setCorreo(correo);
            setNivel(nivel);

        }else {throw new ReglaNegocioException("el id debe ser mayor a 0");}
        // TODO E06: Validar id > 0 y asignar nombre, correo y nivel mediante sus setters.

    }

    /** Encadenamiento de constructores: nivel por omisión. */
    public Miembro(int id, String nombre, String correo) {
        this(id, nombre, correo, Nivel.values()[0]);
    }

    public int id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }

    public Nivel nivel() {
        return nivel;
    }

    public final void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        this.nombre = nombre.trim();
    }

    public final void setCorreo(String correo) {
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw new ReglaNegocioException("Correo inválido: " + correo);
        }
         this.correo=correo;
    }

    public final void setNivel(Nivel nivel) {
        this.nivel = Objects.requireNonNull(nivel, "nivel");
    }

    @Override
    public void writeExternal(ObjectOutput salida) throws IOException {
        //HECHO TODO E14: Escribir id, nombre, correo y el nombre del nivel (writeInt / writeUTF).
        salida.write(id);
        salida.writeUTF(nombre);
        salida.writeUTF(correo);
        salida.writeUTF(nivel.name());
    }

    @Override
    public void readExternal(ObjectInput entrada) throws IOException {
        //HECHO TODO E14: Leer en el mismo orden en que se escribió y reconstruir el nivel con Nivel.valueOf.
        id = entrada.readInt();
        nombre = entrada.readUTF();
        correo = entrada.readUTF();
        nivel = Nivel.valueOf(entrada.readUTF());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Miembro otro && id == otro.id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("%s #%d %s <%s> (%s)", "Miembro", id, nombre, correo, nivel);
    }
}
