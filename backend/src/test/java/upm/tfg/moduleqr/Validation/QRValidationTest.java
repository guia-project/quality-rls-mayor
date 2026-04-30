package upm.tfg.moduleqr.Validation;

import org.junit.jupiter.api.Test;
import upm.tfg.moduleqr.model.RuleType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class QRValidationTest {
    QRValidator sparql = mock(QRValidator.class);
    QRValidator shacl = mock(QRValidator.class);
    QRValidation qrValidation = new QRValidation(List.of(sparql, shacl));

    @Test
    void shouldUseSparqlInRuleValidation() {
        when(sparql.isType(RuleType.SPARQL)).thenReturn(true);
        when(shacl.isType(RuleType.SPARQL)).thenReturn(false);
        when(sparql.validateRule(any())).thenReturn(true);

        boolean result = qrValidation.validateRule( "query", RuleType.SPARQL);

        assertTrue(result);
        verify(shacl,never()).validateRule(any());
    }

    @Test
    void shouldUseShaclInRuleValidation() {
        when(sparql.isType(RuleType.SHACL)).thenReturn(false);
        when(shacl.isType(RuleType.SHACL)).thenReturn(true);
        when(shacl.validateRule(any())).thenReturn(true);

        boolean result = qrValidation.validateRule("query",  RuleType.SHACL);

        assertTrue(result);
        verify(sparql,never()).validateRule(any());
    }

    @Test
    void shouldUseSparqlInGraphValidation() {
        when(sparql.isType(RuleType.SPARQL)).thenReturn(true);
        when(shacl.isType(RuleType.SPARQL)).thenReturn(false);
        when(sparql.validateKnowledgeGraph(any(), any())).thenReturn(true);

        boolean result = qrValidation.validateKnowledgeGraph("graph", "query", RuleType.SPARQL);

        assertTrue(result);
        verify(shacl,never()).validateKnowledgeGraph(any(), any());
    }

    @Test
    void shouldUseShaclInGraphValidation() {
        when(sparql.isType(RuleType.SHACL)).thenReturn(false);
        when(shacl.isType(RuleType.SHACL)).thenReturn(true);
        when(shacl.validateKnowledgeGraph(any(), any())).thenReturn(true);

        boolean result = qrValidation.validateKnowledgeGraph("graph", "query", RuleType.SHACL);

        assertTrue(result);
        verify(sparql,never()).validateKnowledgeGraph(any(), any());
    }

}