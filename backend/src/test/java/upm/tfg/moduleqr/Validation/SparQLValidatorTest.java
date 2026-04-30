package upm.tfg.moduleqr.Validation;

import org.junit.jupiter.api.Test;
import upm.tfg.moduleqr.model.RuleType;

import static org.junit.jupiter.api.Assertions.*;

class SparQLValidatorTest {
    SparQLValidator validator = new SparQLValidator();

    @Test
    void shouldReturnTrueSparqlTrue(){
        boolean res = validator.isType(RuleType.SPARQL);
        assertTrue(res);

    }

    @Test
    void shouldReturnFalseSparqlFalse(){
        boolean res = validator.isType(RuleType.SHACL);
        assertFalse(res);

    }

    @Test
    void shouldReturnTrueCorrectSparql(){
        String content = "PREFIX ex: <http://example.com/>\n" +
                "\n" +
                "ASK {\n" +
                "    ex:subject ex:predicate ?o .\n" +
                "}";
        boolean res = validator.validateRule(content);
        assertTrue(res);
    }

    @Test
    void shouldReturnFalseCorrectIncorrectSparql(){
        String content = "Cualquier cosa";
        boolean res = validator.validateRule(content);
        assertFalse(res);
    }

    @Test
    void shouldReturnTrueKgCompliesAskSparql(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="PREFIX ex: <http://example.com/>\n" +
                "\n" +
                "ASK {\n" +
                "    ex:subject ex:predicate ?o .\n" +
                "}";

        boolean res = validator.validateKnowledgeGraph(gcontent,rcontent);

        assertTrue(res);
    }

    @Test
    void shouldReturnFalseKgNotCompliesAskSparql(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="PREFIX ex: <http://example.com/>\n" +
                "\n" +
                "ASK {\n" +
                "    ex:subject ex:age ?o .\n" +
                "}";

        boolean res = validator.validateKnowledgeGraph(gcontent,rcontent);

        assertFalse(res);
    }

    @Test
    void shouldReturnTrueKgCompliesSelectSparql(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="PREFIX ex: <http://example.com/>\n" +
                "\n" +
                "SELECT ?s WHERE {\n" +
                "    FILTER NOT EXISTS {\n" +
                "        ex:subject ex:predicate ?o .\n" +
                "    }\n" +
                "}";

        boolean res = validator.validateKnowledgeGraph(gcontent,rcontent);

        assertTrue(res);
    }

    @Test
    void shouldReturnFalseKgNotCompliesSelectSparql(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="PREFIX ex: <http://example.com/>\n" +
                "\n" +
                "SELECT ?s WHERE {\n" +
                "    FILTER NOT EXISTS {\n" +
                "        ex:subject ex:age ?o .\n" +
                "    }\n" +
                "}";

        boolean res = validator.validateKnowledgeGraph(gcontent,rcontent);

        assertFalse(res);
    }
}