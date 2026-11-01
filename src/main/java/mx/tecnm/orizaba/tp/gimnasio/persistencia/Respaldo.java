package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Programa;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Respaldo binario mediante serialización de objetos (complementa a SQLite).
 * Escribe y lee en el mismo orden (véase {@code java_4_modelo/t7_persistencia/a_serializacion/Gusano_07}).
 */
public final class Respaldo {

    public static final String ENCABEZADO = "Respaldo Gimnasio v1";

    private Respaldo() {
    }

    /** Contenido recuperado de un respaldo. */
    public record Contenido(String encabezado, LocalDateTime fecha, Programa catalogo,
                            List<Miembro> miembros, List<Inscripcion> registros) {
    }

    public static void exportar(Path archivo, Programa catalogo, Collection<Miembro> miembros,
                                Collection<Inscripcion> registros) throws IOException {
        // TODO E14: ObjectOutputStream(BufferedOutputStream(Files.newOutputStream)): encabezado, fecha, catálogo, listas.
        throw new UnsupportedOperationException("TODO E14");
    }

    @SuppressWarnings("unchecked")
    public static Contenido importar(Path archivo) throws IOException, ClassNotFoundException {
        // TODO E14: Leer en el mismo orden; validar el encabezado (IOException si no coincide).
        throw new UnsupportedOperationException("TODO E14");
    }
}
