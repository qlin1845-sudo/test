package net.mooctest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryEnrollmentRepository implements EnrollmentRepository {
    private final Map<String, Enrollment> enrollmentsById = new HashMap<>();

    @Override
    public synchronized Enrollment save(Enrollment enrollment) {
        if (enrollment == null) throw new ValidationException("enrollment must not be null");
        enrollmentsById.put(enrollment.getId(), enrollment);
        return enrollment;
    }

    @Override
    public synchronized Optional<Enrollment> findById(String id) {
        return Optional.ofNullable(enrollmentsById.get(id));
    }

    @Override
    public synchronized List<Enrollment> findByStudentId(String studentId) {
        return enrollmentsById.values().stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Enrollment> findByCourseId(String courseId) {
        return enrollmentsById.values().stream()
                .filter(e -> e.getCourseId().equals(courseId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Enrollment> findAll() {
        return new ArrayList<>(enrollmentsById.values());
    }

    @Override
    public synchronized void deleteById(String id) {
        enrollmentsById.remove(id);
    }
}


