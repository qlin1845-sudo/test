package net.mooctest;

import java.util.Map;

public class GradeService {
    private final EnrollmentRepository enrollmentRepository;
    private final GradingPolicy gradingPolicy;

    public GradeService(EnrollmentRepository enrollmentRepository, GradingPolicy gradingPolicy) {
        this.enrollmentRepository = enrollmentRepository;
        this.gradingPolicy = gradingPolicy;
    }

    public void recordGrade(String enrollmentId, GradeComponentType type, double score) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new DomainException("Enrollment not found: " + enrollmentId));
        ensureComponentExists(type);
        GradeRecord record = new GradeRecord(type, score);
        enrollment.recordGrade(record);
        enrollmentRepository.save(enrollment);
    }

    public void updateGrade(String enrollmentId, GradeComponentType type, double newScore) {
        recordGrade(enrollmentId, type, newScore);
    }

    public double computePercentage(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new DomainException("Enrollment not found: " + enrollmentId));
        Map<GradeComponentType, GradeComponent> components = gradingPolicy.getComponents();
        return enrollment.getAverageScore(components);
    }

    public double computeGpa(String enrollmentId) {
        double percentage = computePercentage(enrollmentId);
        return toGpa(percentage);
    }

    public double toGpa(double percentage) {
        ValidationUtil.requireBetween(percentage, 0.0, 100.0, "percentage");
        if (percentage >= 93) return 4.0;      // A
        if (percentage >= 90) return 3.7;      // A-
        if (percentage >= 87) return 3.3;      // B+
        if (percentage >= 83) return 3.0;      // B
        if (percentage >= 80) return 2.7;      // B-
        if (percentage >= 77) return 2.3;      // C+
        if (percentage >= 73) return 2.0;      // C
        if (percentage >= 70) return 1.7;      // C-
        if (percentage >= 67) return 1.3;      // D+
        if (percentage >= 63) return 1.0;      // D
        if (percentage >= 60) return 0.7;      // D-
        return 0.0;                            // F
    }

    public void ensureComponentExists(GradeComponentType type) {
        if (!gradingPolicy.getComponents().containsKey(type)) {
            throw new ValidationException("Component not in grading policy: " + type);
        }
    }
}



