package upm.tfg.moduleqr.validation;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb2.TDB2Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SparQLValidatorTest {

    private final SparQLValidator validator = new SparQLValidator();

    @Test
    void shouldReturnTrueForSparqlType() {
        assertTrue(validator.isType(RuleType.SPARQL));
    }

    @Test
    void shouldReturnFalseForShaclType() {
        assertFalse(validator.isType(RuleType.SHACL));
    }

    @Test
    void shouldValidateCorrectQuery() {
        String query = """
                SELECT * WHERE {
                    ?s ?p ?o
                }
                """;
        assertTrue(validator.validateRule(query));
    }

    @Test
    void shouldReturnFalseForInvalidQuery() {
        String query = """
                ESTO NO ES SPARQL
                """;
        assertFalse(validator.validateRule(query));
    }

    @TempDir
    Path tempDir;
    @Test
    void shouldValidateKnowledgeGraphWithAskQuery() {
        String datasetId = "dataset1";
        String datasetPath = tempDir.resolve("tdb").toString();

        Dataset dataset = TDB2Factory.connectDataset(datasetPath);

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

        String askQuery = """
                ASK {
                    ?s ?p ?o
                }
                """;

        try (MockedStatic<TDB2Factory> mocked = Mockito.mockStatic(TDB2Factory.class)) {

            mocked.when(() -> TDB2Factory.connectDataset("/app/data/datasets/" + datasetId + "/tdb")).thenReturn(dataset);

            ValidatorResult result = validator.validateKnowledgeGraph(datasetId, askQuery);

            assertNotNull(result);
            assertTrue(result.isPassed());
            assertNull(result.getQueryResults());
        }
    }

    @Test
    void shouldValidateKnowledgeGraphWithSelectQuery() {

        String datasetId = "dataset1";
        String datasetPath = tempDir.resolve("tdb-select").toString();
        Dataset dataset = TDB2Factory.connectDataset(datasetPath);
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

        String selectQuery = """
                SELECT ?s
                WHERE {
                    ?s ?p ?o
                }
                """;

        try (MockedStatic<TDB2Factory> mocked = Mockito.mockStatic(TDB2Factory.class)) {

            mocked.when(() -> TDB2Factory.connectDataset("/app/data/datasets/" + datasetId + "/tdb")).thenReturn(dataset);

            ValidatorResult result = validator.validateKnowledgeGraph(datasetId, selectQuery);

            assertNotNull(result);
            assertTrue(result.isPassed());
            assertNotNull(result.getQueryResults());
            assertFalse(result.getQueryResults().isEmpty());
        }
    }

    @Test
    void shouldThrowExceptionForUnsupportedQueryType() {

        String datasetId = "dataset1";
        String datasetPath = tempDir.resolve("tdb-construct").toString();
        Dataset dataset = TDB2Factory.connectDataset(datasetPath);

        String constructQuery = """
                CONSTRUCT {
                    ?s ?p ?o
                }
                WHERE {
                    ?s ?p ?o
                }
                """;

        try (MockedStatic<TDB2Factory> mocked = Mockito.mockStatic(TDB2Factory.class)) {

            mocked.when(() -> TDB2Factory.connectDataset("/app/data/datasets/" + datasetId + "/tdb")).thenReturn(dataset);

            assertThrows(IllegalArgumentException.class, () -> validator.validateKnowledgeGraph(datasetId, constructQuery));
        }
    }
}