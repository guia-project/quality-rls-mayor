package upm.tfg.moduleqr.validation;

import org.apache.jena.tdb2.TDB2Factory;
import org.junit.jupiter.api.Test;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShaclRuleValidatorTest {

    private final ShaclRuleValidator validator = new ShaclRuleValidator();

    @Test
    void shouldReturnTrueForShaclType() {
        assertTrue(validator.isType(RuleType.SHACL));
    }

    @Test
    void shouldReturnFalseForSparqlType() {
        assertFalse(validator.isType(RuleType.SPARQL));
    }

    @Test
    void shouldValidateCorrectShaclRule() {
        String shacl = """
                @prefix sh: <http://www.w3.org/ns/shacl#> .
                @prefix ex: <http://example.com/> .

                ex:PersonShape
                    a sh:NodeShape ;
                    sh:targetClass ex:Person .
                """;

        assertTrue(validator.validateRule(shacl));
    }

    @Test
    void shouldReturnFalseForInvalidShaclRule() {

        String invalid = """
                esto no es turtle
                {@@@
                """;

        assertFalse(validator.validateRule(invalid));
    }
    @TempDir
    Path tempDir;
    @Test
    void shouldValidateKnowledgeGraph() {
        String datasetId = "dataset1";
        String datasetPath =
                tempDir.resolve("tdb").toString();
        Dataset dataset =
                TDB2Factory.connectDataset(datasetPath);

        dataset.begin(ReadWrite.WRITE);
        try {
            Resource person = dataset.getDefaultModel().createResource("http://example.com/person1");
            dataset.getDefaultModel()
                    .add(person, dataset.getDefaultModel().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                            dataset.getDefaultModel().createResource("http://example.com/Person"));
            dataset.commit();

        } finally {
            dataset.end();
        }

        String shacl = """
                @prefix sh: <http://www.w3.org/ns/shacl#> .
                @prefix ex: <http://example.com/> .
                ex:PersonShape
                    a sh:NodeShape ;
                    sh:targetClass ex:Person .
                """;

        ShaclRuleValidator validator = new ShaclRuleValidator();
        try (MockedStatic<TDB2Factory> mocked = Mockito.mockStatic(TDB2Factory.class)) {

            mocked.when(() -> TDB2Factory.connectDataset("/app/data/datasets/" + datasetId + "/tdb")).thenReturn(dataset);

            ValidatorResult result = validator.validateKnowledgeGraph(datasetId, shacl);

            assertNotNull(result);
        }
    }
}