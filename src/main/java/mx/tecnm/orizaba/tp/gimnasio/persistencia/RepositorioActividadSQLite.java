package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.CodigoActividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.servicio.Gimnasio;

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
        return switch (e){
            case Actividad.ClaseGrupal x-> x.disciplina();
            case Actividad.AccesoLibre x->x.turno();
            case Actividad.Entrenamiento x->Integer.toString(x.intensidad());

        };
        // HECHO TODO E13: switch (e) { case Actividad.ClaseGrupal x -> ...; ... } sin default (la jerarquía es sellada).

    }

    @Override
    public void guardar(Actividad e) {
        String sql = """
                    INSERT INTO actividad (clave, tipo, nombre, precio_base, atributo)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(clave) DO UPDATE SET
                        tipo = excluded.tipo,
                        nombre = excluded.nombre,
                        precio_base = excluded.precio_base,
                        atributo = excluded.atributo
                    """;

        try (PreparedStatement ps = bd.conexion().prepareStatement(sql)){
            ps.setString(1, e.clave().toString());
            ps.setString(2, e.tipo());
            ps.setString(3, e.nombre());
            ps.setDouble(4, e.precioBase());
            ps.setString(5, atributoDe(e));

            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new PersistenciaException("Error al guardar", ex);
        }


        // HECHO TODO E13: INSERT ... ON CONFLICT(clave) DO UPDATE ... con PreparedStatement.

    }

    @Override
    public Optional<Actividad> buscar(CodigoActividad clave) {
        String sql = "SELECT * FROM actividad WHERE clave = ?";
        try (PreparedStatement ps = bd.conexion().prepareStatement(sql)) {
            ps.setString(1, clave.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapeador.mapear(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("Error al buscar la actividad", ex);
        }

        //HECHO TODO E13: SELECT por clave; mapear con mapeador si hay fila.

    }

    @Override
    public List<Actividad> todos() {
        List<Actividad> lista = new ArrayList<>();
        try (PreparedStatement ps = bd.conexion().prepareStatement("SELECT * FROM actividad ORDER BY clave");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapeador.mapear(rs));
            }
        } catch (SQLException ex) {
            throw new PersistenciaException("Error al listar actividades", ex);
        }
        return lista;
        //HECHO TODO E13: SELECT ordenado por clave; mapear cada fila.

    }


    @Override
    public boolean eliminar(CodigoActividad clave) {

        String sql = """
                DELETE FROM actividad
                WHERE clave = ?
                """;

        try(PreparedStatement ps = bd.conexion().prepareStatement(sql)){

            ps.setString(1, clave.toString());
            int filas = ps.executeUpdate();

            return filas>0;

        } catch (SQLException e) {
            throw new PersistenciaException("Error al buscar", e);
        }
        //HECHO TODO E13: DELETE por clave; devolver si se afectó alguna fila.

    }
}
