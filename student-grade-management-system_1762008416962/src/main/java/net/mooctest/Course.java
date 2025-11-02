package net.mooctest;

import java.util.Objects;
import java.util.UUID;

public class Course {
    private final String id;
    private String code;
    private String title;
    private int creditHours;

    public Course(String code, String title, int creditHours) {
        ValidationUtil.requireNonBlank(code, "code");
        ValidationUtil.requireNonBlank(title, "title");
        ValidationUtil.requirePositive(creditHours, "creditHours");
        this.id = UUID.randomUUID().toString();
        this.code = code.trim().toUpperCase();
        this.title = title.trim();
        this.creditHours = creditHours;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        ValidationUtil.requireNonBlank(code, "code");
        this.code = code.trim().toUpperCase();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        ValidationUtil.requireNonBlank(title, "title");
        this.title = title.trim();
    }

    public int getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(int creditHours) {
        ValidationUtil.requirePositive(creditHours, "creditHours");
        this.creditHours = creditHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return id.equals(course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


