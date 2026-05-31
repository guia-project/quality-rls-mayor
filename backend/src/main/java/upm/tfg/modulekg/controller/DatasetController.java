package upm.tfg.modulekg.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upm.tfg.modulekg.model.DatasetRequest;
import upm.tfg.modulekg.model.Dataset;
import upm.tfg.modulekg.service.DatasetService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/kg")
@Slf4j
public class DatasetController {
    private final DatasetService service;
    public DatasetController(DatasetService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> createDataset(@RequestBody DatasetRequest request) {
        String datasetId = UUID.randomUUID().toString();
        log.info("name"+request.getName()+" endpointUrl"+request.getEndpointUrl());
        service.createDataset(request.getName(), request.getEndpointUrl(),datasetId);
        return ResponseEntity.ok("Dataset creado correctamente ID: "+ datasetId);
    }
    @GetMapping
    public ResponseEntity<List<Dataset>> getKnowledgeGraphs() {
        return ResponseEntity.ok(service.getKnowledgeGraphs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dataset> getKnowledgeGraph(@PathVariable String id) {
        return ResponseEntity.ok(service.getKnowledgeGraph(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deleteRule(@PathVariable String id) {
        service.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}
