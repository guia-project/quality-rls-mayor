package upm.tfg.modulekg.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.stereotype.Service;
import upm.tfg.exception.KnowledgeGraphException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.modulekg.repository.DatasetRepository;
import upm.tfg.modulekg.model.Dataset;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class DatasetService {
    private final DatasetRepository repository;
    public DatasetService(DatasetRepository repository) {
        this.repository = repository;
    }

    public void createDataset(String datasetName, String datasetUrl, String datasetId){
        fetchGraphContent(datasetUrl, datasetId);
        Dataset dataset = Dataset.builder()
                        .id(datasetId)
                        .name(datasetName)
                        .endpointUrl(datasetUrl)
                        .build();
        repository.save(dataset);
    }
    private void fetchGraphContent(String url,String datasetId) {

        String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";

        try {
            log.info("Obteniendo el graph desde: {}", url);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String fullUrl = url + "?query=" + encodedQuery;


            HttpClient client = HttpClient.newBuilder()
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Accept", "text/turtle")
                    .GET()
                    .build();

            Path datasetFolder = Paths.get("/app/data/datasets/" + datasetId);
            Files.createDirectories(datasetFolder);
            Path outputPath = datasetFolder.resolve("kg.ttl");
            log.info("Obteniendo el graph desde: {}", fullUrl);
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(outputPath));
            if (response.statusCode() != 200) {
                log.error("Error HTTP {}: {}", response.statusCode(), response.body());
                throw new KnowledgeGraphException("Error HTTP: " + response.statusCode());
            }

            importDataset(datasetId);

        }catch (Exception e) {
            log.error(e.getMessage());
            throw new KnowledgeGraphException("Error al obtener knowledge graph");
        }
    }

    private void importDataset(String datasetId) {
        String basePath = "/app/data/datasets/" + datasetId;
        Path ttlPath = Paths.get(basePath, "kg.ttl");
        String tdbPath = basePath + "/tdb";
        org.apache.jena.query.Dataset dataset = TDB2Factory.connectDataset(tdbPath);
        dataset.begin(ReadWrite.WRITE);

        try {
            Model model = dataset.getDefaultModel();
            if (!model.isEmpty()) {
                dataset.commit();
                return;
            }

            RDFDataMgr.read(model, Files.newInputStream(ttlPath), Lang.TURTLE
            );
            dataset.commit();

        } catch (Exception e) {
            dataset.abort();
            throw new RuntimeException("Error importando dataset");
        } finally {
            dataset.end();
        }
    }

    public List<Dataset> getKnowledgeGraphs() {
        return repository.findAll();
    }
    public Dataset getKnowledgeGraph(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Dataset con id " + id + " no encontrado"));
    }

    public void deleteDataset(String id) {
        repository.delete(getKnowledgeGraph(id));
    }
}
