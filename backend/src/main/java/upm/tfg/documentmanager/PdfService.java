package upm.tfg.documentmanager;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import upm.tfg.moduleqr.model.ValidationResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfService {

    public static ByteArrayInputStream exportResultPdf(List<ValidationResult> results){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);
        PdfDocument pdfDoc    = new PdfDocument(pdfWriter);
        Document document  = new Document(pdfDoc);
        document.add(new Paragraph("Informe de Validación del Knowledge Graph")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        document.add(new Paragraph("Fecha: " + timestamp).setFontSize(10));
        long passed = results.stream().filter(ValidationResult::isPassed).count();
        document.add(new Paragraph(
                String.format("Resultado global: %d/%d reglas superadas", passed, results.size()))
                .setFontSize(11).setBold());
        document.add(new Paragraph(" "));
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 4, 2, 5})).useAllAvailableWidth();

        for (String header : new String[]{"Nombre", "Tipo", "Descripción", "Resultado", "Mensaje"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }
        for (ValidationResult r : results) {
            table.addCell(r.getRuleName());
            table.addCell(r.getRuleType().name());
            table.addCell(r.getDescription() != null ? r.getDescription() : "");
            Cell statusCell = new Cell().add(
                    new Paragraph(r.isPassed() ? "PASS" : "✘ FAIL").setBold());
            statusCell.setFontColor(r.isPassed() ? ColorConstants.GREEN : ColorConstants.RED);
            table.addCell(statusCell);
            table.addCell(r.getMessage());
        }
        document.add(table);
        document.close();

        return new ByteArrayInputStream(out.toByteArray());
    }
}
