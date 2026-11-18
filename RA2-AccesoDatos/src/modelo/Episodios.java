package Modelo;

public class Episodios {

	private int id;
	private String titulo;
	private double longitudMinutos;
	private String diaPublicacion;
	private Momento momentoDia;
	private int numAnuncios;
	private Emocion clasificacionEmocional;
	private double tiempoEscuchaMedia;
	private int idPodcast;

	public Episodios(int id, String titulo, double longitudMinutos, String diaPublicacion, Momento momentoDia,
			int numAnuncios, Emocion clasificacionEmocional, double tiempoEscuchaMedia, int idPodcast) {
		super();
		this.id = id;
		this.titulo = titulo;
		this.longitudMinutos = longitudMinutos;
		this.diaPublicacion = diaPublicacion;
		this.momentoDia = momentoDia;
		this.numAnuncios = numAnuncios;
		this.clasificacionEmocional = clasificacionEmocional;
		this.tiempoEscuchaMedia = tiempoEscuchaMedia;
		this.idPodcast = idPodcast;
	}
	public int getId() {return id;}
	public void setId(int id) {this.id = id;}
	public String getTitulo() {return titulo;}
	public void setTitulo(String titulo) {this.titulo = titulo;}
	public double getLongitudMinutos() {return longitudMinutos;}
	public void setLongitudMinutos(double longitudMinutos) {this.longitudMinutos = longitudMinutos;}
	public String getDiaPublicacion() {return diaPublicacion;}
	public void setDiaPublicacion(String diaPublicacion) {this.diaPublicacion = diaPublicacion;}
	public Momento getMomentoDia() {return momentoDia;}
	public void setMomentoDia(Momento momentoDia) {this.momentoDia = momentoDia;}
	public int getNumAnuncios() {return numAnuncios;}
	public void setNumAnuncios(int numAnuncios) {this.numAnuncios = numAnuncios;}
	public Emocion getClasificacionEmocional() {return clasificacionEmocional;}
	public void setClasificacionEmocional(Emocion clasificacionEmocional) {this.clasificacionEmocional = clasificacionEmocional;}
	public double getTiempoEscuchaMedia() {return tiempoEscuchaMedia;}
	public void setTiempoEscuchaMedia(double tiempoEscuchaMedia) {this.tiempoEscuchaMedia = tiempoEscuchaMedia;}
	public int getIdPodcast() {return idPodcast;}
	public void setIdPodcast(int idPodcast) {this.idPodcast = idPodcast;}

	public String toString() {
		return "Episodio " + id + ": " + titulo + " (" + longitudMinutos + " min, " + diaPublicacion
				+ " " + momentoDia + ", " + clasificacionEmocional + ")";
	}
}