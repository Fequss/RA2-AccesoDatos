package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ConexionBD {

    private static final String FICHERO = "db.properties";
    private static final Properties props = cargarPropiedades();

    private ConexionBD() {}

    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = ConexionBD.class.getClassLoader().getResourceAsStream(FICHERO)) {
            if (in != null) {
                p.load(in);
                return p;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo " + FICHERO, e);
        }
        try (FileInputStream in = new FileInputStream(FICHERO)) {
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró " + FICHERO, e);
        }
        return p;
    }

    /** "sqlite" o "postgres" (Neon). */
    public static String getTipo() {
        return props.getProperty("tipo", "sqlite").trim().toLowerCase();
    }

    public static boolean esSQLite() {
        return "sqlite".equals(getTipo());
    }

    public static boolean esPostgres() {
        return "postgres".equals(getTipo());
    }

    /** Definición de la clave primaria autoincremental según el SGBD. */
    public static String autoincremental() {
        return esSQLite()
                ? "INTEGER PRIMARY KEY AUTOINCREMENT"
                : "SERIAL PRIMARY KEY";
    }

    public static Connection getConnection() throws SQLException {
        if (!esSQLite() && !esPostgres()) {
            throw new SQLException("Valor no válido para 'tipo' en " + FICHERO
                    + ": " + getTipo() + " (usa sqlite o postgres)");
        }

        String prefijo = getTipo() + ".";
        String url = props.getProperty(prefijo + "url");
        String usuario = props.getProperty(prefijo + "usuario", "");
        String password = props.getProperty(prefijo + "password", "");

        if (url == null || url.trim().isEmpty()) {
            throw new SQLException("Falta la propiedad '" + prefijo + "url' en " + FICHERO);
        }

        Connection con;
        if (usuario.trim().isEmpty()) {
            con = DriverManager.getConnection(url);
        } else {
            con = DriverManager.getConnection(url, usuario, password);
        }

        if (esSQLite()) {
            try (Statement st = con.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return con;
    }
}