package upm.tfg.modulekg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import upm.tfg.exception.NotFoundException;
import upm.tfg.modulekg.model.Dataset;
import upm.tfg.modulekg.model.DatasetRequest;
import upm.tfg.modulekg.service.DatasetService;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DatasetService service;

    @Test
    void createDataset_returnsOkWithId() throws Exception {
        DatasetRequest request = new DatasetRequest();
        request.setName("TestDataset");
        request.setEndpointUrl("https://example.com/sparql");

        doNothing().when(service).createDataset(any(), any(), any());

        mockMvc.perform(post("/kg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dataset creado correctamente ID:")));

        verify(service, times(1)).createDataset(
                eq("TestDataset"), eq("https://example.com/sparql"), any());
    }

    @Test
    void createDataset_callsServiceWithCorrectArguments() throws Exception {
        DatasetRequest request = new DatasetRequest();
        request.setName("MiGrafo");
        request.setEndpointUrl("https://triplestore.example.org/sparql");

        doNothing().when(service).createDataset(any(), any(), any());

        mockMvc.perform(post("/kg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(service).createDataset(eq("MiGrafo"),
                eq("https://triplestore.example.org/sparql"), any());
    }

    @Test
    void getKnowledgeGraphs_returnsListOfDatasets() throws Exception {
        Dataset d1 = Dataset.builder().id("id-1").name("Grafo1").endpointUrl("https://ep1.com").build();
        Dataset d2 = Dataset.builder().id("id-2").name("Grafo2").endpointUrl("https://ep2.com").build();

        when(service.getKnowledgeGraphs()).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/kg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("id-1"))
                .andExpect(jsonPath("$[0].name").value("Grafo1"))
                .andExpect(jsonPath("$[1].id").value("id-2"))
                .andExpect(jsonPath("$[1].name").value("Grafo2"));
    }

    @Test
    void getKnowledgeGraphs_returnsEmptyList() throws Exception {
        when(service.getKnowledgeGraphs()).thenReturn(List.of());

        mockMvc.perform(get("/kg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getKnowledgeGraph_existingId_returnsDataset() throws Exception {
        Dataset dataset = Dataset.builder()
                .id("abc-123")
                .name("GrafoKG")
                .endpointUrl("https://kg.example.com/sparql")
                .build();

        when(service.getKnowledgeGraph("abc-123")).thenReturn(dataset);

        mockMvc.perform(get("/kg/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.name").value("GrafoKG"))
                .andExpect(jsonPath("$.endpointUrl").value("https://kg.example.com/sparql"));
    }

    @Test
    void getKnowledgeGraph_notFound_returns404() throws Exception {
        when(service.getKnowledgeGraph("no-existe"))
                .thenThrow(new NotFoundException("Dataset con id no-existe no encontrado"));

        mockMvc.perform(get("/kg/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDataset_existingId_returnsNoContent() throws Exception {
        doNothing().when(service).deleteDataset("abc-123");

        mockMvc.perform(delete("/kg/abc-123"))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteDataset("abc-123");
    }

    @Test
    void deleteDataset_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Dataset con id no-existe no encontrado"))
                .when(service).deleteDataset("no-existe");

        mockMvc.perform(delete("/kg/no-existe"))
                .andExpect(status().isNotFound());
    }
}