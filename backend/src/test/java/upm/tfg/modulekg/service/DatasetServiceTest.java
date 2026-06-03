package upm.tfg.modulekg.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import upm.tfg.exception.KnowledgeGraphException;
import upm.tfg.exception.NotFoundException;
import upm.tfg.modulekg.model.Dataset;
import upm.tfg.modulekg.repository.DatasetRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private DatasetRepository repository;

    @InjectMocks
    private DatasetService service;

    private Dataset dataset;

    @BeforeEach
    void setUp() {
        dataset = Dataset.builder()
                .id("test-id-123")
                .name("TestDataset")
                .endpointUrl("https://example.com/sparql")
                .build();
    }

    @Test
    void getKnowledgeGraphs_returnsAllDatasets() {
        when(repository.findAll()).thenReturn(List.of(dataset));

        List<Dataset> result = service.getKnowledgeGraphs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("test-id-123");
        verify(repository, times(1)).findAll();
    }

    @Test
    void getKnowledgeGraphs_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Dataset> result = service.getKnowledgeGraphs();

        assertThat(result).isEmpty();
    }

    @Test
    void getKnowledgeGraph_existingId_returnsDataset() {
        when(repository.findById("test-id-123")).thenReturn(Optional.of(dataset));

        Dataset result = service.getKnowledgeGraph("test-id-123");

        assertThat(result.getId()).isEqualTo("test-id-123");
        assertThat(result.getName()).isEqualTo("TestDataset");
    }

    @Test
    void getKnowledgeGraph_notFound_throwsNotFoundException() {
        when(repository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getKnowledgeGraph("no-existe"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    void deleteDataset_existingId_callsRepositoryDelete() {
        when(repository.findById("test-id-123")).thenReturn(Optional.of(dataset));
        doNothing().when(repository).delete(dataset);

        service.deleteDataset("test-id-123");

        verify(repository, times(1)).delete(dataset);
    }

    @Test
    void deleteDataset_notFound_throwsNotFoundException() {
        when(repository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDataset("no-existe"))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void createDataset_invalidEndpoint_throwsKnowledgeGraphException() {
        assertThatThrownBy(() ->
                service.createDataset("NombreDataset", "https://endpoint-invalido.xyz/sparql", "id-test"))
                .isInstanceOf(KnowledgeGraphException.class)
                .hasMessageContaining("Error al obtener knowledge graph");

        verify(repository, never()).save(any());
    }

    @Test
    void createDataset_emptyUrl_throwsKnowledgeGraphException() {
        assertThatThrownBy(() ->
                service.createDataset("NombreDataset", "", "id-test"))
                .isInstanceOf(KnowledgeGraphException.class);

        verify(repository, never()).save(any());
    }
}