package upm.tfg.moduleqr;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    public ResponseEntity<QualityRule> createQualityRule(@RequestBody QrDto request) {
        QualityRule qualityRule = service.createQualityRule(request.getContent(),request.getType()
                ,request.getName(),request.getDescription());
        return ResponseEntity.status(201).body(qualityRule);
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

    /*
    @GetMapping({"/validate/{url}/{qrid}"})
    public ResponseEntity<Void> validateGraph(@PathVariable String url, @PathVariable String qrid){
        service.validateGraph(url,qrid);
        return ResponseEntity.ok().build();
    }

     */

    @PostMapping("/import")
    public ResponseEntity<Void> upload(@RequestParam MultipartFile file) throws IOException {
        service.createFromCsv(file);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCsv() throws IOException {
        ByteArrayInputStream csv = service.exportToCsv();
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
