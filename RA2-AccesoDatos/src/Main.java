

import java.io.File;

import org.knowm.xchart.ChartEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class Main 
{
    public static void main(String[] args) 
    {
        String tempImgPath = "./grafico_temporal";
        String fullImgPath = "./grafico_temporal.png";
        String pdfOutputPath = "Informe_Podcasts.pdf";

        PDFont fuenteNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont fuenteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
         // Desactivar los mensajes de información y advertencias de PDFBox y FontBox
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.fontbox").setLevel(java.util.logging.Level.SEVERE);

        try 
        {
            // --- PASO 1: Generar el gráfico estadístico en segundo plano ---
            PieChart chart = new PieChartBuilder()
                    .width(450)
                    .height(350)
                    .title("Distribución de Episodios por Género")
                    .build();
            
            // Datos de prueba (aquí irían los datos procesados desde los DAOs)
            chart.addSeries("Technology", 120);
            chart.addSeries("True Crime", 80);
            chart.addSeries("History", 45);

            // Guardamos el gráfico como una imagen temporal en el disco
            ChartEncoder.saveChart(chart, tempImgPath, "png");

            // --- PASO 2: Crear el documento PDF estructurado (Uso de Try-with-resources) ---
            try (PDDocument document = new PDDocument()) 
            {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) 
                {
                    // Coordenada de inicio vertical (El origen 0,0 en PDFBox está abajo a la izquierda)
                    float currentY = 730; 

                    // 2.1. Título del Informe
                    contentStream.beginText();
                    contentStream.setFont(fuenteNegrita, 22);
                    contentStream.newLineAtOffset(80, currentY);
                    contentStream.showText("INFORME ESTADÍSTICO DE PODCASTS");
                    contentStream.endText();

                    // 2.2. Línea divisoria decorativa
                    currentY -= 20;
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(80, currentY);
                    contentStream.lineTo(530, currentY);
                    contentStream.stroke();

                    // 2.3. Inserción del Gráfico Estadístico
                    currentY -= 380; // Espacio suficiente para albergar la altura del gráfico (350)
                    PDImageXObject pdfImage = PDImageXObject.createFromFile(fullImgPath, document);
                    
                    // Centramos el gráfico horizontalmente (Ancho de página estándar es ~612 puntos)
                    float xCentrado = (612 - 450) / 2; 
                    contentStream.drawImage(pdfImage, xCentrado, currentY);
                }

                // Guardamos el PDF definitivo una vez cerrado el flujo de contenido
                document.save(pdfOutputPath);
            }

            System.out.println("¡PDF '" + pdfOutputPath + "' generado con éxito!");

        } catch (Exception e) {
            System.err.println("Error crítico durante la generación del informe: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // --- PASO 3: Limpieza estricta del entorno ---
            File tempFile = new File(fullImgPath);
            if (tempFile.exists()) {
                boolean eliminado = tempFile.delete();
                if (!eliminado) {
                    System.err.println("Advertencia: No se pudo eliminar el archivo temporal del gráfico.");
                }
            }
        }
    }
}

/*

public class Main {
    public static void main(String[] args) throws Exception {
        // --- PASO 1: Generar el gráfico estadístico en segundo plano ---
        PieChart chart = new PieChartBuilder().width(400).height(300).title("Distribución por Género").build();
        chart.addSeries("Technology", 120);
        chart.addSeries("True Crime", 80);

        // Guardamos el gráfico como una imagen temporal en el disco
        ChartEncoder.saveChart(chart, "./grafico_temporal", "png");

        // --- PASO 2: Crear el documento PDF e insertar la imagen ---
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Abrimos el flujo de contenido para escribir en la página
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        // Creamos el objeto de imagen de PDFBox leyendo nuestro archivo temporal
        PDImageXObject pdfImage = PDImageXObject.createFromFile("./grafico_temporal.png", document);
        
        // Dibujamos la imagen en las coordenadas X, Y deseadas (sistema de puntos del PDF)
        contentStream.drawImage(pdfImage, 100, 400); 
        contentStream.close(); // Importante cerrar el flujo

        // Guardamos el PDF definitivo y limpiamos el archivo temporal
        document.save("Informe_Podcasts.pdf");
        document.close();
        new File("./grafico_temporal.png").delete(); 
        
        System.out.println("¡PDF generado con éxito sin entorno gráfico!");
    }
}

*/