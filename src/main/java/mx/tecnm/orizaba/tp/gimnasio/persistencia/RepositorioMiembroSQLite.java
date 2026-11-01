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
        // TODO E13: Upsert por id con PreparedStatement.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public Optional<Miembro> buscar(Integer id) {
        // TODO E13: SELECT por id.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public List<Miembro> todos() {
        // TODO E13: SELECT ordenado por id.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public boolean eliminar(Integer id) {
        // TODO E13: DELETE por id.
        throw new UnsupportedOperationException("TODO E13");
    }
}
