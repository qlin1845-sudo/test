package net.mooctest;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(String id);
    Optional<Course> findByCode(String code);
    List<Course> findAll();
    void deleteById(String id);
}


