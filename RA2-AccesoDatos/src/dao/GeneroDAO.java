package dao;

import java.util.List;

import modelo.Genero;

public interface GeneroDAO {

	void crearTabla() throws Exception;
	int insertar(Genero genero) throws Exception;
	Genero buscarPorNombre(String nombre) throws Exception;
	List<Genero> listarTodos() throws Exception;
}
