package upm.tfg.moduleqr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import upm.tfg.documentmanager.CsvService;
import upm.tfg.documentmanager.PdfService;
import upm.tfg.exception.DocumentGenerationException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.modulekg.model.Dataset;
import upm.tfg.modulekg.repository.DatasetRepository;
import upm.tfg.moduleqr.model.*;
import upm.tfg.moduleqr.repository.QualityRuleRepository;
import upm.tfg.moduleqr.validation.QRValidation;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityRuleServiceTest {

    @Mock
    private QRValidation validator;

    @Mock
    private QualityRuleRepository repository;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private PdfService pdfService;

    @Mock
    private CsvService csvService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private QualityRuleService service;

    private Dataset dataset;
    private QualityRule rule;

    @BeforeEach
    void setUp() {
        dataset = new Dataset();

        rule = new QualityRule();
        rule.setId("rule-1");
        rule.setName("Rule");
        rule.setDescription("Description");
        rule.setContent("content");
        rule.setRuleType(RuleType.SPARQL);
        rule.setEnabled(true);
    }

    @Test
    void shouldCreateQualityRule() {
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));
        when(validator.validateRule("content", RuleType.SPARQL)).thenReturn(true);

        service.createQualityRule(
                "content",
                RuleType.SPARQL,
                "Rule",
                "Description",
                "dataset1"
        );

        verify(repository).save(any(QualityRule.class));
    }

    @Test
    void shouldThrowWhenDatasetNotFoundOnCreate() {
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createQualityRule(
                        "content",
                        RuleType.SPARQL,
                        "Rule",
                        "Description",
                        "dataset1"
                )
        );
    }

    @Test
    void shouldThrowWhenRuleIsInvalidOnCreate() {
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));
        when(validator.validateRule("content", RuleType.SPARQL)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.createQualityRule(
                        "content",
                        RuleType.SPARQL,
                        "Rule",
                        "Description",
                        "dataset1"
                )
        );
    }

    @Test
    void shouldToggleRule() {
        rule.setEnabled(true);
        when(repository.findById("rule-1")).thenReturn(Optional.of(rule));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        QualityRule result = service.toggleRule("rule-1");

        assertFalse(result.isEnabled());
        verify(repository).save(rule);
    }

    @Test
    void shouldGetQualityRule() {
        when(repository.findById("rule-1")).thenReturn(Optional.of(rule));
        QualityRule result = service.getQualityRule("rule-1");

        assertEquals("rule-1", result.getId());
    }

    @Test
    void shouldThrowWhenQualityRuleNotFound() {
        when(repository.findById("rule-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getQualityRule("rule-1"));
    }

    @Test
    void shouldUpdateQualityRule() {
        QrDto dto = new QrDto();
        dto.setContent("new-content");
        dto.setName("new-name");
        dto.setDescription("new-description");
        dto.setType(RuleType.SHACL);

        when(validator.validateRule("new-content", RuleType.SHACL)).thenReturn(true);
        when(repository.findById("rule-1")).thenReturn(Optional.of(rule));

        service.updateQualityRule("rule-1", dto);

        verify(repository).save(rule);

        assertEquals("new-name", rule.getName());
        assertEquals("new-description", rule.getDescription());
        assertEquals("new-content", rule.getContent());
        assertEquals(RuleType.SHACL, rule.getRuleType());
    }

    @Test
    void shouldThrowWhenUpdateRuleIsInvalid() {
        QrDto dto = new QrDto();
        dto.setContent("invalid");
        dto.setType(RuleType.SHACL);
        when(validator.validateRule("invalid", RuleType.SHACL)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.updateQualityRule("rule-1", dto));
    }

    @Test
    void shouldGetAllRules() {
        when(repository.findAll()).thenReturn(List.of(rule));
        List<QualityRule> result = service.getQualityRules();

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetDatasetRules() {
        dataset.setRules(List.of(rule));
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        List<QualityRule> result = service.getDtsetQualityRules("dataset1");

        assertEquals(1, result.size());
    }

    @Test
    void shouldDeleteRule() {
        when(repository.findById("rule-1")).thenReturn(Optional.of(rule));
        service.deleteQualityRule("rule-1");

        verify(repository).delete(rule);
    }

    @Test
    void shouldGeneratePdfValidationReport() throws Exception {
        dataset.setRules(List.of(rule));
        ValidatorResult validatorResult = new ValidatorResult();
        validatorResult.setPassed(true);

        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        when(validator.validateKnowledgeGraph(
                "dataset1",
                "content",
                RuleType.SPARQL))
                .thenReturn(validatorResult);

        ByteArrayInputStream pdf = new ByteArrayInputStream("pdf".getBytes());

        when(pdfService.exportResultPdf(anyList())).thenReturn(pdf);

        ByteArrayInputStream result = service.validateGraph("dataset1", "pdf");

        assertNotNull(result);
    }

    @Test
    void shouldGenerateCsvValidationReport() throws Exception {
        dataset.setRules(List.of(rule));

        ValidatorResult validatorResult = new ValidatorResult();
        validatorResult.setPassed(true);

        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        when(validator.validateKnowledgeGraph(
                "dataset1",
                "content",
                RuleType.SPARQL))
                .thenReturn(validatorResult);

        ByteArrayInputStream csv = new ByteArrayInputStream("csv".getBytes());

        when(csvService.exportResultCsv(anyList())).thenReturn(csv);

        ByteArrayInputStream result = service.validateGraph("dataset1", "csv");

        assertNotNull(result);
    }

    @Test
    void shouldThrowWhenNoActiveRules() {
        rule.setEnabled(false);
        dataset.setRules(List.of(rule));
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        assertThrows(IllegalStateException.class, () -> service.validateGraph("dataset1", "pdf"));
    }

    @Test
    void shouldThrowDocumentGenerationException() throws Exception {
        dataset.setRules(List.of(rule));

        ValidatorResult validatorResult = new ValidatorResult();
        validatorResult.setPassed(true);

        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        when(validator.validateKnowledgeGraph(
                "dataset1",
                "content",
                RuleType.SPARQL))
                .thenReturn(validatorResult);

        when(pdfService.exportResultPdf(anyList())).thenThrow(new RuntimeException("error"));

        assertThrows(DocumentGenerationException.class, () -> service.validateGraph("dataset1", "pdf"));
    }

    @Test
    void shouldCreateRulesFromCsv() {
        QrDto dto = new QrDto();
        dto.setContent("content");
        dto.setType(RuleType.SPARQL);
        dto.setName("Rule");
        dto.setDescription("Description");
        dto.setDatasetId("dataset1");

        when(csvService.createFromCsv(multipartFile, "dataset1")).thenReturn(List.of(dto));
        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));
        when(validator.validateRule("content", RuleType.SPARQL)).thenReturn(true);

        service.createQrFromCsv(multipartFile, "dataset1");

        verify(repository).save(any(QualityRule.class));
    }

    @Test
    void shouldExportQrToCsv() {
        dataset.setRules(List.of(rule));
        ByteArrayInputStream csv = new ByteArrayInputStream("csv".getBytes());

        when(datasetRepository.findById("dataset1")).thenReturn(Optional.of(dataset));

        when(csvService.exportToCsv(anyList())).thenReturn(csv);

        ByteArrayInputStream result = service.exportQrToCsv("dataset1");

        assertNotNull(result);
    }
}