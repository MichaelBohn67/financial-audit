package de.bohnottensen.financialaudit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bohnottensen.financialaudit.application.usecase.workpaper.WorkpaperService;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private WorkpaperService workpaperService;

    // --- create ---

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldCreateWorkpaper() throws Exception {
        when(workpaperService.create(anyString(), anyString())).thenReturn(workpaper(1L, "WP-1", "DRAFT", "assistant"));

        mockMvc.perform(post("/api/workpapers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "WP-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("WP-1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // --- startProgress ---

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldStartProgress() throws Exception {
        when(workpaperService.startProgress(anyLong(), anyString())).thenReturn(workpaper(1L, "WP-1", "IN_PROGRESS", "assistant"));

        mockMvc.perform(post("/api/workpapers/1/start")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // --- submit ---

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldSubmitWorkpaper() throws Exception {
        when(workpaperService.submit(anyLong(), anyString())).thenReturn(workpaper(1L, "WP-1", "SUBMITTED", "assistant"));

        mockMvc.perform(post("/api/workpapers/1/submit")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    // --- requestChanges (SENIOR_AUDITOR) ---

    @Test
    @WithMockUser(username = "senior", roles = "LEAD_AUDITOR")
    void seniorAuditorShouldRequestChanges() throws Exception {
        when(workpaperService.requestChanges(anyLong(), anyString(), anyString()))
                .thenReturn(workpaper(1L, "WP-1", "CHANGES_REQUESTED", "senior"));

        mockMvc.perform(post("/api/workpapers/1/request-changes")
                        .with(csrf())
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "Attempt"))))
                .andExpect(status().isForbidden());
    }

    // --- approve (WIRTSCHAFTSPRUEFER only) ---

    @Test
    @WithMockUser(username = "wirtschaftspruefer", roles = "LEAD_AUDITOR")
    void wirtschaftspruefer_shouldApproveWorkpaper() throws Exception {
        when(workpaperService.approve(anyLong(), anyString()))
                .thenReturn(workpaper(1L, "WP-1", "APPROVED", "wirtschaftspruefer"));

        mockMvc.perform(post("/api/workpapers/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void assistantShouldNotBeAllowedToApprove() throws Exception {
        mockMvc.perform(post("/api/workpapers/1/approve")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lead", roles = "LEAD_AUDITOR")
    void leadAuditorShouldSignOffApprovedWorkpaper() throws Exception {
        when(workpaperService.signOff(anyLong(), anyString()))
                .thenReturn(workpaper(1L, "WP-1", "SIGNED_OFF", "lead"));

        mockMvc.perform(post("/api/workpapers/1/sign-off")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED_OFF"));
    }

    @Test
    @WithMockUser(username = "senior", roles = "AUDITOR")
    void seniorAuditorShouldNotBeAllowedToApprove() throws Exception {
        mockMvc.perform(post("/api/workpapers/1/approve")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- get & review actions (any authenticated role) ---

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void shouldGetWorkpaperById() throws Exception {
        when(workpaperService.findById(1L)).thenReturn(workpaper(1L, "WP-1", "DRAFT", "assistant"));

        mockMvc.perform(get("/api/workpapers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("WP-1"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "AUDITOR")
    void shouldReturnReviewActionHistory() throws Exception {
        when(workpaperService.findReviewActions(1L)).thenReturn(List.of(reviewAction("assistant", "START")));

        mockMvc.perform(get("/api/workpapers/1/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor").value("assistant"))
                .andExpect(jsonPath("$[0].action").value("START"));
    }

    @Test
    void unauthenticatedUserShouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/workpapers/1"))
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

    private ReviewAction reviewAction(String actor, String action) {
        ReviewAction ra = new ReviewAction();
        ra.setActor(actor);
        ra.setAction(action);
        return ra;
    }
}

