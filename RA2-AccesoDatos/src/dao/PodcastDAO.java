package dao;

import java.sql.SQLException;
import java.util.List;

import modelo.Podcasts;
import modelo.ResumenPodcast;

public interface PodcastDAO {

	void crearTabla() throws Exception;
	int insertar(Podcasts podcast) throws Exception;
	Podcasts buscarPorNombre(String nombre) throws Exception;
	List<Podcasts> listarPorGenero(String nombreGenero) throws Exception;
	List<ResumenPodcast> resumenPorGenero(String nombreGenero) throws Exception;
	boolean eliminar(int id) throws SQLException;
}
