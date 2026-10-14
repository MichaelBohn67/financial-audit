package de.bohnottensen.financialaudit.infrastructure.web.crud;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DomainModelDescriptor {

    private final String name;
    private final String slug;
    private final String title;
    private final String pluralTitle;
    private final Class<?> entityClass;
    private final List<DomainPropertyDescriptor> properties;
    private final DomainPropertyDescriptor idProperty;
    private final Map<String, DomainPropertyDescriptor> propertyMap;

    public DomainModelDescriptor(
            String name,
            String slug,
            String title,
            String pluralTitle,
            Class<?> entityClass,
            List<DomainPropertyDescriptor> properties,
            DomainPropertyDescriptor idProperty) {
        this.name = name;
        this.slug = slug;
        this.title = title;
        this.pluralTitle = pluralTitle;
        this.entityClass = entityClass;
        this.properties = Collections.unmodifiableList(properties);
        this.idProperty = idProperty;
        this.propertyMap = properties.stream()
                .collect(Collectors.toMap(DomainPropertyDescriptor::getName, Function.identity(), (a, b) -> a));
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getPluralTitle() {
        return pluralTitle;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public List<DomainPropertyDescriptor> getProperties() {
        return properties;
    }

    public DomainPropertyDescriptor getIdProperty() {
        return idProperty;
    }

    public DomainPropertyDescriptor getProperty(String name) {
        return propertyMap.get(name);
    }

    public Object extractId(Object entity) {
        if (entity == null || idProperty == null) {
            return null;
        }
        return idProperty.getValue(entity);
    }
}
