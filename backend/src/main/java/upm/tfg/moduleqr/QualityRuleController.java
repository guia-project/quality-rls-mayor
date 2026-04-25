package upm.tfg.moduleqr;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/qr")
public class QualityRuleController {

    private final QualityRuleService service;
    public QualityRuleController(QualityRuleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> createQualityRule(@RequestBody QrDto request) {
        service.createQualityRule(request.getContent(),request.getType(),request.getName(),request.getDescription());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity <QualityRule> getQualityRule(@PathVariable String id) {
        QualityRule qualityRule = service.getQualityRule(id);
        return ResponseEntity.ok(qualityRule);
    }

    @GetMapping()
    public ResponseEntity<List<QualityRule>> getQualityRules() {
        return ResponseEntity.ok(service.getQualityRules());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deleteGraph(@PathVariable String id) {
        service.deleteQualityRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate/{url}/{tipo}")
    public ResponseEntity<InputStreamResource> validateGraph(@PathVariable String url, @PathVariable String tipo) {
        ByteArrayInputStream stream = service.validateGraph(url, tipo);
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
    public ResponseEntity<Void> upload(@RequestParam MultipartFile file) {
        service.createQrFromCsv(file);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCsv(){
        ByteArrayInputStream csv = service.exportQrToCsv();
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
