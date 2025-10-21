package net.mooctest;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;

/**
 * 电梯系统综合测试类
 * 包含电梯基础功能、调度策略、监控维护、通知日志等测试
 */
public class ElevatorManagerTest {
    
    private Elevator elevator;
    private Scheduler scheduler;
    private SecurityMonitor securityMonitor;
    private MaintenanceManager maintenanceManager;
    private AnalyticsEngine analyticsEngine;
    private NotificationService notificationService;
    private LogManager logManager;
    private ThreadPoolManager threadPoolManager;
    
    /**
     * 测试前的准备工作
     * 初始化所有组件
     */
    @Before
    public void setUp() {
        List<Elevator> elevators = new ArrayList<>();
        scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        elevator = new Elevator(1, scheduler);
        elevators.add(elevator);
        
        securityMonitor = SecurityMonitor.getInstance();
        maintenanceManager = MaintenanceManager.getInstance();
        analyticsEngine = AnalyticsEngine.getInstance();
        notificationService = NotificationService.getInstance();
        logManager = LogManager.getInstance();
        threadPoolManager = ThreadPoolManager.getInstance();
    }
    
    // ============ 电梯基础功能测试 ============
    
    /**
     * 测试电梯基础属性和初始化
     * 验证电梯ID、楼层、状态等基本属性
     */
    @Test(timeout = 4000)
    public void testElevatorBasicProperties() {
        assertEquals("电梯ID应正确设置", 1, elevator.getId());
        assertEquals("电梯初始楼层应为1", 1, elevator.getCurrentFloor());
        assertEquals("电梯初始状态应为IDLE", ElevatorStatus.IDLE, elevator.getStatus());
        assertTrue("电梯初始应为空", elevator.isEmpty());
    }
    
    /**
     * 测试电梯移动功能
     * 验证上下移动和停止
     */
    @Test(timeout = 4000)
    public void testElevatorMovement() {
        // 测试上行
        elevator.setCurrentFloor(1);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevator.setTargetFloor(5);
        elevator.setStatus(ElevatorStatus.MOVING_UP);
        elevator.move();
        assertTrue("电梯应向上移动", elevator.getCurrentFloor() > 1);
        assertEquals("电梯状态应为上行", ElevatorStatus.MOVING_UP, elevator.getStatus());
        
        // 测试下行
        elevator.setCurrentFloor(5);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevator.setTargetFloor(2);
        elevator.setStatus(ElevatorStatus.MOVING_DOWN);
        elevator.move();
        assertTrue("电梯应向下移动", elevator.getCurrentFloor() < 5);
        assertEquals("电梯状态应为下行", ElevatorStatus.MOVING_DOWN, elevator.getStatus());
    }
    
    /**
     * 测试电梯状态转换
     * 验证各种状态间的转换
     */
    @Test(timeout = 4000)
    public void testElevatorStatusTransition() {
        elevator.setStatus(ElevatorStatus.MOVING_UP);
        assertEquals("状态应变为上行", ElevatorStatus.MOVING_UP, elevator.getStatus());
        
        elevator.setStatus(ElevatorStatus.DOOR_OPEN);
        assertEquals("状态应变为开门", ElevatorStatus.DOOR_OPEN, elevator.getStatus());
    }
    
    // ============ 调度策略测试 ============
    
    /**
     * 测试最近电梯策略
     * 验证选择最近的合适电梯
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategy() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        // 创建多部电梯
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        
        // 设置电梯状态和位置
        elevator1.setCurrentFloor(2);
        elevator1.setStatus(ElevatorStatus.IDLE);
        elevator2.setCurrentFloor(5);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        // 测试选择最近电梯（上行请求）
        Elevator selected = strategy.selectElevator(elevators, 3, Direction.UP);
        assertNotNull("应选择到合适的电梯", selected);
        assertEquals("应选择最近的电梯", elevator1.getId(), selected.getId());
        
        // 测试电梯不可用情况
        elevator1.setStatus(ElevatorStatus.FAULT);
        selected = strategy.selectElevator(elevators, 3, Direction.UP);
        assertNotNull("应选择其他可用电梯", selected);
        assertEquals("应选择电梯2", elevator2.getId(), selected.getId());
    }
    
    /**
     * 测试节能策略
     * 验证选择节能的电梯
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategy() {
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        // 创建电梯并设置状态
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        Elevator elevator3 = new Elevator(3, scheduler);
        
        // 设置电梯状态和位置
        elevator1.setStatus(ElevatorStatus.IDLE);
        elevator1.setCurrentFloor(2);
        
        elevator2.setStatus(ElevatorStatus.MOVING_UP);
        elevator2.setCurrentFloor(4);
        
        elevator3.setStatus(ElevatorStatus.MOVING_UP);
        elevator3.setCurrentFloor(7);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        elevators.add(elevator3);
        
        // 测试选择空闲电梯
        Elevator selected = strategy.selectElevator(elevators, 3, Direction.UP);
        assertNotNull("应选择到合适的电梯", selected);
        assertEquals("应优先选择空闲电梯", elevator1.getId(), selected.getId());
        
        // 测试无空闲电梯时选择最近的同向电梯
        elevator1.setStatus(ElevatorStatus.FAULT);
        selected = strategy.selectElevator(elevators, 3, Direction.UP);
        assertNotNull("应选择到合适的电梯", selected);
        assertEquals("应选择较近的同向电梯", elevator2.getId(), selected.getId());
        
        // 测试超出距离限制
        selected = strategy.selectElevator(elevators, 10, Direction.UP);
        assertNull("超出距离限制应返回null", selected);
    }
    
    // ============ 监控和维护测试 ============
    
    /**
     * 测试安全监控
     * 验证紧急事件处理
     */
    @Test(timeout = 4000)
    public void testSecurityMonitoring() {
        // 创建紧急事件
        EventBus.Event emergencyEvent = new EventBus.Event(EventType.EMERGENCY, "Fire alarm on floor 5");
        securityMonitor.onEvent(emergencyEvent);
        
        // 验证事件处理
        SecurityMonitor.SecurityEvent event = securityMonitor.getSecurityEvents().get(0);
        assertNotNull("应创建安全事件", event);
        assertEquals("事件描述应匹配", "Emergency situation", event.getDescription());
    }
    
    /**
     * 测试维护管理
     * 验证维护任务处理
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagement() {
        // 调度维护任务
        maintenanceManager.scheduleMaintenance(elevator);
        
        // 等待任务处理
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证维护记录
        List<MaintenanceManager.MaintenanceRecord> records = maintenanceManager.getMaintenanceRecords();
        assertFalse("应有维护记录", records.isEmpty());
    }
    
    // ============ 通知和日志测试 ============
    
    /**
     * 测试通知服务
     * 验证不同类型通知的发送
     */
    @Test(timeout = 4000)
    public void testNotificationService() {
        // 创建紧急通知
        NotificationService.Notification emergency = new NotificationService.Notification(
            NotificationService.NotificationType.EMERGENCY,
            "Fire alarm on floor 5",
            "Security Team"
        );
        
        // 发送通知
        notificationService.sendNotification(emergency);
        
        // 创建维护通知
        NotificationService.Notification maintenance = new NotificationService.Notification(
            NotificationService.NotificationType.MAINTENANCE,
            "Scheduled maintenance for Elevator #1",
            "Maintenance Team"
        );
        
        // 发送通知
        notificationService.sendNotification(maintenance);
    }
    
    /**
     * 测试日志管理
     * 验证日志记录和查询
     */
    @Test(timeout = 4000)
    public void testLogManagement() {
        // 记录电梯事件
        logManager.recordElevatorEvent(1, "Door opened");
        
        // 记录调度事件
        logManager.recordSchedulerEvent("Assigned elevator #1 to floor 5");
        
        // 查询日志
        List<LogManager.SystemLog> logs = logManager.queryLogs(
            "Elevator",
            System.currentTimeMillis() - 5000,
            System.currentTimeMillis()
        );
        
        assertFalse("应查询到日志记录", logs.isEmpty());
        assertEquals("日志来源应正确", "Elevator", logs.get(0).getSource());
    }
    
    /**
     * 测试线程池管理
     * 验证任务提交和执行
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagement() throws Exception {
        // 创建测试任务
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);
        
        Runnable task = () -> {
            taskExecuted.set(true);
            latch.countDown();
        };
        
        // 提交任务
        threadPoolManager.submitTask(task);
        
        // 等待任务完成
        assertTrue("任务应在超时前完成", latch.await(2, TimeUnit.SECONDS));
        assertTrue("任务应被执行", taskExecuted.get());
    }
    
    // ============ 并发和边界测试 ============
    
    /**
     * 测试并发事件处理
     * 验证多线程环境下的系统行为
     */
    @Test(timeout = 4000)
    public void testConcurrentEventHandling() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        
        // 多线程发送事件
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int floor = i + 1;
            new Thread(() -> {
                try {
                    barrier.await();
                    EventBus.Event event = new EventBus.Event(
                        EventType.EMERGENCY,
                        "Emergency on floor " + floor
                    );
                    securityMonitor.onEvent(event);
                    latch.countDown();
                } catch (Exception e) {
                    fail("并发测试异常: " + e.getMessage());
                }
            }).start();
        }
        
        latch.await(2, TimeUnit.SECONDS);
        assertFalse("应处理所有并发事件", securityMonitor.getSecurityEvents().isEmpty());
    }
    
    /**
     * 测试边界条件
     * 验证极端情况下的系统行为
     */
    @Test(timeout = 4000)
    public void testBoundaryConditions() {
        // 测试电梯极限楼层
        elevator.setCurrentFloor(1);
        elevator.setTargetFloor(20);
        elevator.move();
        assertTrue("电梯应能到达最高楼", elevator.getCurrentFloor() <= 20);
        
        // 测试最大乘客数
        for (int i = 0; i < 10; i++) {
            elevator.addPassenger();
        }
        assertFalse("电梯不应超载", elevator.addPassenger());
        
        // 测试零乘客数量
        analyticsEngine.updateFloorPassengerCount(1, 0);
        assertFalse("零乘客时不应为高峰时段", analyticsEngine.isPeakHours());
    }

    /**
     * 测试电梯能耗计算
     * 验证不同情况下的能耗统计
     */
    @Test(timeout = 4000)
    public void testEnergyConsumption() {
        // 测试空载能耗
        elevator.setCurrentFloor(1);
        elevator.setTargetFloor(10);
        elevator.move();
        double emptyEnergy = elevator.getEnergyConsumption();
        
        // 测试满载能耗
        elevator.setCurrentFloor(10);
        for (int i = 0; i < 8; i++) {
            elevator.addPassenger();
        }
        elevator.setTargetFloor(1);
        elevator.move();
        double fullEnergy = elevator.getEnergyConsumption();
        
        assertTrue("满载能耗应大于空载能耗", fullEnergy > emptyEnergy);
    }

    /**
     * 测试电梯调度优先级
     * 验证不同请求的处理顺序
     */
    @Test(timeout = 4000)
    public void testSchedulingPriority() {
        // 创建多个请求
        scheduler.addRequest(5, Direction.UP);
        scheduler.addRequest(3, Direction.UP);
        scheduler.addRequest(7, Direction.DOWN);
        
        // 验证请求处理顺序
        assertEquals("应优先处理同向且较近的请求", 3, 
            scheduler.getNextRequest(elevator).getTargetFloor());
    }

    /**
     * 测试电梯门控制
     * 验证开关门操作和安全机制
     */
    @Test(timeout = 4000)
    public void testDoorControl() {
        // 测试正常开关门
        elevator.setStatus(ElevatorStatus.IDLE);
        assertTrue("静止状态应能开门", elevator.openDoor());
        assertEquals(ElevatorStatus.DOOR_OPEN, elevator.getStatus());
        
        assertTrue("开门状态应能关门", elevator.closeDoor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        
        // 测试运行时开门
        elevator.setStatus(ElevatorStatus.MOVING_UP);
        assertFalse("运行状态不应能开门", elevator.openDoor());
    }

    /**
     * 测试紧急制动系统
     * 验证紧急情况下的响应
     */
    @Test(timeout = 4000)
    public void testEmergencyBraking() {
        // 设置电梯运行状态
        elevator.setStatus(ElevatorStatus.MOVING_UP);
        elevator.setCurrentFloor(5);
        elevator.setTargetFloor(10);
        
        // 触发紧急制动
        elevator.emergencyStop();
        
        assertEquals("应立即停止", ElevatorStatus.EMERGENCY_STOP, elevator.getStatus());
        assertEquals("应保持在当前楼层", 5, elevator.getCurrentFloor());
    }

    /**
     * 测试重量传感器
     * 验证载重检测功能
     */
    @Test(timeout = 4000)
    public void testWeightSensor() {
        // 测试正常载重
        for (int i = 0; i < 5; i++) {
            assertTrue("正常载重范围内应能添加乘客", elevator.addPassenger());
        }
        
        // 测试临界载重
        for (int i = 0; i < 5; i++) {
            elevator.addPassenger();
        }
        assertFalse("超过载重限制应拒绝添加乘客", elevator.addPassenger());
        
        // 测试减重
        elevator.removePassenger();
        assertTrue("减重后应能添加乘客", elevator.addPassenger());
    }

    /**
     * 测试楼层显示系统
     * 验证楼层信息显示
     */
    @Test(timeout = 4000)
    public void testFloorDisplay() {
        // 测试楼层更新
        elevator.setCurrentFloor(1);
        assertEquals("显示楼层应与实际楼层一致", 1, elevator.getCurrentFloor());
        
        elevator.setTargetFloor(5);
        elevator.move();
        assertTrue("显示楼层应随运行更新", elevator.getCurrentFloor() > 1);
        
        // 测试方向指示
        assertEquals("上行状态应正确显示", ElevatorStatus.MOVING_UP, elevator.getStatus());
    }

    /**
     * 测试电梯性能监控
     * 验证性能数据收集和分析
     */
    @Test(timeout = 4000)
    public void testPerformanceMonitoring() {
        // 记录运行数据
        for (int i = 0; i < 5; i++) {
            elevator.setCurrentFloor(1);
            elevator.setTargetFloor(10);
            elevator.move();
            analyticsEngine.processStatusReport(elevator.getId(), "Normal operation");
        }
        
        // 生成性能报告
        AnalyticsEngine.Report report = analyticsEngine.generatePerformanceReport();
        assertNotNull("应生成性能报告", report);
        assertTrue("报告时间应合理", report.getGeneratedTime() <= System.currentTimeMillis());
    }

    /**
     * 测试系统恢复机制
     * 验证故障后的恢复流程
     */
    @Test(timeout = 4000)
    public void testSystemRecovery() {
        // 模拟故障
        elevator.setStatus(ElevatorStatus.FAULT);
        maintenanceManager.scheduleMaintenance(elevator);
        
        // 执行维护
        MaintenanceManager.MaintenanceTask task = new MaintenanceManager.MaintenanceTask(
            elevator.getId(),
            System.currentTimeMillis(),
            "System recovery test"
        );
        maintenanceManager.performMaintenance(task);
        
        // 验证恢复
        elevator.setStatus(ElevatorStatus.IDLE);
        assertEquals("系统应恢复到空闲状态", ElevatorStatus.IDLE, elevator.getStatus());
        assertTrue("恢复后应能正常运行", elevator.isOperational());
    }
}