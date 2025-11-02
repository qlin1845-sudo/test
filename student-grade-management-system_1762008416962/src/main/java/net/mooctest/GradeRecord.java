package net.mooctest;

public class GradeRecord {
    private final GradeComponentType componentType;
    private double score; // 0..100

    public GradeRecord(GradeComponentType componentType, double score) {
        if (componentType == null) {
            throw new ValidationException("componentType must not be null");
        }
        ValidationUtil.requireBetween(score, 0.0, 100.0, "score");
        this.componentType = componentType;
        this.score = score;
    }

    public GradeComponentType getComponentType() {
        return componentType;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        ValidationUtil.requireBetween(score, 0.0, 100.0, "score");
        this.score = score;
    }
}


