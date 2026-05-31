package upm.tfg.moduleqr.service;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import upm.tfg.documentmanager.CsvService;
import upm.tfg.documentmanager.PdfService;
import upm.tfg.exception.DocumentGenerationException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.modulekg.model.Dataset;
import upm.tfg.modulekg.repository.DatasetRepository;
import upm.tfg.moduleqr.repository.QualityRuleRepository;
import upm.tfg.moduleqr.model.*;
import upm.tfg.moduleqr.validation.QRValidation;
import lombok.extern.slf4j.Slf4j;


import java.io.*;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class QualityRuleService {


    private final QRValidation validator;
    private final QualityRuleRepository repository;
    private final DatasetRepository datasetRepository;
    private final PdfService pdfService;

    private final CsvService csvService;


    public QualityRuleService(QRValidation validator, QualityRuleRepository repository, DatasetRepository datasetRepository, PdfService pdfService, CsvService csvService) {
        this.validator = validator;
        this.repository = repository;
        this.datasetRepository = datasetRepository;
        this.pdfService = pdfService;
        this.csvService = csvService;
    }

    public void createQualityRule(String content, RuleType type, String name, String description,String datasetId)
    {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(()->new NotFoundException("Dataset no encontrado"));
        if (!validator.validateRule(content, type)) {
            throw new IllegalArgumentException("Quality Rule invalido");
        }
        QualityRule qr = new QualityRule(content, type, name, description,dataset);

        repository.save(qr);
    }
    public QualityRule toggleRule(String id) {
        QualityRule rule = getQualityRule(id);
        rule.setEnabled(!rule.isEnabled());
        return repository.save(rule);
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

    public List<QualityRule> getDtsetQualityRules(String datasetId) {
        Dataset dataset =datasetRepository
                .findById(datasetId)
                .orElseThrow(() -> new NotFoundException("Dataset no encontrado"));

        return dataset.getRules();
    }

    public void deleteQualityRule(String id) {
        repository.delete(getQualityRule(id));
    }

    public ByteArrayInputStream validateGraph(String datasetId, String tipo) {
        Dataset dataset =datasetRepository
                        .findById(datasetId)
                        .orElseThrow(() -> new NotFoundException("Dataset no encontrado"));

        List<QualityRule> rules =dataset.getRules()
                .stream()
                .filter(QualityRule ::isEnabled)
                .toList();
        if (rules.isEmpty()) throw new IllegalStateException("No hay reglas activas para validar");
        List<ValidationResult> results = new ArrayList<>();

        for (QualityRule rule : rules) {
            ValidatorResult res = validator.validateKnowledgeGraph(datasetId,rule.getContent(),rule.getRuleType());
            results.add(ValidationResult.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .ruleType(rule.getRuleType())
                    .description(rule.getDescription())
                    .passed(res.isPassed())
                    .queryResults(res.getQueryResults())
                    .build()
            );
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


    public void createQrFromCsv(MultipartFile file, String datasetId) {
        List<QrDto> rules = csvService.createFromCsv(file,datasetId);
        for (QrDto qr : rules) {
            createQualityRule(qr.getContent(), qr.getType(), qr.getName(), qr.getDescription(), qr.getDatasetId());
        }
    }

    public ByteArrayInputStream exportQrToCsv(String datasetId) {
        return csvService.exportToCsv(getDtsetQualityRules(datasetId));
    }

}
