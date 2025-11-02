package net.mooctest;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class StudentTest {

    private GradingPolicy createStandardPolicy() {
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.4));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.6));
        return new GradingPolicy(components);
    }

    @Test
    public void testCourseBasicBehavior() {
        // 测试课程对象正常创建及属性变更后的期望行为
        Course course = new Course(" cs101 ", " 计算机导论 ", 3);
        assertEquals("CS101", course.getCode());
        assertEquals("计算机导论", course.getTitle());
        assertEquals(3, course.getCreditHours());
        String id = course.getId();

        course.setCode("cs102");
        course.setTitle(" 数据结构 ");
        course.setCreditHours(4);

        assertEquals("CS102", course.getCode());
        assertEquals("数据结构", course.getTitle());
        assertEquals(4, course.getCreditHours());
        assertEquals(id, course.getId());
        assertEquals(course, course);
        assertNotEquals(course, new Course("CS103", "离散数学", 3));
        assertNotEquals(course, new Object());
    }

    @Test
    public void testCourseValidation() {
        // 验证课程对象在非法参数下会抛出校验异常
        try {
            new Course("", "名称", 3);
            fail("应当因为代码为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            new Course("CS101", "", 3);
            fail("应当因为标题为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            new Course("CS101", "名称", 0);
            fail("应当因为学分为零而抛出异常");
        } catch (ValidationException expected) {
        }
        Course valid = new Course("CS101", "名称", 3);
        try {
            valid.setCode("   ");
            fail("应当因为新代码为空白而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            valid.setTitle(null);
            fail("应当因为新标题为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            valid.setCreditHours(-1);
            fail("应当因为学分为负而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testStudentBasicBehavior() {
        // 测试学生对象的正常创建、属性修改以及相等性判断
        Student student = new Student(" 张三 ", LocalDate.now().minusYears(20));
        assertEquals("张三", student.getName());
        LocalDate newDate = LocalDate.now().minusYears(19);
        student.setName(" 李四 ");
        student.setDateOfBirth(newDate);
        assertEquals("李四", student.getName());
        assertEquals(newDate, student.getDateOfBirth());
        assertEquals(student, student);
        assertNotEquals(student, new Student("王五", LocalDate.now().minusYears(21)));
        assertNotEquals(student, new Object());
    }

    @Test
    public void testStudentValidation() {
        // 验证学生对象在非法数据下的异常处理逻辑
        try {
            new Student("   ", LocalDate.now());
            fail("应当因为姓名为空白而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            new Student("张三", LocalDate.now().plusDays(1));
            fail("应当因为生日在未来而抛出异常");
        } catch (ValidationException expected) {
        }
        Student student = new Student("张三", LocalDate.now());
        try {
            student.setName(null);
            fail("应当因为姓名为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            student.setDateOfBirth(LocalDate.now().plusDays(1));
            fail("应当因为生日在未来而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testValidationUtilFailures() {
        // 检查校验工具在各种非法输入下是否能准确抛出异常
        try {
            ValidationUtil.requireNonBlank(" ", "blank");
            fail("应当因为字符串为空白而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            ValidationUtil.requirePositive(0, "positive");
            fail("应当因为数值非正而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            ValidationUtil.requireNonNegative(-1, "nonNegative");
            fail("应当因为数值为负而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            ValidationUtil.requireBetween(10.1, 0.0, 10.0, "between");
            fail("应当因为超出范围而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            ValidationUtil.requirePastOrPresent(null, "date");
            fail("应当因为日期为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            ValidationUtil.requirePastOrPresent(LocalDate.now().plusDays(1), "date");
            fail("应当因为日期在未来而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testValidationUtilSuccess() {
        // 验证校验工具在合法输入下不会抛出异常
        ValidationUtil.requireNonBlank("ok", "test");
        ValidationUtil.requirePositive(1, "test");
        ValidationUtil.requireNonNegative(0, "test");
        ValidationUtil.requireBetween(5.0, 0.0, 10.0, "test");
        ValidationUtil.requirePastOrPresent(LocalDate.now(), "test");
    }

    @Test
    public void testGradeComponentBehavior() {
        // 测试成绩组成对象的正常属性读写与相等性
        GradeComponent component = new GradeComponent(GradeComponentType.ASSIGNMENT, 0.5);
        component.setType(GradeComponentType.PROJECT);
        component.setWeight(0.3);
        assertEquals(GradeComponentType.PROJECT, component.getType());
        assertEquals(0.3, component.getWeight(), 1e-9);
        GradeComponent sameType = new GradeComponent(GradeComponentType.PROJECT, 0.9);
        assertEquals(component, sameType);
        assertEquals(component.hashCode(), sameType.hashCode());
        assertNotEquals(component, new GradeComponent(GradeComponentType.FINAL, 0.3));
    }

    @Test
    public void testGradeComponentValidation() {
        // 验证成绩组成对象在非法输入下的防御性校验
        try {
            new GradeComponent(null, 0.1);
            fail("应当因为类型为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            new GradeComponent(GradeComponentType.ASSIGNMENT, -0.1);
            fail("应当因为权重越界而抛出异常");
        } catch (ValidationException expected) {
        }
        GradeComponent component = new GradeComponent(GradeComponentType.QUIZ, 0.2);
        try {
            component.setType(null);
            fail("应当因为设置空类型而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            component.setWeight(1.5);
            fail("应当因为权重超过上限而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testGradeRecordBehavior() {
        // 验证成绩记录的得分读写及越界防护
        GradeRecord record = new GradeRecord(GradeComponentType.MIDTERM, 80.0);
        assertEquals(GradeComponentType.MIDTERM, record.getComponentType());
        assertEquals(80.0, record.getScore(), 1e-9);
        record.setScore(95.0);
        assertEquals(95.0, record.getScore(), 1e-9);
        try {
            record.setScore(-1.0);
            fail("应当因为分数越界而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testGradingPolicyCreationAndAccess() {
        // 检查评分策略创建成功后返回的视图不可修改且数据正确
        GradingPolicy policy = createStandardPolicy();
        Map<GradeComponentType, GradeComponent> view = policy.getComponents();
        assertEquals(2, view.size());
        assertTrue(view.containsKey(GradeComponentType.ASSIGNMENT));
        try {
            view.put(GradeComponentType.PROJECT, new GradeComponent(GradeComponentType.PROJECT, 0.1));
            fail("评分策略的组件集合应为只读");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGradingPolicyValidation() {
        // 验证评分策略在非法配置下的异常提示
        try {
            new GradingPolicy(null);
            fail("应当因为组件为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            Map<GradeComponentType, GradeComponent> empty = Collections.emptyMap();
            new GradingPolicy(empty);
            fail("应当因为组件列表为空而抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            Map<GradeComponentType, GradeComponent> invalid = new EnumMap<>(GradeComponentType.class);
            invalid.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.3));
            invalid.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.3));
            new GradingPolicy(invalid);
            fail("应当因为权重之和不为1而抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testEnrollmentStateTransitions() {
        // 测试选课对象在不同状态之间转换时的限制规则
        Enrollment enrollment = new Enrollment("stu", "course", 2024, Term.SPRING);
        enrollment.complete();
        assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
        try {
            enrollment.drop();
            fail("完成状态不允许退课");
        } catch (DomainException expected) {
        }

        Enrollment dropped = new Enrollment("stu", "course", 2024, Term.SPRING);
        dropped.drop();
        assertEquals(EnrollmentStatus.DROPPED, dropped.getStatus());
        try {
            dropped.complete();
            fail("退课状态不允许结课");
        } catch (DomainException expected) {
        }

        Enrollment incomplete = new Enrollment("stu", "course", 2024, Term.SPRING);
        incomplete.markIncomplete();
        assertEquals(EnrollmentStatus.INCOMPLETE, incomplete.getStatus());
        try {
            incomplete.markIncomplete();
            fail("非在读状态不允许再次标记未完成");
        } catch (DomainException expected) {
        }
    }

    @Test
    public void testEnrollmentRecordGradeAndAverage() {
        // 验证选课记录成绩后能够正确计入平均分，并确保视图不可变
        Enrollment enrollment = new Enrollment("stu", "course", 2024, Term.SPRING);
        GradeRecord record = new GradeRecord(GradeComponentType.ASSIGNMENT, 80.0);
        enrollment.recordGrade(record);
        assertEquals(record, enrollment.getGradesByComponent().get(GradeComponentType.ASSIGNMENT));
        Map<GradeComponentType, GradeComponent> policyComponents = new EnumMap<>(GradeComponentType.class);
        policyComponents.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.5));
        policyComponents.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.5));
        double average = enrollment.getAverageScore(policyComponents);
        assertEquals(40.0, average, 1e-9);
        try {
            enrollment.getGradesByComponent().put(GradeComponentType.FINAL, record);
            fail("成绩映射应保持只读");
        } catch (UnsupportedOperationException expected) {
        }

        Map<GradeComponentType, GradeComponent> zeroWeight = new EnumMap<>(GradeComponentType.class);
        zeroWeight.put(GradeComponentType.PROJECT, new GradeComponent(GradeComponentType.PROJECT, 0.0));
        try {
            enrollment.getAverageScore(null);
            fail("策略为空时应抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            enrollment.getAverageScore(Collections.<GradeComponentType, GradeComponent>emptyMap());
            fail("策略为空集合时应抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            enrollment.getAverageScore(zeroWeight);
            fail("总权重不大于零时应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testEnrollmentRecordGradeValidations() {
        // 验证选课在非法状态或数据下的成绩录入防护
        Enrollment enrollment = new Enrollment("stu", "course", 2024, Term.SPRING);
        try {
            enrollment.recordGrade(null);
            fail("成绩记录为空应当抛出异常");
        } catch (ValidationException expected) {
        }
        enrollment.drop();
        try {
            enrollment.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90.0));
            fail("退课后不应允许录入成绩");
        } catch (DomainException expected) {
        }
    }

    @Test
    public void testInMemoryCourseRepository() {
        // 验证课程仓储的增删查以及大小写无关查询能力
        InMemoryCourseRepository repo = new InMemoryCourseRepository();
        Course course = repo.save(new Course("CS101", "课程", 3));
        assertTrue(repo.findById(course.getId()).isPresent());
        assertEquals(course, repo.findById(course.getId()).get());
        assertTrue(repo.findByCode(" cs101 ").isPresent());
        assertFalse(repo.findByCode(null).isPresent());
        assertEquals(1, repo.findAll().size());
        repo.deleteById(course.getId());
        assertFalse(repo.findById(course.getId()).isPresent());
        try {
            repo.save(null);
            fail("保存空课程应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testInMemoryStudentRepository() {
        // 验证学生仓储的常用操作以及名称查询的容错特性
        InMemoryStudentRepository repo = new InMemoryStudentRepository();
        Student student = repo.save(new Student("张三", LocalDate.now().minusYears(18)));
        assertTrue(repo.findById(student.getId()).isPresent());
        assertTrue(repo.findByName(" 张三 ").isPresent());
        assertFalse(repo.findByName(null).isPresent());
        List<Student> all = repo.findAll();
        assertEquals(1, all.size());
        repo.deleteById(student.getId());
        assertFalse(repo.findById(student.getId()).isPresent());
        try {
            repo.save(null);
            fail("保存空学生应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testInMemoryEnrollmentRepository() {
        // 验证选课仓储的查询能力及空值防护
        InMemoryEnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment enrollment = repo.save(new Enrollment("stu1", "course1", 2024, Term.FALL));
        Enrollment enrollment2 = repo.save(new Enrollment("stu1", "course2", 2024, Term.FALL));
        assertTrue(repo.findById(enrollment.getId()).isPresent());
        assertEquals(2, repo.findByStudentId("stu1").size());
        assertEquals(1, repo.findByCourseId("course2").size());
        assertEquals(2, repo.findAll().size());
        repo.deleteById(enrollment2.getId());
        assertEquals(1, repo.findAll().size());
        try {
            repo.save(null);
            fail("保存空选课应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testGradeServiceRecordAndCompute() {
        // 测试成绩服务在录入、更新以及计算百分比和绩点时的行为
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        GradeService gradeService = new GradeService(enrollmentRepository, policy);

        Enrollment enrollment = new Enrollment("stu", "course", 2024, Term.SPRING);
        enrollmentRepository.save(enrollment);

        gradeService.recordGrade(enrollment.getId(), GradeComponentType.ASSIGNMENT, 80.0);
        gradeService.updateGrade(enrollment.getId(), GradeComponentType.ASSIGNMENT, 90.0);
        gradeService.recordGrade(enrollment.getId(), GradeComponentType.FINAL, 100.0);

        double percentage = gradeService.computePercentage(enrollment.getId());
        assertEquals(0.4 * 90 + 0.6 * 100, percentage, 1e-9);
        assertEquals(gradeService.toGpa(percentage), gradeService.computeGpa(enrollment.getId()), 1e-9);
    }

    @Test
    public void testGradeServiceErrorPaths() {
        // 验证成绩服务在缺失数据或非法参数时的异常处理
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        Map<GradeComponentType, GradeComponent> map = new EnumMap<>(GradeComponentType.class);
        map.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 1.0));
        GradingPolicy policy = new GradingPolicy(map);
        GradeService gradeService = new GradeService(enrollmentRepository, policy);

        try {
            gradeService.recordGrade("missing", GradeComponentType.ASSIGNMENT, 80.0);
            fail("找不到选课应抛出异常");
        } catch (DomainException expected) {
        }
        Enrollment enrollment = new Enrollment("stu", "course", 2024, Term.SPRING);
        enrollmentRepository.save(enrollment);
        try {
            gradeService.recordGrade(enrollment.getId(), GradeComponentType.FINAL, 80.0);
            fail("策略中未定义的组成应抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            gradeService.ensureComponentExists(GradeComponentType.PROJECT);
            fail("缺失的组成应抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            gradeService.computePercentage("missing");
            fail("无法找到选课应抛出异常");
        } catch (DomainException expected) {
        }
        try {
            gradeService.toGpa(-1);
            fail("百分比越界应抛出异常");
        } catch (ValidationException expected) {
        }
        try {
            gradeService.toGpa(101);
            fail("百分比超过100应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testGradeServiceToGpaMapping() {
        // 通过多组边界数据验证百分比到绩点的映射准确性
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        Map<GradeComponentType, GradeComponent> map = new EnumMap<>(GradeComponentType.class);
        map.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 1.0));
        GradingPolicy policy = new GradingPolicy(map);
        GradeService gradeService = new GradeService(enrollmentRepository, policy);

        assertEquals(4.0, gradeService.toGpa(93), 1e-9);
        assertEquals(3.7, gradeService.toGpa(90), 1e-9);
        assertEquals(3.3, gradeService.toGpa(87), 1e-9);
        assertEquals(3.0, gradeService.toGpa(83), 1e-9);
        assertEquals(2.7, gradeService.toGpa(80), 1e-9);
        assertEquals(2.3, gradeService.toGpa(77), 1e-9);
        assertEquals(2.0, gradeService.toGpa(73), 1e-9);
        assertEquals(1.7, gradeService.toGpa(70), 1e-9);
        assertEquals(1.3, gradeService.toGpa(67), 1e-9);
        assertEquals(1.0, gradeService.toGpa(63), 1e-9);
        assertEquals(0.7, gradeService.toGpa(60), 1e-9);
        assertEquals(0.0, gradeService.toGpa(50), 1e-9);
    }

    @Test
    public void testEnrollmentServiceEnrollFlow() {
        // 通过真实的内存仓储验证选课服务的正常报名流程
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        EnrollmentService enrollmentService = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, policy);

        Student student = studentRepository.save(new Student("张三", LocalDate.now().minusYears(18)));
        Course course = courseRepository.save(new Course("CS101", "课程", 3));

        Enrollment enrollment = enrollmentService.enroll(student.getId(), course.getId(), 2024, Term.SPRING);
        assertNotNull(enrollment.getId());
        assertTrue(enrollmentRepository.findById(enrollment.getId()).isPresent());
    }

    @Test
    public void testEnrollmentServiceValidations() {
        // 验证选课服务在关键校验场景下的异常处理
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        EnrollmentService enrollmentService = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, policy);

        Course course = courseRepository.save(new Course("CS101", "课程", 3));
        try {
            enrollmentService.enroll("missing", course.getId(), 2024, Term.SPRING);
            fail("找不到学生时应抛出异常");
        } catch (DomainException expected) {
        }
        Student student = studentRepository.save(new Student("张三", LocalDate.now().minusYears(18)));
        try {
            enrollmentService.enroll(student.getId(), "missing", 2024, Term.SPRING);
            fail("找不到课程时应抛出异常");
        } catch (DomainException expected) {
        }
        enrollmentService.enroll(student.getId(), course.getId(), 2024, Term.SPRING);
        Enrollment duplicate = new Enrollment(student.getId(), course.getId(), 2024, Term.SPRING);
        enrollmentRepository.save(duplicate);
        try {
            enrollmentService.enroll(student.getId(), course.getId(), 2024, Term.SPRING);
            fail("重复报名相同课程应抛出异常");
        } catch (DomainException expected) {
        }
    }

    @Test
    public void testEnrollmentServiceDropAndCompute() {
        // 测试退课操作以及选课百分比计算的正确性
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        EnrollmentService enrollmentService = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, policy);

        Student student = studentRepository.save(new Student("张三", LocalDate.now().minusYears(18)));
        Course course = courseRepository.save(new Course("CS101", "课程", 3));
        Enrollment enrollment = enrollmentService.enroll(student.getId(), course.getId(), 2024, Term.SPRING);
        enrollment.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 85.0));
        enrollment.recordGrade(new GradeRecord(GradeComponentType.FINAL, 95.0));
        enrollmentRepository.save(enrollment);

        double percentage = enrollmentService.computeEnrollmentPercentage(enrollment.getId());
        assertEquals(0.4 * 85 + 0.6 * 95, percentage, 1e-9);

        enrollmentService.drop(enrollment.getId());
        Enrollment afterDrop = enrollmentRepository.findById(enrollment.getId()).get();
        assertEquals(EnrollmentStatus.DROPPED, afterDrop.getStatus());
    }

    @Test
    public void testReportServiceBuildTranscript() {
        // 构造多种状态的选课记录验证成绩单生成逻辑
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        GradeService gradeService = new GradeService(enrollmentRepository, policy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, policy, gradeService);

        Student student = studentRepository.save(new Student("张三", LocalDate.now().minusYears(18)));
        Course course1 = courseRepository.save(new Course("CS101", "课程一", 3));
        Course course2 = courseRepository.save(new Course("CS102", "课程二", 4));
        Course course3 = courseRepository.save(new Course("CS103", "课程三", 2));

        Enrollment completed = new Enrollment(student.getId(), course1.getId(), 2024, Term.SPRING);
        completed.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 90));
        completed.recordGrade(new GradeRecord(GradeComponentType.FINAL, 95));
        completed.complete();
        enrollmentRepository.save(completed);

        Enrollment incomplete = new Enrollment(student.getId(), course2.getId(), 2024, Term.SUMMER);
        incomplete.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 80));
        incomplete.recordGrade(new GradeRecord(GradeComponentType.FINAL, 70));
        incomplete.markIncomplete();
        enrollmentRepository.save(incomplete);

        Enrollment dropped = new Enrollment(student.getId(), course3.getId(), 2024, Term.FALL);
        dropped.drop();
        enrollmentRepository.save(dropped);

        Transcript transcript = reportService.buildTranscript(student.getId());
        List<Transcript.LineItem> items = transcript.getItems();
        assertEquals(2, items.size());
        assertEquals("CS101", items.get(0).getCourseCode());
        assertEquals("课程二", items.get(1).getCourseTitle());
        assertTrue(transcript.computeCumulativeGpa() > 0);
    }

    @Test
    public void testReportServiceValidationPaths() {
        // 验证成绩单服务在缺失基本数据时会抛出清晰异常
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        InMemoryEnrollmentRepository enrollmentRepository = new InMemoryEnrollmentRepository();
        GradingPolicy policy = createStandardPolicy();
        GradeService gradeService = new GradeService(enrollmentRepository, policy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, policy, gradeService);

        try {
            reportService.buildTranscript("missing");
            fail("未找到学生应抛出异常");
        } catch (DomainException expected) {
        }

        Student student = studentRepository.save(new Student("张三", LocalDate.now().minusYears(18)));
        Enrollment enrollment = new Enrollment(student.getId(), "missingCourse", 2024, Term.SPRING);
        enrollment.complete();
        enrollment.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 90));
        enrollment.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90));
        enrollmentRepository.save(enrollment);
        try {
            reportService.buildTranscript(student.getId());
            fail("课程缺失应抛出异常");
        } catch (DomainException expected) {
        }
    }

    @Test
    public void testTranscriptOperations() {
        // 通过多条项目验证成绩单的累计绩点计算
        Transcript transcript = new Transcript();
        Transcript.LineItem item1 = new Transcript.LineItem("CS101", "课程一", 3, 90, 3.7);
        Transcript.LineItem item2 = new Transcript.LineItem("CS102", "课程二", 4, 95, 4.0);
        transcript.addItem(item1);
        transcript.addItem(item2);
        List<Transcript.LineItem> items = transcript.getItems();
        assertEquals(2, items.size());
        double expectedGpa = (3 * 3.7 + 4 * 4.0) / 7;
        assertEquals(expectedGpa, transcript.computeCumulativeGpa(), 1e-9);
        try {
            items.add(item1);
            fail("返回的成绩单条目应为只读");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testTranscriptValidation() {
        // 验证成绩单在无课程数据或空条目时的行为
        Transcript transcript = new Transcript();
        assertEquals(0.0, transcript.computeCumulativeGpa(), 1e-9);
        try {
            transcript.addItem(null);
            fail("添加空条目应抛出异常");
        } catch (ValidationException expected) {
        }
    }

    @Test
    public void testDomainAndValidationExceptionHierarchy() {
        // 检查领域异常与校验异常的继承及消息保留情况
        DomainException domainException = new DomainException("domain error");
        assertEquals("domain error", domainException.getMessage());
        ValidationException validationException = new ValidationException("validation error");
        assertTrue(validationException instanceof DomainException);
        assertEquals("validation error", validationException.getMessage());
    }
}
