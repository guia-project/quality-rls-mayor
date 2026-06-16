package upm.tfg.documentmanager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CsvService {

    public List<QrDto> createFromCsv(MultipartFile file, String datasetId) {
        List<QrDto> res = new ArrayList<>();
        try {
            Reader reader = new InputStreamReader(file.getInputStream());
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
            for (CSVRecord record : csvParser) {

                String ruleTypeStr = record.get("ruleType");
                res.add(QrDto.builder()
                        .name(record.get("name"))
                        .type(RuleType.valueOf(ruleTypeStr.toUpperCase()))
                        .content(record.get("content"))
                        .description(record.get("description"))
                        .datasetId(datasetId)
                        .build());
            }
        } catch (Exception e) {
            throw new CsvProcessingException("Error en la lectura del csv ");
        }
        return res;
    }

    public ByteArrayInputStream exportToCsv(List<QualityRule> rules) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("name","description", "content","ruleType"))) {
            for (QualityRule qr : rules) {
                csvPrinter.printRecord(
                        qr.getName(),
                        qr.getDescription(),
                        qr.getContent(),
                        qr.getRuleType().name()
                );
            }
            csvPrinter.flush();
        } catch (IOException e) {
            throw new DocumentGenerationException("Error generando el CSV");
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportResultCsv(List<ValidationResult> results) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader(
                             "ruleId", "ruleName", "ruleType", "description", "passed", "queryResults"))) {
            for (ValidationResult r : results) {
                String queryResults =formatQueryResults(r);
                csv.printRecord(
                        r.getRuleId(),
                        r.getRuleName(),
                        r.getRuleType().name(),
                        r.getDescription(),
                        r.isPassed() ? "PASS" : "FAIL",
                        queryResults);
            }
            csv.flush();
        } catch (IOException e) {
            throw new DocumentGenerationException("Error generando el CSV");
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String formatQueryResults(ValidationResult result) {
        if (result.getQueryResults() == null) {
            return "";
        }

        return result.getQueryResults()
                .stream()
                .map(Map::toString)
                .collect(Collectors.joining("\n"));
    }
}
