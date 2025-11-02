package net.mooctest;

import java.util.List;
import java.util.Map;

public class ReportService {
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradingPolicy gradingPolicy;
    private final GradeService gradeService;

    public ReportService(StudentRepository studentRepository,
                         CourseRepository courseRepository,
                         EnrollmentRepository enrollmentRepository,
                         GradingPolicy gradingPolicy,
                         GradeService gradeService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradingPolicy = gradingPolicy;
        this.gradeService = gradeService;
    }

    public Transcript buildTranscript(String studentId) {
        studentRepository.findById(studentId).orElseThrow(() -> new DomainException("Student not found: " + studentId));
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        Transcript transcript = new Transcript();
        Map<GradeComponentType, GradeComponent> components = gradingPolicy.getComponents();
        for (Enrollment e : enrollments) {
            if (e.getStatus() != EnrollmentStatus.COMPLETED && e.getStatus() != EnrollmentStatus.INCOMPLETE) {
                continue;
            }
            Course course = courseRepository.findById(e.getCourseId())
                    .orElseThrow(() -> new DomainException("Course not found: " + e.getCourseId()));
            double percentage = e.getAverageScore(components);
            double gpaPoints = gradeService.toGpa(percentage);
            transcript.addItem(new Transcript.LineItem(course.getCode(), course.getTitle(), course.getCreditHours(), percentage, gpaPoints));
        }
        return transcript;
    }
}


