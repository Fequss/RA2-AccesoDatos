package dao.implementacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dao.PodcastDAO;
import db.ConexionBD;
import modelo.Podcasts;
import modelo.ResumenPodcast;

public class PodcastDAOImp implements PodcastDAO {

	// CREAR TABLA
	@Override
	public void crearTabla() throws Exception{
		String sql = "CRAETE TABLE IF NOT EXISTS podcast ("
				+ "id " + ConexionBD.autoincremental() + ", "
				+ "nombre TEXT NOT NULL UNIQUE, "
				+ "id_genero INTEGER NOT NULL, "
				+ "FOREIGN KEY (id_genero) REFERENCES genero(id) "
				+ "ON UPDATE CASCADE ON DELETE RESTRICT)";
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.execute();
		}
	}
	
	// INSERTAR
	@Override
	public int insertar(Podcasts podcast) throws Exception {
		String sql = "INSERT INTO podcast (nombre, id_genero) VALUES (?, ?)";
		
		try(Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql, new String[] {"id"})){
			pstmt.setString(1, podcast.getNombre());
			pstmt.setInt(2, podcast.getIdGenero());
			pstmt.executeUpdate();
			
			try (ResultSet rs = pstmt.getGeneratedKeys()){
				if(rs.next())
					podcast.setId(rs.getInt(1));
				return podcast.getId();
			}
		}
		
	}
	
	// BUSCAR POR NOMBRE
	@Override
	public Podcasts buscarPorNombre(String nombre) throws Exception{
		String sql = "SELECT id, nombre, id_genero FROM podcast WHERE nombre  = ?";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setString(1, nombre);
			
			try (ResultSet rs = pstmt.executeQuery()){
				if (rs.next())
					return new Podcasts(rs.getInt("id"), rs.getString("nombre"), rs.getInt("id_genero"));
			}
		}
		return null;
	}
	
	// LISTAR POR GÉNERO
	@Override
	public List<Podcasts> listarPorGenero(String nombreGenero) throws Exception{
		List<Podcasts> lista = new ArrayList<>();
		String sql = "SELECT p.id, p.nombre, p.id_genero "
				+ "FROM podcast p JOIN genero g ON p.id_genero = g.id "
				+ "WHERE g.nombre = ? ORDER BY p.nombre";
		
		try(Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setString(1, nombreGenero);
			
			try(ResultSet rs = pstmt.executeQuery()){
				while (rs.next()) {
					lista.add(new Podcasts(rs.getInt("id"), rs.getString("nombre"), rs.getInt("id_genero")));
				}
			}
		}
		
		return lista;
	}
	
	// RESUMEN POR GENERO
	@Override
	public List<ResumenPodcast> resumenPorGenero(String nombreGenero) throws Exception{
		List <ResumenPodcast> lista = new ArrayList<>();
		String sql = "SELECT p.nombre, COUNT(e.id) AS num_episodios, "
				+ "COALESCE(SUM(e.longitud_minutos), 0) AS duracion_total "
				+ "FROM prodcast p "
				+ "JOIN genero g ON p.id:genero = g.id "
				+ "LEFT JOIN episodio e ON e.id_podcast = p.id "
				+ "WHERE g.nombre = ? "
				+ "GROUP BY p.id, p.nombre ORDER BY p.nombre";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			try(ResultSet rs = pstmt.executeQuery()){
				while(rs.next()) {
					lista.add(new ResumenPodcast(rs.getString("nombre"),
							rs.getInt("num_episodios"), rs.getDouble("duracion_total")));
				}
			}
		}
		
		return lista;
	}
	
	// ELIMINAR
	@Override
	public boolean eliminar (int id) throws SQLException {
		String sql = "DELETE FROM podcast WHERE id = ?";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, id);
			return pstmt.executeUpdate() > 0;
		}	
	}

}
