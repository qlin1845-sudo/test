package net.mooctest;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class GradingPolicy {
    private final Map<GradeComponentType, GradeComponent> components;

    public GradingPolicy(Map<GradeComponentType, GradeComponent> components) {
        if (components == null || components.isEmpty()) {
            throw new ValidationException("components must not be empty");
        }
        double total = 0.0;
        for (GradeComponent c : components.values()) {
            total += c.getWeight();
        }
        if (Math.abs(total - 1.0) > 1e-6) {
            throw new ValidationException("Total weight must equal 1.0");
        }
        this.components = new EnumMap<>(components);
    }

    public Map<GradeComponentType, GradeComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }
}


