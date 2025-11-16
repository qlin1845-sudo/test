package net.mooctest;

import static org.junit.Assert.*;
import java.util.*;
import java.time.LocalDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * 开发者做题前，请仔细阅读以下说明：
 *
 * 1、该测试类为测试类示例，不要求完全按照该示例类的格式；
 *	    考生可自行创建测试类，类名可自行定义，但需遵循JUnit命名规范，格式为xxxTest.java，提交类似test.java的文件名将因不符合语法而判0分！
 *
 * 2、所有测试方法放在该顶层类中，不建议再创建内部类。若必需创建内部类，则需检查JUnit对于内部测试类的要求，并添加相关注释，否则将因无法执行而判0分！
 *
 * 3、本比赛使用jdk1.8+JUnit4，未使用以上版本编写测试用例者，不再接受低分申诉；
 *
 * 4、不要修改被测代码；
 *
 * 5、建议尽量避免卡点提交答案，尤其是两份报告的zip包。
 *
 * */
public class BudgetTest {

    private Budget budget;
    private Budget.Item item1;
    private Budget.Item item2;

    @Before
    public void setUp() {
        budget = new Budget();
        item1 = new Budget.Item("Laptop", 1000.0, 0.8, "ELECTRONICS");
        item2 = new Budget.Item("Desk", 500.0, 0.6, "FURNITURE");
    }

    @Test
    public void testItemConstructorWithValidParameters() {
        Budget.Item item = new Budget.Item("Test", 100.0, 0.5, "CATEGORY");
        assertEquals("Test", item.getName());
        assertEquals(100.0, item.getCost(), 0.001);
        assertEquals(0.5, item.getValue(), 0.001);
        assertEquals("CATEGORY", item.getCategory());
    }

    @Test
    public void testItemConstructorWithNullName() {
        Budget.Item item = new Budget.Item(null, 100.0, 0.5, "CATEGORY");
        assertEquals("", item.getName());
    }

    // 以下测试覆盖所有业务类，所有测试均集中于一个测试文件
    // ======================== Budget ========================

    /**
     * 用例目的：验证Budget.add对null安全；验证totalCost/totalValue的累加逻辑。
     * 预期结果：加入非空条目后总成本与总价值正确；加入null不影响结果。
     */
    @Test
    public void testBudgetAddAndTotals() {
        budget.add(item1);
        budget.add(item2);
        budget.add(null);
        assertEquals(1500.0, budget.totalCost(), 0.0001);
        assertEquals(1.4, budget.totalValue(), 0.0001);
        assertEquals(2, budget.getItems().size());
    }

    /**
     * 用例目的：验证通胀率边界收敛逻辑（<-0.5 与 >1.0）。
     * 预期结果：通胀率被夹紧到[-0.5, 1.0]范围内。
     */
    @Test
    public void testBudgetForecastCostClamping() {
        budget.add(new Budget.Item("A", 1000, 0.0, "GEN"));
        assertEquals(500.0, budget.forecastCost(-0.6), 0.0001); // clamp到-0.5
        assertEquals(2000.0, budget.forecastCost(1.2), 0.0001); // clamp到1.0
    }

    /**
     * 用例目的：验证备用金计算逻辑与最小值门槛。
     * 预期结果：当计算值<1000时返回1000；否则返回比例计算值。
     */
    @Test
    public void testBudgetRequiredReserve() {
        budget.add(new Budget.Item("Low", 5000, 0.0, "GEN"));
        assertEquals(1000.0, budget.requiredReserve(), 0.0001); // 5000*0.1=500 < 1000
        budget = new Budget();
        budget.add(new Budget.Item("High", 20000, 0.0, "GEN"));
        assertEquals(2000.0, budget.requiredReserve(), 0.0001);
    }

    /**
     * 用例目的：验证备用金比例设置的上下界。
     * 预期结果：小于0被设为0；大于0.5被设为0.5；正常值按设定生效。
     */
    @Test
    public void testBudgetSetReserveRatioBounds() {
        budget.add(new Budget.Item("X", 10000, 0.0, "GEN"));
        budget.setReserveRatio(-1.0);
        assertEquals(1000.0, budget.requiredReserve(), 0.0001); // 10000*0 = 0 -> 最小1000
        budget.setReserveRatio(0.6);
        assertEquals(5000.0, budget.requiredReserve(), 0.0001); // 10000*0.5
        budget.setReserveRatio(0.3);
        assertEquals(3000.0, budget.requiredReserve(), 0.0001);
    }

    /**
     * 用例目的：验证Item构造对负值与null类别处理。
     * 预期结果：负成本/价值被置0；类别默认GENERAL。
     */
    @Test
    public void testItemNegativeValuesAndNullCategory() {
        Budget.Item item = new Budget.Item("Z", -10.0, -5.0, null);
        assertEquals(0.0, item.getCost(), 0.0001);
        assertEquals(0.0, item.getValue(), 0.0001);
        assertEquals("GENERAL", item.getCategory());
    }

    // ======================== BudgetOptimizer ========================

    /**
     * 用例目的：验证BudgetOptimizer.optimize的异常分支与limit负值处理。
     * 预期结果：传入null预算抛出DomainException；负limit当作0处理。
     */
    @Test(expected = DomainException.class)
    public void testOptimizeNullBudgetThrows() {
        new BudgetOptimizer().optimize(null, 10);
    }

    /**
     * 用例目的：验证基础0-1背包选择与四舍五入成本影响。
     * 预期结果：在limit约束下选择最大价值组合；成本按Math.round处理。
     */
    @Test
    public void testOptimizeBasicSelection() {
        Budget b = new Budget();
        Budget.Item A = new Budget.Item("A", 5.0, 10.0, "G");
        Budget.Item B = new Budget.Item("B", 4.0, 9.0, "G");
        Budget.Item C = new Budget.Item("C", 2.0, 4.0, "G");
        b.add(A); b.add(B); b.add(C);
        BudgetOptimizer.Selection sel = new BudgetOptimizer().optimize(b, 7.0);
        assertEquals(14.0, sel.getTotalValue(), 0.0001);
        assertTrue(sel.getItems().contains(A));
        assertTrue(sel.getItems().contains(C));
        assertFalse(sel.getItems().contains(B));
    }

    /**
     * 用例目的：验证limit为0时的选择与成本四舍五入为0的条目被选中。
     * 预期结果：成本0.4被四舍五入为0，仍可被选中；总成本为原始0.4。
     */
    @Test
    public void testOptimizeZeroLimitWithZeroRoundedCostItem() {
        Budget b = new Budget();
        Budget.Item freeVal = new Budget.Item("FreeVal", 0.4, 100.0, "G");
        b.add(freeVal);
        BudgetOptimizer.Selection sel = new BudgetOptimizer().optimize(b, 0.0);
        assertEquals(100.0, sel.getTotalValue(), 0.0001);
        assertEquals(0.4, sel.getTotalCost(), 0.0001);
        assertEquals(1, sel.getItems().size());
        assertEquals("FreeVal", sel.getItems().get(0).getName());
    }

    /**
     * 用例目的：验证背包在物品成本恰等于容量时可被选择的边界。
     * 预期结果：选择成本等于limit的物品。
     */
    @Test
    public void testBudgetOptimizerPickExactCapacityItem() {
        Budget b = new Budget();
        Budget.Item exact = new Budget.Item("Exact", 7.0, 10.0, "G");
        Budget.Item small = new Budget.Item("Small", 6.0, 9.0, "G");
        b.add(exact);
        b.add(small);
        BudgetOptimizer.Selection sel = new BudgetOptimizer().optimize(b, 7.0);
        assertTrue(sel.getItems().contains(exact));
    }

    // ======================== Task ========================

    /**
     * 用例目的：验证Task构造与默认值、参数夹紧逻辑。
     * 预期结果：空名、负工期、空优先级被设置为""、0、MEDIUM；状态为PLANNED。
     */
    @Test
    public void testTaskConstructorDefaultsAndClamping() {
        Task t = new Task(null, -5, Task.Priority.CRITICAL);
        assertEquals("", t.getName());
        assertEquals(0, t.getDuration());
        assertEquals(Task.Priority.CRITICAL, t.getPriority());
        assertEquals(Task.Status.PLANNED, t.getStatus());
        t.setName(null);
        t.setDuration(-10);
        t.setPriority(null);
        assertEquals("", t.getName());
        assertEquals(0, t.getDuration());
        assertEquals(Task.Priority.MEDIUM, t.getPriority());
    }

    /**
     * 用例目的：验证技能需求设置的边界与合并逻辑（取最大等级）。
     * 预期结果：null/空技能被忽略；等级夹紧到[0,10]；重复设置取最大值。
     */
    @Test
    public void testTaskRequireSkillMergingAndBounds() {
        Task t = new Task("T", 5, Task.Priority.LOW);
        t.requireSkill(null, 5);
        t.requireSkill("", 5);
        assertTrue(t.getRequiredSkills().isEmpty());
        t.requireSkill("AI", -3);
        assertEquals(0, t.getRequiredSkills().get("AI").intValue());
        t.requireSkill("AI", 5);
        assertEquals(5, t.getRequiredSkills().get("AI").intValue());
        t.requireSkill("AI", 3);
        assertEquals(5, t.getRequiredSkills().get("AI").intValue());
        t.requireSkill("AI", 12);
        assertEquals(10, t.getRequiredSkills().get("AI").intValue());
    }

    /**
     * 用例目的：验证依赖添加、判定与去重逻辑。
     * 预期结果：null/自身依赖返回false；首次添加成功、重复添加失败；dependsOn对null返回false。
     */
    @Test
    public void testTaskDependencies() {
        Task a = new Task("A", 2, Task.Priority.MEDIUM);
        Task b = new Task("B", 3, Task.Priority.MEDIUM);
        assertFalse(a.addDependency(null));
        assertFalse(a.addDependency(a));
        assertTrue(a.addDependency(b));
        assertFalse(a.addDependency(b));
        assertTrue(a.dependsOn(b));
        assertFalse(a.dependsOn(null));
    }

    /**
     * 用例目的：验证进度与调度设置的边界夹紧逻辑。
     * 预期结果：进度被夹紧到[0,1]；负调度参数被夹紧为非负且满足顺序约束。
     */
    @Test
    public void testTaskProgressAndScheduleClamping() {
        Task t = new Task("T", 5, Task.Priority.HIGH);
        t.updateProgress(-0.3);
        assertEquals(0.0, t.getProgress(), 0.0001);
        t.updateProgress(1.2);
        assertEquals(1.0, t.getProgress(), 0.0001);
        t.setSchedule(-1, 0, -2, -3);
        assertEquals(0, t.getEst());
        assertEquals(0, t.getEft());
        assertEquals(0, t.getLst());
        assertEquals(0, t.getLft());
    }

    /**
     * 用例目的：验证任务状态转换与松弛时间计算。
     * 预期结果：start仅在PLANNED下生效；cancel/complete直接覆盖；slack非负正确。
     */
    @Test
    public void testTaskStatusTransitionsAndSlack() {
        Task t = new Task("T", 2, Task.Priority.LOW);
        assertEquals(Task.Status.PLANNED, t.getStatus());
        t.start();
        assertEquals(Task.Status.IN_PROGRESS, t.getStatus());
        t.start(); // 再次start不改变
        assertEquals(Task.Status.IN_PROGRESS, t.getStatus());
        t.cancel();
        assertEquals(Task.Status.CANCELLED, t.getStatus());
        t.complete();
        assertEquals(Task.Status.DONE, t.getStatus());
        t.setSchedule(0, 2, 5, 7);
        assertEquals(5, t.slack());
        t.setSchedule(3, 5, 1, 6);
        assertEquals(0, t.slack());
    }

    /**
     * 用例目的：验证任务分配的记录。
     * 预期结果：assignTo设置研究员ID可读取。
     */
    @Test
    public void testTaskAssignTo() {
        Task t = new Task("T", 2, Task.Priority.LOW);
        t.assignTo(123L);
        assertEquals(Long.valueOf(123L), t.getAssignedResearcherId());
    }

    // ======================== GraphUtils ========================

    /**
     * 用例目的：验证拓扑排序在无环情况下返回包含所有任务的序列。
     * 预期结果：返回列表大小等于任务数且包含所有任务。
     */
    @Test
    public void testTopologicalSortAcyclic() {
        Task c = new Task("C", 4, Task.Priority.HIGH);
        Task b = new Task("B", 3, Task.Priority.MEDIUM);
        Task a = new Task("A", 2, Task.Priority.LOW);
        b.addDependency(a);
        c.addDependency(b);
        List<Task> order = GraphUtils.topologicalSort(Arrays.asList(a, b, c));
        assertEquals(3, order.size());
        assertTrue(order.contains(a));
        assertTrue(order.contains(b));
        assertTrue(order.contains(c));
        assertFalse(GraphUtils.hasCycle(Arrays.asList(a, b, c)));
    }

    /**
     * 用例目的：验证拓扑排序在有环情况下抛出异常；hasCycle返回true。
     * 预期结果：抛出DomainException；hasCycle为true。
     */
    @Test
    public void testTopologicalSortCycleDetection() {
        Task a = new Task("A", 2, Task.Priority.MEDIUM);
        Task b = new Task("B", 3, Task.Priority.MEDIUM);
        a.addDependency(b);
        b.addDependency(a);
        try {
            GraphUtils.topologicalSort(Arrays.asList(a, b));
            fail("应当检测到环并抛出异常");
        } catch (DomainException e) {
            // 预期异常
        }
        assertTrue(GraphUtils.hasCycle(Arrays.asList(a, b)));
    }

    /**
     * 用例目的：验证最长路径工期计算。
     * 预期结果：依据当前实现，返回最大单任务工期（非传统路径和）。
     */
    @Test
    public void testLongestPathDurationBehavior() {
        Task a = new Task("A", 2, Task.Priority.LOW);
        Task b = new Task("B", 3, Task.Priority.MEDIUM);
        Task c = new Task("C", 4, Task.Priority.HIGH);
        b.addDependency(a);
        c.addDependency(b);
        Task d = new Task("D", 10, Task.Priority.LOW);
        int dur = GraphUtils.longestPathDuration(Arrays.asList(a, b, c, d));
        assertEquals(10, dur);
    }

    // ======================== Scheduler ========================

    /**
     * 用例目的：验证调度计算的时间字段（EST/EFT/LST/LFT）与松弛。
     * 预期结果：基于当前拓扑逻辑，计算结果与预期匹配。
     */
    @Test
    public void testSchedulerSchedule() {
        Task a = new Task("A", 2, Task.Priority.LOW);
        Task b = new Task("B", 3, Task.Priority.MEDIUM);
        Task c = new Task("C", 4, Task.Priority.HIGH);
        b.addDependency(a);
        c.addDependency(b);
        new Scheduler().schedule(Arrays.asList(a, b, c));
        // 依据实现推导的最终值：
        assertEquals(0, a.getEst());
        assertEquals(2, a.getEft());
        assertEquals(2, a.getLst());
        assertEquals(4, a.getLft());
        assertEquals(2, a.slack());

        assertEquals(0, b.getEst());
        assertEquals(3, b.getEft());
        assertEquals(0, b.getLst());
        assertEquals(3, b.getLft());
        assertEquals(0, b.slack());

        assertEquals(0, c.getEst());
        assertEquals(4, c.getEft());
        assertEquals(0, c.getLst());
        assertEquals(4, c.getLft());
        assertEquals(0, c.slack());
    }

    // ======================== Risk ========================

    /**
     * 用例目的：验证clamp范围、score与priority边界。
     * 预期结果：clamp至[0,1]；优先级阈值正确。
     */
    @Test
    public void testRiskClampScorePriority() {
        Risk r = new Risk("R", "C", -0.2, 1.2);
        assertEquals(0.0, r.getProbability(), 0.0001);
        assertEquals(1.0, r.getImpact(), 0.0001);
        assertEquals(0.0, new Risk("A", "C", 0, 0).score(), 0.0001);
        assertEquals(3, new Risk("H", "C", 1.0, 0.5).priority());
        assertEquals(2, new Risk("M", "C", 0.5, 0.5).priority());
        assertEquals(1, new Risk("L", "C", 0.1, 0.1).priority());
        assertEquals(0, new Risk("Z", "C", 0.0, 0.0).priority());
    }

    /**
     * 用例目的：验证compareTo排序规则：优先级降序，其次影响降序，其次名称升序。
     * 预期结果：比较结果符合规则。
     */
    @Test
    public void testRiskCompareTo() {
        Risk high = new Risk("A", "C", 1.0, 1.0);
        Risk mid1 = new Risk("B", "C", 0.5, 0.5);
        Risk mid2 = new Risk("C", "C", 0.5, 0.6);
        Risk low = new Risk("D", "C", 0.1, 0.1);
        // 高优先级应排在低优先级之前
        assertTrue(high.compareTo(low) < 0);
        // 同优先级按影响值降序
        assertTrue(mid2.compareTo(mid1) < 0);
        // 影响相同按名称升序
        Risk midSameImpact1 = new Risk("A", "C", 0.5, 0.5);
        Risk midSameImpact2 = new Risk("B", "C", 0.5, 0.5);
        assertTrue(midSameImpact1.compareTo(midSameImpact2) < 0);
    }

    // ======================== RiskAnalyzer ========================

    /**
     * 用例目的：验证simulate对空风险或迭代次数<=0的处理。
     * 预期结果：返回均为0的SimulationResult。
     */
    @Test
    public void testRiskAnalyzerSimulateEmptyOrZeroIterations() {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        RiskAnalyzer.SimulationResult r1 = analyzer.simulate(Collections.emptyList(), 100);
        assertEquals(0.0, r1.getMeanImpact(), 0.0001);
        assertEquals(0.0, r1.getP90Impact(), 0.0001);
        assertEquals(0.0, r1.getWorstCaseImpact(), 0.0001);
        RiskAnalyzer.SimulationResult r2 = analyzer.simulate(Arrays.asList(new Risk("A", "C", 0.1, 0.2)), 0);
        assertEquals(0.0, r2.getMeanImpact(), 0.0001);
    }

    /**
     * 用例目的：验证概率为1的确定性场景下统计值计算。
     * 预期结果：mean/p90/worst均为所有影响之和。
     */
    @Test
    public void testRiskAnalyzerDeterministicScenario() {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        List<Risk> risks = Arrays.asList(
                new Risk("R1", "C", 1.0, 0.4),
                new Risk("R2", "C", 1.0, 0.6)
        );
        RiskAnalyzer.SimulationResult r = analyzer.simulate(risks, 10);
        assertEquals(1.0, r.getMeanImpact(), 0.0001);
        assertEquals(1.0, r.getP90Impact(), 0.0001);
        assertEquals(1.0, r.getWorstCaseImpact(), 0.0001);
    }

    /**
     * 用例目的：验证随机数生成范围。
     * 预期结果：rnd返回值均在[0,1)内。
     */
    @Test
    public void testRiskAnalyzerRndRange() {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        for (int i = 0; i < 100; i++) {
            double v = analyzer.rnd();
            assertTrue(v >= 0.0 && v < 1.0);
        }
    }

    // ======================== Resource ========================

    /**
     * 用例目的：验证资源可用性检查的边界情况（null与时间顺序）。
     * 预期结果：start或end为null、不满足end>start时返回false。
     */
    @Test
    public void testResourceAvailabilityEdgeCases() {
        Resource r = new Resource("Res", "GEN");
        LocalDateTime now = LocalDateTime.now();
        assertFalse(r.isAvailable(null, now.plusHours(1)));
        assertFalse(r.isAvailable(now, null));
        assertFalse(r.isAvailable(now, now.minusHours(1)));
    }

    /**
     * 用例目的：验证end等于start时不可用的边界。
     * 预期结果：isAvailable返回false。
     */
    @Test
    public void testResourceEqualEndNotAfterStart() {
        Resource r = new Resource("Res", "GEN");
        LocalDateTime s = LocalDateTime.now();
        assertFalse(r.isAvailable(s, s));
    }

    /**
     * 用例目的：验证预订、取消与floorEntry冲突检测。
     * 预期结果：冲突时不可预订；取消后移除对应预订记录。
     */
    @Test
    public void testResourceBookCancelAndFloorConflict() {
        Resource r = new Resource("Res", "GEN");
        LocalDateTime t1 = LocalDateTime.now();
        LocalDateTime t2 = t1.plusHours(2);
        assertTrue(r.book(t1, t2));
        // floor条目结束时间未早于新开始 -> 冲突
        assertFalse(r.isAvailable(t1.plusHours(1), t1.plusHours(3)));
        assertFalse(r.book(t1.plusHours(1), t1.plusHours(3)));
        assertEquals(1, r.listBookings().size());
        r.cancel(null); // 无操作
        r.cancel(t1);
        assertEquals(0, r.listBookings().size());
    }

    /**
     * 用例目的：验证floorEntry非冲突场景（前一预订结束早于新开始）。
     * 预期结果：isAvailable返回true，且可成功预订。
     */
    @Test
    public void testResourceFloorNonConflictAvailable() {
        Resource r = new Resource("Res", "GEN");
        LocalDateTime start1 = LocalDateTime.now();
        LocalDateTime end1 = start1.plusHours(2);
        assertTrue(r.book(start1, end1));
        LocalDateTime start2 = end1.plusHours(1);
        LocalDateTime end2 = start2.plusHours(2);
        assertTrue(r.isAvailable(start2, end2));
        assertTrue(r.book(start2, end2));
        assertEquals(2, r.listBookings().size());
    }

    /**
     * 用例目的：验证conflicts方法当前实现总返回false。
     * 预期结果：任意输入均返回false。
     */
    @Test
    public void testResourceConflictsAlwaysFalse() {
        Resource r = new Resource("Res", "GEN");
        LocalDateTime a = LocalDateTime.now();
        assertFalse(r.conflicts(a, a.plusHours(1)));
    }

    // ======================== IdGenerator ========================

    /**
     * 用例目的：验证ID生成正数唯一性与字符串转换。
     * 预期结果：nextId返回正且不重复；nextIdStr可解析为非零无符号长整型。
     */
    @Test
    public void testIdGeneratorBasics() {
        long id1 = IdGenerator.nextId();
        long id2 = IdGenerator.nextId();
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertNotEquals(id1, id2);
        String s = IdGenerator.nextIdStr();
        long parsed = IdGenerator.fromString(s);
        assertTrue(parsed > 0);
    }

    /**
     * 用例目的：验证Item零值边界与预算通胀率边界等值。
     * 预期结果：零成本/价值保持为0；通胀率-0.5与1.0按边界输出。
     */
    @Test
    public void testBudgetZeroItemAndForecastBoundaryExact() {
        Budget b = new Budget();
        b.add(new Budget.Item("Z", 0.0, 0.0, "G"));
        assertEquals(0.0, b.totalCost(), 0.0001);
        assertEquals(0.0, b.totalValue(), 0.0001);
        b.add(new Budget.Item("X", 1000.0, 0.0, "G"));
        assertEquals(500.0, b.forecastCost(-0.5), 0.0001);
        assertEquals(2000.0, b.forecastCost(1.0), 0.0001);
    }

    /**
     * 用例目的：验证备用金计算在恰好1000边界的行为。
     * 预期结果：当reserve=1000时保持1000不被提升。
     */
    @Test
    public void testBudgetRequiredReserveExactThreshold() {
        Budget b = new Budget();
        b.add(new Budget.Item("X", 10000.0, 0.0, "G"));
        // 默认比例0.1 -> 恰好1000
        assertEquals(1000.0, b.requiredReserve(), 0.0001);
    }

    /**
     * 用例目的：验证字符串到ID的异常分支。
     * 预期结果：null、空串、非数字、"0"均抛出DomainException。
     */
    @Test
    public void testIdGeneratorFromStringErrors() {
        try { IdGenerator.fromString(null); fail(); } catch (DomainException e) {}
        try { IdGenerator.fromString(""); fail(); } catch (DomainException e) {}
        try { IdGenerator.fromString("abc"); fail(); } catch (DomainException e) {}
        try { IdGenerator.fromString("0"); fail(); } catch (DomainException e) {}
    }

    // ======================== Researcher ========================

    /**
     * 用例目的：验证技能维护、工时分配释放、评分更新与分配能力判断。
     * 预期结果：技能等级夹紧；分配/释放调整容量；评分按指数平滑更新；可分配判断基于容量和任务工期。
     */
    @Test
    public void testResearcherSkillsCapacityRatingAndAssign() {
        Researcher r = new Researcher("R", 10);
        r.addSkill(null, 5);
        r.addSkill("", 5);
        assertTrue(r.getSkills().isEmpty());
        r.addSkill("AI", 12);
        assertEquals(10, r.getSkillLevel("AI"));
        r.addSkill("AI", -3);
        assertEquals(0, r.getSkillLevel("AI"));
        r.addSkill("AI", 7);
        assertEquals(7, r.getSkillLevel("AI"));

        assertFalse(r.allocateHours(0));
        assertFalse(r.allocateHours(100));
        assertTrue(r.allocateHours(5));
        assertEquals(5, r.getCapacity());
        r.releaseHours(-1); // 无效
        assertEquals(5, r.getCapacity());
        r.releaseHours(10);
        assertEquals(15, r.getCapacity());

        r.updateRating(-10);
        assertTrue(r.getRating() >= 0 && r.getRating() <= 100);
        r.updateRating(200);
        assertTrue(r.getRating() >= 0 && r.getRating() <= 100);

        Task tSmall = new Task("T", 5, Task.Priority.LOW);
        assertTrue(r.canAssign(tSmall));
        assertTrue(r.assignTask(tSmall));
        assertEquals(10, r.getCapacity());
    }

    /**
     * 用例目的：验证completeTask释放工时并更新评分。
     * 预期结果：返回true；容量增加；评分更新。
     */
    @Test
    public void testResearcherCompleteTask() {
        Researcher r = new Researcher("R", 8);
        Task t = new Task("T", 5, Task.Priority.LOW);
        assertTrue(r.assignTask(t));
        int capAfterAssign = r.getCapacity();
        assertTrue(r.completeTask(t, 80.0));
        assertTrue(r.getCapacity() > capAfterAssign);
        assertTrue(r.getRating() > 0);
    }

    /**
     * 用例目的：验证Researcher在容量恰等于任务工期时可分配。
     * 预期结果：canAssign返回true。
     */
    @Test
    public void testResearcherCapacityExactlyEqual() {
        Researcher r = new Researcher("R", 5);
        Task t = new Task("T", 5, Task.Priority.LOW);
        assertTrue(r.canAssign(t));
    }

    /**
     * 用例目的：验证释放工时触达40上限的边界。
     * 预期结果：容量不超过40。
     */
    @Test
    public void testResearcherReleaseHoursClampTo40() {
        Researcher r = new Researcher("R", 39);
        r.releaseHours(5);
        assertEquals(40, r.getCapacity());
    }

    // ======================== MatchingEngine ========================

    /**
     * 用例目的：验证评分公式计算正确。
     * 预期结果：score= min(capacity,duration)*0.1 + rating*0.05。
     */
    @Test
    public void testMatchingEngineScore() {
        Researcher r = new Researcher("R", 10);
        Task t = new Task("T", 5, Task.Priority.LOW);
        r.updateRating(50);
        double s = new MatchingEngine().score(r, t);
        assertEquals(0.5 + 0.75, s, 0.0001);
    }

    /**
     * 用例目的：验证匹配分配：按任务优先级/工期排序，研究员不重复分配。
     * 预期结果：高优先级任务优先获得分配；每个研究员最多一个任务。
     */
    @Test
    public void testMatchingEngineMatchAssignments() {
        Researcher r1 = new Researcher("R1", 5);
        Researcher r2 = new Researcher("R2", 10);
        r1.updateRating(10);
        r2.updateRating(90);
        Task t1 = new Task("T1", 5, Task.Priority.LOW);
        Task t2 = new Task("T2", 5, Task.Priority.CRITICAL);
        List<MatchingEngine.Assignment> res = new MatchingEngine().match(Arrays.asList(r1, r2), Arrays.asList(t1, t2));
        assertEquals(2, res.size());
        // 高优先级任务应在结果中，且被能力较强（评分高）研究员获取
        MatchingEngine.Assignment aCritical = res.stream().filter(a -> a.getTask().getPriority() == Task.Priority.CRITICAL).findFirst().orElse(null);
        assertNotNull(aCritical);
        // CRITICAL任务的分配应使用未被占用的研究员
        assertNotEquals(res.get(0).getResearcher().getId(), aCritical.getResearcher().getId());
        // 研究员不重复分配
        assertNotEquals(res.get(0).getResearcher().getId(), res.get(1).getResearcher().getId());
        // 分配后任务记录了研究员ID
        assertNotNull(t1.getAssignedResearcherId());
        assertNotNull(t2.getAssignedResearcherId());
    }

    /**
     * 用例目的：验证空列表输入处理。
     * 预期结果：返回空分配列表。
     */
    @Test
    public void testMatchingEngineNullLists() {
        List<MatchingEngine.Assignment> res = new MatchingEngine().match(null, null);
        assertTrue(res.isEmpty());
    }

    /**
     * 用例目的：验证匹配算法在两研究员评分相等时的tie-break策略（选择列表顺序的第一个）。
     * 预期结果：分配的研究员为列表中的第一个。
     */
    @Test
    public void testMatchingEngineTieBreakSelectFirst() {
        Researcher r1 = new Researcher("R1", 5);
        Researcher r2 = new Researcher("R2", 5);
        r1.updateRating(50);
        r2.updateRating(50);
        Task t = new Task("T", 5, Task.Priority.MEDIUM);
        List<MatchingEngine.Assignment> res = new MatchingEngine().match(Arrays.asList(r1, r2), Arrays.asList(t));
        assertEquals(1, res.size());
        assertEquals(r1.getId(), res.get(0).getResearcher().getId());
    }

    // ======================== Project ========================

    /**
     * 用例目的：验证项目任务/研究员添加、状态统计与关键路径工期。
     * 预期结果：空名被置为""；statusCounts包含所有状态；criticalPathDuration符合当前实现行为。
     */
    @Test
    public void testProjectBasicsAndStatusCounts() {
        Project p = new Project(null);
        assertEquals("", p.getName());
        Task t1 = p.addTask(new Task("T1", 2, Task.Priority.MEDIUM));
        Task t2 = p.addTask(new Task("T2", 5, Task.Priority.LOW));
        assertNotNull(t1);
        assertNotNull(t2);
        p.addTask(null);
        Researcher r = p.addResearcher(new Researcher("R", 8));
        assertNotNull(r);
        p.addResearcher(null);
        // 状态统计
        t1.start(); t1.complete();
        t2.cancel();
        Map<Task.Status, Long> counts = p.statusCounts();
        assertEquals(Long.valueOf(1), counts.get(Task.Status.DONE));
        assertEquals(Long.valueOf(1), counts.get(Task.Status.CANCELLED));
        assertEquals(Long.valueOf(0), counts.get(Task.Status.PLANNED));
        assertEquals(Long.valueOf(0), counts.get(Task.Status.IN_PROGRESS));
        assertEquals(Long.valueOf(0), counts.get(Task.Status.BLOCKED));
        // 关键路径工期：依据实现为最大单任务工期
        assertEquals(5, p.criticalPathDuration());
    }

    /**
     * 用例目的：验证项目分配与风险分析。
     * 预期结果：分配记录数量合理且每研究员最多一个；风险分析结果合理（非负）。
     */
    @Test
    public void testProjectAssignmentsAndRiskAnalysis() {
        Project p = new Project("P");
        Researcher r1 = p.addResearcher(new Researcher("R1", 5));
        Researcher r2 = p.addResearcher(new Researcher("R2", 10));
        p.addTask(new Task("T1", 5, Task.Priority.CRITICAL));
        p.addTask(new Task("T2", 3, Task.Priority.MEDIUM));
        List<MatchingEngine.Assignment> assigns = p.planAssignments();
        assertEquals(2, assigns.size());
        assertNotEquals(assigns.get(0).getResearcher().getId(), assigns.get(1).getResearcher().getId());
        // 风险分析
        p.addRisk(new Risk("R", "C", 1.0, 0.5));
        RiskAnalyzer.SimulationResult r = p.analyzeRisk(10);
        assertTrue(r.getMeanImpact() >= 0);
        assertTrue(r.getWorstCaseImpact() >= r.getMeanImpact());
    }

    /**
     * 用例目的：验证setBudget对null的处理与对象保持。
     * 预期结果：传入null不改变预算对象，传入非null正常替换。
     */
    @Test
    public void testProjectSetBudgetNullNoChange() {
        Project p = new Project("P");
        Budget original = p.getBudget();
        p.setBudget(null);
        assertSame(original, p.getBudget());
        Budget b2 = new Budget();
        p.setBudget(b2);
        assertSame(b2, p.getBudget());
    }

    /**
     * 用例目的：验证addTask与addResearcher在输入为null时返回null。
     * 预期结果：返回null且不影响集合。
     */
    @Test
    public void testProjectAddNullReturnsNull() {
        Project p = new Project("P");
        assertNull(p.addTask(null));
        assertNull(p.addResearcher(null));
        assertTrue(p.getTasks().isEmpty());
        assertTrue(p.getResearchers().isEmpty());
    }

    /**
     * 用例目的：验证研究员完成任务的空输入分支。
     * 预期结果：completeTask(null, x)返回false。
     */
    @Test
    public void testResearcherCompleteTaskNullReturnsFalse() {
        Researcher r = new Researcher("R", 8);
        assertFalse(r.completeTask(null, 50.0));
    }

    /**
     * 用例目的：验证匹配算法每研究员最多一个任务，第三个任务不分配。
     * 预期结果：仅分配两个任务；第三个任务未被分配。
     */
    @Test
    public void testMatchingEngineAtMostOneTaskPerResearcher() {
        Researcher r1 = new Researcher("R1", 5);
        Researcher r2 = new Researcher("R2", 5);
        Task t1 = new Task("T1", 5, Task.Priority.CRITICAL);
        Task t2 = new Task("T2", 5, Task.Priority.HIGH);
        Task t3 = new Task("T3", 5, Task.Priority.MEDIUM);
        List<MatchingEngine.Assignment> res = new MatchingEngine().match(Arrays.asList(r1, r2), Arrays.asList(t1, t2, t3));
        assertEquals(2, res.size());
        assertNotEquals(res.get(0).getResearcher().getId(), res.get(1).getResearcher().getId());
    }

    // ======================== ReportGenerator ========================

    /**
     * 用例目的：验证报告生成的空项目处理。
     * 预期结果：传入null返回空字符串。
     */
    @Test
    public void testReportGeneratorNullProject() {
        assertEquals("", new ReportGenerator().generate(null));
    }

    /**
     * 用例目的：验证报告内容包含关键字段。
     * 预期结果：包含Project/Status/CriticalPath/Budget与Risk相关字段。
     */
    @Test
    public void testReportGeneratorContent() {
        Project p = new Project("Proj");
        p.addTask(new Task("T1", 2, Task.Priority.MEDIUM));
        p.addTask(new Task("T2", 5, Task.Priority.LOW));
        p.getBudget().add(new Budget.Item("I1", 1000, 0.5, "G"));
        String report = new ReportGenerator().generate(p);
        assertTrue(report.contains("Project:"));
        assertTrue(report.contains("Status PLANNED:"));
        assertTrue(report.contains("CriticalPath:"));
        assertTrue(report.contains("BudgetCost:"));
        assertTrue(report.contains("BudgetValue:"));
        assertTrue(report.contains("RiskMean:"));
        assertTrue(report.contains("RiskP90:"));
        assertTrue(report.contains("RiskWorst:"));
    }
}
