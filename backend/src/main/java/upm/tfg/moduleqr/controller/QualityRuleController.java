package upm.tfg.moduleqr.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.service.QualityRuleService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/qr")
@Slf4j
public class QualityRuleController {

    private final QualityRuleService service;
    public QualityRuleController(QualityRuleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> createQualityRule(@RequestBody QrDto request) {
        log.info("Create Quality Rule");
        service.createQualityRule(request.getContent(),request.getType(),request.getName(),request.getDescription(),
                request.getDatasetId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody QrDto dto) {
        log.info("Update Quality Rule");
        service.updateQualityRule(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<QualityRule> toggleRule(@PathVariable String id) {
        log.info("Toggle Quality Rule {}", id);
        QualityRule updated = service.toggleRule(id);
        return ResponseEntity.ok(updated);
    }


    @GetMapping()
    public ResponseEntity<List<QualityRule>> getAllQualityRules() {
        log.info("Get Quality Rules");
        return ResponseEntity.ok(service.getQualityRules());
    }
    @GetMapping("/{id}")
    public ResponseEntity<List<QualityRule>> getDtsetQualityRules(@PathVariable String id) {
        return ResponseEntity.ok(service.getDtsetQualityRules(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deleteRule(@PathVariable String id) {
        log.info("Delete Quality Rule");
        service.deleteQualityRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<InputStreamResource> validateGraph(@RequestParam String datasetId, @RequestParam String tipo) {
        log.info("Validate Quality Rule");
        ByteArrayInputStream stream = service.validateGraph(datasetId, tipo);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if ("pdf".equalsIgnoreCase(tipo)) {
            String filename = "validation_report_" + timestamp + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(stream));
        } else {
            String filename = "validation_report_" + timestamp + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(new InputStreamResource(stream));
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<Void> upload(@RequestParam MultipartFile file, @RequestParam String datasetId) {
        log.info("Upload Quality Rule");
        service.createQrFromCsv(file,datasetId);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCsv(@RequestParam String datasetId) {
        log.info("Export Quality Rule");
        ByteArrayInputStream csv = service.exportQrToCsv(datasetId);
        String filename = "quality_rules_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL,"no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA,"no-cache")
                .header(HttpHeaders.EXPIRES,"0")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new InputStreamResource(csv));
    }


}
