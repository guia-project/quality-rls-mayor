package upm.tfg.moduleqr.service;


import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sparql.streamline.core.SparqlEndpoint;
import sparql.streamline.core.SparqlEndpointConfiguration;
import upm.tfg.documentmanager.CsvService;
import upm.tfg.documentmanager.PdfService;
import upm.tfg.exception.DocumentGenerationException;
import upm.tfg.exception.KnowledgeGraphException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.moduleqr.QualityRuleRepository;
import upm.tfg.moduleqr.model.*;
import upm.tfg.moduleqr.validation.QRValidation;
import lombok.extern.slf4j.Slf4j;


import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class QualityRuleService {


    private final QRValidation validator;
    private final QualityRuleRepository repository;
    private final PdfService pdfService;

    private final CsvService csvService;
    private final JsonToModelService jsonToModelService;


    public QualityRuleService(QRValidation validator, QualityRuleRepository repository, PdfService pdfService, CsvService csvService, JsonToModelService jsonToModelService) {
        this.validator = validator;
        this.repository = repository;
        this.pdfService = pdfService;
        this.csvService = csvService;
        this.jsonToModelService = jsonToModelService;
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
        if (!validator.validateRule(dto.getContent(), dto.getType())){
            throw new IllegalArgumentException("Quality Rule invalido");
        }
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
            results.add(new ValidationResult(
                    rule.getId(),
                    rule.getName(),
                    rule.getRuleType(),
                    rule.getDescription(),
                    validator.validateKnowledgeGraph(graphContent, rule.getContent(), rule.getRuleType())));
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
        //SparqlEndpoint endpoint = createEndpoint(url);
        String query = """
            SELECT ?s ?p ?o
            FROM <https://guia-kg.skai.etsisi.upm.es/data>
            WHERE { ?s ?p ?o }
            LIMIT 10000
            """;

        try {
            log.info("Obteniendo el graph desde: {}", url);
            /*
            log.info("Ejecutando query");
            ByteArrayOutputStream res = endpoint.query(query,ResultsFormat.FMT_NONE);
            log.info("Query ejecutada");
            String pr = res.toString();

             */
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String fullUrl = url + "?query=" + encodedQuery;


            HttpClient client = HttpClient.newBuilder()
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Accept", "application/sparql-results+json")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Error HTTP {}: {}", response.statusCode(), response.body());
                throw new KnowledgeGraphException("Error HTTP: " + response.statusCode());
            }
            log.info("Query ejecutada");
            return convertJsonToTurtle(response.body());
        }catch (Exception e) {
            log.error(e.getMessage());
            throw new KnowledgeGraphException("Error al obtener knowledge graph");
        }
    }
    private String convertJsonToTurtle(String json) {
        Model model = jsonToModelService.convertJsonToModel(json);
        return jsonToModelService.convertModelToRdfString(model);
    }
}
