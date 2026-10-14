package de.bohnottensen.financialaudit.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HomeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageIsAccessibleAndContainsViewLinks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("crudModels"))
                .andExpect(content().string(containsString("/dashboard")))
                .andExpect(content().string(containsString("/sampling")))
                .andExpect(content().string(containsString("/bookings")))
                .andExpect(content().string(containsString("/crud")))
                .andExpect(content().string(containsString("/crud/account")));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void authorizedUserCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }
}
