package upm.tfg.moduleqr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import upm.tfg.documentmanager.CsvService;
import upm.tfg.documentmanager.PdfService;
import upm.tfg.exception.NotFoundException;
import upm.tfg.moduleqr.Validation.QRValidation;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.exception.DocumentGenerationException;

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
    private PdfService pdfService;

    @Mock
    private CsvService csvService;

    @InjectMocks
    private QualityRuleService service;

    private QualityRule rule;

    @BeforeEach
    void setUp() {
        rule = new QualityRule("content", RuleType.SPARQL, "name", "desc");
        rule.setId("1");
    }

    @Test
    void shouldCreateQualityRule_whenValid() {
        when(validator.validateRule("content", RuleType.SPARQL)).thenReturn(true);
        service.createQualityRule("content", RuleType.SPARQL, "name", "desc");

        verify(repository, times(1)).save(any(QualityRule.class));
    }

    @Test
    void shouldThrowException_whenInvalidRule() {
        when(validator.validateRule("content", RuleType.SPARQL)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                service.createQualityRule("content", RuleType.SPARQL, "name", "desc"));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnRule_whenExists() {
        when(repository.findById("1")).thenReturn(Optional.of(rule));
        QualityRule result = service.getQualityRule("1");

        assertEquals("name", result.getName());
    }

    @Test
    void shouldThrowNotFound_whenRuleNotExists() {
        when(repository.findById("1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getQualityRule("1"));
    }

    @Test
    void shouldUpdateRule() {
        QrDto dto = new QrDto("newContent", RuleType.SHACL, "newName", "newDesc");
        when(repository.findById("1")).thenReturn(Optional.of(rule));
        service.updateQualityRule("1", dto);

        assertEquals("newName", rule.getName());
        assertEquals("newDesc", rule.getDescription());
        assertEquals("newContent", rule.getContent());
        verify(repository).save(rule);
    }

    @Test
    void shouldDeleteRule() {
        when(repository.findById("1")).thenReturn(Optional.of(rule));
        service.deleteQualityRule("1");

        verify(repository).delete(rule);
    }

    @Test
    void shouldReturnAllRules() {
        when(repository.findAll()).thenReturn(List.of(rule));
        List<QualityRule> result = service.getQualityRules();

        assertEquals(1, result.size());
    }

    @Test
    void validateGraph_pdf_ok() {
        QualityRuleService spy = Mockito.spy(service);
        doReturn("graph").when(spy).fetchGraphContent(any());
        when(repository.findAll()).thenReturn(List.of(rule));
        when(validator.validateKnowledgeGraph(any(), any(), any())).thenReturn(true);
        when(pdfService.exportResultPdf(any())).thenReturn(new ByteArrayInputStream(new byte[0]));

        ByteArrayInputStream result = spy.validateGraph("url", "pdf");

        assertNotNull(result);
        verify(pdfService).exportResultPdf(any());
    }

    @Test
    void validateGraph_csv_ok() {
        QualityRuleService spy = Mockito.spy(service);
        doReturn("graph").when(spy).fetchGraphContent(any());
        when(repository.findAll()).thenReturn(List.of(rule));
        when(validator.validateKnowledgeGraph(any(), any(), any())).thenReturn(true);
        when(csvService.exportResultCsv(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayInputStream result = spy.validateGraph("url", "csv");

        assertNotNull(result);
        verify(csvService).exportResultCsv(any());
    }

    @Test
    void validateGraph_documentError() {
        QualityRuleService spy = Mockito.spy(service);
        doReturn("graph").when(spy).fetchGraphContent(any());
        when(repository.findAll()).thenReturn(List.of(rule));
        when(validator.validateKnowledgeGraph(any(), any(), any())).thenReturn(true);
        when(pdfService.exportResultPdf(any())).thenThrow(new RuntimeException("fail"));

        assertThrows(DocumentGenerationException.class, () -> spy.validateGraph("url", "pdf"));

    }

    @Test

    void createQrFromCsv_ok() {
        MultipartFile file = mock(MultipartFile.class);
        QrDto dto = new QrDto("content", RuleType.SPARQL, "name", "desc");
        when(csvService.createFromCsv(file)).thenReturn(List.of(dto));
        when(validator.validateRule(any(), any())).thenReturn(true);
        service.createQrFromCsv(file);

        verify(repository).save(any());
    }

    @Test

    void exportQrToCsv_ok() {
        when(repository.findAll()).thenReturn(List.of(rule));
        when(csvService.exportToCsv(any()))
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayInputStream result = service.exportQrToCsv();

        assertNotNull(result);
        verify(csvService).exportToCsv(any());
    }
}