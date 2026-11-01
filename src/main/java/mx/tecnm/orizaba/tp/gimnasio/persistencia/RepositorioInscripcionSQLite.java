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
        // TODO E13: En bd.enTransaccion: upsert del encabezado, borrar sus líneas e insertarlas en lote (addBatch/executeBatch).
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public Optional<Inscripcion> buscar(Integer id) {
        // TODO E13: Leer el encabezado, resolver el actor y los elementos con sus repositorios, agregar líneas y restaurar el estado.
        throw new UnsupportedOperationException("TODO E13");
    }

    private Inscripcion reconstruir(Connection c, ResultSet rs) throws SQLException {
        // TODO E13: Construir el objeto en estado ABIERTA, agregar las líneas y al final restaurarEstado(estado leído).
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public List<Inscripcion> todos() {
        // TODO E13: Recorrer los ids ordenados y reconstruir cada registro.
        throw new UnsupportedOperationException("TODO E13");
    }

    @Override
    public boolean eliminar(Integer id) {
        // TODO E13: DELETE del encabezado (las líneas se eliminan en cascada).
        throw new UnsupportedOperationException("TODO E13");
    }
}
