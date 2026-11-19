package dao.implementacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import dao.GeneroDAO;
import db.ConexionBD;
import modelo.Genero;

public class GeneroDAOImp implements GeneroDAO{
	
	// CREAR TABLA
	@Override
	public void crearTabla() throws Exception{
		String sql = "CREATE TABLE IF NOT EXISTS genero ("
				+ "id " + ConexionBD.autoincremental() + ", "
				+ "nombre TEXT NOT NULL UNIQUE)";
		try (Connection conn = ConexionBD.getConnection(); 
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.execute();
		}
	}
 	
	// INSERTAR
	@Override
	public int insertar(Genero genero) throws Exception{
		String sql = "INSERT INTO genero (nombre) VALUES (?)";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql, new String[] {"id"})){
			pstmt.setString(1, genero.getNombre());
			pstmt.executeUpdate();
			
			try(ResultSet rs = pstmt.getGeneratedKeys()){
				if(rs.next())
					genero.setId(rs.getInt(1));
			}
			
			return genero.getId();
		}
	}
	
	// BUSCAR POR NOMBRE
	@Override
	public Genero buscarPorNombre(String nombre) throws Exception {
		String sql = "SELECT id, nombre FROM genero WHERE nombre = ?";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setString(1, nombre);
			try(ResultSet rs = pstmt.executeQuery()){
				if(rs.next())
					return new Genero(rs.getInt("id"), rs.getString("nombre"));
			}
		}
		
		return null;
	}
	
	// LISTAR TODOS
	@Override
	public List<Genero> listarTodos() throws Exception{
		List<Genero> lista = new ArrayList<>();
		String sql = "SELECT id, nombre FROM genero ORDER BY nombre";
		
		try (Connection conn = ConexionBD.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()){
			while(rs.next()) {
				lista.add(new Genero(rs.getInt("id"), rs.getString("nombre")));
			}
		}
		
		return lista;
	}

}
