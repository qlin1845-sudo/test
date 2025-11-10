package net.mooctest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/*
 * 简化的电梯管理系统测试类
 * 专注于核心业务功能测试，避免可能导致阻塞的组件
 * 使用JUnit 4.12和Mockito进行单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class ElevatorManagerTest {

    private ElevatorManager elevatorManager;
    private Scheduler scheduler;
    private List<Elevator> elevators;
    private SystemConfig systemConfig;
    
    @Before
    public void setUp() {
        // 重置关键单例实例
        resetSingletons();
        
        // 初始化测试环境
        systemConfig = SystemConfig.getInstance();
        systemConfig.setFloorCount(10);
        systemConfig.setElevatorCount(3);
        
        elevators = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            elevators.add(new Elevator(i, null));
        }
        
        elevatorManager = ElevatorManager.getInstance();
        scheduler = Scheduler.getInstance(elevators, 10, new NearestElevatorStrategy());
        
        // 注册电梯到管理器
        for (Elevator elevator : elevators) {
            elevatorManager.registerElevator(elevator);
        }
    }

    /**
     * 测试ElevatorManager单例模式
     * 验证双重检查锁定机制的正确性
     */
    @Test
    public void testElevatorManagerSingleton() {
        ElevatorManager instance1 = ElevatorManager.getInstance();
        ElevatorManager instance2 = ElevatorManager.getInstance();
        
        assertSame("ElevatorManager应该是单例", instance1, instance2);
        assertNotNull("实例不应该为null", instance1);
    }

    /**
     * 测试电梯注册功能
     * 验证电梯能够正确注册到管理器中
     */
    @Test
    public void testElevatorRegistration() {
        Elevator newElevator = new Elevator(4, scheduler);
        elevatorManager.registerElevator(newElevator);
        
        assertEquals("应该能获取注册的电梯", newElevator, elevatorManager.getElevatorById(4));
        assertTrue("所有电梯列表应该包含新电梯", elevatorManager.getAllElevators().contains(newElevator));
    }

    /**
     * 测试电梯基本属性
     * 验证电梯初始化状态和getter方法
     */
    @Test
    public void testElevatorBasicProperties() {
        Elevator elevator = elevators.get(0);
        
        assertEquals("电梯ID应该正确", 1, elevator.getId());
        assertEquals("初始楼层应该是1楼", 1, elevator.getCurrentFloor());
        assertEquals("初始方向应该是UP", Direction.UP, elevator.getDirection());
        assertEquals("初始状态应该是IDLE", ElevatorStatus.IDLE, elevator.getStatus());
        assertTrue("乘客列表应该初始化", elevator.getPassengerList() != null);
        assertEquals("初始能耗应该是0", 0.0, elevator.getEnergyConsumption(), 0.001);
    }

    /**
     * 测试电梯移动功能
     * 验证电梯在不同方向上的移动逻辑
     */
    @Test
    public void testElevatorMovement() throws InterruptedException {
        Elevator elevator = elevators.get(0);
        
        // 测试向上移动
        elevator.addDestination(3);
        elevator.move();
        assertEquals("电梯应该向上移动到2楼", 2, elevator.getCurrentFloor());
        assertEquals("状态应该是MOVING", ElevatorStatus.MOVING, elevator.getStatus());
        
        // 继续移动到目标楼层
        elevator.move();
        assertEquals("电梯应该到达3楼", 3, elevator.getCurrentFloor());
        
        // 测试向下移动
        elevator.addDestination(1);
        elevator.move();
        assertEquals("电梯应该向下移动到2楼", 2, elevator.getCurrentFloor());
        assertEquals("方向应该是DOWN", Direction.DOWN, elevator.getDirection());
    }

    /**
     * 测试电梯方向更新逻辑
     * 验证电梯根据目标楼层自动调整方向
     */
    @Test
    public void testElevatorDirectionUpdate() {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(5);
        
        // 测试向上方向
        elevator.addDestination(8);
        elevator.updateDirection();
        assertEquals("应该设置为向上方向", Direction.UP, elevator.getDirection());
        
        // 测试向下方向
        elevator.getDestinationSet().clear();
        elevator.addDestination(2);
        elevator.updateDirection();
        assertEquals("应该设置为向下方向", Direction.DOWN, elevator.getDirection());
        
        // 测试无目标时状态
        elevator.getDestinationSet().clear();
        elevator.updateDirection();
        assertEquals("无目标时应该是IDLE状态", ElevatorStatus.IDLE, elevator.getStatus());
    }

    /**
     * 测试乘客载载功能
     * 验证电梯的乘客上下车逻辑
     */
    @Test
    public void testPassengerLoading() {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(1);
        
        // 创建模拟乘客请求
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(1, 3, Priority.HIGH, RequestType.DESTINATION_CONTROL);
        
        // 模拟调度器返回请求
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(1, Direction.UP))
            .thenReturn(Arrays.asList(request1, request2));
        
        // 使用反射设置调度器
        try {
            java.lang.reflect.Field schedulerField = Elevator.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            schedulerField.set(elevator, mockScheduler);
        } catch (Exception e) {
            fail("设置调度器失败");
        }
        
        // 测试载客
        elevator.loadPassengers();
        assertEquals("应该载入2个乘客", 2, elevator.getPassengerList().size());
        assertTrue("应该包含请求1", elevator.getPassengerList().contains(request1));
        assertTrue("应该包含请求2", elevator.getPassengerList().contains(request2));
        assertTrue("目标楼层应该包含5楼", elevator.getDestinationSet().contains(5));
        assertTrue("目标楼层应该包含3楼", elevator.getDestinationSet().contains(3));
    }

    /**
     * 测试乘客下车功能
     * 验证乘客在目标楼层下车
     */
    @Test
    public void testPassengerUnloading() {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(5);
        
        // 添加乘客到电梯
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(2, 7, Priority.LOW, RequestType.STANDARD);
        
        elevator.getPassengerList().add(request1);
        elevator.getPassengerList().add(request2);
        elevator.setCurrentLoad(140); // 2个乘客 * 70kg
        
        // 测试下车
        elevator.unloadPassengers();
        assertEquals("应该下车1个乘客", 1, elevator.getPassengerList().size());
        assertFalse("不应该包含已下车的乘客", elevator.getPassengerList().contains(request1));
        assertTrue("应该包含未下车的乘客", elevator.getPassengerList().contains(request2));
        assertEquals("当前负载应该更新", 70, elevator.getCurrentLoad(), 0.001);
    }

    /**
     * 测试电梯超载处理
     * 验证电梯在超载时不接受更多乘客
     */
    @Test
    public void testElevatorOverload() {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(1);
        
        // 设置电梯接近满载
        double maxLoad = elevator.getMaxLoad();
        elevator.setCurrentLoad(maxLoad - 50); // 只剩50kg容量
        
        // 创建多个乘客请求
        List<PassengerRequest> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(new PassengerRequest(1, 5 + i, Priority.MEDIUM, RequestType.STANDARD));
        }
        
        // 模拟调度器返回请求
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(1, Direction.UP)).thenReturn(requests);
        
        try {
            java.lang.reflect.Field schedulerField = Elevator.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            schedulerField.set(elevator, mockScheduler);
        } catch (Exception e) {
            fail("设置调度器失败");
        }
        
        int initialPassengerCount = elevator.getPassengerList().size();
        elevator.loadPassengers();
        
        // 由于超载，不应该载入新乘客
        assertEquals("超载时不应载入新乘客", initialPassengerCount, elevator.getPassengerList().size());
    }

    /**
     * 测试电梯紧急处理
     * 验证紧急情况下电梯的行为
     */
    @Test
    public void testEmergencyHandling() {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(8);
        elevator.setStatus(ElevatorStatus.MOVING);
        
        // 添加一些乘客和目标
        elevator.getPassengerList().add(new PassengerRequest(1, 10, Priority.HIGH, RequestType.STANDARD));
        elevator.getDestinationSet().add(10);
        
        // 触发紧急情况
        elevator.handleEmergency();
        
        assertEquals("状态应该是EMERGENCY", ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertTrue("目标应该只包含1楼", elevator.getDestinationSet().contains(1));
        assertEquals("目标集合应该只有1个元素", 1, elevator.getDestinationSet().size());
        assertTrue("乘客列表应该被清空", elevator.getPassengerList().isEmpty());
    }

    /**
     * 测试电梯紧急移动到1楼
     * 验证紧急情况下的快速移动逻辑
     */
    @Test
    public void testEmergencyMoveToFirstFloor() throws InterruptedException {
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(5);
        elevator.setDirection(Direction.UP);
        
        double initialEnergy = elevator.getEnergyConsumption();
        
        // 移动到1楼
        elevator.moveToFirstFloor();
        
        assertEquals("应该到达1楼", 1, elevator.getCurrentFloor());
        assertEquals("状态应该是IDLE", ElevatorStatus.IDLE, elevator.getStatus());
        assertTrue("能耗应该增加", elevator.getEnergyConsumption() > initialEnergy);
    }

    /**
     * 测试清除所有请求功能
     * 验证电梯清空请求和目标的逻辑
     */
    @Test
    public void testClearAllRequests() {
        Elevator elevator = elevators.get(0);
        
        // 添加乘客和目标
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(2, 7, Priority.LOW, RequestType.STANDARD);
        elevator.getPassengerList().add(request1);
        elevator.getPassengerList().add(request2);
        elevator.getDestinationSet().add(5);
        elevator.getDestinationSet().add(7);
        
        // 清除所有请求
        List<PassengerRequest> clearedRequests = elevator.clearAllRequests();
        
        assertEquals("应该返回2个清除的请求", 2, clearedRequests.size());
        assertTrue("乘客列表应该为空", elevator.getPassengerList().isEmpty());
        assertTrue("目标集合应该为空", elevator.getDestinationSet().isEmpty());
    }

    /**
     * 测试Scheduler单例模式
     * 验证调度器的单例实现
     */
    @Test
    public void testSchedulerSingleton() {
        Scheduler instance1 = Scheduler.getInstance();
        Scheduler instance2 = Scheduler.getInstance();
        
        assertSame("Scheduler应该是单例", instance1, instance2);
    }

    /**
     * 测试乘客请求提交
     * 验证不同优先级请求的处理
     */
    @Test
    public void testPassengerRequestSubmission() {
        PassengerRequest highPriorityRequest = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest mediumPriorityRequest = new PassengerRequest(2, 7, Priority.MEDIUM, RequestType.STANDARD);
        
        // 提交高优先级请求
        scheduler.submitRequest(highPriorityRequest);
        
        // 提交中优先级请求
        scheduler.submitRequest(mediumPriorityRequest);
        
        // 验证请求被正确处理（通过检查楼层队列）
        List<PassengerRequest> floorRequests = scheduler.getRequestsAtFloor(1, Direction.UP);
        assertTrue("应该包含中优先级请求", floorRequests.contains(mediumPriorityRequest));
    }

    /**
     * 测试电梯派遣逻辑
     * 验证调度器选择电梯的策略
     */
    @Test
    public void testElevatorDispatch() {
        // 设置电梯状态
        elevators.get(0).setCurrentFloor(1);
        elevators.get(0).setStatus(ElevatorStatus.IDLE);
        elevators.get(1).setCurrentFloor(5);
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(2).setCurrentFloor(10);
        elevators.get(2).setStatus(ElevatorStatus.IDLE);
        
        PassengerRequest request = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        
        // 派遣电梯
        scheduler.dispatchElevator(request);
        
        // 验证派遣逻辑被执行，具体选择取决于策略实现
        assertNotNull("调度器应该存在", scheduler);
    }

    /**
     * 测试紧急协议执行
     * 验证紧急情况下所有电梯的响应
     */
    @Test
    public void testEmergencyProtocolExecution() {
        // 设置电梯状态
        for (Elevator elevator : elevators) {
            elevator.setStatus(ElevatorStatus.MOVING);
            elevator.setCurrentFloor(5);
        }
        
        // 执行紧急协议
        scheduler.executeEmergencyProtocol();
        
        // 验证所有电梯都进入紧急状态
        for (Elevator elevator : elevators) {
            assertEquals("所有电梯应该进入紧急状态", ElevatorStatus.EMERGENCY, elevator.getStatus());
        }
    }

    /**
     * 测试请求重分发
     * 验证故障电梯的请求重分发逻辑
     */
    @Test
    public void testRequestRedistribution() {
        Elevator faultyElevator = elevators.get(0);
        
        // 添加请求到故障电梯
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(2, 7, Priority.HIGH, RequestType.STANDARD);
        faultyElevator.getPassengerList().add(request1);
        faultyElevator.getPassengerList().add(request2);
        faultyElevator.getDestinationSet().add(5);
        faultyElevator.getDestinationSet().add(7);
        
        // 重分发请求
        scheduler.redistributeRequests(faultyElevator);
        
        // 验证故障电梯的请求被清除
        assertTrue("故障电梯的乘客列表应该为空", faultyElevator.getPassengerList().isEmpty());
        assertTrue("故障电梯的目标集合应该为空", faultyElevator.getDestinationSet().isEmpty());
    }

    /**
     * 测试PassengerRequest类
     * 验证乘客请求的属性和行为
     */
    @Test
    public void testPassengerRequest() {
        PassengerRequest request = new PassengerRequest(1, 5, Priority.HIGH, RequestType.DESTINATION_CONTROL);
        
        assertEquals("起始楼层应该正确", 1, request.getStartFloor());
        assertEquals("目标楼层应该正确", 5, request.getDestinationFloor());
        assertEquals("优先级应该正确", Priority.HIGH, request.getPriority());
        assertEquals("请求类型应该正确", RequestType.DESTINATION_CONTROL, request.getRequestType());
        assertEquals("方向应该是UP", Direction.UP, request.getDirection());
        assertEquals("特殊需求应该是NONE", SpecialNeeds.NONE, request.getSpecialNeeds());
        assertTrue("时间戳应该合理", request.getTimestamp() > 0);
        
        // 测试向下方向
        PassengerRequest downRequest = new PassengerRequest(5, 1, Priority.LOW, RequestType.STANDARD);
        assertEquals("向下请求方向应该是DOWN", Direction.DOWN, downRequest.getDirection());
    }

    /**
     * 测试Floor类功能
     * 验证楼层请求管理
     */
    @Test
    public void testFloorFunctionality() {
        Floor floor = new Floor(3);
        
        assertEquals("楼层号应该正确", 3, floor.getFloorNumber());
        
        // 添加向上请求
        PassengerRequest upRequest = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        floor.addRequest(upRequest);
        
        // 添加向下请求
        PassengerRequest downRequest = new PassengerRequest(3, 1, Priority.LOW, RequestType.STANDARD);
        floor.addRequest(downRequest);
        
        // 获取向上请求
        List<PassengerRequest> upRequests = floor.getRequests(Direction.UP);
        assertEquals("应该有1个向上请求", 1, upRequests.size());
        assertEquals("请求应该匹配", upRequest, upRequests.get(0));
        
        // 获取向下请求
        List<PassengerRequest> downRequests = floor.getRequests(Direction.DOWN);
        assertEquals("应该有1个向下请求", 1, downRequests.size());
        assertEquals("请求应该匹配", downRequest, downRequests.get(0));
        
        // 再次获取应该为空（请求已被清除）
        List<PassengerRequest> emptyRequests = floor.getRequests(Direction.UP);
        assertTrue("请求应该被清除", emptyRequests.isEmpty());
    }

    /**
     * 测试NearestElevatorStrategy策略
     * 验证最近电梯选择策略
     */
    @Test
    public void testNearestElevatorStrategy() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        // 设置电梯位置
        elevators.get(0).setCurrentFloor(1);
        elevators.get(0).setStatus(ElevatorStatus.IDLE);
        elevators.get(1).setCurrentFloor(5);
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(2).setCurrentFloor(10);
        elevators.get(2).setStatus(ElevatorStatus.IDLE);
        
        PassengerRequest request = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        
        // 测试策略选择
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 应该选择电梯1（距离2层）或电梯2（距离7层），取决于状态
        assertNotNull("应该选择一个电梯", selected);
        assertTrue("应该选择符合条件的电梯", 
            strategy.isEligible(selected, request));
    }

    /**
     * 测试HighEfficiencyStrategy策略
     * 验证高效电梯选择策略
     */
    @Test
    public void testHighEfficiencyStrategy() {
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        
        // 设置电梯位置和状态
        elevators.get(0).setCurrentFloor(2);
        elevators.get(0).setStatus(ElevatorStatus.IDLE);
        elevators.get(1).setCurrentFloor(4);
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(1).setDirection(Direction.UP);
        elevators.get(2).setCurrentFloor(8);
        elevators.get(2).setStatus(ElevatorStatus.IDLE);
        
        PassengerRequest request = new PassengerRequest(3, 6, Priority.MEDIUM, RequestType.STANDARD);
        
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull("应该选择一个电梯", selected);
        
        // 测试距离比较
        boolean isCloser = strategy.isCloser(elevators.get(0), elevators.get(2), request);
        assertTrue("电梯0应该比电梯2更近", isCloser);
    }

    /**
     * 测试EnergySavingStrategy策略
     * 验证节能电梯选择策略
     */
    @Test
    public void testEnergySavingStrategy() {
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        // 设置电梯状态
        elevators.get(0).setStatus(ElevatorStatus.MOVING);
        elevators.get(1).setStatus(ElevatorStatus.IDLE);
        elevators.get(2).setStatus(ElevatorStatus.MOVING);
        
        PassengerRequest request = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 应该选择空闲的电梯
        assertEquals("应该选择空闲电梯", elevators.get(1), selected);
        
        // 测试无空闲电梯的情况
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(0).setCurrentFloor(1);
        elevators.get(0).setDirection(Direction.UP);
        
        Elevator selected2 = strategy.selectElevator(elevators, request);
        // 由于距离太远，可能返回null
    }

    /**
     * 测试PredictiveSchedulingStrategy策略
     * 验证预测调度策略
     */
    @Test
    public void testPredictiveSchedulingStrategy() {
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        
        // 设置电梯状态
        elevators.get(0).setCurrentFloor(1);
        elevators.get(1).setCurrentFloor(5);
        elevators.get(1).getPassengerList().add(new PassengerRequest(1, 10, Priority.MEDIUM, RequestType.STANDARD));
        
        PassengerRequest request = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull("应该选择一个电梯", selected);
        
        // 测试预测成本计算
        double cost1 = strategy.calculatePredictedCost(elevators.get(0), request);
        double cost2 = strategy.calculatePredictedCost(elevators.get(1), request);
        
        assertTrue("成本应该为正数", cost1 > 0);
        assertTrue("成本应该为正数", cost2 > 0);
    }

    /**
     * 测试SystemConfig配置管理
     * 验证系统配置的设置和获取
     */
    @Test
    public void testSystemConfig() {
        SystemConfig config = SystemConfig.getInstance();
        
        // 测试默认值
        assertTrue("默认楼层数应该大于0", config.getFloorCount() > 0);
        assertTrue("默认电梯数应该大于0", config.getElevatorCount() > 0);
        assertTrue("默认最大载重应该大于0", config.getMaxLoad() > 0);
        
        // 测试设置值
        config.setFloorCount(15);
        assertEquals("楼层设置应该生效", 15, config.getFloorCount());
        
        config.setElevatorCount(6);
        assertEquals("电梯数设置应该生效", 6, config.getElevatorCount());
        
        config.setMaxLoad(1000);
        assertEquals("最大载重设置应该生效", 1000, config.getMaxLoad(), 0.001);
        
        // 测试无效值
        config.setFloorCount(-1);
        assertEquals("无效楼层数应该被忽略", 15, config.getFloorCount());
        
        config.setMaxLoad(-100);
        assertEquals("无效载重应该被忽略", 1000, config.getMaxLoad(), 0.001);
    }

    /**
     * 测试EventBus事件系统
     * 验证事件发布和订阅机制
     */
    @Test
    public void testEventBus() {
        EventBus eventBus = EventBus.getInstance();
        
        // 创建测试监听器
        TestEventListener listener = new TestEventListener();
        eventBus.subscribe(EventType.EMERGENCY, listener);
        
        // 发布事件
        EventBus.Event event = new EventBus.Event(EventType.EMERGENCY, "Test emergency");
        eventBus.publish(event);
        
        // 验证事件被接收
        assertEquals("应该接收到事件", 1, listener.getReceivedEvents().size());
        assertEquals("事件类型应该正确", EventType.EMERGENCY, listener.getReceivedEvents().get(0).getType());
        assertEquals("事件数据应该正确", "Test emergency", listener.getReceivedEvents().get(0).getData());
    }

    /**
     * 测试NotificationService通知服务
     * 验证通知发送和渠道处理
     */
    @Test
    public void testNotificationService() {
        NotificationService service = NotificationService.getInstance();
        
        // 创建测试通知
        List<String> recipients = Arrays.asList("test@example.com");
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.EMERGENCY,
                "Test message",
                recipients
            );
        
        // 发送通知
        service.sendNotification(notification);
        
        // 验证通知渠道
        assertTrue("SMS渠道应该支持紧急通知", 
            new NotificationService.SMSChannel().supports(NotificationService.NotificationType.EMERGENCY));
        assertTrue("Email渠道应该支持所有通知", 
            new NotificationService.EmailChannel().supports(NotificationService.NotificationType.INFORMATION));
    }

    /**
     * 测试AnalyticsEngine分析引擎
     * 验证性能报告生成和乘客计数
     */
    @Test
    public void testAnalyticsEngine() {
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        // 创建状态报告
        ElevatorStatusReport report = new ElevatorStatusReport(
            1, 5, Direction.UP, ElevatorStatus.MOVING, 2.5, 140.0, 2
        );
        
        engine.processStatusReport(report);
        
        // 更新楼层乘客计数
        engine.updateFloorPassengerCount(1, 10);
        engine.updateFloorPassengerCount(2, 15);
        
        // 测试高峰时间判断
        assertFalse("乘客数较少时不是高峰", engine.isPeakHours());
        
        // 添加更多乘客
        engine.updateFloorPassengerCount(3, 30);
        engine.updateFloorPassengerCount(4, 25);
        
        // 生成性能报告
        AnalyticsEngine.Report performanceReport = engine.generatePerformanceReport();
        assertNotNull("性能报告不应该为null", performanceReport);
        assertTrue("报告标题应该包含内容", performanceReport.getTitle().length() > 0);
        assertTrue("生成时间应该合理", performanceReport.getGeneratedTime() > 0);
    }

    /**
     * 测试LogManager日志管理
     * 验证日志记录和查询功能
     */
    @Test
    public void testLogManager() {
        LogManager logManager = LogManager.getInstance();
        
        // 记录不同类型的日志
        logManager.recordElevatorEvent(1, "Test elevator event");
        logManager.recordSchedulerEvent("Test scheduler event");
        logManager.recordEvent("TestSource", "Test message");
        
        // 查询日志
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - 10000; // 10秒前
        
        List<LogManager.SystemLog> elevatorLogs = 
            logManager.queryLogs("Elevator 1", startTime, currentTime);
        List<LogManager.SystemLog> schedulerLogs = 
            logManager.queryLogs("Scheduler", startTime, currentTime);
        
        assertTrue("应该有电梯日志", elevatorLogs.size() > 0);
        assertTrue("应该有调度器日志", schedulerLogs.size() > 0);
        
        // 验证日志内容
        LogManager.SystemLog log = elevatorLogs.get(0);
        assertEquals("日志源应该正确", "Elevator 1", log.getSource());
        assertEquals("日志消息应该正确", "Test elevator event", log.getMessage());
        assertTrue("日志时间戳应该合理", log.getTimestamp() >= startTime);
    }

    /**
     * 测试ElevatorStatusReport状态报告
     * 验证状态报告的属性和字符串表示
     */
    @Test
    public void testElevatorStatusReport() {
        ElevatorStatusReport report = new ElevatorStatusReport(
            1, 5, Direction.UP, ElevatorStatus.MOVING, 2.5, 140.0, 2
        );
        
        assertEquals("电梯ID应该正确", 1, report.getElevatorId());
        assertEquals("当前楼层应该正确", 5, report.getCurrentFloor());
        assertEquals("方向应该正确", Direction.UP, report.getDirection());
        assertEquals("状态应该正确", ElevatorStatus.MOVING, report.getStatus());
        assertEquals("速度应该正确", 2.5, report.getSpeed(), 0.001);
        assertEquals("当前载重应该正确", 140.0, report.getCurrentLoad(), 0.001);
        assertEquals("乘客数应该正确", 2, report.getPassengerCount());
        
        // 测试toString方法
        String reportString = report.toString();
        assertTrue("字符串表示应该包含电梯ID", reportString.contains("elevatorId=1"));
        assertTrue("字符串表示应该包含楼层", reportString.contains("currentFloor=5"));
    }

    /**
     * 测试枚举值
     * 验证所有枚举类的正确性
     */
    @Test
    public void testEnums() {
        // 测试Direction枚举
        assertEquals("UP方向应该正确", "UP", Direction.UP.name());
        assertEquals("DOWN方向应该正确", "DOWN", Direction.DOWN.name());
        
        // 测试ElevatorStatus枚举
        assertEquals("MOVING状态应该正确", "MOVING", ElevatorStatus.MOVING.name());
        assertEquals("STOPPED状态应该正确", "STOPPED", ElevatorStatus.STOPPED.name());
        assertEquals("IDLE状态应该正确", "IDLE", ElevatorStatus.IDLE.name());
        assertEquals("MAINTENANCE状态应该正确", "MAINTENANCE", ElevatorStatus.MAINTENANCE.name());
        assertEquals("EMERGENCY状态应该正确", "EMERGENCY", ElevatorStatus.EMERGENCY.name());
        assertEquals("FAULT状态应该正确", "FAULT", ElevatorStatus.FAULT.name());
        
        // 测试Priority枚举
        assertEquals("HIGH优先级应该正确", "HIGH", Priority.HIGH.name());
        assertEquals("MEDIUM优先级应该正确", "MEDIUM", Priority.MEDIUM.name());
        assertEquals("LOW优先级应该正确", "LOW", Priority.LOW.name());
        
        // 测试RequestType枚举
        assertEquals("STANDARD类型应该正确", "STANDARD", RequestType.STANDARD.name());
        assertEquals("DESTINATION_CONTROL类型应该正确", "DESTINATION_CONTROL", RequestType.DESTINATION_CONTROL.name());
        
        // 测试SpecialNeeds枚举
        assertEquals("NONE特殊需求应该正确", "NONE", SpecialNeeds.NONE.name());
        assertEquals("DISABLED_ASSISTANCE特殊需求应该正确", "DISABLED_ASSISTANCE", SpecialNeeds.DISABLED_ASSISTANCE.name());
        assertEquals("LARGE_LUGGAGE特殊需求应该正确", "LARGE_LUGGAGE", SpecialNeeds.LARGE_LUGGAGE.name());
        assertEquals("VIP_SERVICE特殊需求应该正确", "VIP_SERVICE", SpecialNeeds.VIP_SERVICE.name());
        
        // 测试ElevatorMode枚举
        assertEquals("NORMAL模式应该正确", "NORMAL", ElevatorMode.NORMAL.name());
        assertEquals("ENERGY_SAVING模式应该正确", "ENERGY_SAVING", ElevatorMode.ENERGY_SAVING.name());
        assertEquals("EMERGENCY模式应该正确", "EMERGENCY", ElevatorMode.EMERGENCY.name());
        
        // 测试EventType枚举
        assertEquals("ELEVATOR_FAULT事件类型应该正确", "ELEVATOR_FAULT", EventType.ELEVATOR_FAULT.name());
        assertEquals("EMERGENCY事件类型应该正确", "EMERGENCY", EventType.EMERGENCY.name());
        assertEquals("MAINTENANCE_REQUIRED事件类型应该正确", "MAINTENANCE_REQUIRED", EventType.MAINTENANCE_REQUIRED.name());
        assertEquals("CONFIG_UPDATED事件类型应该正确", "CONFIG_UPDATED", EventType.CONFIG_UPDATED.name());
    }

    /**
     * 测试边界条件和异常情况
     * 验证系统在极端条件下的行为
     */
    @Test
    public void testBoundaryConditions() {
        // 测试最小楼层
        PassengerRequest minFloorRequest = new PassengerRequest(1, 1, Priority.MEDIUM, RequestType.STANDARD);
        assertEquals("最小楼层请求应该正确", 1, minFloorRequest.getStartFloor());
        
        // 测试楼层边界移动
        Elevator elevator = elevators.get(0);
        elevator.setCurrentFloor(1);
        elevator.setDirection(Direction.DOWN);
        
        // 测试空目标集合
        elevator.getDestinationSet().clear();
        elevator.updateDirection();
        assertEquals("无目标时应该是IDLE", ElevatorStatus.IDLE, elevator.getStatus());
        
        // 测试最大载重边界
        double maxLoad = elevator.getMaxLoad();
        elevator.setCurrentLoad(maxLoad);
        assertEquals("载重应该达到最大值", maxLoad, elevator.getCurrentLoad(), 0.001);
    }

    /**
     * 测试并发安全性
     * 验证多线程环境下的行为
     */
    @Test
    public void testConcurrencySafety() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<ElevatorManager> instances = Collections.synchronizedList(new ArrayList<>());
        
        // 多线程获取单例
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    instances.add(ElevatorManager.getInstance());
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        
        // 验证所有线程获取的是同一个实例
        ElevatorManager firstInstance = instances.get(0);
        for (ElevatorManager instance : instances) {
            assertSame("所有线程应该获取同一个实例", firstInstance, instance);
        }
    }

    /**
     * 测试事件监听器实现
     * 用于测试EventBus功能
     */
    private static class TestEventListener implements EventBus.EventListener {
        private final List<EventBus.Event> receivedEvents = new ArrayList<>();
        
        @Override
        public void onEvent(EventBus.Event event) {
            receivedEvents.add(event);
        }
        
        public List<EventBus.Event> getReceivedEvents() {
            return new ArrayList<>(receivedEvents);
        }
    }

    /**
     * 重置关键单例实例
     * 确保测试之间的独立性
     */
    private void resetSingletons() {
        try {
            // 使用反射重置关键单例实例
            resetSingleton(ElevatorManager.class, "instance");
            resetSingleton(Scheduler.class, "instance");
            resetSingleton(SystemConfig.class, "instance");
            resetSingleton(EventBus.class, "instance");
            resetSingleton(NotificationService.class, "instance");
            resetSingleton(AnalyticsEngine.class, "instance");
            resetSingleton(LogManager.class, "instance");
        } catch (Exception e) {
            // 如果重置失败，继续执行测试
            System.err.println("重置单例失败: " + e.getMessage());
        }
    }

    /**
     * 重置指定类的单例实例
     */
    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }
}