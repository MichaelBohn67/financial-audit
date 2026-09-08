package de.bohnottensen.financialaudit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bohnottensen.financialaudit.application.usecase.workpaper.WorkpaperService;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.ReviewActionType;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkpaperReviewApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkpaperService workpaperService;

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldCreateWorkpaper() throws Exception {
        when(workpaperService.create("WP-1", "TENANT-1", "PROJECT-1", "assistant")).thenReturn(workpaper(1L, "WP-1", "DRAFT", "assistant"));

        mockMvc.perform(post("/api/workpapers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "WP-1", "tenantId", "TENANT-1", "projectId", "PROJECT-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("WP-1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldStartProgress() throws Exception {
        when(workpaperService.findById(1L, "TENANT-1", "PROJECT-1")).thenReturn(workpaper(1L, "WP-1", "DRAFT", "assistant"));
        when(workpaperService.startProgress(1L, "TENANT-1", "PROJECT-1", "assistant")).thenReturn(workpaper(1L, "WP-1", "IN_PROGRESS", "assistant"));

        mockMvc.perform(post("/api/workpapers/1/start").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldSubmitWorkpaper() throws Exception {
        when(workpaperService.submit(1L, "TENANT-1", "PROJECT-1", "assistant")).thenReturn(workpaper(1L, "WP-1", "SUBMITTED", "assistant"));

        mockMvc.perform(post("/api/workpapers/1/submit").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "senior", roles = "LEAD_AUDITOR")
    void seniorAuditorShouldRequestChanges() throws Exception {
        when(workpaperService.requestChanges(1L, "TENANT-1", "PROJECT-1", "senior", "Need more evidence"))
                .thenReturn(workpaper(1L, "WP-1", "CHANGES_REQUESTED", "senior"));

        mockMvc.perform(post("/api/workpapers/1/request-changes")
                        .with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "Need more evidence"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldNotBeAllowedToRequestChanges() throws Exception {
        mockMvc.perform(post("/api/workpapers/1/request-changes")
                        .with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "Attempt"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "wirtschaftspruefer", roles = "LEAD_AUDITOR")
    void wirtschaftsprueferShouldApproveWorkpaper() throws Exception {
        when(workpaperService.approve(1L, "TENANT-1", "PROJECT-1", "wirtschaftspruefer"))
                .thenReturn(workpaper(1L, "WP-1", "APPROVED", "wirtschaftspruefer"));

        mockMvc.perform(post("/api/workpapers/1/approve").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldNotBeAllowedToApprove() throws Exception {
        mockMvc.perform(post("/api/workpapers/1/approve").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lead", roles = "LEAD_AUDITOR")
    void leadAuditorShouldSignOffApprovedWorkpaper() throws Exception {
        when(workpaperService.signOff(1L, "TENANT-1", "PROJECT-1", "lead"))
                .thenReturn(workpaper(1L, "WP-1", "SIGNED_OFF", "lead"));

        mockMvc.perform(post("/api/workpapers/1/sign-off").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED_OFF"));
    }

    @Test
    @WithMockUser(username = "senior", roles = "AUDITOR")
    void seniorAuditorShouldNotBeAllowedToApprove() throws Exception {
        mockMvc.perform(post("/api/workpapers/1/approve").with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void shouldGetWorkpaperById() throws Exception {
        when(workpaperService.findById(1L, "TENANT-1", "PROJECT-1")).thenReturn(workpaper(1L, "WP-1", "DRAFT", "assistant"));

        mockMvc.perform(get("/api/workpapers/1")
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("WP-1"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void shouldReturnReviewActionHistory() throws Exception {
        when(workpaperService.findReviewActions(1L, "TENANT-1", "PROJECT-1")).thenReturn(List.of(reviewAction("assistant", ReviewActionType.START)));

        mockMvc.perform(get("/api/workpapers/1/actions")
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor").value("assistant"))
                .andExpect(jsonPath("$[0].action").value("START"));
    }

    @Test
    void unauthenticatedUserShouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/workpapers/1")
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1"))
                .andExpect(status().isUnauthorized());
    }

    private Workpaper workpaper(Long id, String title, String status, String createdBy) {
        Workpaper wp = new Workpaper();
        wp.setId(id);
        wp.setTitle(title);
        wp.setStatus(status);
        wp.setCreatedBy(createdBy);
        return wp;
    }

    private ReviewAction reviewAction(String actor, ReviewActionType action) {
        ReviewAction ra = new ReviewAction();
        ra.setActor(actor);
        ra.setAction(action);
        return ra;
    }
}
