package upm.tfg.moduleqr;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sparql.streamline.core.SparqlEndpoint;
import sparql.streamline.core.SparqlEndpointConfiguration;
import upm.tfg.exception.CsvProcessingException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.exception.QualityRuleViolationException;
import org.apache.commons.csv.*;
import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Service
public class QualityRuleService {

    List<QualityRule> rules = new ArrayList<>();
    private final QRValidation validator;

    public QualityRuleService(QRValidation validator) {
        this.validator = validator;
    }

    public QualityRule createQualityRule(String content, RuleType type,String name, String description) {
        if(!validator.validateRule(content,type)) {
            throw new IllegalArgumentException("Quality Rule invalido");
        }
        QualityRule qr = new QualityRule(content, type, name, description);
        rules.add(qr);
        return qr;
    }

    public QualityRule getQualityRule(String id) {
        QualityRule rule = rules.stream().filter(qr -> qr.getId().equals(id)).findFirst().orElse(null);
        if(rule == null) {
            throw new NotFoundException("Quality Rule con id " + id + " no encontrado");
        }
        return rule;
    }

    public List<QualityRule> getQualityRules() {
        return rules;
    }

    public void deleteQualityRule(String id) {
        QualityRule qr = getQualityRule(id);
        rules.remove(qr);
    }

    /*

    public void validateGraph(String url, String qrId){
        SparqlEndpoint endpoint = sparqlEndpoint(url);
        String kg = endpoint.
        QualityRule qr = getQualityRule(qrId);
        if(validator.validateKnowledgeGraph(kg,qr.getContent(),qr.getRuleType())) {
            throw new QualityRuleViolationException("Knowledge Graph no satisface Quality Rule");
        }
    }

    */


    public void createFromCsv(MultipartFile file) throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream());
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
        for (CSVRecord record : csvParser) {
            try {
                String name = record.get("name");
                String description = record.get("description");
                String content = record.get("content");
                String ruleTypeStr = record.get("ruleType");
                RuleType ruleType = RuleType.valueOf(ruleTypeStr.toUpperCase());

                QualityRule qr = createQualityRule(content, ruleType, name, description);
            } catch (Exception e) {
                throw new CsvProcessingException(
                        "Error en línea " + record.getRecordNumber() + ": " + e.getMessage()
                );
            }
        }
    }


    public ByteArrayInputStream exportToCsv() throws IOException {
        List<QualityRule> rules = qualityRuleRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader("id", "name", "description", "ruleType", "content"))) {
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
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private SparqlEndpoint createEndpoint(String url) {
        SparqlEndpointConfiguration configuration = new SparqlEndpointConfiguration();
        configuration.setEndpointQuery(url);
        return new SparqlEndpoint(configuration);
    }
}
