package upm.tfg.moduleqr.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRValidationTest {

    @Mock
    private QRValidator sparqlValidator;

    @Mock
    private QRValidator shaclValidator;

    private QRValidation qrValidation;

    @BeforeEach
    void setUp() {
        qrValidation = new QRValidation(List.of(sparqlValidator, shaclValidator));
    }

    @Test
    void shouldValidateSparqlRule() {

        when(sparqlValidator.isType(RuleType.SPARQL)).thenReturn(true);
        when(sparqlValidator.validateRule("content")).thenReturn(true);

        boolean result = qrValidation.validateRule("content", RuleType.SPARQL);

        assertTrue(result);

        verify(sparqlValidator).validateRule("content");
        verify(shaclValidator, never()).validateRule(anyString());
    }

    @Test
    void shouldValidateShaclRule() {

        when(sparqlValidator.isType(RuleType.SHACL)).thenReturn(false);

        when(shaclValidator.isType(RuleType.SHACL)).thenReturn(true);

        when(shaclValidator.validateRule("content")).thenReturn(true);

        boolean result = qrValidation.validateRule("content", RuleType.SHACL);

        assertTrue(result);

        verify(shaclValidator).validateRule("content");
        verify(sparqlValidator, never()).validateRule(anyString());
    }

    @Test
    void shouldReturnFalseWhenRuleIsInvalid() {

        when(sparqlValidator.isType(RuleType.SPARQL)).thenReturn(true);
        when(sparqlValidator.validateRule("invalid")).thenReturn(false);

        boolean result = qrValidation.validateRule("invalid", RuleType.SPARQL);

        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenValidatorNotFoundForRuleValidation() {

        when(sparqlValidator.isType(RuleType.SPARQL)).thenReturn(false);

        when(shaclValidator.isType(RuleType.SPARQL)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> qrValidation.validateRule(
                        "content",
                        RuleType.SPARQL));

        assertEquals("No validator found", exception.getMessage());
    }

    @Test
    void shouldValidateKnowledgeGraphUsingSparqlValidator() {

        ValidatorResult validatorResult = new ValidatorResult();
        validatorResult.setPassed(true);

        when(sparqlValidator.isType(RuleType.SPARQL)).thenReturn(true);
        when(sparqlValidator.validateKnowledgeGraph("dataset1", "ruleContent")).thenReturn(validatorResult);

        ValidatorResult result = qrValidation.validateKnowledgeGraph("dataset1", "ruleContent", RuleType.SPARQL);

        assertNotNull(result);
        assertTrue(result.isPassed());

        verify(sparqlValidator).validateKnowledgeGraph("dataset1", "ruleContent");
    }

    @Test
    void shouldValidateKnowledgeGraphUsingShaclValidator() {

        ValidatorResult validatorResult = new ValidatorResult();
        validatorResult.setPassed(true);

        when(sparqlValidator.isType(RuleType.SHACL)).thenReturn(false);

        when(shaclValidator.isType(RuleType.SHACL)).thenReturn(true);

        when(shaclValidator.validateKnowledgeGraph("dataset1", "ruleContent")).thenReturn(validatorResult);

        ValidatorResult result = qrValidation.validateKnowledgeGraph("dataset1", "ruleContent", RuleType.SHACL);

        assertNotNull(result);
        assertTrue(result.isPassed());

        verify(shaclValidator).validateKnowledgeGraph("dataset1", "ruleContent");
    }

    @Test
    void shouldThrowExceptionWhenValidatorNotFoundForKnowledgeGraphValidation() {

        when(sparqlValidator.isType(RuleType.SHACL)).thenReturn(false);

        when(shaclValidator.isType(RuleType.SHACL)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> qrValidation.validateKnowledgeGraph(
                        "dataset1",
                        "content",
                        RuleType.SHACL));

        assertEquals("No validator found", exception.getMessage());
    }
}