package de.bohnottensen.financialaudit.infrastructure.web.crud;

import de.bohnottensen.financialaudit.domain.model.Tenant;
import de.bohnottensen.financialaudit.infrastructure.persistence.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class CrudWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldRenderCrudHubWithAllModels() throws Exception {
        mockMvc.perform(get("/crud"))
                .andExpect(status().isOk())
                .andExpect(view().name("crud-hub"))
                .andExpect(model().attributeExists("models"))
                .andExpect(content().string(containsString("Domain Model CRUD Views")))
                .andExpect(content().string(containsString("User")))
                .andExpect(content().string(containsString("Account")))
                .andExpect(content().string(containsString("Booking")))
                .andExpect(content().string(containsString("Tenant")));
    }

    @Test
    void shouldListEntitiesForModel() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("ACME-KEY-1");
        tenant.setName("ACME Corp");
        tenantRepository.save(tenant);

        mockMvc.perform(get("/crud/tenants"))
                .andExpect(status().isOk())
                .andExpect(view().name("crud-list"))
                .andExpect(model().attributeExists("descriptor", "displayProperties", "rows", "totalCount"))
                .andExpect(content().string(containsString("ACME Corp")));
    }

    @Test
    void shouldShowCreateFormForModel() throws Exception {
        mockMvc.perform(get("/crud/tenants/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("crud-form"))
                .andExpect(model().attributeExists("descriptor", "isNew", "properties", "values", "refOptions"))
                .andExpect(content().string(containsString("Create Tenant")));
    }

    @Test
    void shouldCreateNewEntityViaPost() throws Exception {
        mockMvc.perform(post("/crud/tenants").with(csrf())
                        .param("tenantKey", "HORIZON-KEY-1")
                        .param("name", "New Horizon Ltd"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/crud/tenant?success=Tenant+created+successfully"));
    }

    @Test
    void shouldShowEntityDetails() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("DETAIL-KEY-1");
        tenant.setName("Detail Corp");
        Tenant saved = tenantRepository.save(tenant);

        mockMvc.perform(get("/crud/tenants/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("crud-detail"))
                .andExpect(model().attributeExists("descriptor", "entity", "id", "propertyValues", "summary"))
                .andExpect(content().string(containsString("Detail Corp")));
    }

    @Test
    void shouldShowEditFormForEntity() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("EDIT-KEY-1");
        tenant.setName("Edit Corp");
        Tenant saved = tenantRepository.save(tenant);

        mockMvc.perform(get("/crud/tenants/" + saved.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("crud-form"))
                .andExpect(model().attributeExists("descriptor", "isNew", "id", "entity", "values"))
                .andExpect(content().string(containsString("Edit Tenant")))
                .andExpect(content().string(containsString("Edit Corp")));
    }

    @Test
    void shouldUpdateEntityViaPost() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("INIT-KEY-1");
        tenant.setName("Initial Corp");
        Tenant saved = tenantRepository.save(tenant);

        mockMvc.perform(post("/crud/tenants/" + saved.getId()).with(csrf())
                        .param("tenantKey", "INIT-KEY-UPDATED")
                        .param("name", "Modified Corp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/crud/tenant?success=Tenant+updated+successfully"));
    }

    @Test
    void shouldDeleteEntityViaPost() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("DELETE-KEY-1");
        tenant.setName("To Delete Corp");
        Tenant saved = tenantRepository.save(tenant);

        mockMvc.perform(post("/crud/tenants/" + saved.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/crud/tenant?success=Tenant+deleted+successfully"));
    }

    @Test
    void shouldReturnNotFoundForInvalidModel() throws Exception {
        mockMvc.perform(get("/crud/non-existing-model"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistingEntityId() throws Exception {
        mockMvc.perform(get("/crud/tenants/9999999"))
                .andExpect(status().isNotFound());
    }
}
