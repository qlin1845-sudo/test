package net.mooctest;

import java.util.Objects;

public class GradeComponent {
    private GradeComponentType type;
    private double weight; // 0..1

    public GradeComponent(GradeComponentType type, double weight) {
        if (type == null) {
            throw new ValidationException("type must not be null");
        }
        ValidationUtil.requireBetween(weight, 0.0, 1.0, "weight");
        this.type = type;
        this.weight = weight;
    }

    public GradeComponentType getType() {
        return type;
    }

    public void setType(GradeComponentType type) {
        if (type == null) {
            throw new ValidationException("type must not be null");
        }
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        ValidationUtil.requireBetween(weight, 0.0, 1.0, "weight");
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeComponent)) return false;
        GradeComponent that = (GradeComponent) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}


