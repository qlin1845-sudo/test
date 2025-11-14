package net.mooctest;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ElevatorManagerTest {

    @Before
    public void resetSingletons() throws Exception {
        // 统一重置所有单例，保证各测试之间互不干扰
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(ElevatorManager.class);
        resetSingleton(EventBus.class);
        resetSingleton(LogManager.class);
        resetSingleton(MaintenanceManager.class);
        resetSingleton(NotificationService.class);
        resetSingleton(Scheduler.class);
        resetSingleton(SecurityMonitor.class);
        resetSingleton(SystemConfig.class);
        resetSingleton(ThreadPoolManager.class);
    }

    private void resetSingleton(Class<?> clazz) throws Exception {
        try {
            Field instanceField = findField(clazz, "instance");
            Object current = instanceField.get(null);
            if (current != null) {
                if (current instanceof ThreadPoolManager) {
                    ((ThreadPoolManager) current).shutdown();
                }
                if (current instanceof MaintenanceManager) {
                    shutdownExecutor(current, "executorService");
                }
                if (current instanceof SecurityMonitor) {
                    shutdownExecutor(current, "executorService");
                }
            }
            instanceField.set(null, null);
        } catch (NoSuchFieldException ignored) {
            // 没有instance字段的类无需重置
        }
    }

    private void shutdownExecutor(Object target, String fieldName) throws Exception {
        Field executorField = findField(target.getClass(), fieldName);
        ExecutorService executor = (ExecutorService) executorField.get(target);
        executor.shutdownNow();
    }

    private Field findField(Class<?> clazz, String name) throws Exception {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Object getFieldValue(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        return field.get(target);
    }


    private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = findField(clazz, fieldName);
        field.set(null, value);
    }

    private static class QuietMaintenanceManager extends MaintenanceManager {
        @Override
        public void processTasks() {
            // 覆盖父类耗时的循环，避免测试阻塞
        }
    }

    private static class TrackingElevator extends Elevator {
        private boolean emergencyHandled;

        public TrackingElevator(int id, Scheduler scheduler) {
            super(id, scheduler);
        }

        @Override
        public void handleEmergency() {
            emergencyHandled = true;
            super.handleEmergency();
        }

        public boolean isEmergencyHandled() {
            return emergencyHandled;
        }
    }

    private static class StubScheduler extends Scheduler {
        private final Map<String, List<PassengerRequest>> pending = new HashMap<>();

        public StubScheduler() {
            super(new ArrayList<>(), 20, (elevators, request) -> null);
        }

        public void setRequests(int floor, Direction direction, List<PassengerRequest> requests) {
            pending.put(key(floor, direction), new ArrayList<>(requests));
        }

        @Override
        public List<PassengerRequest> getRequestsAtFloor(int floorNumber, Direction direction) {
            String key = key(floorNumber, direction);
            List<PassengerRequest> requests = pending.getOrDefault(key, new ArrayList<>());
            pending.remove(key);
            return new ArrayList<>(requests);
        }

        private String key(int floor, Direction direction) {
            return floor + "-" + direction;
        }
    }

    private static class FaultyElevator extends Elevator {
        private final List<PassengerRequest> pending;

        public FaultyElevator(int id, Scheduler scheduler, List<PassengerRequest> pending) {
            super(id, scheduler);
            this.pending = new ArrayList<>(pending);
        }

        @Override
        public List<PassengerRequest> clearAllRequests() {
            return new ArrayList<>(pending);
        }
    }

    private Elevator createElevator(int id, int floor, Direction direction, ElevatorStatus status) {
        Scheduler scheduler = new Scheduler(new ArrayList<>(), 20, (elevators, request) -> null);
        Elevator elevator = new Elevator(id, scheduler);
        elevator.setCurrentFloor(floor);
        elevator.setDirection(direction);
        elevator.setStatus(status);
        return elevator;
    }

    @Test(timeout = 5000)
    public void testSystemConfigSetters() throws Exception {
        // 测试说明：验证系统配置的默认值、合法设置与非法设置分支，确保配置修改逻辑健壮
        SystemConfig config = SystemConfig.getInstance();
        assertEquals(20, config.getFloorCount());
        assertEquals(4, config.getElevatorCount());
        assertEquals(800.0, config.getMaxLoad(), 0.0001);

        config.setFloorCount(30);
        assertEquals(30, config.getFloorCount());
        config.setFloorCount(0);
        assertEquals("非法楼层数不应生效", 30, config.getFloorCount());

        config.setElevatorCount(6);
        assertEquals(6, config.getElevatorCount());
        config.setElevatorCount(-5);
        assertEquals("非法电梯数量不应被接受", 6, config.getElevatorCount());

        config.setMaxLoad(900.5);
        assertEquals(900.5, config.getMaxLoad(), 0.0001);
        config.setMaxLoad(-1);
        assertEquals("非法载重应被拒绝", 900.5, config.getMaxLoad(), 0.0001);
    }

    @Test(timeout = 5000)
    public void testElevatorManagerSingletonAndRegistration() {
        // 测试说明：验证电梯管理器单例与注册逻辑，确保电梯查找与集合返回正常
        ElevatorManager manager = ElevatorManager.getInstance();
        ElevatorManager same = ElevatorManager.getInstance();
        assertSame("单例应返回同一实例", manager, same);

        Scheduler scheduler = new Scheduler(new ArrayList<>(), 5, (elevators, request) -> null);
        Elevator elevator = new Elevator(101, scheduler);

        manager.registerElevator(elevator);
        assertEquals(elevator, manager.getElevatorById(101));
        Collection<Elevator> elevators = manager.getAllElevators();
        assertTrue(elevators.contains(elevator));
    }

    @Test(timeout = 5000)
    public void testPassengerRequestProperties() {
        // 测试说明：验证乘客请求的方向推断、属性访问以及字符串描述，覆盖上下行两种分支
        PassengerRequest upRequest = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        assertEquals(Direction.UP, upRequest.getDirection());
        assertTrue(upRequest.toString().contains("HIGH"));

        PassengerRequest downRequest = new PassengerRequest(8, 3, Priority.LOW, RequestType.DESTINATION_CONTROL);
        assertEquals(Direction.DOWN, downRequest.getDirection());
        assertTrue(downRequest.toString().contains("DESTINATION_CONTROL"));
    }

    @Test(timeout = 5000)
    public void testFloorQueueOperations() {
        // 测试说明：验证楼层请求队列的入队和清空逻辑，确保线程安全的锁处理有效
        Floor floor = new Floor(3);
        PassengerRequest request = new PassengerRequest(3, 7, Priority.MEDIUM, RequestType.STANDARD);
        floor.addRequest(request);

        List<PassengerRequest> fetched = floor.getRequests(Direction.UP);
        assertEquals(1, fetched.size());
        assertEquals(request, fetched.get(0));
        assertTrue("取出后队列应清空", floor.getRequests(Direction.UP).isEmpty());
    }

    @Test(timeout = 5000)
    public void testEnergySavingStrategySelection() {
        // 测试说明：覆盖节能策略的空闲优先与方向距离匹配分支，确保返回结果正确
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        PassengerRequest request = new PassengerRequest(2, 6, Priority.LOW, RequestType.STANDARD);

        Elevator idle = createElevator(1, 3, Direction.UP, ElevatorStatus.IDLE);
        Elevator moving = createElevator(2, 4, Direction.UP, ElevatorStatus.MOVING);

        List<Elevator> elevators = Arrays.asList(moving, idle);
        assertSame("应优先选择空闲电梯", idle, strategy.selectElevator(elevators, request));

        idle.setStatus(ElevatorStatus.MOVING);
        idle.setDirection(Direction.DOWN);
        idle.setCurrentFloor(10);
        assertSame("应选择方向一致且距离合适的电梯", moving, strategy.selectElevator(elevators, request));

        moving.setCurrentFloor(20);
        assertNull("无符合条件的电梯应返回null", strategy.selectElevator(elevators, request));
    }

    @Test(timeout = 5000)
    public void testHighEfficiencyStrategySelection() {
        // 测试说明：验证高效策略在候选电梯之间选取距离最近者并复用isCloser方法
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        PassengerRequest request = new PassengerRequest(5, 9, Priority.MEDIUM, RequestType.STANDARD);

        Elevator first = createElevator(3, 10, Direction.UP, ElevatorStatus.IDLE);
        Elevator second = createElevator(4, 4, Direction.UP, ElevatorStatus.MOVING);

        Elevator selected = strategy.selectElevator(Arrays.asList(first, second), request);
        assertSame("应选取距离更近的第二台电梯", second, selected);
        assertTrue("isCloser结果应匹配距离判断", strategy.isCloser(second, first, request));
    }

    @Test(timeout = 5000)
    public void testNearestElevatorStrategyEligibility() {
        // 测试说明：验证最近策略对空闲、同向行驶及不同方向的处理分支
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        PassengerRequest request = new PassengerRequest(4, 1, Priority.MEDIUM, RequestType.STANDARD);

        Elevator idle = createElevator(5, 6, Direction.UP, ElevatorStatus.IDLE);
        Elevator sameDirection = createElevator(6, 5, Direction.DOWN, ElevatorStatus.MOVING);
        Elevator opposite = createElevator(7, 2, Direction.UP, ElevatorStatus.MOVING);

        List<Elevator> list = Arrays.asList(opposite, sameDirection, idle);
        Elevator firstSelected = strategy.selectElevator(list, request);
        assertSame("首次应选择距离最近的同向电梯", sameDirection, firstSelected);
        assertTrue(strategy.isEligible(sameDirection, request));
        assertFalse(strategy.isEligible(opposite, request));

        idle.setCurrentFloor(3);
        sameDirection.setCurrentFloor(8);
        Elevator secondSelected = strategy.selectElevator(list, request);
        assertSame("当空闲电梯更近时应被选中", idle, secondSelected);
    }

    @Test(timeout = 5000)
    public void testPredictiveSchedulingStrategy() throws Exception {
        // 测试说明：验证预测调度策略的成本计算与选择逻辑，覆盖距离与载荷因子的组合
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        PassengerRequest request = new PassengerRequest(3, 9, Priority.LOW, RequestType.STANDARD);

        Elevator elevatorA = createElevator(8, 10, Direction.UP, ElevatorStatus.MOVING);
        @SuppressWarnings("unchecked")
        List<PassengerRequest> passengerListA = (List<PassengerRequest>) getFieldValue(elevatorA, "passengerList");
        passengerListA.add(new PassengerRequest(1, 5, Priority.LOW, RequestType.STANDARD));
        passengerListA.add(new PassengerRequest(2, 6, Priority.MEDIUM, RequestType.STANDARD));

        Elevator elevatorB = createElevator(9, 2, Direction.UP, ElevatorStatus.MOVING);

        double costA = strategy.calculatePredictedCost(elevatorA, request);
        double costB = strategy.calculatePredictedCost(elevatorB, request);
        assertTrue("电梯B应具备更低成本", costB < costA);

        Elevator chosen = strategy.selectElevator(Arrays.asList(elevatorA, elevatorB), request);
        assertSame(elevatorB, chosen);
    }

    @Test(timeout = 5000)
    public void testLogManagerRecordAndQuery() throws Exception {
        // 测试说明：验证日志记录与时间区间查询逻辑，确保过滤条件生效
        LogManager logManager = LogManager.getInstance();
        long start = System.currentTimeMillis();
        logManager.recordElevatorEvent(1, "Arrived");
        logManager.recordSchedulerEvent("Dispatch");
        logManager.recordEvent("Custom", "Message");
        long end = System.currentTimeMillis();

        List<LogManager.SystemLog> elevatorLogs = logManager.queryLogs("Elevator 1", start, end);
        assertEquals(1, elevatorLogs.size());
        assertEquals("Arrived", elevatorLogs.get(0).getMessage());

        List<LogManager.SystemLog> schedulerLogs = logManager.queryLogs("Scheduler", start, end);
        assertEquals(1, schedulerLogs.size());
        assertEquals("Dispatch", schedulerLogs.get(0).getMessage());
    }

    @Test(timeout = 5000)
    public void testAnalyticsEngineProcessing() throws Exception {
        // 测试说明：验证分析引擎的状态缓存、峰值判断和报表生成逻辑
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        ElevatorStatusReport report = new ElevatorStatusReport(1, 5, Direction.UP, ElevatorStatus.MOVING, 1.5, 350, 5);
        engine.processStatusReport(report);
        engine.updateFloorPassengerCount(1, 30);
        engine.updateFloorPassengerCount(2, 25);
        assertTrue("累计乘客超过阈值应视为高峰", engine.isPeakHours());

        Object storedReports = getFieldValue(engine, "statusReports");
        assertTrue(((List<?>) storedReports).contains(report));

        AnalyticsEngine.Report performance = engine.generatePerformanceReport();
        assertEquals("System Performance Report", performance.getTitle());
        assertTrue(performance.getGeneratedTime() <= System.currentTimeMillis());
    }

    @Test(timeout = 5000)
    public void testEventBusSubscription() {
        // 测试说明：验证事件总线的订阅与发布机制，覆盖无监听分支
        EventBus bus = EventBus.getInstance();
        AtomicReference<String> message = new AtomicReference<>();
        bus.subscribe(EventType.CONFIG_UPDATED, event -> message.set((String) event.getData()));

        bus.publish(new EventBus.Event(EventType.CONFIG_UPDATED, "配置生效"));
        assertEquals("配置生效", message.get());

        // 发布无监听事件，确保不会抛出异常
        bus.publish(new EventBus.Event(EventType.MAINTENANCE_REQUIRED, "无人处理"));
    }

    @Test(timeout = 5000)
    public void testNotificationServiceChannelFiltering() throws Exception {
        // 测试说明：验证通知服务仅调用支持的通道，确保过滤逻辑准确
        NotificationService service = new NotificationService();
        @SuppressWarnings("unchecked")
        List<NotificationService.NotificationChannel> channels = (List<NotificationService.NotificationChannel>) getFieldValue(service, "channels");
        channels.clear();

        AtomicInteger counter = new AtomicInteger();
        NotificationService.NotificationChannel unsupported = new NotificationService.NotificationChannel() {
            @Override
            public boolean supports(NotificationService.NotificationType type) {
                return false;
            }

            @Override
            public void send(NotificationService.Notification notification) {
                fail("不支持的通道不应被调用");
            }
        };
        NotificationService.NotificationChannel supported = new NotificationService.NotificationChannel() {
            @Override
            public boolean supports(NotificationService.NotificationType type) {
                return type == NotificationService.NotificationType.MAINTENANCE;
            }

            @Override
            public void send(NotificationService.Notification notification) {
                counter.incrementAndGet();
            }
        };
        channels.add(unsupported);
        channels.add(supported);

        NotificationService.Notification notification = new NotificationService.Notification(
                NotificationService.NotificationType.MAINTENANCE,
                "需要保养",
                Arrays.asList("a@b.com")
        );
        service.sendNotification(notification);
        assertEquals("仅支持通道应被触发一次", 1, counter.get());

        NotificationService.SMSChannel smsChannel = new NotificationService.SMSChannel();
        assertTrue(smsChannel.supports(NotificationService.NotificationType.EMERGENCY));
        assertFalse(smsChannel.supports(NotificationService.NotificationType.INFORMATION));
    }

    @Test(timeout = 5000)
    public void testMaintenanceManagerSchedulingAndRecords() throws Exception {
        // 测试说明：验证维护管理器的任务排队、事件响应和记录生成逻辑
        QuietMaintenanceManager manager = new QuietMaintenanceManager();
        Elevator elevator = createElevator(13, 1, Direction.UP, ElevatorStatus.IDLE);

        manager.scheduleMaintenance(elevator);
        @SuppressWarnings("unchecked")
        Queue<MaintenanceManager.MaintenanceTask> queue = (Queue<MaintenanceManager.MaintenanceTask>) getFieldValue(manager, "taskQueue");
        assertEquals(1, queue.size());
        MaintenanceManager.MaintenanceTask task = queue.peek();
        assertEquals(13, task.getElevatorId());

        MaintenanceManager.MaintenanceTask fetched = queue.poll();
        manager.performMaintenance(fetched);
        manager.recordMaintenanceResult(13, "再次确认");
        @SuppressWarnings("unchecked")
        List<MaintenanceManager.MaintenanceRecord> records = (List<MaintenanceManager.MaintenanceRecord>) getFieldValue(manager, "maintenanceRecords");
        assertEquals(2, records.size());

        MaintenanceManager.MaintenanceRecord record = records.get(0);
        assertEquals(13, record.getElevatorId());
        assertTrue(record.getResult().contains("Maintenance"));

        int sizeAfterProcessing = queue.size();
        manager.onEvent(new EventBus.Event(EventType.EMERGENCY, elevator));
        assertEquals(sizeAfterProcessing, queue.size());
        manager.onEvent(new EventBus.Event(EventType.ELEVATOR_FAULT, elevator));
        assertEquals(sizeAfterProcessing + 1, queue.size());

        MaintenanceManager managerSingleton = MaintenanceManager.getInstance();
        assertNotNull(managerSingleton);

        shutdownExecutor(manager, "executorService");
        shutdownExecutor(managerSingleton, "executorService");
    }

    @Test(timeout = 5000)
    public void testMaintenanceTaskAndRecordGetters() {
        // 测试说明：验证维护任务与维护记录的数据封装是否正确
        MaintenanceManager.MaintenanceTask task = new MaintenanceManager.MaintenanceTask(5, 123L, "检查");
        assertEquals(5, task.getElevatorId());
        assertEquals(123L, task.getScheduledTime());
        assertEquals("检查", task.getDescription());

        MaintenanceManager.MaintenanceRecord record = new MaintenanceManager.MaintenanceRecord(6, 456L, "完成");
        assertEquals(6, record.getElevatorId());
        assertEquals(456L, record.getMaintenanceTime());
        assertEquals("完成", record.getResult());
    }

    @Test(timeout = 5000)
    public void testElevatorMoveAndLoadPassengers() throws Exception {
        // 测试说明：验证电梯向上行驶、开门载客及能耗统计逻辑，覆盖开门与乘客装载分支
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(1, scheduler);
        elevator.addDestination(2);
        PassengerRequest request = new PassengerRequest(2, 3, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.setRequests(2, Direction.UP, Collections.singletonList(request));

        elevator.move();
        assertEquals(2, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());
        assertEquals(ElevatorStatus.STOPPED, elevator.getStatus());
        assertEquals(1.0, elevator.getEnergyConsumption(), 0.0001);
        assertEquals(1, elevator.getPassengerList().size());
        assertTrue(elevator.getDestinationSet().contains(3));
        assertEquals(70.0, elevator.getCurrentLoad(), 0.0001);
    }

    @Test(timeout = 5000)
    public void testElevatorMoveDownPath() throws Exception {
        // 测试说明：验证电梯向下行驶时的方向调整和能耗累计，确保未到站时保持移动
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(2, scheduler);
        elevator.setCurrentFloor(5);
        elevator.addDestination(3);
        elevator.setDirection(Direction.UP);

        elevator.move();
        assertEquals(4, elevator.getCurrentFloor());
        assertEquals(Direction.DOWN, elevator.getDirection());
        assertEquals(ElevatorStatus.MOVING, elevator.getStatus());
        assertEquals(1.0, elevator.getEnergyConsumption(), 0.0001);
        assertTrue(elevator.getDestinationSet().contains(3));
    }

    @Test(timeout = 5000)
    public void testElevatorHandleEmergency() throws Exception {
        // 测试说明：验证电梯紧急模式下的清空操作与目标楼层回退
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(3, scheduler);
        @SuppressWarnings("unchecked")
        List<PassengerRequest> passengerList = (List<PassengerRequest>) getFieldValue(elevator, "passengerList");
        passengerList.add(new PassengerRequest(1, 5, Priority.LOW, RequestType.STANDARD));
        passengerList.add(new PassengerRequest(2, 6, Priority.MEDIUM, RequestType.STANDARD));
        @SuppressWarnings("unchecked")
        Set<Integer> destinations = (Set<Integer>) getFieldValue(elevator, "destinationSet");
        destinations.add(5);
        destinations.add(6);

        elevator.handleEmergency();
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertTrue(elevator.getPassengerList().isEmpty());
        assertTrue(elevator.getDestinationSet().contains(1));
        assertEquals(1, elevator.getDestinationSet().size());
    }

    @Test(timeout = 5000)
    public void testElevatorNotifyObservers() {
        // 测试说明：验证自定义观察者通知机制，确保事件对象能准确传递
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(4, scheduler);
        AtomicReference<Event> captured = new AtomicReference<>();
        Observer observer = (obs, arg) -> captured.set((Event) arg);
        elevator.addObserver(observer);
        Event event = new Event(EventType.CONFIG_UPDATED, "变更");
        elevator.notifyObservers(event);
        assertEquals(event, captured.get());
    }

    @Test(timeout = 5000)
    public void testElevatorClearAllRequests() throws Exception {
        // 测试说明：验证清空请求时返回值与内部状态复位，确保锁控制正确
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(5, scheduler);
        @SuppressWarnings("unchecked")
        List<PassengerRequest> passengerList = (List<PassengerRequest>) getFieldValue(elevator, "passengerList");
        PassengerRequest r1 = new PassengerRequest(1, 4, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest r2 = new PassengerRequest(2, 5, Priority.MEDIUM, RequestType.STANDARD);
        passengerList.add(r1);
        passengerList.add(r2);
        @SuppressWarnings("unchecked")
        Set<Integer> destinations = (Set<Integer>) getFieldValue(elevator, "destinationSet");
        destinations.add(4);
        destinations.add(5);

        List<PassengerRequest> cleared = elevator.clearAllRequests();
        assertEquals(2, cleared.size());
        assertTrue(elevator.getPassengerList().isEmpty());
        assertTrue(elevator.getDestinationSet().isEmpty());
    }

    @Test(timeout = 5000)
    public void testElevatorMoveToFirstFloor() throws Exception {
        // 测试说明：验证紧急回到首层时的循环逻辑与能耗统计
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(6, scheduler);
        elevator.setCurrentFloor(2);
        elevator.setDirection(Direction.DOWN);
        elevator.moveToFirstFloor();
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals(1.0, elevator.getEnergyConsumption(), 0.0001);
    }

    @Test(timeout = 5000)
    public void testElevatorLoadPassengersStopsAtMax() throws Exception {
        // 测试说明：验证满载时不会继续装载乘客，覆盖容量判断分支
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(7, scheduler);
        elevator.setCurrentLoad(elevator.getMaxLoad());
        scheduler.setRequests(1, Direction.UP,
                Collections.singletonList(new PassengerRequest(1, 5, Priority.LOW, RequestType.STANDARD)));
        elevator.loadPassengers();
        assertTrue(elevator.getPassengerList().isEmpty());
        assertTrue(elevator.getDestinationSet().isEmpty());
    }

    @Test(timeout = 5000)
    public void testSchedulerSubmitAndDispatch() throws Exception {
        // 测试说明：验证调度器的请求入队、高优先级队列与派梯逻辑
        List<Elevator> elevatorList = new ArrayList<>();
        DispatchStrategy strategy = new DispatchStrategy() {
            @Override
            public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
                return elevators.isEmpty() ? null : elevators.get(0);
            }
        };
        Scheduler scheduler = new Scheduler(elevatorList, 10, strategy);
        Elevator elevator = new Elevator(8, scheduler);
        elevatorList.add(elevator);

        PassengerRequest high = new PassengerRequest(3, 9, Priority.HIGH, RequestType.STANDARD);
        scheduler.submitRequest(high);
        @SuppressWarnings("unchecked")
        Queue<PassengerRequest> highQueue = (Queue<PassengerRequest>) getFieldValue(scheduler, "highPriorityQueue");
        assertEquals(1, highQueue.size());

        PassengerRequest low = new PassengerRequest(4, 6, Priority.LOW, RequestType.STANDARD);
        scheduler.submitRequest(low);
        assertTrue(elevator.getDestinationSet().contains(low.getStartFloor()));

        List<PassengerRequest> floorRequests = scheduler.getRequestsAtFloor(low.getStartFloor(), low.getDirection());
        assertEquals(1, floorRequests.size());
        assertTrue(scheduler.getRequestsAtFloor(low.getStartFloor(), low.getDirection()).isEmpty());

        scheduler.setDispatchStrategy((elevators, request) -> null);
        scheduler.dispatchElevator(low);
    }

    @Test(timeout = 5000)
    public void testSchedulerRedistributeRequestsViaUpdate() throws Exception {
        // 测试说明：验证调度器在故障上报时重新分配请求，并在紧急事件时触发应急协议
        List<Elevator> elevatorList = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevatorList, 10, (elevators, request) -> elevators.isEmpty() ? null : elevators.get(0));
        TrackingElevator healthy = new TrackingElevator(9, scheduler);
        elevatorList.add(healthy);

        PassengerRequest r1 = new PassengerRequest(2, 7, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest r2 = new PassengerRequest(3, 8, Priority.LOW, RequestType.STANDARD);
        FaultyElevator faulty = new FaultyElevator(99, scheduler, Arrays.asList(r1, r2));

        scheduler.update(faulty, new Event(EventType.ELEVATOR_FAULT, null));
        assertTrue(healthy.getDestinationSet().contains(r1.getStartFloor()));
        assertTrue(healthy.getDestinationSet().contains(r2.getStartFloor()));

        scheduler.update(healthy, new Event(EventType.EMERGENCY, null));
        assertTrue("紧急协议应触发处理", healthy.isEmergencyHandled());
    }

    @Test(timeout = 5000)
    public void testSchedulerExecuteEmergencyProtocol() throws Exception {
        // 测试说明：直接验证执行应急协议时遍历所有电梯的行为
        List<Elevator> elevatorList = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevatorList, 5, (elevators, request) -> null);
        TrackingElevator elevatorA = new TrackingElevator(10, scheduler);
        TrackingElevator elevatorB = new TrackingElevator(11, scheduler);
        elevatorList.add(elevatorA);
        elevatorList.add(elevatorB);

        scheduler.executeEmergencyProtocol();
        assertTrue(elevatorA.isEmergencyHandled());
        assertTrue(elevatorB.isEmergencyHandled());
    }

    @Test(timeout = 5000)
    public void testSecurityMonitorHandleEmergency() throws Exception {
        // 测试说明：验证安保监控在接收到紧急事件时的日志、通知及调度联动逻辑
        List<Elevator> elevatorList = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevatorList, 5, (elevators, request) -> null);
        TrackingElevator trackingElevator = new TrackingElevator(12, scheduler);
        elevatorList.add(trackingElevator);
        setStaticField(Scheduler.class, "instance", scheduler);
        assertSame(scheduler, Scheduler.getInstance());

        NotificationService service = NotificationService.getInstance();
        @SuppressWarnings("unchecked")
        List<NotificationService.NotificationChannel> channels = (List<NotificationService.NotificationChannel>) getFieldValue(service, "channels");
        channels.clear();
        List<NotificationService.Notification> sent = new ArrayList<>();
        channels.add(new NotificationService.NotificationChannel() {
            @Override
            public boolean supports(NotificationService.NotificationType type) {
                return true;
            }

            @Override
            public void send(NotificationService.Notification notification) {
                sent.add(notification);
            }
        });

        SecurityMonitor monitor = SecurityMonitor.getInstance();
        monitor.handleEmergency("火警");
        monitor.onEvent(new EventBus.Event(EventType.EMERGENCY, "二次告警"));

        @SuppressWarnings("unchecked")
        List<SecurityMonitor.SecurityEvent> events = (List<SecurityMonitor.SecurityEvent>) getFieldValue(monitor, "securityEvents");
        assertEquals(2, events.size());
        assertEquals(ElevatorStatus.EMERGENCY, trackingElevator.getStatus());
        assertTrue(trackingElevator.getDestinationSet().contains(1));

        LogManager logManager = LogManager.getInstance();
        @SuppressWarnings("unchecked")
        List<LogManager.SystemLog> logs = (List<LogManager.SystemLog>) getFieldValue(logManager, "logs");
        assertTrue(logs.stream().anyMatch(log -> log.getMessage().toLowerCase().contains("emergency")));

        assertEquals(2, sent.size());
        assertTrue(sent.get(0).getMessage().contains("火警"));

        shutdownExecutor(monitor, "executorService");
    }

    @Test(timeout = 5000)
    public void testThreadPoolManagerTaskExecution() throws Exception {
        // 测试说明：验证线程池管理器提交任务与关闭流程，确保任务能被执行
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        manager.submitTask(latch::countDown);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        manager.shutdown();
    }

    @Test(timeout = 5000)
    public void testElevatorStatusReportToString() {
        // 测试说明：验证状态报告的数据封装与字符串格式
        ElevatorStatusReport report = new ElevatorStatusReport(1, 10, Direction.DOWN, ElevatorStatus.MOVING, 2.5, 400.0, 6);
        assertEquals(1, report.getElevatorId());
        assertEquals(10, report.getCurrentFloor());
        assertEquals(Direction.DOWN, report.getDirection());
        assertEquals(ElevatorStatus.MOVING, report.getStatus());
        assertEquals(2.5, report.getSpeed(), 0.0001);
        assertEquals(400.0, report.getCurrentLoad(), 0.0001);
        assertEquals(6, report.getPassengerCount());
        assertTrue(report.toString().contains("elevatorId=1"));
    }

    @Test(timeout = 5000)
    public void testEventClassesGetters() {
        // 测试说明：验证事件类与事件总线事件的属性访问
        Event event = new Event(EventType.CONFIG_UPDATED, "数据");
        assertEquals(EventType.CONFIG_UPDATED, event.getType());
        assertEquals("数据", event.getData());

        EventBus.Event busEvent = new EventBus.Event(EventType.MAINTENANCE_REQUIRED, 123);
        assertEquals(EventType.MAINTENANCE_REQUIRED, busEvent.getType());
        assertEquals(123, busEvent.getData());
    }

    @Test(timeout = 5000)
    public void testNotificationEntities() {
        // 测试说明：验证通知实体的属性封装
        List<String> recipients = Arrays.asList("a", "b");
        NotificationService.Notification notification = new NotificationService.Notification(
                NotificationService.NotificationType.INFORMATION,
                "日常广播",
                recipients
        );
        assertEquals(NotificationService.NotificationType.INFORMATION, notification.getType());
        assertEquals("日常广播", notification.getMessage());
        assertEquals(recipients, notification.getRecipients());
    }

    @Test(timeout = 5000)
    public void testDirectionAndPriorityEnums() {
        // 测试说明：验证枚举值可正常使用，保证分支覆盖完整
        assertEquals(Direction.UP, Direction.valueOf("UP"));
        assertEquals(ElevatorMode.EMERGENCY, ElevatorMode.valueOf("EMERGENCY"));
        assertEquals(Priority.MEDIUM, Priority.valueOf("MEDIUM"));
        assertEquals(RequestType.STANDARD, RequestType.valueOf("STANDARD"));
        assertEquals(SpecialNeeds.VIP_SERVICE, SpecialNeeds.valueOf("VIP_SERVICE"));
        assertEquals(EventType.EMERGENCY, EventType.valueOf("EMERGENCY"));
        assertEquals(ElevatorStatus.MOVING, ElevatorStatus.valueOf("MOVING"));
    }
}
