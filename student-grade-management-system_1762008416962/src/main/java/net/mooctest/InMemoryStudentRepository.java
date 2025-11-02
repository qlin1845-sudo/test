package net.mooctest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryStudentRepository implements StudentRepository {
    private final Map<String, Student> studentsById = new HashMap<>();

    @Override
    public synchronized Student save(Student student) {
        if (student == null) throw new ValidationException("student must not be null");
        studentsById.put(student.getId(), student);
        return student;
    }

    @Override
    public synchronized Optional<Student> findById(String id) {
        return Optional.ofNullable(studentsById.get(id));
    }

    @Override
    public synchronized Optional<Student> findByName(String name) {
        if (name == null) return Optional.empty();
        String target = name.trim();
        return studentsById.values().stream()
                .filter(s -> s.getName().equalsIgnoreCase(target))
                .findFirst();
    }

    @Override
    public synchronized List<Student> findAll() {
        return new ArrayList<>(studentsById.values());
    }

    @Override
    public synchronized void deleteById(String id) {
        studentsById.remove(id);
    }
}


