package upm.tfg.moduleqr;


import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sparql.streamline.core.SparqlEndpoint;
import sparql.streamline.core.SparqlEndpointConfiguration;
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

            log.info("Ejecutando query");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Error HTTP {}: {}", response.statusCode(), response.body());
                throw new KnowledgeGraphException("Error HTTP: " + response.statusCode());
            }
            log.info("Query ejecutada");
            return convertJsonToTurtle(response.body());
        }catch (Exception e) {
            System.out.println(e.getMessage());
            throw new KnowledgeGraphException("Error al obtener knowledge graph");
        }
    }
    private String convertJsonToTurtle(String jsonResults) {
        try {
            JSONObject json = new JSONObject(jsonResults);
            JSONArray bindings = json.getJSONObject("results").getJSONArray("bindings");

            Model model = ModelFactory.createDefaultModel();

            log.info("Procesando bindings");

            for (int i = 0; i < bindings.length(); i++) {
                JSONObject binding = bindings.getJSONObject(i);

                Resource subject = createResource(model, binding.getJSONObject("s"));
                Property predicate = createProperty(model, binding.getJSONObject("p"));
                RDFNode object = createRDFNode(model, binding.getJSONObject("o"));

                model.add(subject, predicate, object);
            }

            StringWriter writer = new StringWriter();
            model.write(writer, "TURTLE");

            log.info("Conversion completa");
            return writer.toString();

        } catch (Exception e) {
            log.error("Error convirtiendo JSON a Turtle", e);
            throw new KnowledgeGraphException("Error procesando resultados: " + e.getMessage());
        }
    }

    private Resource createResource(Model model, JSONObject node) {
        String type = node.getString("type");
        String value = node.getString("value");

        if ("uri".equals(type)) {
            return model.createResource(value);
        } else if ("bnode".equals(type)) {
            return model.createResource(AnonId.create(value));
        }
        throw new IllegalArgumentException("Tipo de sujeto no soportado: " + type);
    }

    private Property createProperty(Model model, JSONObject node) {
        return model.createProperty(node.getString("value"));
    }

    private RDFNode createRDFNode(Model model, JSONObject node) {
        String type = node.getString("type");
        String value = node.getString("value");

        switch (type) {
            case "uri":
                return model.createResource(value);
            case "bnode":
                return model.createResource(AnonId.create(value));
            case "literal":
                if (node.has("datatype")) {
                    return model.createTypedLiteral(value, node.getString("datatype"));
                } else if (node.has("xml:lang")) {
                    return model.createLiteral(value, node.getString("xml:lang"));
                } else {
                    return model.createLiteral(value);
                }
            default:
                throw new IllegalArgumentException("Tipo de nodo no soportado: " + type);
        }
    }
}
