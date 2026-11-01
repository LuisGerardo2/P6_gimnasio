package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.CodigoActividad;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio JDBC de actividades (tabla de herencia única: columna {@code tipo}).
 * <ul>
 *   <li>Escritura: {@code switch} con <em>pattern matching</em> exhaustivo sobre la jerarquía sellada.</li>
 *   <li>Lectura: mapa de reconstructores (lambdas) indexado por el nombre del subtipo.</li>
 * </ul>
 */
public class RepositorioActividadSQLite implements Repositorio<Actividad, CodigoActividad> {

    /** Interfaz funcional privada anidada. */
    @FunctionalInterface
    private interface Reconstructor {
        Actividad crear(CodigoActividad clave, String nombre, double precio, String atributo);
    }

    private static final Map<String, Reconstructor> RECONSTRUCTORES = Map.of(
            "ClaseGrupal", (c, n, p, a) -> new Actividad.ClaseGrupal(c, n, p, a),
            "Entrenamiento", (c, n, p, a) -> new Actividad.Entrenamiento(c, n, p, Integer.parseInt(a)),
            "AccesoLibre", (c, n, p, a) -> new Actividad.AccesoLibre(c, n, p, a)
    );

    private final ConexionSQLite bd;
    private final MapeadorFila<Actividad> mapeador = rs -> {
        Reconstructor r = RECONSTRUCTORES.get(rs.getString("tipo"));
        if (r == null) {
            throw new SQLException("Tipo desconocido: " + rs.getString("tipo"));
        }
        return r.crear(CodigoActividad.parse(rs.getString("clave")), rs.getString("nombre"),
                rs.getDouble("precio_base"), rs.getString("atributo"));
    };

    public RepositorioActividadSQLite(ConexionSQLite bd) {
        this.bd = bd;
    }

    /** Atributo específico del subtipo, obtenido con pattern matching. */
    static String atributoDe(Actividad e) {
        // TODO E13: switch (e) { case Actividad.ClaseGrupal x -> ...; ... } sin default (la jerarquía es sellada).
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public void guardar(Actividad e) {
        // TODO E13: INSERT ... ON CONFLICT(clave) DO UPDATE ... con PreparedStatement.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public Optional<Actividad> buscar(CodigoActividad clave) {
        // TODO E13: SELECT por clave; mapear con mapeador si hay fila.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public List<Actividad> todos() {
        // TODO E13: SELECT ordenado por clave; mapear cada fila.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public boolean eliminar(CodigoActividad clave) {
        // TODO E13: DELETE por clave; devolver si se afectó alguna fila.
        throw new UnsupportedOperationException("TODO E13");
    }
}
