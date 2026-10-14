package de.bohnottensen.financialaudit.infrastructure.web.crud;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class DomainPropertyDescriptor {

    private final String name;
    private final String label;
    private final Class<?> type;
    private final DomainPropertyType propertyType;
    private final boolean required;
    private final boolean readOnly;
    private final List<String> enumValues;
    private final Class<?> targetEntityClass;
    private final String targetEntitySlug;
    private final Method getter;
    private final Method setter;
    private final Field field;

    public DomainPropertyDescriptor(
            String name,
            String label,
            Class<?> type,
            DomainPropertyType propertyType,
            boolean required,
            boolean readOnly,
            List<String> enumValues,
            Class<?> targetEntityClass,
            String targetEntitySlug,
            Method getter,
            Method setter,
            Field field) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.propertyType = propertyType;
        this.required = required;
        this.readOnly = readOnly;
        this.enumValues = enumValues != null ? enumValues : Collections.emptyList();
        this.targetEntityClass = targetEntityClass;
        this.targetEntitySlug = targetEntitySlug;
        this.getter = getter;
        this.setter = setter;
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Class<?> getType() {
        return type;
    }

    public DomainPropertyType getPropertyType() {
        return propertyType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public Class<?> getTargetEntityClass() {
        return targetEntityClass;
    }

    public String getTargetEntitySlug() {
        return targetEntitySlug;
    }

    public Method getGetter() {
        return getter;
    }

    public Method getSetter() {
        return setter;
    }

    public Field getField() {
        return field;
    }

    public Object getValue(Object target) {
        if (target == null) {
            return null;
        }
        try {
            if (getter != null) {
                return getter.invoke(target);
            }
            if (field != null) {
                field.setAccessible(true);
                return field.get(target);
            }
        } catch (Exception e) {
            // fallback
        }
        return null;
    }

    public void setValue(Object target, Object value) {
        if (target == null || readOnly) {
            return;
        }
        try {
            if (setter != null) {
                setter.invoke(target, value);
            } else if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Exception e) {
            // fallback
        }
    }
}
