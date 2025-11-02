package net.mooctest;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Enrollment {
    private final String id;
    private final String studentId;
    private final String courseId;
    private final int year;
    private final Term term;
    private EnrollmentStatus status;
    private final Map<GradeComponentType, GradeRecord> gradesByComponent;

    public Enrollment(String studentId, String courseId, int year, Term term) {
        ValidationUtil.requireNonBlank(studentId, "studentId");
        ValidationUtil.requireNonBlank(courseId, "courseId");
        ValidationUtil.requirePositive(year, "year");
        if (term == null) throw new ValidationException("term must not be null");
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.courseId = courseId;
        this.year = year;
        this.term = term;
        this.status = EnrollmentStatus.ENROLLED;
        this.gradesByComponent = new EnumMap<>(GradeComponentType.class);
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public int getYear() { return year; }
    public Term getTerm() { return term; }
    public EnrollmentStatus getStatus() { return status; }

    public void drop() {
        if (status == EnrollmentStatus.COMPLETED) {
            throw new DomainException("Cannot drop a completed enrollment");
        }
        status = EnrollmentStatus.DROPPED;
    }

    public void complete() {
        if (status == EnrollmentStatus.DROPPED) {
            throw new DomainException("Cannot complete a dropped enrollment");
        }
        status = EnrollmentStatus.COMPLETED;
    }

    public void markIncomplete() {
        if (status != EnrollmentStatus.ENROLLED) {
            throw new DomainException("Incomplete allowed only from ENROLLED state");
        }
        status = EnrollmentStatus.INCOMPLETE;
    }

    public Map<GradeComponentType, GradeRecord> getGradesByComponent() {
        return Collections.unmodifiableMap(gradesByComponent);
    }

    public void recordGrade(GradeRecord record) {
        if (status == EnrollmentStatus.DROPPED) {
            throw new DomainException("Cannot record grades for dropped enrollment");
        }
        if (record == null) throw new ValidationException("record must not be null");
        gradesByComponent.put(record.getComponentType(), record);
    }

    public double getAverageScore(Map<GradeComponentType, GradeComponent> policyComponents) {
        if (policyComponents == null || policyComponents.isEmpty()) {
            throw new ValidationException("policyComponents must not be empty");
        }
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        for (Map.Entry<GradeComponentType, GradeComponent> entry : policyComponents.entrySet()) {
            GradeComponentType type = entry.getKey();
            GradeComponent component = entry.getValue();
            double weight = component.getWeight();
            GradeRecord record = gradesByComponent.get(type);
            if (record != null) {
                weightedSum += record.getScore() * weight;
            }
            totalWeight += weight;
        }
        if (totalWeight <= 0.0) {
            throw new ValidationException("Total weight must be positive");
        }
        return weightedSum / totalWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enrollment)) return false;
        Enrollment that = (Enrollment) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


