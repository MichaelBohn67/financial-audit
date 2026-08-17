package de.bohnottensen.financialaudit.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SamplingWebControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void authorizedUserCanViewSamplingPage() throws Exception {
        mockMvc.perform(get("/sampling"))
                .andExpect(status().isOk())
                .andExpect(view().name("sampling"));
    }

    @Test
    void unauthenticatedUserCannotViewSamplingPage() throws Exception {
        mockMvc.perform(get("/sampling")).andExpect(status().isUnauthorized());
    }
}
