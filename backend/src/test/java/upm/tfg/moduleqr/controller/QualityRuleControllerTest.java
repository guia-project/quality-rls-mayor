package upm.tfg.moduleqr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.service.QualityRuleService;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static upm.tfg.moduleqr.model.RuleType.*;

@WebMvcTest(QualityRuleController.class)

class QualityRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QualityRuleService service;

    @Test
    void shouldCreateQualityRule() throws Exception {
        String body = """
                {
                  "content":"rule-content",
                  "type":"SPARQL",
                  "name":"Rule1",
                  "description":"description",
                  "datasetId":"dataset-1"
                }
                """;

        mockMvc.perform(post("/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(service).createQualityRule(
                "rule-content",
                SPARQL,
                "Rule1",
                "description",
                "dataset-1"
        );
    }

    @Test
    void shouldUpdateRule() throws Exception {

        String body = """
                {
                  "content":"updated-content",
                  "type":"SHACL",
                  "name":"Rule1",
                  "description":"updated-description",
                  "datasetId":"dataset-1"
                }
                """;
        mockMvc.perform(put("/qr/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(service).updateQualityRule(eq("123"), any(QrDto.class));
    }

    @Test
    void shouldToggleRule() throws Exception {
        QualityRule rule = new QualityRule();
        when(service.toggleRule("123")).thenReturn(rule);
        mockMvc.perform(put("/qr/123/toggle"))
                .andExpect(status().isOk());

        verify(service).toggleRule("123");
    }

    @Test
    void shouldGetAllRules() throws Exception {
        when(service.getQualityRules())
                .thenReturn(List.of(new QualityRule()));

        mockMvc.perform(get("/qr"))
                .andExpect(status().isOk());

        verify(service).getQualityRules();
    }

    @Test
    void shouldGetDatasetRules() throws Exception {
        when(service.getDtsetQualityRules("dataset1"))
                .thenReturn(List.of(new QualityRule()));

        mockMvc.perform(get("/qr/dataset1"))
                .andExpect(status().isOk());

        verify(service).getDtsetQualityRules("dataset1");
    }

    @Test
    void shouldDeleteRule() throws Exception {
        mockMvc.perform(delete("/qr/123"))
                .andExpect(status().isNoContent());

        verify(service).deleteQualityRule("123");
    }

    @Test
    void shouldValidateGraphPdf() throws Exception {
        when(service.validateGraph("dataset1", "pdf"))
                .thenReturn(new ByteArrayInputStream("pdf-content".getBytes()));
        mockMvc.perform(get("/qr/validate")
                                .param("datasetId", "dataset1")
                                .param("tipo", "pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString(".pdf")));

        verify(service).validateGraph("dataset1", "pdf");
    }

    @Test
    void shouldValidateGraphCsv() throws Exception {
        when(service.validateGraph("dataset1", "csv"))
                .thenReturn(new ByteArrayInputStream("csv-content".getBytes()));
        mockMvc.perform(get("/qr/validate")
                                .param("datasetId", "dataset1")
                                .param("tipo", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString(".csv")));

        verify(service).validateGraph("dataset1", "csv");
    }

    @Test
    void shouldUploadCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rules.csv",
                "text/csv",
                "content".getBytes()
        );
        mockMvc.perform(multipart("/qr/upload")
                        .file(file)
                        .param("datasetId", "dataset1"))
                .andExpect(status().isNoContent());

        verify(service).createQrFromCsv(any(), eq("dataset1"));
    }

    @Test
    void shouldExportCsv() throws Exception {
        when(service.exportQrToCsv("dataset1"))
                .thenReturn(new ByteArrayInputStream("id,name\n1,test".getBytes()));

        mockMvc.perform(get("/qr/export")
                        .param("datasetId", "dataset1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString(".csv")));

        verify(service).exportQrToCsv("dataset1");
    }
}