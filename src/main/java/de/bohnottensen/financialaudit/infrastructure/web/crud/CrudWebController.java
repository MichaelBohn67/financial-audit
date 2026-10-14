package de.bohnottensen.financialaudit.infrastructure.web.crud;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/crud")
@PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN')")
public class CrudWebController {

    private final DomainModelCrudService crudService;

    public CrudWebController(DomainModelCrudService crudService) {
        this.crudService = crudService;
    }

    public record ModelSummary(
            String name,
            String slug,
            String title,
            String pluralTitle,
            long count,
            int propertyCount
    ) {}

    public record PropertyValueDisplay(
            String name,
            String label,
            String value,
            DomainPropertyType propertyType
    ) {}

    public record EntityRow(
            Object id,
            String summary,
            List<String> values
    ) {}

    @GetMapping
    public String hub(Model model) {
        List<ModelSummary> modelSummaries = crudService.getAllDescriptors().stream()
                .map(desc -> new ModelSummary(
                        desc.getName(),
                        desc.getSlug(),
                        desc.getTitle(),
                        desc.getPluralTitle(),
                        crudService.count(desc),
                        desc.getProperties().size()
                ))
                .toList();

        model.addAttribute("models", modelSummaries);
        return "crud-hub";
    }

    @GetMapping("/{model}")
    public String list(
            @PathVariable("model") String modelName,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        List<Object> records = crudService.findAll(descriptor);

        List<DomainPropertyDescriptor> displayProps = descriptor.getProperties().stream()
                .filter(p -> p.getPropertyType() != DomainPropertyType.EMBEDDED)
                .limit(7) // limit list columns to 7 for table readability
                .toList();

        List<EntityRow> rows = new ArrayList<>();
        for (Object record : records) {
            Object id = descriptor.extractId(record);
            String summary = crudService.formatEntitySummary(descriptor, record);
            List<String> values = new ArrayList<>();
            for (DomainPropertyDescriptor prop : displayProps) {
                values.add(crudService.formatDisplay(record, prop));
            }
            rows.add(new EntityRow(id, summary, values));
        }

        model.addAttribute("descriptor", descriptor);
        model.addAttribute("displayProperties", displayProps);
        model.addAttribute("rows", rows);
        model.addAttribute("totalCount", records.size());
        model.addAttribute("success", success);
        model.addAttribute("error", error);

        return "crud-list";
    }

    @GetMapping("/{model}/new")
    public String showCreateForm(@PathVariable("model") String modelName, Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, List<ReferenceOption>> refOptions = new LinkedHashMap<>();

        for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
            values.put(prop.getName(), "");
            if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
                refOptions.put(prop.getName(), crudService.getReferenceOptions(prop));
            }
        }

        model.addAttribute("descriptor", descriptor);
        model.addAttribute("isNew", true);
        model.addAttribute("properties", descriptor.getProperties());
        model.addAttribute("values", values);
        model.addAttribute("refOptions", refOptions);
        model.addAttribute("actionUrl", "/crud/" + descriptor.getSlug());

        return "crud-form";
    }

    @PostMapping("/{model}")
    public String handleCreate(
            @PathVariable("model") String modelName,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        try {
            crudService.create(descriptor, request.getParameterMap());
            redirectAttributes.addAttribute("success", descriptor.getTitle() + " created successfully");
            return "redirect:/crud/" + descriptor.getSlug();
        } catch (Exception e) {
            model.addAttribute("descriptor", descriptor);
            model.addAttribute("isNew", true);
            model.addAttribute("properties", descriptor.getProperties());
            Map<String, String> values = new LinkedHashMap<>();
            Map<String, List<ReferenceOption>> refOptions = new LinkedHashMap<>();
            for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
                String[] vals = request.getParameterValues(prop.getName());
                values.put(prop.getName(), vals != null && vals.length > 0 ? vals[0] : "");
                if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
                    refOptions.put(prop.getName(), crudService.getReferenceOptions(prop));
                }
            }
            model.addAttribute("values", values);
            model.addAttribute("refOptions", refOptions);
            model.addAttribute("actionUrl", "/crud/" + descriptor.getSlug());
            model.addAttribute("errorMessage", "Error creating " + descriptor.getTitle() + ": " + e.getMessage());
            return "crud-form";
        }
    }

    @GetMapping("/{model}/{id}")
    public String showDetail(
            @PathVariable("model") String modelName,
            @PathVariable("id") String id,
            Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        Object entity = crudService.findById(descriptor, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, descriptor.getTitle() + " not found with id: " + id));

        List<PropertyValueDisplay> propertyValues = new ArrayList<>();
        for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
            String val = crudService.formatDisplay(entity, prop);
            propertyValues.add(new PropertyValueDisplay(prop.getName(), prop.getLabel(), val, prop.getPropertyType()));
        }

        model.addAttribute("descriptor", descriptor);
        model.addAttribute("entity", entity);
        model.addAttribute("id", id);
        model.addAttribute("propertyValues", propertyValues);
        model.addAttribute("summary", crudService.formatEntitySummary(descriptor, entity));

        return "crud-detail";
    }

    @GetMapping("/{model}/{id}/edit")
    public String showEditForm(
            @PathVariable("model") String modelName,
            @PathVariable("id") String id,
            Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        Object entity = crudService.findById(descriptor, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, descriptor.getTitle() + " not found with id: " + id));

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, List<ReferenceOption>> refOptions = new LinkedHashMap<>();

        for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
            values.put(prop.getName(), crudService.formatFormValue(entity, prop));
            if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
                refOptions.put(prop.getName(), crudService.getReferenceOptions(prop));
            }
        }

        model.addAttribute("descriptor", descriptor);
        model.addAttribute("isNew", false);
        model.addAttribute("id", id);
        model.addAttribute("entity", entity);
        model.addAttribute("properties", descriptor.getProperties());
        model.addAttribute("values", values);
        model.addAttribute("refOptions", refOptions);
        model.addAttribute("actionUrl", "/crud/" + descriptor.getSlug() + "/" + id);

        return "crud-form";
    }

    @PostMapping("/{model}/{id}")
    public String handleUpdate(
            @PathVariable("model") String modelName,
            @PathVariable("id") String id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        try {
            crudService.update(descriptor, id, request.getParameterMap());
            redirectAttributes.addAttribute("success", descriptor.getTitle() + " updated successfully");
            return "redirect:/crud/" + descriptor.getSlug();
        } catch (Exception e) {
            model.addAttribute("descriptor", descriptor);
            model.addAttribute("isNew", false);
            model.addAttribute("id", id);
            model.addAttribute("properties", descriptor.getProperties());
            Map<String, String> values = new LinkedHashMap<>();
            Map<String, List<ReferenceOption>> refOptions = new LinkedHashMap<>();
            for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
                String[] vals = request.getParameterValues(prop.getName());
                values.put(prop.getName(), vals != null && vals.length > 0 ? vals[0] : "");
                if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
                    refOptions.put(prop.getName(), crudService.getReferenceOptions(prop));
                }
            }
            model.addAttribute("values", values);
            model.addAttribute("refOptions", refOptions);
            model.addAttribute("actionUrl", "/crud/" + descriptor.getSlug() + "/" + id);
            model.addAttribute("errorMessage", "Error updating " + descriptor.getTitle() + ": " + e.getMessage());
            return "crud-form";
        }
    }

    // Destructive operations are POST-only. A GET-based delete bypasses the CSRF token
    // requirement, so a single crafted link could delete a record for any logged-in user.
    @PostMapping("/{model}/{id}/delete")
    public String handleDelete(
            @PathVariable("model") String modelName,
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {
        DomainModelDescriptor descriptor = resolveDescriptorOr404(modelName);
        try {
            crudService.delete(descriptor, id);
            redirectAttributes.addAttribute("success", descriptor.getTitle() + " deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Failed to delete " + descriptor.getTitle() + ": " + e.getMessage());
        }
        return "redirect:/crud/" + descriptor.getSlug();
    }

    private DomainModelDescriptor resolveDescriptorOr404(String modelName) {
        return crudService.findDescriptor(modelName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown domain model: " + modelName));
    }
}
