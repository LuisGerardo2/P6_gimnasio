package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import mx.tecnm.orizaba.tp.gimnasio.modelo.Miembro;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Actividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.CodigoActividad;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Estado;
import mx.tecnm.orizaba.tp.gimnasio.modelo.Inscripcion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JDBC de inscripciones y sus líneas. Guardar es <strong>atómico</strong>:
 * encabezado y líneas se escriben en una sola transacción (commit/rollback).
 */
public class RepositorioInscripcionSQLite implements Repositorio<Inscripcion, Integer> {

    private final ConexionSQLite bd;
    private final Repositorio<Miembro, Integer> miembros;
    private final Repositorio<Actividad, CodigoActividad> elementos;

    public RepositorioInscripcionSQLite(ConexionSQLite bd, Repositorio<Miembro, Integer> miembros,
                         Repositorio<Actividad, CodigoActividad> elementos) {
        this.bd = bd;
        this.miembros = miembros;
        this.elementos = elementos;
    }

    @Override
    public void guardar(Inscripcion t) {
        bd.enTransaccion(c -> {
            String sql= """
                    INSERT INTO inscripcion(id, miembro_id, fecha, estado) 
                    VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET 
                    miembro_id = excluded.miembro_id,fecha = excluded.fecha,estado = excluded.estado
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, t.id());
                ps.setInt(2, t.miembro().id());
                ps.setString(3, t.fecha().toString());
                ps.setString(4, t.estado().name());

                ps.executeUpdate();
            }
            String sqlBorrar= """ 
                    DELETE FROM inscripcion_linea WHERE inscripcion_id = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlBorrar)) {
                ps.setInt(1, t.id());
                ps.executeUpdate();
            }
            String sqlLinea= """
                    INSERT INTO inscripcion_linea(inscripcion_id, clave_actividad, unidades) VALUES (?, ?, ?)
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlLinea)) {
                for (Inscripcion.Linea l : t.lineas()) {
                    ps.setInt(1, t.id());
                    ps.setString(2, l.elemento().clave().toString());
                    ps.setInt(3, l.unidades());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
        // HECHO TODO E13: En bd.enTransaccion: upsert del encabezado, borrar sus líneas e insertarlas en lote (addBatch/executeBatch).

    }

    @Override
    public Optional<Inscripcion> buscar(Integer id) {
        String sql = """
                SELECT (id, socio_id, fecha, estado)
                FROM inscripcion
                WHERE id = ?
                """;

        Connection c = bd.conexion();

        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    return Optional.of(reconstruir(c, rs));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // HECHO TODO E13: Leer el encabezado, resolver el actor y los elementos con sus repositorios, agregar líneas y restaurar el estado.

    }

    private Inscripcion reconstruir(Connection c, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int miembroId = rs.getInt("miembro_id");
        LocalDate fecha = LocalDate.parse(rs.getString("fecha"));
        Estado estado = Estado.valueOf(rs.getString("estado"));

        Miembro miembro = miembros.buscar(miembroId)
                .orElseThrow(() -> new SQLException("Miembro inexistente: " + miembroId));

        Inscripcion t = new Inscripcion(id, miembro, fecha);

        String sql= """
                SELECT clave_actividad, unidades FROM inscripcion_linea WHERE inscripcion_id = ? ORDER BY clave_actividad
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet lr = ps.executeQuery()) {
                while (lr.next()) {
                    CodigoActividad clave = CodigoActividad.parse(lr.getString("clave_actividad"));
                    Actividad a = elementos.buscar(clave)
                            .orElseThrow(() -> new SQLException("Actividad inexistente: " + clave));
                    t.agregar(a, lr.getInt("unidades"));
                }
            }
        }
        t.restaurarEstado(estado);
        return t;
        // TODO E13: Construir el objeto en estado ABIERTA, agregar las líneas y al final restaurarEstado(estado leído).

    }

    @Override
    public List<Inscripcion> todos() {
        String sql= """
                SELECT id, miembro_id, fecha, estado
                FROM inscripcion
                ORDER BY id
                """;

        List<Inscripcion> inscripciones=new ArrayList<>();
        Connection c= bd.conexion();
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                inscripciones.add(reconstruir(c,rs));
            }

            return inscripciones;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //HECHO  TODO E13: Recorrer los ids ordenados y reconstruir cada registro.

    }

    @Override
    public boolean eliminar(Integer id) {
        String sql= """
                DELETE from inscripcion WHERE id=?
                """;
        Connection c=bd.conexion();
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, id);

            int filas = ps.executeUpdate();

            return filas > 0;


        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar",e);
        }

        //HECHO TODO E13: DELETE del encabezado (las líneas se eliminan en cascada).

    }
}
