package modelo;

public class Podcasts {

	private int id;
	private String nombre;
	private int idGenero;

	public Podcasts(int id, String nombre, int idGenero) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.idGenero = idGenero;
	}
	public int getId() {return id;}
	public void setId(int id) {this.id = id;}
	public String getNombre() {return nombre;}
	public void setNombre(String nombre) {this.nombre = nombre;}
	public int getIdGenero() {return idGenero;}
	public void setIdGenero(int idGenero) {this.idGenero = idGenero;}

	public String toString() {
		return "Podcast " + id + ": " + nombre + " (género " + idGenero + ")";
	}
}