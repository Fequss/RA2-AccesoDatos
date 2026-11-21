package dao.implementacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dao.EpisodioDAO;
import dao.PodcastNotFoundException;
import db.ConexionBD;
import modelo.Emocion;
import modelo.Episodios;
import modelo.Momento;

public class EpisodioDAOImp implements EpisodioDAO {

	private static final String SQL_INSERT = "INSERT INTO episodio (titulo, longitud_minutos, dia_publicacion,"
			+ "momento_dia, num_anuncios, emocion, tiempo_escucha, id_podcast) VALUES (?,?,?,?,?,?,?,?)";
	
	// MÉTODOS
	
	// CREAR TABLA
	@Override
	public void crearTabla() throws Exception{
		String sql = "CREATE TABLE IF NOT EXISTS episodio ("
				+ "id "+ConexionBD.autoincremental()+", "
				+ "titulo TEXT NOT NULL, "
				+ "longitud_minutos DOUBLE PRECISION NOT NULL CHECK (longitud_minutos>0), "
				+ "dia_publicacion TEXT, "
				+ "momento_dia TEXT CHECK (momento_dia IN ('MORNING','AFTERNOON','EVENING','NIGHT')),"
				+ "num_anuncios INTEGER CHECK (num_anuncios>=0)"
				+ "emocion TEXT NOT NULL CHECK (emocion IN ('POSITIVE','NEUTRAL','NEGATIVE' )),"
				+ "tiempo_escucha DOUBLE PRECISION CHECK (tiempo_escucha>=0), "
				+ "id_podcast INTEGER NOT NULL, "
				+ "FOREIGN KEY (id_podcast) REFERENCES podcast(id) ON UPDATE CASCADE ON DELETE CASCADE)";
		try (Connection conn = ConexionBD.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.execute();
		}
	}
	
	// INSERTAR
	@Override
	public int insertar (Episodios ep) throws Exception{
		// Validación previa
		if (ep.getTitulo()==null || ep.getTitulo().trim().isEmpty())
			throw new IllegalArgumentException("El dítulo del episodio es obligatorio");

		if (ep.getLongitudMinutos()<=0)
			throw new IllegalArgumentException("La duración debe ser un valor positivo");

		if(ep.getClasificacionEmocional()==null)
			throw new IllegalArgumentException("La clasificación emocional es obligatoria (Positive, Neutral o Negative)");

		try (Connection conn = ConexionBD.getConnection()){
			// Comprobar si el podcast Existe
			try(PreparedStatement chk = conn.prepareStatement("SELECT 1 FROM podcast WHERE id = ?")){
				chk.setInt(1, ep.getIdPodcast());
				try(ResultSet rs = chk.executeQuery()){
					if(!rs.next())
						throw new PodcastNotFoundException(ep.getIdPodcast());
				}
			}

			// Insertar parámetros indexados
			try(PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT, new String[] {"id"})){
				rellenar(pstmt,ep);
				pstmt.executeUpdate();

				try(ResultSet rs = pstmt.getGeneratedKeys()){
					if(rs.next())
						ep.setId(rs.getInt(1));
				}
			}
			return ep.getId();
		}
	}
	
	// INSERTAR LOTE
	@Override
	public void insertarLote(List<Episodios> episodios) throws Exception{
		try(Connection conn = ConexionBD.getConnection()){
			conn.setAutoCommit(false);
			try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT)){
				int pendientes = 0;
				for (Episodios ep : episodios) {
					rellenar(pstmt, ep);
					pstmt.addBatch();
					if (++pendientes%1000==0)
						pstmt.executeBatch();
				}
				pstmt.executeBatch();
				conn.commit();
			}catch(SQLException e) {
				conn.rollback();
				throw e;
			}
		}
	}
	
	// LISTAR POR PODCAST
	@Override
	public List<Episodios> listarPorPodcast(int idPodcast) throws Exception{
		List<Episodios> lista = new ArrayList<>();
		String sql = "SELECT id, titulo, longitud_minutos, dia_publicacion, momento_dia, "
				+ "num_anuncios, emocion, tiempo_escicha, id_podcast "
				+ "FROM episodio WHERE id_podcast = ? ORDER BY id";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setInt(1, idPodcast);
			try(ResultSet rs = pstmt.executeQuery()){
				while(rs.next()) {
					lista.add(new Episodios(
							rs.getInt("id"),
							rs.getString("titulo"),
							rs.getDouble("longitud_publicacion"),
							rs.getString("dia_publicacion"),
							Momento.fromTexto(rs.getString("momento_dia")),
							rs.getInt("num_anuncios"),
							Emocion.fromTexto(rs.getString("emocion")),
							rs.getDouble("tiempo_escucha"),
							rs.getInt("id_podcast")));
				}
			}
		}
		return lista;
	}
	// CONTAR POR EMOCION
	@Override
	public Map<Emocion, Integer> contarPorEmocion() throws Exception{
		Map<Emocion, Integer> resultado = new EnumMap<>(Emocion.class);
		for (Emocion e : Emocion.values())
			resultado.put(e, 0);
		
		String sql = "SELECT emocion, COULT(*) AS total FROM episodio GROUP BY emocion";
		
		try(Connection conn = ConexionBD.getConnection(); 
				PreparedStatement pstmt = conn.prepareStatement(sql); 
				ResultSet rs = pstmt.executeQuery()){
			while (rs.next()) {
				Emocion e = Emocion.fromTexto(rs.getString("emocion"));
				if (e != null)
					resultado.put(e, rs.getInt("total"));
			}
		}
		
		return resultado;
	}
	
	// RELLENAR
	private void rellenar (PreparedStatement pstmt, Episodios ep) throws SQLException{
		pstmt.setString(1, ep.getTitulo().trim());
		pstmt.setDouble(2, ep.getLongitudMinutos());
		pstmt.setString(3, ep.getDiaPublicacion());
		pstmt.setString(4, ep.getMomentoDia()==null? null:ep.getMomentoDia().name());
		pstmt.setInt(5, ep.getNumAnuncios());
		pstmt.setString(6, ep.getClasificacionEmocional().name());
		pstmt.setDouble(7, ep.getTiempoEscuchaMedia());
		pstmt.setInt(8, ep.getIdPodcast());
	}
	
	
}
