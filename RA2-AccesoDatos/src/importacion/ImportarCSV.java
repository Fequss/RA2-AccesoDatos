package importacion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.EpisodioDAO;
import dao.GeneroDAO;
import dao.PodcastDAO;
import dao.implementacion.EpisodioDAOImp;
import dao.implementacion.GeneroDAOImp;
import dao.implementacion.PodcastDAOImp;
import modelo.Emocion;
import modelo.Episodios;
import modelo.Genero;
import modelo.Momento;
import modelo.Podcasts;

/**
 * Importación masiva del CSV desnormalizado a las tablas genero, podcast y episodio.
 * Solo se importan las filas 100% correctas y se generan dos ficheros de salida:
 *  - informeErrores.txt  : descripción de todas las inconsistencias detectadas
 *  - inconsistencias.csv : filas originales que no se han podido importar
 * Trabaja siempre contra las interfaces DAO (GeneroDAO, PodcastDAO, EpisodioDAO).
 */
public class ImportarCSV {

	// FICHEROS DE SALIDA
	public static final String FICHERO_ERRORES = "informeErrores.txt";
	public static final String FICHERO_DESCARTES = "inconsistencias.csv";

	// TIPOS DE INCONSISTENCIA (los tres bloques del enunciado)
	private static final String ERR_INTEGRIDAD = "[INTEGRIDAD LOGICA]";
	private static final String ERR_OBLIGATORIO = "[CAMPO OBLIGATORIO]";
	private static final String ERR_FORMATO = "[DATO INCOHERENTE]";

	private static final List<String> DIAS = Arrays.asList(
			"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

	// Polimorfismo: interfaz = implementación
	private GeneroDAO generoDAO;
	private PodcastDAO podcastDAO;
	private EpisodioDAO episodioDAO;

	// CONSTRUCTORES
	public ImportarCSV() {
		this(new GeneroDAOImp(), new PodcastDAOImp(), new EpisodioDAOImp());
	}

	public ImportarCSV(GeneroDAO generoDAO, PodcastDAO podcastDAO, EpisodioDAO episodioDAO) {
		this.generoDAO = generoDAO;
		this.podcastDAO = podcastDAO;
		this.episodioDAO = episodioDAO;
	}

	// IMPORTAR (devuelve el número de episodios importados)
	public int importar(String rutaCsv) throws Exception {

		// 1. Las tablas deben existir antes de insertar (orden por las claves foráneas)
		generoDAO.crearTabla();
		podcastDAO.crearTabla();
		episodioDAO.crearTabla();

		// 2. Leer el CSV línea a línea
		List<RegistroCSV> registros = new ArrayList<>();
		String cabecera;
		try (BufferedReader br = new BufferedReader(new FileReader(rutaCsv))) {
			cabecera = br.readLine();
			if (cabecera == null)
				throw new IOException("El fichero CSV está vacío");
			cabecera = cabecera.replace("\uFEFF", ""); // por si el fichero trae BOM

			String linea;
			int numLinea = 1; // la cabecera es la línea 1
			while ((linea = br.readLine()) != null) {
				numLinea++;
				if (!linea.trim().isEmpty())
					registros.add(new RegistroCSV(numLinea, linea));
			}
		}

		// 3. Validar cada fila (obligatorios y tipos de datos)
		for (RegistroCSV r : registros)
			validar(r);

		// 4. Detectar podcasts con géneros distintos (multigénero)
		detectarMultigenero(registros);

		// 5. Separar correctos y descartados
		List<RegistroCSV> validos = new ArrayList<>();
		List<RegistroCSV> descartados = new ArrayList<>();
		for (RegistroCSV r : registros) {
			if (r.tieneErrores())
				descartados.add(r);
			else
				validos.add(r);
		}

		// 6. Guardar en la base de datos solo los correctos
		int importados = guardar(validos);

		// 7. Generar los ficheros de salida
		escribirInformeErrores(rutaCsv, registros.size(), importados, descartados);
		escribirDescartes(cabecera, descartados);

		System.out.println("Importación finalizada: " + importados + " episodios importados, "
				+ descartados.size() + " filas descartadas.");
		System.out.println("Revisa '" + FICHERO_ERRORES + "' y '" + FICHERO_DESCARTES + "'.");
		return importados;
	}

	// ---------------------------------------------------------------
	// VALIDACIÓN
	// ---------------------------------------------------------------
	private void validar(RegistroCSV r) {

		// Número de columnas incorrecto: no se puede validar nada más
		if (r.getCamposLeidos() != RegistroCSV.NUM_CAMPOS) {
			r.addError(ERR_FORMATO + " Se esperaban " + RegistroCSV.NUM_CAMPOS
					+ " columnas y la fila tiene " + r.getCamposLeidos());
			return;
		}

		// Campos obligatorios
		if (r.getNombrePodcast().isEmpty())
			r.addError(ERR_OBLIGATORIO + " El nombre del podcast está vacío");
		if (r.getTitulo().isEmpty())
			r.addError(ERR_OBLIGATORIO + " El título del episodio está vacío");
		if (r.getGenero().isEmpty())
			r.addError(ERR_OBLIGATORIO + " El género está vacío");

		// Duración: obligatoria, numérica y positiva
		if (r.getLongitud().isEmpty()) {
			r.addError(ERR_OBLIGATORIO + " La duración del episodio está vacía");
		} else if (!esNumero(r.getLongitud())) {
			r.addError(ERR_FORMATO + " La duración no es un número válido: '" + r.getLongitud() + "'");
		} else if (Double.parseDouble(r.getLongitud()) <= 0) {
			r.addError(ERR_FORMATO + " La duración debe ser positiva: '" + r.getLongitud() + "'");
		}

		// Clasificación emocional: obligatoria y dentro del dominio (POSITIVE, NEUTRAL, NEGATIVE)
		if (r.getSentimiento().isEmpty()) {
			r.addError(ERR_OBLIGATORIO + " La clasificación emocional está vacía");
		} else if (Emocion.fromTexto(r.getSentimiento()) == null) {
			r.addError(ERR_FORMATO + " Clasificación emocional fuera del dominio: '" + r.getSentimiento() + "'");
		}

		// Día de publicación: opcional, pero si viene debe ser un día de la semana
		if (!r.getDiaPublicacion().isEmpty() && !DIAS.contains(r.getDiaPublicacion().toUpperCase()))
			r.addError(ERR_FORMATO + " Día de publicación no válido: '" + r.getDiaPublicacion() + "'");

		// Momento del día: opcional, pero si viene debe ser MORNING, AFTERNOON, EVENING o NIGHT
		if (!r.getMomentoPublicacion().isEmpty() && Momento.fromTexto(r.getMomentoPublicacion()) == null)
			r.addError(ERR_FORMATO + " Momento del día no válido: '" + r.getMomentoPublicacion() + "'");

		// Número de anuncios: opcional, pero si viene debe ser un entero >= 0
		if (!r.getNumAnuncios().isEmpty()) {
			if (!r.getNumAnuncios().matches("-?\\d+"))
				r.addError(ERR_FORMATO + " El número de anuncios no es un entero: '" + r.getNumAnuncios() + "'");
			else if (Integer.parseInt(r.getNumAnuncios()) < 0)
				r.addError(ERR_FORMATO + " El número de anuncios no puede ser negativo: '" + r.getNumAnuncios() + "'");
		}

		// Tiempo de escucha: opcional, pero si viene debe ser un número >= 0
		if (!r.getTiempoEscucha().isEmpty()) {
			if (!esNumero(r.getTiempoEscucha()))
				r.addError(ERR_FORMATO + " El tiempo de escucha no es un número: '" + r.getTiempoEscucha() + "'");
			else if (Double.parseDouble(r.getTiempoEscucha()) < 0)
				r.addError(ERR_FORMATO + " El tiempo de escucha no puede ser negativo: '" + r.getTiempoEscucha() + "'");
		}

		// Popularidades (no se guardan en la BD, pero se comprueba que sean porcentajes válidos)
		validarPorcentaje("Host_Popularity_percentage", r.getPopularidadPresentador(), r);
		validarPorcentaje("Guest_Popularity_percentage", r.getPopularidadInvitado(), r);
	}

	private void validarPorcentaje(String campo, String valor, RegistroCSV r) {
		if (valor.isEmpty()) return;
		if (!esNumero(valor))
			r.addError(ERR_FORMATO + " " + campo + " no es un número: '" + valor + "'");
		else if (Double.parseDouble(valor) < 0 || Double.parseDouble(valor) > 100)
			r.addError(ERR_FORMATO + " " + campo + " fuera de rango (0-100): '" + valor + "'");
	}

	// Número decimal con signo opcional: 12, -3, 45.67
	private boolean esNumero(String texto) {
		return texto.matches("-?\\d+(\\.\\d+)?");
	}

	// ---------------------------------------------------------------
	// MULTIGÉNERO
	// ---------------------------------------------------------------
	private void detectarMultigenero(List<RegistroCSV> registros) {

		// Para cada podcast, lista de géneros distintos con los que aparece
		Map<String, List<String>> generosPorPodcast = new HashMap<>();
		for (RegistroCSV r : registros) {
			if (r.getNombrePodcast().isEmpty() || r.getGenero().isEmpty()) continue;

			List<String> generos = generosPorPodcast.get(r.getNombrePodcast());
			if (generos == null) {
				generos = new ArrayList<>();
				generosPorPodcast.put(r.getNombrePodcast(), generos);
			}
			if (!generos.contains(r.getGenero()))
				generos.add(r.getGenero());
		}

		// Si un podcast tiene más de un género no sabemos cuál es el correcto,
		// así que se descartan TODAS sus filas
		for (RegistroCSV r : registros) {
			List<String> generos = generosPorPodcast.get(r.getNombrePodcast());
			if (generos != null && generos.size() > 1)
				r.addError(ERR_INTEGRIDAD + " El podcast '" + r.getNombrePodcast()
						+ "' aparece con géneros distintos: " + generos);
		}
	}

	// ---------------------------------------------------------------
	// PERSISTENCIA
	// ---------------------------------------------------------------
	private int guardar(List<RegistroCSV> validos) throws Exception {

		// Cachés nombre -> id para no consultar la BD en cada una de las miles de filas
		Map<String, Integer> idsGenero = new HashMap<>();
		Map<String, Integer> idsPodcast = new HashMap<>();
		List<Episodios> episodios = new ArrayList<>();

		for (RegistroCSV r : validos) {

			// Género (se crea solo si no existe)
			Integer idGenero = idsGenero.get(r.getGenero());
			if (idGenero == null) {
				idGenero = obtenerOCrearGenero(r.getGenero());
				idsGenero.put(r.getGenero(), idGenero);
			}

			// Podcast (se crea solo si no existe)
			Integer idPodcast = idsPodcast.get(r.getNombrePodcast());
			if (idPodcast == null) {
				idPodcast = obtenerOCrearPodcast(r.getNombrePodcast(), idGenero);
				idsPodcast.put(r.getNombrePodcast(), idPodcast);
			}

			episodios.add(aEpisodio(r, idPodcast));
		}

		// Inserción por lotes (la transacción la gestiona EpisodioDAOImp.insertarLote)
		if (!episodios.isEmpty())
			episodioDAO.insertarLote(episodios);
		return episodios.size();
	}

	private int obtenerOCrearGenero(String nombre) throws Exception {
		Genero g = generoDAO.buscarPorNombre(nombre);
		if (g != null)
			return g.getId();
		return generoDAO.insertar(new Genero(0, nombre));
	}

	private int obtenerOCrearPodcast(String nombre, int idGenero) throws Exception {
		Podcasts p = podcastDAO.buscarPorNombre(nombre);
		if (p != null)
			return p.getId();
		return podcastDAO.insertar(new Podcasts(0, nombre, idGenero));
	}

	// Convierte una fila ya validada en un objeto del modelo
	private Episodios aEpisodio(RegistroCSV r, int idPodcast) {
		String dia = r.getDiaPublicacion().isEmpty() ? null : r.getDiaPublicacion();
		int anuncios = r.getNumAnuncios().isEmpty() ? 0 : Integer.parseInt(r.getNumAnuncios());
		double escucha = r.getTiempoEscucha().isEmpty() ? 0 : Double.parseDouble(r.getTiempoEscucha());

		return new Episodios(0, r.getTitulo(), Double.parseDouble(r.getLongitud()), dia,
				Momento.fromTexto(r.getMomentoPublicacion()), anuncios,
				Emocion.fromTexto(r.getSentimiento()), escucha, idPodcast);
	}

	// ---------------------------------------------------------------
	// FICHEROS DE SALIDA
	// ---------------------------------------------------------------
	private void escribirInformeErrores(String rutaCsv, int leidas, int importadas,
			List<RegistroCSV> descartados) throws IOException {

		// Contamos errores por tipo (una fila puede tener varios)
		int nIntegridad = 0, nObligatorio = 0, nFormato = 0;
		for (RegistroCSV r : descartados) {
			for (String e : r.getErrores()) {
				if (e.startsWith(ERR_INTEGRIDAD)) nIntegridad++;
				else if (e.startsWith(ERR_OBLIGATORIO)) nObligatorio++;
				else nFormato++;
			}
		}

		String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(FICHERO_ERRORES))) {
			bw.write("============================================================"); bw.newLine();
			bw.write(" INFORME DE ERRORES DE IMPORTACION"); bw.newLine();
			bw.write("============================================================"); bw.newLine();
			bw.write("Fichero origen: " + rutaCsv); bw.newLine();
			bw.write("Fecha: " + fecha); bw.newLine();
			bw.newLine();
			bw.write("RESUMEN"); bw.newLine();
			bw.write("  Filas leídas (sin cabecera): " + leidas); bw.newLine();
			bw.write("  Filas importadas: " + importadas); bw.newLine();
			bw.write("  Filas descartadas: " + descartados.size()); bw.newLine();
			bw.write("  Errores de integridad lógica (multigénero): " + nIntegridad); bw.newLine();
			bw.write("  Errores de campos obligatorios vacíos: " + nObligatorio); bw.newLine();
			bw.write("  Errores de datos incoherentes o corruptos: " + nFormato); bw.newLine();
			bw.newLine();
			bw.write("DETALLE (una fila puede acumular varios errores)"); bw.newLine();
			bw.write("------------------------------------------------------------"); bw.newLine();

			for (RegistroCSV r : descartados) {
				bw.write("Línea " + r.getNumLinea() + " | Podcast: " + mostrar(r.getNombrePodcast())
						+ " | Episodio: " + mostrar(r.getTitulo()));
				bw.newLine();
				for (String e : r.getErrores()) {
					bw.write("     " + e);
					bw.newLine();
				}
			}
		}
	}

	private void escribirDescartes(String cabecera, List<RegistroCSV> descartados) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(FICHERO_DESCARTES))) {
			bw.write(cabecera);
			bw.newLine();
			for (RegistroCSV r : descartados) {
				bw.write(r.getLineaOriginal()); // fila original, sin modificar
				bw.newLine();
			}
		}
	}

	private String mostrar(String texto) {
		return texto.isEmpty() ? "(vacío)" : texto;
	}
}