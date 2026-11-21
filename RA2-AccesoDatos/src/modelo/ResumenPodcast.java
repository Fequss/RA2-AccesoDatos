package modelo;

public class ResumenPodcast {
	// ATRIBUTOS
	private String nombre;
	private int numEpisodios;
	private double duracionTotal;
	
	// CONSTRUCTOR
	public ResumenPodcast(String nombre, int numEpisodios, double duracionTotal) {
		this.nombre = nombre;
		this.numEpisodios = numEpisodios;
		this.duracionTotal = duracionTotal;
	}

	// GETTERS
	public String getNombre() {
		return nombre;
	}

	public int getNumEpisodios() {
		return numEpisodios;
	}

	public double getDuracionTotal() {
		return duracionTotal;
	}
	
	// TO STRING
	@Override
	public String toString() {
		String resultado = "";
		resultado +="Nombre: " + this.nombre + " | Número de episodios: " + this.numEpisodios + " | Duración total: " + this.duracionTotal;
		return resultado;
	}
	
}
