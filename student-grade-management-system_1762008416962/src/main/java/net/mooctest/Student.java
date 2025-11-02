package net.mooctest;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Student {
    private final String id;
    private String name;
    private LocalDate dateOfBirth;

    public Student(String name, LocalDate dateOfBirth) {
        ValidationUtil.requireNonBlank(name, "name");
        ValidationUtil.requirePastOrPresent(dateOfBirth, "dateOfBirth");
        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
        this.dateOfBirth = dateOfBirth;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        ValidationUtil.requireNonBlank(name, "name");
        this.name = name.trim();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        ValidationUtil.requirePastOrPresent(dateOfBirth, "dateOfBirth");
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student student = (Student) o;
        return id.equals(student.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


