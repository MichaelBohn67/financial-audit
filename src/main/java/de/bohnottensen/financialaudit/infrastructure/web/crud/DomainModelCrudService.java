package de.bohnottensen.financialaudit.infrastructure.web.crud;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class DomainModelCrudService {

    private static final List<Class<?>> DOMAIN_ENTITY_CLASSES = List.of(
            Account.class,
            AccountHolder.class,
            Address.class,
            AuditEngagement.class,
            AuditEvent.class,
            AuditProcedure.class,
            AuditProject.class,
            AuditRisk.class,
            BenfordAnalysisRun.class,
            BenfordDigitStat.class,
            Booking.class,
            Finding.class,
            ImportJob.class,
            ImportJobProtocolEntry.class,
            Industriekontenrahmen.class,
            MaterialityConfig.class,
            PatternAnalysisIssue.class,
            PatternAnalysisRun.class,
            Permission.class,
            ProjectMembership.class,
            ReportArchive.class,
            ReportRun.class,
            ReportTemplate.class,
            ReviewAction.class,
            Role.class,
            RolePermission.class,
            SamplingRun.class,
            SamplingRunItem.class,
            Tenant.class,
            User.class,
            UserRole.class,
            Workpaper.class,
            WorkpaperEvidence.class
    );

    private final EntityManager entityManager;
    private final AuditTrailWriter auditTrailWriter;
    private final Map<String, DomainModelDescriptor> descriptorsBySlug = new LinkedHashMap<>();
    private final Map<Class<?>, DomainModelDescriptor> descriptorsByClass = new LinkedHashMap<>();

    public DomainModelCrudService(EntityManager entityManager, AuditTrailWriter auditTrailWriter) {
        this.entityManager = entityManager;
        this.auditTrailWriter = auditTrailWriter;
        initDescriptors();
    }

    private void initDescriptors() {
        for (Class<?> entityClass : DOMAIN_ENTITY_CLASSES) {
            DomainModelDescriptor descriptor = buildDescriptor(entityClass);
            descriptorsByClass.put(entityClass, descriptor);
            descriptorsBySlug.put(descriptor.getSlug(), descriptor);
        }
    }

    private DomainModelDescriptor buildDescriptor(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        String slug = toKebabCase(simpleName);
        String title = splitCamelCase(simpleName);
        String pluralTitle = title + (title.endsWith("s") || title.endsWith("x") || title.endsWith("ch") || title.endsWith("sh") ? "es" :
                (title.endsWith("y") && !title.endsWith("ay") && !title.endsWith("ey") ? "ies" : "s"));

        List<DomainPropertyDescriptor> properties = new ArrayList<>();
        DomainPropertyDescriptor idProperty = null;

        // Inspect fields from class and superclasses
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                if (!fieldMap.containsKey(f.getName())) {
                    fieldMap.put(f.getName(), f);
                }
            }
            current = current.getSuperclass();
        }

        for (Field field : fieldMap.values()) {
            String propName = field.getName();
            Class<?> propType = field.getType();
            String label = splitCamelCase(propName);

            Method getter = findGetter(entityClass, propName, propType);
            Method setter = findSetter(entityClass, propName, propType);

            boolean isId = field.isAnnotationPresent(Id.class) || (getter != null && getter.isAnnotationPresent(Id.class)) || "id".equals(propName);
            boolean required = false;
            boolean readOnly = false;

            Column col = field.getAnnotation(Column.class);
            if (col != null) {
                if (!col.nullable()) {
                    required = true;
                }
                if (!col.updatable()) {
                    readOnly = true;
                }
            }
            if (propType.isPrimitive()) {
                required = true;
            }
            if (isId) {
                readOnly = true;
            }

            DomainPropertyType domainPropertyType = determinePropertyType(field, propType, isId);
            List<String> enumValues = Collections.emptyList();
            if (propType.isEnum()) {
                enumValues = Arrays.stream(propType.getEnumConstants())
                        .map(Object::toString)
                        .toList();
            }

            Class<?> targetEntityClass = null;
            String targetEntitySlug = null;
            if (domainPropertyType == DomainPropertyType.MANY_TO_ONE) {
                targetEntityClass = propType;
                targetEntitySlug = toKebabCase(propType.getSimpleName());
            }

            // If it is OneToMany or ManyToMany or collection, we skip it for form fields/list columns to avoid recursion
            if (Collection.class.isAssignableFrom(propType) || Map.class.isAssignableFrom(propType)) {
                continue;
            }

            DomainPropertyDescriptor propDesc = new DomainPropertyDescriptor(
                    propName,
                    label,
                    propType,
                    domainPropertyType,
                    required,
                    readOnly,
                    enumValues,
                    targetEntityClass,
                    targetEntitySlug,
                    getter,
                    setter,
                    field
            );

            if (isId && idProperty == null) {
                idProperty = propDesc;
            }
            properties.add(propDesc);
        }

        return new DomainModelDescriptor(
                simpleName,
                slug,
                title,
                pluralTitle,
                entityClass,
                properties,
                idProperty
        );
    }

    private DomainPropertyType determinePropertyType(Field field, Class<?> type, boolean isId) {
        if (isId) {
            return DomainPropertyType.ID;
        }
        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            return DomainPropertyType.MANY_TO_ONE;
        }
        if (field.isAnnotationPresent(Embedded.class)) {
            return DomainPropertyType.EMBEDDED;
        }
        if (type == String.class) {
            return DomainPropertyType.STRING;
        }
        if (type == Long.class || type == long.class ||
                type == Integer.class || type == int.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class ||
                type == Short.class || type == short.class ||
                type == BigDecimal.class || type == BigInteger.class) {
            return DomainPropertyType.NUMBER;
        }
        if (type == Boolean.class || type == boolean.class) {
            return DomainPropertyType.BOOLEAN;
        }
        if (type == LocalDate.class) {
            return DomainPropertyType.DATE;
        }
        if (type == LocalDateTime.class) {
            return DomainPropertyType.DATETIME;
        }
        if (type.isEnum()) {
            return DomainPropertyType.ENUM;
        }
        return DomainPropertyType.UNKNOWN;
    }

    private Method findGetter(Class<?> clazz, String propName, Class<?> propType) {
        String capitalized = Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
        String[] candidates = (propType == boolean.class || propType == Boolean.class)
                ? new String[]{"is" + capitalized, "get" + capitalized, propName}
                : new String[]{"get" + capitalized, propName};

        for (String candidate : candidates) {
            try {
                return clazz.getMethod(candidate);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private Method findSetter(Class<?> clazz, String propName, Class<?> propType) {
        String capitalized = Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
        String candidate = "set" + capitalized;
        try {
            return clazz.getMethod(candidate, propType);
        } catch (NoSuchMethodException ignored) {
        }
        // Try any single-parameter method with the candidate name
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(candidate) && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    public List<DomainModelDescriptor> getAllDescriptors() {
        return new ArrayList<>(descriptorsByClass.values());
    }

    public Optional<DomainModelDescriptor> findDescriptor(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return Optional.empty();
        }
        String clean = modelName.trim();
        String kebab = toKebabCase(clean);

        if (descriptorsBySlug.containsKey(kebab)) {
            return Optional.of(descriptorsBySlug.get(kebab));
        }

        // Try exact simple name (case-insensitive)
        for (DomainModelDescriptor desc : descriptorsByClass.values()) {
            if (desc.getName().equalsIgnoreCase(clean)
                    || desc.getSlug().equalsIgnoreCase(kebab)
                    || desc.getTitle().equalsIgnoreCase(clean)
                    || desc.getPluralTitle().equalsIgnoreCase(clean)) {
                return Optional.of(desc);
            }
        }

        // Try singular/plural normalization
        String singular = clean.endsWith("ies") ? clean.substring(0, clean.length() - 3) + "y" :
                (clean.endsWith("es") ? clean.substring(0, clean.length() - 2) :
                        (clean.endsWith("s") ? clean.substring(0, clean.length() - 1) : clean));
        String singularKebab = toKebabCase(singular);

        for (DomainModelDescriptor desc : descriptorsByClass.values()) {
            if (desc.getName().equalsIgnoreCase(singular)
                    || desc.getSlug().equalsIgnoreCase(singularKebab)) {
                return Optional.of(desc);
            }
        }

        return Optional.empty();
    }

    public long count(DomainModelDescriptor descriptor) {
        try {
            String jpql = "SELECT COUNT(e) FROM " + descriptor.getEntityClass().getSimpleName() + " e";
            return entityManager.createQuery(jpql, Long.class).getSingleResult();
        } catch (Exception e) {
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object> findAll(DomainModelDescriptor descriptor) {
        String jpql = "SELECT e FROM " + descriptor.getEntityClass().getSimpleName() + " e";
        if (descriptor.getIdProperty() != null) {
            jpql += " ORDER BY e." + descriptor.getIdProperty().getName() + " ASC";
        }
        return entityManager.createQuery(jpql, (Class<Object>) descriptor.getEntityClass()).getResultList();
    }

    public Optional<Object> findById(DomainModelDescriptor descriptor, Object id) {
        if (id == null) {
            return Optional.empty();
        }
        Object convertedId = convertId(id, descriptor.getIdProperty());
        if (convertedId == null) {
            return Optional.empty();
        }
        Object entity = entityManager.find(descriptor.getEntityClass(), convertedId);
        return Optional.ofNullable(entity);
    }

    public Object create(DomainModelDescriptor descriptor, Map<String, String[]> parameters) {
        try {
            Object instance = descriptor.getEntityClass().getDeclaredConstructor().newInstance();
            populateProperties(instance, descriptor, parameters, true);
            entityManager.persist(instance);
            entityManager.flush();

            Object id = descriptor.extractId(instance);
            recordAudit(descriptor.getName(), id, "ENTITY_CREATED", "Created " + descriptor.getName(), null, instanceSnapshot(descriptor, instance));
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + descriptor.getName() + ": " + e.getMessage(), e);
        }
    }

    public Object update(DomainModelDescriptor descriptor, Object id, Map<String, String[]> parameters) {
        Object existing = findById(descriptor, id)
                .orElseThrow(() -> new IllegalArgumentException(descriptor.getName() + " with id " + id + " not found"));

        String previousSnapshot = instanceSnapshot(descriptor, existing);
        populateProperties(existing, descriptor, parameters, false);
        Object merged = entityManager.merge(existing);
        entityManager.flush();

        recordAudit(descriptor.getName(), id, "ENTITY_UPDATED", "Updated " + descriptor.getName(), previousSnapshot, instanceSnapshot(descriptor, merged));
        return merged;
    }

    public boolean delete(DomainModelDescriptor descriptor, Object id) {
        Object existing = findById(descriptor, id)
                .orElseThrow(() -> new IllegalArgumentException(descriptor.getName() + " with id " + id + " not found"));

        String previousSnapshot = instanceSnapshot(descriptor, existing);
        entityManager.remove(existing);
        entityManager.flush();

        recordAudit(descriptor.getName(), id, "ENTITY_DELETED", "Deleted " + descriptor.getName(), previousSnapshot, null);
        return true;
    }

    private void recordAudit(String entityType, Object entityId, String eventType, String summary, String prev, String curr) {
        if (auditTrailWriter != null && entityId != null) {
            try {
                Long longId = entityId instanceof Long l ? l : Long.parseLong(entityId.toString());
                auditTrailWriter.record(entityType, longId, eventType, "WEB_USER", summary, prev, curr);
            } catch (Exception ignored) {
            }
        }
    }

    private String instanceSnapshot(DomainModelDescriptor descriptor, Object instance) {
        if (instance == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
            if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
                Object ref = prop.getValue(instance);
                if (ref != null) {
                    DomainModelDescriptor targetDesc = descriptorsByClass.get(prop.getTargetEntityClass());
                    Object refId = targetDesc != null ? targetDesc.extractId(ref) : ref.toString();
                    sb.append(prop.getName()).append("=").append(refId).append(";");
                }
            } else {
                Object val = prop.getValue(instance);
                if (val != null) {
                    sb.append(prop.getName()).append("=").append(val).append(";");
                }
            }
        }
        return sb.toString();
    }

    public void populateProperties(Object instance, DomainModelDescriptor descriptor, Map<String, String[]> parameters, boolean isNew) {
        for (DomainPropertyDescriptor prop : descriptor.getProperties()) {
            if (prop.isReadOnly() && (!isNew || prop.getPropertyType() == DomainPropertyType.ID)) {
                continue;
            }
            String paramName = prop.getName();
            if (parameters.containsKey(paramName)) {
                String[] values = parameters.get(paramName);
                String rawValue = values != null && values.length > 0 ? values[0] : null;
                applyPropertyValue(instance, prop, rawValue);
            } else if (prop.getPropertyType() == DomainPropertyType.BOOLEAN && !isNew) {
                // Checkbox unchecked handling
                applyPropertyValue(instance, prop, "false");
            }
        }
    }

    public void applyPropertyValue(Object instance, DomainPropertyDescriptor prop, String rawValue) {
        Object converted = convertValue(rawValue, prop);
        prop.setValue(instance, converted);
    }

    public Object convertValue(String rawValue, DomainPropertyDescriptor prop) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            if (prop.getType() == boolean.class) {
                return false;
            }
            return null;
        }
        String val = rawValue.trim();
        Class<?> type = prop.getType();

        if (type == String.class) {
            return val;
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(val);
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(val);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(val);
        }
        if (type == Float.class || type == float.class) {
            return Float.parseFloat(val);
        }
        if (type == Short.class || type == short.class) {
            return Short.parseShort(val);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(val);
        }
        if (type == BigInteger.class) {
            return new BigInteger(val);
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(val) || "1".equals(val) || "yes".equalsIgnoreCase(val) || "on".equalsIgnoreCase(val);
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(val);
        }
        if (type == LocalDateTime.class) {
            if (val.length() == 16) { // yyyy-MM-ddTHH:mm
                val += ":00";
            }
            return LocalDateTime.parse(val, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (type.isEnum()) {
            for (Object enumConstant : type.getEnumConstants()) {
                if (((Enum<?>) enumConstant).name().equalsIgnoreCase(val)) {
                    return enumConstant;
                }
            }
            return null;
        }
        if (prop.getPropertyType() == DomainPropertyType.MANY_TO_ONE && prop.getTargetEntityClass() != null) {
            Long refId = Long.parseLong(val);
            return entityManager.find(prop.getTargetEntityClass(), refId);
        }

        return val;
    }

    public List<ReferenceOption> getReferenceOptions(DomainPropertyDescriptor prop) {
        if (prop.getPropertyType() != DomainPropertyType.MANY_TO_ONE || prop.getTargetEntityClass() == null) {
            return Collections.emptyList();
        }
        Class<?> targetClass = prop.getTargetEntityClass();
        DomainModelDescriptor targetDesc = descriptorsByClass.get(targetClass);
        if (targetDesc == null) {
            return Collections.emptyList();
        }

        List<?> records = entityManager.createQuery("SELECT e FROM " + targetClass.getSimpleName() + " e", targetClass)
                .setMaxResults(100)
                .getResultList();

        List<ReferenceOption> options = new ArrayList<>();
        for (Object record : records) {
            Object id = targetDesc.extractId(record);
            if (id != null) {
                String label = formatEntitySummary(targetDesc, record);
                options.add(new ReferenceOption(id.toString(), label));
            }
        }
        return options;
    }

    public String formatEntitySummary(DomainModelDescriptor descriptor, Object entity) {
        if (entity == null) {
            return "";
        }
        Object id = descriptor.extractId(entity);
        StringBuilder label = new StringBuilder();
        if (id != null) {
            label.append("#").append(id);
        }

        // Look for common display fields: name, title, username, customerNumber, iban, description, ruleName
        for (String candidate : List.of("name", "title", "username", "customerNumber", "iban", "description", "ruleName", "clientName", "key")) {
            DomainPropertyDescriptor p = descriptor.getProperty(candidate);
            if (p != null) {
                Object val = p.getValue(entity);
                if (val != null && !val.toString().isBlank()) {
                    label.append(" - ").append(val);
                    break;
                }
            }
        }
        return label.toString();
    }

    public String formatDisplay(Object entity, DomainPropertyDescriptor property) {
        if (entity == null) {
            return "";
        }
        Object val = property.getValue(entity);
        if (val == null) {
            return "-";
        }
        if (property.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
            DomainModelDescriptor targetDesc = descriptorsByClass.get(property.getTargetEntityClass());
            if (targetDesc != null) {
                return formatEntitySummary(targetDesc, val);
            }
            return val.toString();
        }
        if (val instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (val instanceof LocalDate d) {
            return d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (val instanceof Boolean b) {
            return b ? "Yes" : "No";
        }
        return val.toString();
    }

    public String formatFormValue(Object entity, DomainPropertyDescriptor property) {
        if (entity == null) {
            return "";
        }
        Object val = property.getValue(entity);
        if (val == null) {
            return "";
        }
        if (property.getPropertyType() == DomainPropertyType.MANY_TO_ONE) {
            DomainModelDescriptor targetDesc = descriptorsByClass.get(property.getTargetEntityClass());
            if (targetDesc != null) {
                Object refId = targetDesc.extractId(val);
                return refId != null ? refId.toString() : "";
            }
            return "";
        }
        if (val instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        }
        if (val instanceof LocalDate d) {
            return d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return val.toString();
    }

    private Object convertId(Object id, DomainPropertyDescriptor idProp) {
        if (id == null) {
            return null;
        }
        if (idProp == null) {
            if (id instanceof Long || id instanceof Integer) {
                return id;
            }
            return Long.parseLong(id.toString());
        }
        Class<?> idType = idProp.getType();
        if (idType == Long.class || idType == long.class) {
            return id instanceof Long l ? l : Long.parseLong(id.toString());
        }
        if (idType == Integer.class || idType == int.class) {
            return id instanceof Integer i ? i : Integer.parseInt(id.toString());
        }
        if (idType == String.class) {
            return id.toString();
        }
        return id;
    }

    private static String toKebabCase(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && name.charAt(i - 1) != '-' && name.charAt(i - 1) != '_') {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else if (c == '_') {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String splitCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else if (c == '_') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
