package upm.tfg.moduleqr.Validation;

import org.junit.jupiter.api.Test;
import upm.tfg.moduleqr.model.RuleType;

import static org.junit.jupiter.api.Assertions.*;

class ShaclRuleValidatorTest {
    ShaclRuleValidator shaclRuleValidator = new ShaclRuleValidator();


    @Test
    void shouldReturnTrueShaclTrue(){
        boolean res = shaclRuleValidator.isType(RuleType.SHACL);
        assertTrue(res);

    }

    @Test
    void shouldReturnFalseShaclFalse(){
        boolean res = shaclRuleValidator.isType(RuleType.SPARQL);
        assertFalse(res);

    }

    @Test
    void shouldReturnTrueCorrectShacl(){
        String content = "@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
                "@prefix ex: <http://example.com/> .\n" +
                "\n" +
                "ex:ValidShape\n" +
                "    a sh:NodeShape ;\n" +
                "    sh:targetNode ex:subject ;\n" +
                "    sh:property [\n" +
                "        sh:path ex:predicate ;\n" +
                "        sh:minCount 1 ;\n" +
                "    ] .";
        boolean res = shaclRuleValidator.validateRule(content);
        assertTrue(res);
    }

    @Test
    void shouldReturnFalseCorrectIncorrectShacl(){
        String content = "Cualquier cosa";
        boolean res = shaclRuleValidator.validateRule(content);
        assertFalse(res);
    }

    @Test
    void shouldReturnTrueKgCompliesShacl(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
                "@prefix ex: <http://example.com/> .\n" +
                "\n" +
                "ex:ValidShape\n" +
                "    a sh:NodeShape ;\n" +
                "    sh:targetNode ex:subject ;\n" +
                "    sh:property [\n" +
                "        sh:path ex:predicate ;\n" +
                "        sh:minCount 1 ;\n" +
                "    ] .";

        boolean res = shaclRuleValidator.validateKnowledgeGraph(gcontent,rcontent);

        assertTrue(res);
    }

    @Test
    void shouldReturnFalseKgNotCompliesShacl(){
        String gcontent="""
            @prefix ex: <http://example.com/> .
            ex:subject ex:predicate ex:object .
        """;
        String rcontent="@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
                "@prefix ex: <http://example.com/> .\n" +
                "\n" +
                "ex:ValidShape\n" +
                "    a sh:NodeShape ;\n" +
                "    sh:targetNode ex:subject ;\n" +
                "    sh:property [\n" +
                "        sh:path ex:age ;\n" +
                "        sh:minCount 1 ;\n" +
                "    ] .";

        boolean res = shaclRuleValidator.validateKnowledgeGraph(gcontent,rcontent);

        assertFalse(res);
    }
}