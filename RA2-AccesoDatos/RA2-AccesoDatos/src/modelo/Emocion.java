package modelo;

public enum Emocion {
	POSITIVE, NEUTRAL, NEGATIVE;

	public static Emocion fromTexto(String texto) {
		if (texto == null) return null;
		try {
			return Emocion.valueOf(texto.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}