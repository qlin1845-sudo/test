package net.mooctest;

import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(String id);
    Optional<Student> findByName(String name);
    List<Student> findAll();
    void deleteById(String id);
}


