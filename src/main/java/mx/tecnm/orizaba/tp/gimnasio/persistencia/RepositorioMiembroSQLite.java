package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Nivel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio JDBC de miembros. */
public class RepositorioMiembroSQLite implements Repositorio<Miembro, Integer> {

    private final ConexionSQLite bd;
    private final MapeadorFila<Miembro> mapeador = rs -> new Miembro(rs.getInt("id"),
            rs.getString("nombre"), rs.getString("correo"), Nivel.valueOf(rs.getString("nivel")));

    public RepositorioMiembroSQLite(ConexionSQLite bd) {
        this.bd = bd;
    }

    @Override
    public void guardar(Miembro a) {
        String sql = """
                INSERT INTO miembro(id, nombre, correo, nivel)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                nombre = excluded.nombre,
                correo = excluded.correo,
                nivel = excluded.nivel
                """;

        try (PreparedStatement ps = bd.conexion().prepareStatement(sql)){
            ps.setInt(1,a.id());
            ps.setString(2, a.nombre());
            ps.setString(3,a.correo());
            ps.setString(4,a.nivel().name());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar miembro:" + e.getMessage(),e);
        }
        // HECHO TODO E13: Upsert por id con PreparedStatement.

    }

    @Override
    public Optional<Miembro> buscar(Integer id) {
        String sql = """
            SELECT id, nombre, correo, nivel
            FROM miembro
            WHERE id = ?
            """;

        try (PreparedStatement ps =
                     bd.conexion().prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapeador.mapear(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo realizar la consulta miebro: "
                            + e.getMessage(),
                    e
            );
        }
        // HECHO TODO E13: SELECT por id.

    }

    @Override
    public List<Miembro> todos() {
        String sql = """
                SELECT id, nombre, correo, nivel
                FROM miembro
                ORDER BY id
                """;

        List<Miembro> miembros = new ArrayList<>();


        try(PreparedStatement ps = bd.conexion().prepareStatement(sql); ResultSet rs = ps.executeQuery()){

            while (rs.next()){
                miembros.add(mapeador.mapear(rs));
            }
            return miembros;

        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo realizar la consulta miembro" +e.getMessage(),e);
        }
        // HECHO TODO E13: SELECT ordenado por id.

    }

    @Override
    public boolean eliminar(Integer id) {
        String sql = """
                DELETE FROM miembros
                WHERE id = ?
            
                """;

        try(PreparedStatement ps = bd.conexion().prepareStatement(sql)){
            ps.setInt(1, id);

            int filas = ps.executeUpdate();

            return filas > 0;
        } catch (SQLException e) {
            throw new PersistenciaException("Error al eliminar",e);
        }
        //HECHO TODO E13: DELETE por id.

    }
}
