package net.mooctest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCourseRepository implements CourseRepository {
    private final Map<String, Course> coursesById = new HashMap<>();

    @Override
    public synchronized Course save(Course course) {
        if (course == null) throw new ValidationException("course must not be null");
        coursesById.put(course.getId(), course);
        return course;
    }

    @Override
    public synchronized Optional<Course> findById(String id) {
        return Optional.ofNullable(coursesById.get(id));
    }

    @Override
    public synchronized Optional<Course> findByCode(String code) {
        if (code == null) return Optional.empty();
        String target = code.trim().toUpperCase();
        return coursesById.values().stream()
                .filter(c -> c.getCode().equalsIgnoreCase(target))
                .findFirst();
    }

    @Override
    public synchronized List<Course> findAll() {
        return new ArrayList<>(coursesById.values());
    }

    @Override
    public synchronized void deleteById(String id) {
        coursesById.remove(id);
    }
}


