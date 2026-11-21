package importacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa UNA fila del CSV original (plano y desnormalizado) tal y como viene en el fichero.
 * Todos los campos se guardan como String porque el CSV puede contener "datos sucios"
 * (por ejemplo, texto donde debería haber minutos). La validación y la conversión
 * a los objetos del modelo (Genero, Podcasts, Episodios) las realiza ImportarCSV.
 * Además guarda la línea original y los errores detectados en esa fila.
 */
public class RegistroCSV {

	public static final String SEPARADOR = ",";
	public static final int NUM_CAMPOS = 11;

	// ATRIBUTOS DE CONTROL
	private int numLinea;
	private String lineaOriginal;
	private int camposLeidos;
	private List<String> errores = new ArrayList<>();

	// ATRIBUTOS (una columna del CSV cada uno, en el mismo orden)
	private String nombrePodcast = "";
	private String titulo = "";
	private String longitud = "";
	private String genero = "";
	private String popularidadPresentador = "";
	private String diaPublicacion = "";
	private String momentoPublicacion = "";
	private String popularidadInvitado = "";
	private String numAnuncios = "";
	private String sentimiento = "";
	private String tiempoEscucha = "";

	// CONSTRUCTOR
	public RegistroCSV(int numLinea, String lineaOriginal) {
		super();
		this.numLinea = numLinea;
		this.lineaOriginal = lineaOriginal;

		// El -1 hace que split conserve los campos vacíos del final de la línea
		String[] c = lineaOriginal.split(SEPARADOR, -1);
		this.camposLeidos = c.length;

		// Solo se rellenan los atributos si la fila tiene el número de columnas esperado
		if (c.length == NUM_CAMPOS) {
			this.nombrePodcast = c[0].trim();
			this.titulo = c[1].trim();
			this.longitud = c[2].trim();
			this.genero = c[3].trim();
			this.popularidadPresentador = c[4].trim();
			this.diaPublicacion = c[5].trim();
			this.momentoPublicacion = c[6].trim();
			this.popularidadInvitado = c[7].trim();
			this.numAnuncios = c[8].trim();
			this.sentimiento = c[9].trim();
			this.tiempoEscucha = c[10].trim();
		}
	}

	// GESTIÓN DE ERRORES
	public void addError(String error) {errores.add(error);}
	public boolean tieneErrores() {return !errores.isEmpty();}
	public List<String> getErrores() {return errores;}

	// GETTERS
	public int getNumLinea() {return numLinea;}
	public String getLineaOriginal() {return lineaOriginal;}
	public int getCamposLeidos() {return camposLeidos;}
	public String getNombrePodcast() {return nombrePodcast;}
	public String getTitulo() {return titulo;}
	public String getLongitud() {return longitud;}
	public String getGenero() {return genero;}
	public String getPopularidadPresentador() {return popularidadPresentador;}
	public String getDiaPublicacion() {return diaPublicacion;}
	public String getMomentoPublicacion() {return momentoPublicacion;}
	public String getPopularidadInvitado() {return popularidadInvitado;}
	public String getNumAnuncios() {return numAnuncios;}
	public String getSentimiento() {return sentimiento;}
	public String getTiempoEscucha() {return tiempoEscucha;}

	// TO STRING
	@Override
	public String toString() {
		return "Línea " + numLinea + ": " + nombrePodcast + " | " + titulo + " | " + genero;
	}
}