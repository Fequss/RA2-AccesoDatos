package dao;

import java.util.List;
import java.util.Map;

import modelo.Emocion;
import modelo.Episodios;

public interface EpisodioDAO {

	void crearTabla() throws Exception;
	int insertar(Episodios episodio) throws Exception;
	void insertarLote(List<Episodios> episodios) throws Exception;
	List<Episodios> listarPorPodcast(int idPodcast) throws Exception;
	Map<Emocion, Integer> contarPorEmocion() throws Exception;
}
