package exportacion;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dao.EpisodioDAO;
import dao.GeneroDAO;
import dao.PodcastDAO;
import dao.implementacion.EpisodioDAOImp;
import dao.implementacion.GeneroDAOImp;
import dao.implementacion.PodcastDAOImp;
import modelo.Episodios;
import modelo.Genero;
import modelo.Podcasts;

/**
 * Exporta a JSON los podcasts de un género, con sus episodios anidados
 * dentro de cada podcast (sin repetir los datos del podcast en cada episodio).
 * Antes de escribir el fichero se valida que el JSON es sintácticamente correcto.
 */
public class ExportadorJSON {

	private GeneroDAO generoDAO;
	private PodcastDAO podcastDAO;
	private EpisodioDAO episodioDAO;

	public ExportadorJSON() {
		this(new GeneroDAOImp(), new PodcastDAOImp(), new EpisodioDAOImp());
	}

	public ExportadorJSON(GeneroDAO generoDAO, PodcastDAO podcastDAO, EpisodioDAO episodioDAO) {
		this.generoDAO = generoDAO;
		this.podcastDAO = podcastDAO;
		this.episodioDAO = episodioDAO;
	}

	/** Devuelve el fichero generado, o null si el género no existe. */
	public File exportar(String nombreGenero) throws Exception {

		Genero genero = buscarGenero(nombreGenero);
		if (genero == null) {
			System.out.println("No existe el género '" + nombreGenero + "'.");
			return null;
		}

		List<Podcasts> podcasts = podcastDAO.listarPorGenero(genero.getNombre());
		String json = construirJSON(genero, podcasts);

		// Validación previa a la escritura
		if (!ValidadorJSON.esValido(json))
			throw new IllegalStateException("El JSON generado no es sintácticamente correcto");

		File fichero = new File("exportacion_" + genero.getNombre().replaceAll("[^A-Za-z0-9]+", "_") + ".json");
		try (BufferedWriter bw = Files.newBufferedWriter(fichero.toPath(), StandardCharsets.UTF_8)) {
			bw.write(json);
		}
		System.out.println("Exportados " + podcasts.size() + " podcasts a '" + fichero.getName() + "'.");
		return fichero;
	}

	// Busca sin distinguir mayúsculas, para que "technology" encuentre "Technology"
	private Genero buscarGenero(String nombre) throws Exception {
		if (nombre == null) return null;
		for (Genero g : generoDAO.listarTodos())
			if (g.getNombre().equalsIgnoreCase(nombre.trim()))
				return g;
		return null;
	}

	// ---------------------------------------------------------------
	// CONSTRUCCIÓN DEL JSON (con sangrado para que sea legible)
	// ---------------------------------------------------------------
	private String construirJSON(Genero genero, List<Podcasts> podcasts) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"genero\": ").append(texto(genero.getNombre())).append(",\n");
		sb.append("  \"totalPodcasts\": ").append(podcasts.size()).append(",\n");
		sb.append("  \"podcasts\": [");

		for (int i = 0; i < podcasts.size(); i++) {
			Podcasts p = podcasts.get(i);
			List<Episodios> episodios = episodioDAO.listarPorPodcast(p.getId());

			sb.append(i == 0 ? "\n" : ",\n");
			sb.append("    {\n");
			sb.append("      \"id\": ").append(p.getId()).append(",\n");
			sb.append("      \"nombre\": ").append(texto(p.getNombre())).append(",\n");
			sb.append("      \"episodios\": [");

			for (int j = 0; j < episodios.size(); j++) {
				Episodios e = episodios.get(j);
				sb.append(j == 0 ? "\n" : ",\n");
				sb.append("        {\n");
				sb.append("          \"id\": ").append(e.getId()).append(",\n");
				sb.append("          \"titulo\": ").append(texto(e.getTitulo())).append(",\n");
				sb.append("          \"longitudMinutos\": ").append(e.getLongitudMinutos()).append(",\n");
				sb.append("          \"diaPublicacion\": ").append(texto(e.getDiaPublicacion())).append(",\n");
				sb.append("          \"momentoDia\": ").append(texto(nombreEnum(e.getMomentoDia()))).append(",\n");
				sb.append("          \"numAnuncios\": ").append(e.getNumAnuncios()).append(",\n");
				sb.append("          \"clasificacionEmocional\": ")
						.append(texto(nombreEnum(e.getClasificacionEmocional()))).append(",\n");
				sb.append("          \"tiempoEscuchaMedia\": ").append(e.getTiempoEscuchaMedia()).append("\n");
				sb.append("        }");
			}
			sb.append(episodios.isEmpty() ? "]" : "\n      ]");
			sb.append("\n    }");
		}
		sb.append(podcasts.isEmpty() ? "]" : "\n  ]");
		sb.append("\n}\n");
		return sb.toString();
	}

	private String nombreEnum(Enum<?> e) {
		return e == null ? null : e.name();
	}

	// Cadena JSON entre comillas y con los caracteres especiales escapados
	private String texto(String s) {
		if (s == null) return "null";
		StringBuilder sb = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			switch (c) {
			case '"': sb.append("\\\""); break;
			case '\\': sb.append("\\\\"); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\t': sb.append("\\t"); break;
			default:
				if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
				else sb.append(c);
			}
		}
		return sb.append('"').toString();
	}

	// ---------------------------------------------------------------
	// VALIDADOR DE SINTAXIS JSON (descenso recursivo)
	// ---------------------------------------------------------------
	private static class ValidadorJSON {

		private static final Pattern NUMERO = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");

		private final String s;
		private int i = 0;

		private ValidadorJSON(String s) {
			this.s = s;
		}

		static boolean esValido(String json) {
			try {
				ValidadorJSON v = new ValidadorJSON(json);
				v.espacios();
				v.valor();
				v.espacios();
				return v.i == v.s.length(); // no puede sobrar texto
			} catch (IllegalStateException e) {
				return false;
			}
		}

		private char actual() {
			if (i >= s.length()) throw new IllegalStateException("Fin inesperado");
			return s.charAt(i);
		}

		private void esperar(char c) {
			if (actual() != c) throw new IllegalStateException("Se esperaba " + c);
			i++;
		}

		private void espacios() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
		}

		private void valor() {
			char c = actual();
			if (c == '{') objeto();
			else if (c == '[') array();
			else if (c == '"') cadena();
			else if (s.startsWith("true", i)) i += 4;
			else if (s.startsWith("false", i)) i += 5;
			else if (s.startsWith("null", i)) i += 4;
			else numero();
		}

		private void objeto() {
			esperar('{');
			espacios();
			if (actual() == '}') { i++; return; }
			while (true) {
				espacios();
				if (actual() != '"') throw new IllegalStateException("Se esperaba una clave");
				cadena();
				espacios();
				esperar(':');
				espacios();
				valor();
				espacios();
				if (actual() == ',') { i++; continue; }
				esperar('}');
				return;
			}
		}

		private void array() {
			esperar('[');
			espacios();
			if (actual() == ']') { i++; return; }
			while (true) {
				espacios();
				valor();
				espacios();
				if (actual() == ',') { i++; continue; }
				esperar(']');
				return;
			}
		}

		private void cadena() {
			esperar('"');
			while (true) {
				char c = actual();
				if (c == '"') { i++; return; }
				if (c < 0x20) throw new IllegalStateException("Carácter de control sin escapar");
				if (c == '\\') {
					i++;
					char e = actual();
					if ("\"\\/bfnrt".indexOf(e) >= 0) {
						i++;
					} else if (e == 'u') {
						i++;
						for (int k = 0; k < 4; k++) {
							if (Character.digit(actual(), 16) < 0) throw new IllegalStateException("Escape \\u no válido");
							i++;
						}
					} else {
						throw new IllegalStateException("Escape no válido");
					}
				} else {
					i++;
				}
			}
		}

		private void numero() {
			Matcher m = NUMERO.matcher(s);
			m.region(i, s.length());
			if (!m.lookingAt()) throw new IllegalStateException("Valor no válido");
			i = m.end();
		}
	}
}