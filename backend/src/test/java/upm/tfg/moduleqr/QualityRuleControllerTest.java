package upm.tfg.moduleqr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import upm.tfg.moduleqr.model.QrDto;
import upm.tfg.moduleqr.model.QualityRule;
import upm.tfg.moduleqr.model.RuleType;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QualityRuleController.class)
class QualityRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QualityRuleService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createQualityRule_ok() throws Exception {
        QrDto dto = new QrDto("content", RuleType.SPARQL, "name", "desc");

        mockMvc.perform(post("/qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
        verify(service).createQualityRule("content", RuleType.SPARQL, "name", "desc");
    }

    @Test
    void update_ok() throws Exception {
        QrDto dto = new QrDto("content", RuleType.SPARQL, "name", "desc");

        mockMvc.perform(put("/qr/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
        verify(service).updateQualityRule(eq("1"), any(QrDto.class));
    }

    @Test
    void getQualityRules_ok() throws Exception {
        QualityRule rule = new QualityRule("content", RuleType.SPARQL, "name", "desc");
        when(service.getQualityRules()).thenReturn(List.of(rule));

        mockMvc.perform(get("/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("name"));
    }

    @Test
    void delete_ok() throws Exception {
        mockMvc.perform(delete("/qr/1"))
                .andExpect(status().isNoContent());
        verify(service).deleteQualityRule("1");
    }

    @Test
    void validateGraph_pdf_ok() throws Exception {
        when(service.validateGraph(any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));

        mockMvc.perform(get("/qr/validate")
                        .param("url", "http://test")
                        .param("tipo", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test

    void validateGraph_csv_ok() throws Exception {
        when(service.validateGraph(any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));

        mockMvc.perform(get("/qr/validate")
                        .param("url", "http://test")
                        .param("tipo", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test

    void upload_ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file","test.csv","text/csv","data".getBytes());

        mockMvc.perform(multipart("/qr/upload")
                        .file(file))
                .andExpect(status().isNoContent());
        verify(service).createQrFromCsv(any());
    }

    @Test
    void exportCsv_ok() throws Exception {

        when(service.exportQrToCsv()).thenReturn(new ByteArrayInputStream(new byte[0]));

        mockMvc.perform(get("/qr/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}