package net.mooctest;

import java.util.List;
import java.util.Map;

public class EnrollmentService {
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradingPolicy gradingPolicy;

    public EnrollmentService(StudentRepository studentRepository,
                             CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository,
                             GradingPolicy gradingPolicy) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradingPolicy = gradingPolicy;
    }

    public Enrollment enroll(String studentId, String courseId, int year, Term term) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new DomainException("Student not found: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new DomainException("Course not found: " + courseId));

        List<Enrollment> existing = enrollmentRepository.findByStudentId(student.getId());
        boolean alreadyEnrolled = existing.stream()
                .anyMatch(e -> e.getCourseId().equals(course.getId()) && e.getTerm() == term && e.getYear() == year);
        if (alreadyEnrolled) {
            throw new DomainException("Student already enrolled in course for the term");
        }
        Enrollment enrollment = new Enrollment(student.getId(), course.getId(), year, term);
        return enrollmentRepository.save(enrollment);
    }

    public void drop(String enrollmentId) {
        Enrollment e = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new DomainException("Enrollment not found: " + enrollmentId));
        e.drop();
        enrollmentRepository.save(e);
    }

    public double computeEnrollmentPercentage(String enrollmentId) {
        Enrollment e = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new DomainException("Enrollment not found: " + enrollmentId));
        Map<GradeComponentType, GradeComponent> components = gradingPolicy.getComponents();
        return e.getAverageScore(components);
    }
}


