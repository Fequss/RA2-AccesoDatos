package Modelo;

public enum Momento {
	MORNING, AFTERNOON, EVENING, NIGHT;

	public static Momento fromTexto(String texto) {
		if (texto == null) return null;
		try {
			return Momento.valueOf(texto.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}