package upm.tfg.documentmanager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;
import upm.tfg.exception.CsvProcessingException;
import upm.tfg.exception.DocumentGenerationException;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidationResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvService {

    public static List<QrDto> createFromCsv(MultipartFile file) {
        List<QrDto> res = new ArrayList<>();
        try {
            Reader reader = new InputStreamReader(file.getInputStream());
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
            for (CSVRecord record : csvParser) {

                String name = record.get("name");
                String description = record.get("description");
                String content = record.get("content");
                String ruleTypeStr = record.get("ruleType");
                RuleType ruleType = RuleType.valueOf(ruleTypeStr.toUpperCase());

                res.add(new QrDto(content, ruleType, name, description));
            }
        } catch (IOException e) {
            throw new CsvProcessingException("Error en la lectura del csv ");
        }
        return res;
    }

    public static ByteArrayInputStream exportToCsv(List<QualityRule> rules){
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "name", "description", "ruleType", "content"))) {
            for (QualityRule qr : rules) {
                csvPrinter.printRecord(
                        qr.getId(),
                        qr.getName(),
                        qr.getDescription(),
                        qr.getRuleType().name(),
                        qr.getContent()
                );
            }
            csvPrinter.flush();
        }catch (IOException e){
            throw new DocumentGenerationException("Error generando el CSV");
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public static ByteArrayInputStream exportResultCsv(List<ValidationResult> results) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader(
                             "ruleId", "ruleName", "ruleType", "description", "passed", "message"))) {
            for (ValidationResult r : results) {
                csv.printRecord(
                        r.getRuleId(),
                        r.getRuleName(),
                        r.getRuleType().name(),
                        r.getDescription(),
                        r.isPassed() ? "PASS" : "FAIL",
                        r.getMessage());
            }
            csv.flush();
        }catch (IOException e) {
            throw new DocumentGenerationException("Error generando el CSV");
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}
