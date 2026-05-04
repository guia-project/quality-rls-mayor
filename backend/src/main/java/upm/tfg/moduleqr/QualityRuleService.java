package upm.tfg.moduleqr;

import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sparql.streamline.core.SparqlEndpoint;
import sparql.streamline.core.SparqlEndpointConfiguration;
import sparql.streamline.exception.SparqlConfigurationException;
import sparql.streamline.exception.SparqlQuerySyntaxException;
import sparql.streamline.exception.SparqlRemoteEndpointException;
import upm.tfg.documentmanager.CsvService;
import upm.tfg.documentmanager.PdfService;
import upm.tfg.exception.DocumentGenerationException;
import upm.tfg.exception.KnowledgeGraphException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.moduleqr.Validation.QRValidation;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidationResult;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


@Service
public class QualityRuleService {


    private final QRValidation validator;
    private final QualityRuleRepository repository;
    private final PdfService pdfService;

    private final CsvService csvService;

    public QualityRuleService(QRValidation validator, QualityRuleRepository repository, PdfService pdfService, CsvService csvService) {
        this.validator = validator;
        this.repository = repository;
        this.pdfService = pdfService;
        this.csvService = csvService;
    }

    public void createQualityRule(String content, RuleType type, String name, String description) {
        if (!validator.validateRule(content, type)) {
            throw new IllegalArgumentException("Quality Rule invalido");
        }
        QualityRule qr = new QualityRule(content, type, name, description);
        repository.save(qr);
    }

    public QualityRule getQualityRule(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Quality Rule con id " + id + " no encontrado"));
    }

    public void updateQualityRule(String id, QrDto dto) {
        QualityRule rule = getQualityRule(id);
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setContent(dto.getContent());
        rule.setRuleType(dto.getType());
        repository.save(rule);
    }

    public List<QualityRule> getQualityRules() {
        return repository.findAll();
    }

    public void deleteQualityRule(String id) {
        repository.delete(getQualityRule(id));
    }

    public ByteArrayInputStream validateGraph(String url, String tipo) {
        String graphContent = fetchGraphContent(url);
        List<QualityRule> rules = repository.findAll();
        List<ValidationResult> results = new ArrayList<>();

        for (QualityRule rule : rules) {
            boolean passed;
            String message;
            passed = validator.validateKnowledgeGraph(graphContent, rule.getContent(), rule.getRuleType());
            message = passed ? "El Knowledge Graph CUMPLE la regla." : "El Knowledge Graph NO CUMPLE la regla.";
            results.add(new ValidationResult(
                    rule.getId(),
                    rule.getName(),
                    rule.getRuleType(),
                    rule.getDescription(),
                    passed,
                    message));
        }
        try {
            if ("pdf".equalsIgnoreCase(tipo)) {
                return pdfService.exportResultPdf(results);
            } else {
                return csvService.exportResultCsv(results);
            }
        } catch (Exception e) {
            throw new DocumentGenerationException("Error generando el informe de validación: " + e.getMessage());
        }
    }


    public void createQrFromCsv(MultipartFile file){
        List<QrDto> rules = csvService.createFromCsv(file);
        for (QrDto qr : rules) {
            createQualityRule(qr.getContent(), qr.getType(), qr.getName(), qr.getDescription());
        }

    }

    public ByteArrayInputStream exportQrToCsv(){
        return csvService.exportToCsv(repository.findAll());
    }

    private SparqlEndpoint createEndpoint(String url) {
        SparqlEndpointConfiguration configuration = new SparqlEndpointConfiguration();
        configuration.setEndpointQuery(url);
        return new SparqlEndpoint(configuration);
    }

    protected String fetchGraphContent(String url) {
        SparqlEndpoint endpoint = createEndpoint(url);
        String query = """
                CONSTRUCT {?s ?p ?o}
                WHERE { ?s ?p ?o}
                """;
        try {
        ByteArrayOutputStream res = endpoint.query(query, ResultsFormat.FMT_RDF_TURTLE);
        return res.toString();
        }catch (SparqlRemoteEndpointException |SparqlConfigurationException | SparqlQuerySyntaxException e) {
            throw new KnowledgeGraphException("Error al obtener knowledge graph");
        }
    }
}
