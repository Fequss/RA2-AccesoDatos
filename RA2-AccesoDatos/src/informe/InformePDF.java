package informe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.PieStyler;

import dao.EpisodioDAO;
import dao.implementacion.EpisodioDAOImp;
import modelo.Emocion;

/**
 * Genera InformeEmocional.pdf con la distribución de episodios por clasificación
 * emocional. Los datos se leen de la BD (EpisodioDAO.contarPorEmocion), nada va fijo.
 * El gráfico de tarta se crea con XChart y el documento se maqueta con PDFBox.
 */
public class InformePDF {

	public static final String FICHERO_PDF = "InformeEmocional.pdf";

	private static final float MARGEN = 70;

	private EpisodioDAO episodioDAO;

	public InformePDF() {
		this(new EpisodioDAOImp());
	}

	public InformePDF(EpisodioDAO episodioDAO) {
		this.episodioDAO = episodioDAO;
	}

	public void generar() throws Exception {

		// 1. Consulta agregada a la BD a través del DAO
		Map<Emocion, Integer> datos = episodioDAO.contarPorEmocion();
		int total = 0;
		for (Emocion e : Emocion.values())
			total += valor(datos, e);

		if (total == 0) {
			System.out.println("No hay episodios en la base de datos. Importa primero el CSV.");
			return;
		}

		// 2. Gráfico de tarta en memoria (PNG)
		byte[] png = crearGrafico(datos);

		// 3. Maquetación del PDF
		escribirPDF(datos, total, png);
		System.out.println("Informe generado: " + FICHERO_PDF);
	}

	// ---------------------------------------------------------------
	// GRÁFICO (XChart)
	// ---------------------------------------------------------------
	private byte[] crearGrafico(Map<Emocion, Integer> datos) throws Exception {
		PieChart chart = new PieChartBuilder()
				.width(600).height(400)
				.title("Distribución emocional de los episodios")
				.build();

		chart.getStyler().setAnnotationType(PieStyler.AnnotationType.LabelAndPercentage);

		for (Emocion e : Emocion.values())
			chart.addSeries(etiqueta(e), valor(datos, e));

		return BitmapEncoder.getBitmapBytes(chart, BitmapFormat.PNG);
	}

	// ---------------------------------------------------------------
	// DOCUMENTO (PDFBox)
	// ---------------------------------------------------------------
	private void escribirPDF(Map<Emocion, Integer> datos, int total, byte[] png) throws Exception {

		String fecha = LocalDateTime.now().format(
				DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy - HH:mm", new Locale("es", "ES")));

		try (PDDocument doc = new PDDocument()) {
			PDPage pagina = new PDPage(PDRectangle.A4);
			doc.addPage(pagina);

			PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			PDImageXObject imagen = PDImageXObject.createFromByteArray(doc, png, "grafico");

			float anchoPagina = pagina.getMediaBox().getWidth();

			try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
				float y = 790;

				// Título
				texto(cs, negrita, 15, MARGEN, y, "INFORME EJECUTIVO:");
				y -= 20;
				texto(cs, negrita, 15, MARGEN, y, "ANÁLISIS GLOBAL DE LA PLATAFORMA DE PODCASTS");
				y -= 30;

				// Fecha y hora de generación
				texto(cs, normal, 11, MARGEN, y, "Fecha y hora de generación: " + fecha);
				y -= 25;

				// Gráfico centrado
				float anchoImg = 420, altoImg = 280;
				y -= altoImg;
				cs.drawImage(imagen, (anchoPagina - anchoImg) / 2, y, anchoImg, altoImg);
				y -= 35;

				// Tabla: Efecto | Episodios | %
				float x1 = MARGEN, x2 = MARGEN + 170, x3 = MARGEN + 290;
				texto(cs, negrita, 12, x1, y, "Efecto");
				texto(cs, negrita, 12, x2, y, "Episodios");
				texto(cs, negrita, 12, x3, y, "%");
				y -= 6;
				linea(cs, MARGEN, y, anchoPagina - MARGEN);
				y -= 16;

				for (Emocion e : Emocion.values()) {
					int n = valor(datos, e);
					texto(cs, normal, 12, x1, y, etiqueta(e));
					texto(cs, normal, 12, x2, y, String.valueOf(n));
					texto(cs, normal, 12, x3, y, porcentaje(n, total));
					y -= 18;
				}

				linea(cs, MARGEN, y + 12, anchoPagina - MARGEN);
				y -= 4;
				texto(cs, negrita, 12, x1, y, "Total");
				texto(cs, negrita, 12, x2, y, String.valueOf(total));
				texto(cs, negrita, 12, x3, y, "100.0%");
			}

			doc.save(FICHERO_PDF);
		}
	}

	private void texto(PDPageContentStream cs, PDFont fuente, float tam, float x, float y, String t)
			throws Exception {
		cs.beginText();
		cs.setFont(fuente, tam);
		cs.newLineAtOffset(x, y);
		cs.showText(t);
		cs.endText();
	}

	private void linea(PDPageContentStream cs, float x1, float y, float x2) throws Exception {
		cs.setLineWidth(0.8f);
		cs.moveTo(x1, y);
		cs.lineTo(x2, y);
		cs.stroke();
	}

	// ---------------------------------------------------------------
	// AUXILIARES
	// ---------------------------------------------------------------
	private int valor(Map<Emocion, Integer> datos, Emocion e) {
		Integer n = datos.get(e);
		return n == null ? 0 : n;
	}

	// POSITIVE -> Positive
	private String etiqueta(Emocion e) {
		String n = e.name();
		return n.charAt(0) + n.substring(1).toLowerCase();
	}

	private String porcentaje(int n, int total) {
		return String.format(Locale.US, "%.1f%%", 100.0 * n / total);
	}
}