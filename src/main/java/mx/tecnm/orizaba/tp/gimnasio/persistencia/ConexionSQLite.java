package mx.tecnm.orizaba.tp.gimnasio.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra la conexión JDBC con SQLite y el esquema de la base de datos.
 * <p>Proceso: el bloque estático carga el controlador la primera vez que la clase se inicializa
 * (véase {@code java_2_proceso/Proceso07}: uso activo de una clase).</p>
 */
public final class ConexionSQLite implements AutoCloseable {

    private static final String[] ESQUEMA = {
        """
        CREATE TABLE IF NOT EXISTS actividad (
            clave        TEXT PRIMARY KEY,
            tipo         TEXT NOT NULL,
            nombre       TEXT NOT NULL,
            precio_base  REAL NOT NULL CHECK (precio_base >= 0),
            atributo     TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS miembro (
            id      INTEGER PRIMARY KEY,
            nombre  TEXT NOT NULL,
            correo  TEXT NOT NULL UNIQUE,
            nivel   TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS inscripcion (
            id        INTEGER PRIMARY KEY,
            miembro_id  INTEGER NOT NULL REFERENCES miembro(id) ON DELETE CASCADE,
            fecha     TEXT NOT NULL,
            estado    TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS inscripcion_linea (
            inscripcion_id   INTEGER NOT NULL REFERENCES inscripcion(id) ON DELETE CASCADE,
            clave_actividad TEXT NOT NULL REFERENCES actividad(clave),
            unidades        INTEGER NOT NULL CHECK (unidades > 0),
            PRIMARY KEY (inscripcion_id, clave_actividad)
        )"""
    };

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se encontró el controlador sqlite-jdbc en el classpath");
        }
    }

    private final Connection conexion;

    /** @param rutaArchivo archivo .db (se crea si no existe) */
    public ConexionSQLite(String rutaArchivo) {

        // HECHO TODO E13: Abrir jdbc:sqlite:<ruta>, activar PRAGMA foreign_keys = ON y crear el esquema.
        try {
            conexion = DriverManager.getConnection( "jdbc:sqlite:" + rutaArchivo);

            try (Statement st = conexion.createStatement()){
                st.execute("PRAGMA foreign_keys = ON");

                for(String sql : ESQUEMA){
                    st.execute(sql);
                }
            }
        }catch (SQLException e) {
            throw new PersistenciaException(
                    "Error al iniciar la BD"+ e.getMessage(),
                    e
            );
        }
    }

    public Connection conexion() {
        return conexion;
    }

    /**
     * Ejecuta un trabajo dentro de una transacción: confirma si termina bien y revierte ante error.
     */
    public <R> R enTransaccion(TrabajoSQL<R> trabajo) {
        try {
            conexion.setAutoCommit(false);
            try {
                R resultado = trabajo.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException e) {
                try {
                    conexion.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                throw new PersistenciaException("Error en la transacción, se hizo rollback", e);
            } finally {
                try {
                    conexion.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new PersistenciaException("No se pudo restaurar el autocommit", e);

                }
            }
        } catch (SQLException e) {
            // fallo al hacer setAutoCommit(false)
            throw new PersistenciaException("No se pudo iniciar la transacción", e);
        }

        //HECHO TODO E13: setAutoCommit(false); ejecutar; commit; ante SQLException rollback y PersistenciaException; finalmente setAutoCommit(true).

    }

    /** Interfaz funcional miembro: trabajo que puede lanzar {@link SQLException}. */
    @FunctionalInterface
    public interface TrabajoSQL<R> {
        R ejecutar(Connection c) throws SQLException;
    }

    @Override
    public void close() {
        try {
            conexion.close();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al cerrar la conexión", e);
        }
    }
}
