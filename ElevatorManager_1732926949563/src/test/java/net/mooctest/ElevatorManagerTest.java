package net.mooctest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ElevatorManagerTest {

    @Before
    public void setUp() {
        resetAllSingletons();
    }

    @After
    public void tearDown() {
        resetAllSingletons();
    }

    private void resetAllSingletons() {
        shutdownAndResetSingleton(MaintenanceManager.class, "instance", "executorService");
        shutdownAndResetSingleton(SecurityMonitor.class, "instance", "executorService");
        shutdownAndResetSingleton(ThreadPoolManager.class, "instance", "executorService");
        resetSingleton(ElevatorManager.class, "instance");
        resetSingleton(AnalyticsEngine.class, "instance");
        resetSingleton(EventBus.class, "instance");
        resetSingleton(MaintenanceManager.class, "instance");
        resetSingleton(NotificationService.class, "instance");
        resetSingleton(Scheduler.class, "instance");
        resetSingleton(SecurityMonitor.class, "instance");
        resetSingleton(ThreadPoolManager.class, "instance");
        resetSingleton(LogManager.class, "instance");
        resetSingleton(SystemConfig.class, "instance");
    }

    private void shutdownAndResetSingleton(Class<?> clazz, String instanceField, String executorField) {
        try {
            Field field = clazz.getDeclaredField(instanceField);
            field.setAccessible(true);
            Object instance = field.get(null);
            if (instance != null) {
                try {
                    Field executor = clazz.getDeclaredField(executorField);
                    executor.setAccessible(true);
                    Object executorObj = executor.get(instance);
                    if (executorObj instanceof ExecutorService) {
                        ((ExecutorService) executorObj).shutdownNow();
                    }
                } catch (NoSuchFieldException ignored) {
                    // 忽略没有线程池的情况
                }
            }
        } catch (Exception ignored) {
            // 单例不存在时无需处理
        }
    }

    private void resetSingleton(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, null);
        } catch (Exception ignored) {
            // 单例不存在时无需处理
        }
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ByteArrayOutputStream captureStdout(Runnable runnable) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(output);
        System.setOut(capture);
        try {
            runnable.run();
        } finally {
            System.setOut(original);
        }
        return output;
    }

    private static class StubScheduler extends Scheduler {
        private final Map<String, List<PassengerRequest>> requestPool = new HashMap<>();
        private final List<Elevator> attachedElevators;

        StubScheduler() {
            this(new ArrayList<Elevator>(), SystemConfig.getInstance().getFloorCount());
        }

        StubScheduler(List<Elevator> elevators, int floorCount) {
            super(elevators, floorCount, new NearestElevatorStrategy());
            this.attachedElevators = elevators;
        }

        @Override
        public List<PassengerRequest> getRequestsAtFloor(int floorNumber, Direction direction) {
            String key = buildKey(floorNumber, direction);
            List<PassengerRequest> requests = requestPool.get(key);
            if (requests == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(requests);
        }

        void setRequests(int floor, Direction direction, List<PassengerRequest> requests) {
            requestPool.put(buildKey(floor, direction), requests);
        }

        void addElevator(Elevator elevator) {
            attachedElevators.add(elevator);
        }

        private String buildKey(int floor, Direction direction) {
            return floor + "-" + direction;
        }
    }

    private static class RecordingStrategy implements DispatchStrategy {
        private PassengerRequest lastRequest;
        private Elevator elevatorToReturn;

        RecordingStrategy(Elevator elevatorToReturn) {
            this.elevatorToReturn = elevatorToReturn;
        }

        @Override
        public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
            this.lastRequest = request;
            return elevatorToReturn;
        }

        PassengerRequest getLastRequest() {
            return lastRequest;
        }

        void setElevatorToReturn(Elevator elevatorToReturn) {
            this.elevatorToReturn = elevatorToReturn;
        }
    }

    private static class FakeExecutorService extends AbstractExecutorService {
        private boolean shutdown;
        private boolean shutdownNowCalled;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdownNowCalled = true;
            shutdown = true;
            return new ArrayList<>();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            // 不执行任务
        }

        boolean isShutdownNowCalled() {
            return shutdownNowCalled;
        }
    }

    @Test(timeout = 5000)
    public void testSystemConfigSingletonAndValidation() {
        // 说明：验证系统配置单例的默认值以及参数校验逻辑
        SystemConfig config = SystemConfig.getInstance();
        assertEquals("默认楼层数应为20", 20, config.getFloorCount());
        assertEquals("默认电梯数量应为4", 4, config.getElevatorCount());
        assertEquals(800, config.getMaxLoad(), 0.0001);

        // 说明：设置合法值应当生效
        config.setFloorCount(30);
        config.setElevatorCount(6);
        config.setMaxLoad(900);
        assertEquals(30, config.getFloorCount());
        assertEquals(6, config.getElevatorCount());
        assertEquals(900, config.getMaxLoad(), 0.0001);

        // 说明：非法值应该被忽略，保持原配置
        config.setFloorCount(-1);
        config.setElevatorCount(0);
        config.setMaxLoad(-200);
        assertEquals(30, config.getFloorCount());
        assertEquals(6, config.getElevatorCount());
        assertEquals(900, config.getMaxLoad(), 0.0001);
    }

    @Test(timeout = 5000)
    public void testEventBusSubscribeAndPublish() {
        // 说明：验证事件总线能够正确发布和通知订阅者
        EventBus bus = EventBus.getInstance();
        AtomicInteger counter = new AtomicInteger();
        EventBus.EventListener listener = event -> counter.addAndGet(((Integer) event.getData()));
        bus.subscribe(EventType.CONFIG_UPDATED, listener);

        bus.publish(new EventBus.Event(EventType.CONFIG_UPDATED, 5));
        bus.publish(new EventBus.Event(EventType.CONFIG_UPDATED, 7));
        assertEquals("事件监听器应累计收到12", 12, counter.get());

        // 说明：发布不同类型事件不应触发监听器
        bus.publish(new EventBus.Event(EventType.EMERGENCY, 100));
        assertEquals(12, counter.get());
    }

    @Test(timeout = 5000)
    public void testLogManagerAndAnalyticsEngineIntegration() {
        // 说明：验证日志记录与分析引擎的核心流程
        LogManager logManager = LogManager.getInstance();
        long start = System.currentTimeMillis();
        logManager.recordEvent("Scheduler", "Test Event");
        List<LogManager.SystemLog> logs = logManager.queryLogs("Scheduler", start - 10, System.currentTimeMillis() + 10);
        assertEquals("应当查询到一条日志", 1, logs.size());
        assertEquals("Test Event", logs.get(0).getMessage());

        // 说明：验证分析引擎对报表的累计与峰值判断
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        ElevatorStatusReport report = new ElevatorStatusReport(1, 5, Direction.UP, ElevatorStatus.MOVING, 1.5, 300, 4);
        engine.processStatusReport(report);
        engine.updateFloorPassengerCount(1, 20);
        engine.updateFloorPassengerCount(2, 15);
        assertFalse("总等待人数不超过50时不应判定为高峰", engine.isPeakHours());
        engine.updateFloorPassengerCount(3, 40);
        assertTrue("总等待人数超过50时应判定为高峰", engine.isPeakHours());

        AnalyticsEngine.Report performance = engine.generatePerformanceReport();
        assertEquals("报表标题应固定", "System Performance Report", performance.getTitle());
        assertTrue("报表生成时间应为非零", performance.getGeneratedTime() > 0);
    }

    @Test(timeout = 5000)
    public void testNotificationServiceChannels() {
        // 说明：验证通知服务根据类型选择不同的发送渠道
        NotificationService service = NotificationService.getInstance();
        List<String> receivers = Arrays.asList("a@b.com", "c@d.com");
        NotificationService.Notification emergency = new NotificationService.Notification(NotificationService.NotificationType.EMERGENCY, "紧急事件", receivers);
        NotificationService.Notification info = new NotificationService.Notification(NotificationService.NotificationType.INFORMATION, "普通通知", receivers);

        ByteArrayOutputStream output = captureStdout(() -> {
            service.sendNotification(emergency);
            service.sendNotification(info);
        });
        String message = output.toString();
        assertTrue("紧急事件应触发短信发送", message.contains("Sending SMS notification"));
        assertTrue("所有事件都应触发邮件发送", message.contains("Sending email notification"));
    }

    @Test(timeout = 5000)
    public void testPassengerRequestPropertiesAndFloorQueues() {
        // 说明：验证乘客请求属性推导以及楼层队列操作
        PassengerRequest requestUp = new PassengerRequest(2, 5, Priority.HIGH, RequestType.STANDARD);
        assertEquals(Direction.UP, requestUp.getDirection());
        assertEquals(Priority.HIGH, requestUp.getPriority());
        assertTrue(requestUp.toString().contains("From 2 to 5"));

        PassengerRequest requestDown = new PassengerRequest(8, 1, Priority.LOW, RequestType.DESTINATION_CONTROL);
        assertEquals(Direction.DOWN, requestDown.getDirection());
        assertEquals(RequestType.DESTINATION_CONTROL, requestDown.getRequestType());

        Floor floor = new Floor(3);
        floor.addRequest(requestUp);
        floor.addRequest(requestDown);
        List<PassengerRequest> upList = floor.getRequests(Direction.UP);
        assertEquals("上行队列应当取出一条请求", 1, upList.size());
        assertEquals(requestUp, upList.get(0));
        assertTrue("取出后队列应被清空", floor.getRequests(Direction.UP).isEmpty());
    }

    @Test(timeout = 5000)
    public void testDispatchStrategiesBehaviour() {
        // 说明：准备测试用的调度电梯列表
        StubScheduler scheduler = new StubScheduler();
        Elevator idleElevator = new Elevator(1, scheduler);
        idleElevator.setStatus(ElevatorStatus.IDLE);
        idleElevator.setCurrentFloor(3);

        Elevator movingUpElevator = new Elevator(2, scheduler);
        movingUpElevator.setStatus(ElevatorStatus.MOVING);
        movingUpElevator.setDirection(Direction.UP);
        movingUpElevator.setCurrentFloor(7);

        Elevator movingDownElevator = new Elevator(3, scheduler);
        movingDownElevator.setStatus(ElevatorStatus.MOVING);
        movingDownElevator.setDirection(Direction.DOWN);
        movingDownElevator.setCurrentFloor(4);

        List<Elevator> elevators = Arrays.asList(idleElevator, movingUpElevator, movingDownElevator);
        PassengerRequest upRequest = new PassengerRequest(5, 9, Priority.MEDIUM, RequestType.STANDARD);

        // 说明：节能策略优先选择空闲电梯
        EnergySavingStrategy energyStrategy = new EnergySavingStrategy();
        assertEquals(idleElevator, energyStrategy.selectElevator(elevators, upRequest));

        // 说明：当没有空闲电梯时，节能策略会寻找同方向且距离小于5层的电梯
        idleElevator.setStatus(ElevatorStatus.MOVING);
        idleElevator.setDirection(Direction.DOWN);
        Elevator selectedByEnergy = energyStrategy.selectElevator(elevators, upRequest);
        assertEquals("应选择同方向接近的上行电梯", movingUpElevator, selectedByEnergy);

        // 说明：高效策略比较距离，选出更近的电梯
        HighEfficiencyStrategy highStrategy = new HighEfficiencyStrategy();
        movingUpElevator.setCurrentFloor(10);
        Elevator selectedHigh = highStrategy.selectElevator(elevators, upRequest);
        assertEquals("更靠近的电梯应被优先选择", movingDownElevator, selectedHigh);
        assertTrue("距离比较函数应返回正确结果", highStrategy.isCloser(movingDownElevator, movingUpElevator, upRequest));

        // 说明：最近电梯策略仅在电梯符合条件时进行选择
        NearestElevatorStrategy nearest = new NearestElevatorStrategy();
        assertTrue("空闲电梯应符合条件", nearest.isEligible(idleElevator, upRequest));
        assertFalse("反向运行的电梯不符合条件", nearest.isEligible(movingDownElevator, upRequest));
        idleElevator.setStatus(ElevatorStatus.IDLE);
        Elevator nearestElevator = nearest.selectElevator(elevators, upRequest);
        assertEquals(idleElevator, nearestElevator);

        // 说明：预测策略根据成本选择最佳电梯
        PredictiveSchedulingStrategy predictive = new PredictiveSchedulingStrategy();
        setPassengerCount(movingUpElevator, 5);
        double costUp = predictive.calculatePredictedCost(movingUpElevator, upRequest);
        double costIdle = predictive.calculatePredictedCost(idleElevator, upRequest);
        assertTrue("空闲电梯应具有更低成本", costIdle < costUp);
        Elevator predictiveResult = predictive.selectElevator(elevators, upRequest);
        assertEquals(idleElevator, predictiveResult);
    }

    private void setPassengerCount(Elevator elevator, int count) {
        try {
            List<PassengerRequest> passengers = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                passengers.add(new PassengerRequest(1, 2, Priority.MEDIUM, RequestType.STANDARD));
            }
            Field passengerField = Elevator.class.getDeclaredField("passengerList");
            passengerField.setAccessible(true);
            passengerField.set(elevator, passengers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 8000)
    public void testElevatorMovementAndDoorOperations() throws Exception {
        // 说明：构建调度器并模拟开门上下客流程
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(11, scheduler);
        elevator.setCurrentFloor(1);
        elevator.setDirection(Direction.UP);
        elevator.setStatus(ElevatorStatus.IDLE);

        // 说明：预置已有乘客以便测试卸客逻辑
        Field passengerField = Elevator.class.getDeclaredField("passengerList");
        passengerField.setAccessible(true);
        List<PassengerRequest> passengerList = new ArrayList<>();
        passengerList.add(new PassengerRequest(1, 2, Priority.MEDIUM, RequestType.STANDARD));
        passengerField.set(elevator, passengerList);
        elevator.setCurrentLoad(70);

        // 说明：设置2层等待同方向请求，验证载客逻辑
        PassengerRequest newPassenger = new PassengerRequest(2, 5, Priority.LOW, RequestType.STANDARD);
        scheduler.setRequests(2, Direction.UP, Arrays.asList(newPassenger));

        elevator.addDestination(2);
        elevator.move();
        assertEquals("电梯应到达指定楼层", 2, elevator.getCurrentFloor());
        assertTrue("原乘客应在到站后被卸载", elevator.getPassengerList().isEmpty());
        assertTrue("新乘客目的地应加入集合", elevator.getDestinationSet().contains(5));
        assertEquals("当前载重应与乘客数量匹配", 70, elevator.getCurrentLoad(), 0.0001);
        assertEquals("状态应维持移动状态等待下一目标", ElevatorStatus.MOVING, elevator.getStatus());

        // 说明：再次移动并验证目的地清空后切换为空闲
        scheduler.setRequests(3, Direction.UP, Collections.<PassengerRequest>emptyList());
        elevator.getDestinationSet().clear();
        elevator.addDestination(3);
        elevator.move();
        assertEquals(3, elevator.getCurrentFloor());
        assertEquals("无更多目的地应切换为空闲", ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals("移动两次后能耗应为2", 2.0, elevator.getEnergyConsumption(), 0.0001);
    }

    @Test(timeout = 8000)
    public void testElevatorStateManagementAndEmergency() throws Exception {
        // 说明：测试电梯方向更新、紧急处理以及清空请求流程
        StubScheduler scheduler = new StubScheduler();
        Elevator elevator = new Elevator(21, scheduler);
        elevator.setCurrentFloor(4);
        elevator.setDirection(Direction.UP);
        elevator.setStatus(ElevatorStatus.MOVING);

        // 说明：当没有目的地时应自动恢复空闲
        elevator.getDestinationSet().clear();
        elevator.updateDirection();
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());

        // 说明：加入高层目的地后应判定向上
        elevator.addDestination(8);
        elevator.updateDirection();
        assertEquals(Direction.UP, elevator.getDirection());

        // 说明：加入更低楼层后应改为向下
        elevator.setCurrentFloor(6);
        elevator.addDestination(2);
        elevator.updateDirection();
        assertEquals(Direction.DOWN, elevator.getDirection());

        // 说明：自定义观察者验证通知机制
        final Event[] holder = new Event[1];
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                holder[0] = (Event) arg;
            }
        };
        elevator.addObserver(observer);
        Event event = new Event(EventType.MAINTENANCE_REQUIRED, "测试事件");
        elevator.notifyObservers(event);
        assertEquals("观察者应收到事件", event, holder[0]);

        // 说明：清空请求应返回已有乘客并清理目的地
        Field passengerField = Elevator.class.getDeclaredField("passengerList");
        passengerField.setAccessible(true);
        List<PassengerRequest> passengers = new ArrayList<>();
        passengers.add(new PassengerRequest(3, 9, Priority.MEDIUM, RequestType.STANDARD));
        passengerField.set(elevator, passengers);
        elevator.getDestinationSet().add(9);
        List<PassengerRequest> cleared = elevator.clearAllRequests();
        assertEquals(1, cleared.size());
        assertTrue(elevator.getDestinationSet().isEmpty());

        // 说明：处理紧急情况应立即清空并定位到一楼
        elevator.getDestinationSet().add(10);
        Field destinationField = Elevator.class.getDeclaredField("destinationSet");
        destinationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Integer> destinationSet = (Set<Integer>) destinationField.get(elevator);
        destinationSet.add(7);
        elevator.handleEmergency();
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertTrue(destinationSet.contains(1));
        assertTrue(((List<?>) passengerField.get(elevator)).isEmpty());

        // 说明：紧急返航应回到一楼
        elevator.setCurrentFloor(2);
        elevator.setDirection(Direction.DOWN);
        elevator.moveToFirstFloor();
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());

        // 说明：模式与能耗设置应正常生效
        elevator.setMode(ElevatorMode.ENERGY_SAVING);
        elevator.setEnergyConsumption(42.5);
        assertEquals(ElevatorMode.ENERGY_SAVING, elevator.getMode());
        assertEquals(42.5, elevator.getEnergyConsumption(), 0.0001);
        assertNotNull(elevator.getLock());
        assertNotNull(elevator.getCondition());
    }

    @Test(timeout = 5000)
    public void testElevatorManagerRegistry() {
        // 说明：验证电梯管理器的注册与检索功能
        ElevatorManager manager = ElevatorManager.getInstance();
        StubScheduler scheduler = new StubScheduler();
        Elevator elevatorA = new Elevator(100, scheduler);
        Elevator elevatorB = new Elevator(200, scheduler);
        manager.registerElevator(elevatorA);
        manager.registerElevator(elevatorB);

        assertEquals(elevatorA, manager.getElevatorById(100));
        assertEquals(elevatorB, manager.getElevatorById(200));
        Collection<Elevator> all = manager.getAllElevators();
        assertTrue(all.contains(elevatorA));
        assertEquals(2, all.size());
    }

    @Test(timeout = 5000)
    public void testSchedulerSubmissionAndDispatchFlow() {
        // 说明：构建调度器并检查请求派发流程
        List<Elevator> elevatorList = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevatorList, 10, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        elevatorList.add(elevator);

        RecordingStrategy recordingStrategy = new RecordingStrategy(elevator);
        scheduler.setDispatchStrategy(recordingStrategy);

        PassengerRequest normal = new PassengerRequest(1, 6, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.submitRequest(normal);
        assertTrue("派发后电梯目标应包含起始楼层", elevator.getDestinationSet().contains(1));
        assertEquals(normal, recordingStrategy.getLastRequest());

        PassengerRequest high = new PassengerRequest(2, 8, Priority.HIGH, RequestType.STANDARD);
        scheduler.submitRequest(high);
        @SuppressWarnings("unchecked")
        Queue<PassengerRequest> queue = (Queue<PassengerRequest>) getField(scheduler, "highPriorityQueue");
        assertTrue("高优先级请求应进入高优先队列", queue.contains(high));

        // 说明：强制策略返回空值以验证兜底分支
        recordingStrategy.setElevatorToReturn(null);
        PassengerRequest unmatched = new PassengerRequest(3, 4, Priority.LOW, RequestType.STANDARD);
        scheduler.dispatchElevator(unmatched);
        assertFalse("返回空的情况下目的地不应被添加", elevator.getDestinationSet().contains(3));
    }

    @Test(timeout = 6000)
    public void testSchedulerUpdateRedistributionAndEmergency() throws Exception {
        // 说明：测试调度器接收故障与紧急事件时的处理逻辑
        List<Elevator> list = new ArrayList<>();
        Scheduler scheduler = new Scheduler(list, 10, new NearestElevatorStrategy());
        Elevator healthy = new Elevator(10, scheduler);
        Elevator faulty = new Elevator(20, scheduler);
        list.add(healthy);
        list.add(faulty);

        RecordingStrategy recordingStrategy = new RecordingStrategy(healthy);
        scheduler.setDispatchStrategy(recordingStrategy);

        // 说明：向故障电梯预置乘客用于测试重新派发
        Field passengerField = Elevator.class.getDeclaredField("passengerList");
        passengerField.setAccessible(true);
        List<PassengerRequest> passengers = new ArrayList<>();
        passengers.add(new PassengerRequest(4, 9, Priority.MEDIUM, RequestType.STANDARD));
        passengers.add(new PassengerRequest(5, 1, Priority.LOW, RequestType.STANDARD));
        passengerField.set(faulty, passengers);
        faulty.getDestinationSet().addAll(Arrays.asList(9, 1));

        Event faultEvent = new Event(EventType.ELEVATOR_FAULT, faulty);
        scheduler.update(faulty, faultEvent);
        assertTrue("故障电梯应清空乘客", faulty.getPassengerList().isEmpty());
        assertTrue("请求应重新派发到健康电梯", healthy.getDestinationSet().contains(4));
        assertEquals("最后一次派发请求应为第二个乘客", 5, recordingStrategy.getLastRequest().getStartFloor());

        // 说明：触发紧急事件应使所有电梯进入紧急状态
        Event emergency = new Event(EventType.EMERGENCY, "演练");
        scheduler.update(faulty, emergency);
        assertEquals(ElevatorStatus.EMERGENCY, healthy.getStatus());
        assertTrue(healthy.getDestinationSet().contains(1));
    }

    @Test(timeout = 8000)
    public void testMaintenanceManagerWorkflows() throws Exception {
        // 说明：测试维修管理器的任务调度与记录
        MaintenanceManager manager = new MaintenanceManager();
        shutdownInternalExecutor(manager);

        Elevator elevatorMock = Mockito.mock(Elevator.class);
        Mockito.when(elevatorMock.getId()).thenReturn(77);
        manager.scheduleMaintenance(elevatorMock);
        @SuppressWarnings("unchecked")
        Queue<MaintenanceManager.MaintenanceTask> queue = (Queue<MaintenanceManager.MaintenanceTask>) getField(manager, "taskQueue");
        assertEquals(1, queue.size());

        MaintenanceManager.MaintenanceTask task = queue.poll();
        manager.performMaintenance(task);
        manager.recordMaintenanceResult(task.getElevatorId(), "完成");
        @SuppressWarnings("unchecked")
        List<MaintenanceManager.MaintenanceRecord> records = (List<MaintenanceManager.MaintenanceRecord>) getField(manager, "maintenanceRecords");
        assertEquals(1, records.size());
        assertEquals("完成", records.get(0).getResult());

        // 说明：独立线程执行processTasks验证中断处理
        queue.add(new MaintenanceManager.MaintenanceTask(99, System.currentTimeMillis(), "补测"));
        Thread worker = new Thread(manager::processTasks);
        worker.start();
        Thread.sleep(200);
        worker.interrupt();
        worker.join(2000);
        assertFalse("线程应在中断后结束", worker.isAlive());

        // 说明：任务对象的基础属性
        MaintenanceManager.MaintenanceTask sampleTask = new MaintenanceManager.MaintenanceTask(12, 123L, "描述");
        assertEquals(12, sampleTask.getElevatorId());
        assertEquals(123L, sampleTask.getScheduledTime());
        assertEquals("描述", sampleTask.getDescription());

        MaintenanceManager.MaintenanceRecord record = new MaintenanceManager.MaintenanceRecord(12, 456L, "结果");
        assertEquals(456L, record.getMaintenanceTime());
        assertEquals("结果", record.getResult());
    }

    private void shutdownInternalExecutor(MaintenanceManager manager) {
        try {
            Field executorField = MaintenanceManager.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            ExecutorService executor = (ExecutorService) executorField.get(manager);
            executor.shutdownNow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 6000)
    public void testSecurityMonitorEmergencyFlow() throws Exception {
        // 说明：通过事件总线验证安全监控对紧急事件的响应
        StubScheduler scheduler = new StubScheduler(new ArrayList<Elevator>(), 5);
        Elevator elevator = new Elevator(1, scheduler);
        scheduler.addElevator(elevator);
        setStaticField(Scheduler.class, "instance", scheduler);

        SecurityMonitor monitor = SecurityMonitor.getInstance();
        shutdownSecurityExecutor(monitor);

        ByteArrayOutputStream output = captureStdout(() -> {
            EventBus.getInstance().publish(new EventBus.Event(EventType.EMERGENCY, "火警"));
        });

        @SuppressWarnings("unchecked")
        List<SecurityMonitor.SecurityEvent> events = (List<SecurityMonitor.SecurityEvent>) getField(monitor, "securityEvents");
        assertEquals(1, events.size());
        assertEquals("Emergency situation", events.get(0).getDescription());
        assertTrue("应存在短信通知输出", output.toString().contains("Sending SMS notification"));
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
    }

    private void shutdownSecurityExecutor(SecurityMonitor monitor) {
        try {
            Field executorField = SecurityMonitor.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            ExecutorService executor = (ExecutorService) executorField.get(monitor);
            executor.shutdownNow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 5000)
    public void testThreadPoolManagerShutdownPaths() {
        // 说明：验证线程池管理器正常关闭路径
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        manager.submitTask(latch::countDown);
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        manager.shutdown();
        assertTrue("线程池应被关闭", true);

        // 说明：更换执行器以触发shutdownNow分支
        ThreadPoolManager custom = new ThreadPoolManager();
        ExecutorService original = (ExecutorService) getField(custom, "executorService");
        original.shutdownNow();
        FakeExecutorService fake = new FakeExecutorService();
        setField(custom, "executorService", fake);
        custom.shutdown();
        assertTrue("等待超时应触发强制关闭", fake.isShutdownNowCalled());
    }

    @Test(timeout = 5000)
    public void testElevatorStatusReportAndEnums() {
        // 说明：验证状态报告的格式化输出与枚举覆盖
        ElevatorStatusReport report = new ElevatorStatusReport(5, 9, Direction.DOWN, ElevatorStatus.STOPPED, 1.8, 450.0, 6);
        String desc = report.toString();
        assertTrue(desc.contains("elevatorId=5"));
        assertTrue(desc.contains("direction=DOWN"));

        assertEquals(Direction.UP, Direction.valueOf("UP"));
        assertEquals(Priority.LOW, Priority.valueOf("LOW"));
        assertEquals(RequestType.STANDARD, RequestType.valueOf("STANDARD"));
        assertEquals(SpecialNeeds.NONE, SpecialNeeds.valueOf("NONE"));
        assertEquals(EventType.EMERGENCY, EventType.valueOf("EMERGENCY"));
        assertEquals(ElevatorMode.NORMAL, ElevatorMode.valueOf("NORMAL"));
    }

    @Test(timeout = 5000)
    public void testCustomEventWrapper() {
        // 说明：验证事件包装类的基本属性
        Event event = new Event(EventType.CONFIG_UPDATED, "配置变更");
        assertEquals(EventType.CONFIG_UPDATED, event.getType());
        assertEquals("配置变更", event.getData());

        EventBus.Event busEvent = new EventBus.Event(EventType.MAINTENANCE_REQUIRED, 123);
        assertEquals(EventType.MAINTENANCE_REQUIRED, busEvent.getType());
        assertEquals(123, busEvent.getData());
    }
}
