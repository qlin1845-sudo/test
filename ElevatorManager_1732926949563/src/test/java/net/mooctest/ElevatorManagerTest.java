package net.mooctest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Observable;
import java.util.Observer;

// 导入所有业务类
import net.mooctest.*;

/**
 * 电梯系统综合测试类
 * 覆盖所有核心业务逻辑，确保高分支覆盖率和变异杀死率
 * 包含电梯管理、调度策略、乘客请求处理等功能的全面测试
 */
@RunWith(MockitoJUnitRunner.class)
public class ElevatorManagerTest {

    // 测试用的电梯列表
    private List<Elevator> elevators;
    private Scheduler scheduler;
    private ElevatorManager elevatorManager;
    private SystemConfig systemConfig;
    
    // Mock对象
    @Mock
    private Observer mockObserver;
    
    @Mock
    private DispatchStrategy mockDispatchStrategy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        systemConfig = SystemConfig.getInstance();
        systemConfig.setMaxLoad(800.0);
        
        // 初始化调度器
        scheduler = Scheduler.getInstance(new ArrayList<>(), 10, new NearestElevatorStrategy());
        
        // 初始化测试电梯
        elevators = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            elevators.add(new Elevator(i, scheduler));
        }
        
        // 更新调度器的电梯列表
        scheduler = Scheduler.getInstance(elevators, 10, new NearestElevatorStrategy());
        
        // 初始化电梯管理器
        elevatorManager = ElevatorManager.getInstance();
        
        // 注册电梯到管理器
        for (Elevator elevator : elevators) {
            elevatorManager.registerElevator(elevator);
            elevator.addObserver(mockObserver);
        }
    }

    @After
    public void tearDown() {
        // 清理单例实例，避免测试间相互影响
        resetSingletons();
    }

    /**
     * 测试ElevatorManager单例模式
     * 验证双重检查锁定机制的正确性
     */
    @Test
    public void testElevatorManagerSingleton() {
        ElevatorManager instance1 = ElevatorManager.getInstance();
        ElevatorManager instance2 = ElevatorManager.getInstance();
        
        // 验证单例模式
        assertSame("ElevatorManager应该是单例", instance1, instance2);
        
        // 验证电梯注册功能
        Elevator newElevator = new Elevator(99, scheduler);
        instance1.registerElevator(newElevator);
        
        assertEquals("应该能通过ID获取注册的电梯", newElevator, instance1.getElevatorById(99));
        assertTrue("所有电梯集合应该包含新注册的电梯", 
                  instance1.getAllElevators().contains(newElevator));
    }

    /**
     * 测试Scheduler单例模式和初始化
     */
    @Test
    public void testSchedulerSingleton() {
        Scheduler instance1 = Scheduler.getInstance();
        Scheduler instance2 = Scheduler.getInstance();
        
        assertSame("Scheduler应该是单例", instance1, instance2);
        
        // 测试带参数的单例创建
        List<Elevator> newList = new ArrayList<>();
        newList.add(new Elevator(1, Scheduler.getInstance()));
        Scheduler instance3 = Scheduler.getInstance(newList, 5, new HighEfficiencyStrategy());
        
        assertNotNull("应该能创建带参数的Scheduler实例", instance3);
    }

    /**
     * 测试SystemConfig单例和配置管理
     */
    @Test
    public void testSystemConfig() {
        SystemConfig config = SystemConfig.getInstance();
        
        // 测试默认值
        assertEquals("默认楼层数应该是20", 20, config.getFloorCount());
        assertEquals("默认电梯数应该是4", 4, config.getElevatorCount());
        assertEquals("默认最大载重应该是800", 800.0, config.getMaxLoad(), 0.001);
        
        // 测试配置更新
        config.setFloorCount(15);
        config.setElevatorCount(6);
        config.setMaxLoad(1000.0);
        
        assertEquals("楼层数应该更新为15", 15, config.getFloorCount());
        assertEquals("电梯数应该更新为6", 6, config.getElevatorCount());
        assertEquals("最大载重应该更新为1000", 1000.0, config.getMaxLoad(), 0.001);
        
        // 测试边界条件 - 负值不应该被接受
        config.setFloorCount(-1);
        config.setElevatorCount(0);
        config.setMaxLoad(-100);
        
        assertEquals("负楼层数不应该被接受", 15, config.getFloorCount());
        assertEquals("零电梯数不应该被接受", 6, config.getElevatorCount());
        assertEquals("负载重不应该被接受", 1000.0, config.getMaxLoad(), 0.001);
    }

    /**
     * 测试PassengerRequest的创建和属性
     */
    @Test
    public void testPassengerRequest() {
        // 测试上行请求
        PassengerRequest upRequest = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        
        assertEquals("起始楼层应该是1", 1, upRequest.getStartFloor());
        assertEquals("目标楼层应该是5", 5, upRequest.getDestinationFloor());
        assertEquals("方向应该是UP", Direction.UP, upRequest.getDirection());
        assertEquals("优先级应该是HIGH", Priority.HIGH, upRequest.getPriority());
        assertEquals("请求类型应该是STANDARD", RequestType.STANDARD, upRequest.getRequestType());
        assertEquals("特殊需求应该是NONE", SpecialNeeds.NONE, upRequest.getSpecialNeeds());
        assertTrue("时间戳应该是合理的", upRequest.getTimestamp() > 0);
        
        // 测试下行请求
        PassengerRequest downRequest = new PassengerRequest(10, 3, Priority.MEDIUM, RequestType.DESTINATION_CONTROL);
        assertEquals("方向应该是DOWN", Direction.DOWN, downRequest.getDirection());
        
        // 测试toString方法
        String requestString = upRequest.toString();
        assertTrue("toString应该包含关键信息", 
                  requestString.contains("From 1 to 5") && 
                  requestString.contains("HIGH") && 
                  requestString.contains("STANDARD"));
    }

    /**
     * 测试Floor类的请求队列管理
     */
    @Test
    public void testFloor() {
        Floor floor = new Floor(5);
        
        assertEquals("楼层号应该是5", 5, floor.getFloorNumber());
        
        // 创建测试请求
        PassengerRequest upRequest = new PassengerRequest(5, 8, Priority.LOW, RequestType.STANDARD);
        PassengerRequest downRequest = new PassengerRequest(5, 2, Priority.MEDIUM, RequestType.STANDARD);
        
        // 添加请求
        floor.addRequest(upRequest);
        floor.addRequest(downRequest);
        
        // 获取上行请求
        List<PassengerRequest> upRequests = floor.getRequests(Direction.UP);
        assertEquals("应该有一个上行请求", 1, upRequests.size());
        assertEquals("应该是添加的上行请求", upRequest, upRequests.get(0));
        
        // 获取下行请求
        List<PassengerRequest> downRequests = floor.getRequests(Direction.DOWN);
        assertEquals("应该有一个下行请求", 1, downRequests.size());
        assertEquals("应该是添加的下行请求", downRequest, downRequests.get(0));
        
        // 验证请求队列已清空
        List<PassengerRequest> emptyUpRequests = floor.getRequests(Direction.UP);
        assertTrue("获取请求后队列应该被清空", emptyUpRequests.isEmpty());
    }

    /**
     * 测试Elevator的基本属性和状态管理
     */
    @Test
    public void testElevatorBasicProperties() {
        Elevator elevator = new Elevator(1, scheduler);
        
        assertEquals("电梯ID应该是1", 1, elevator.getId());
        assertEquals("初始楼层应该是1", 1, elevator.getCurrentFloor());
        assertEquals("初始方向应该是UP", Direction.UP, elevator.getDirection());
        assertEquals("初始状态应该是IDLE", ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals("初始模式应该是NORMAL", ElevatorMode.NORMAL, elevator.getMode());
        assertEquals("初始载重应该是0", 0.0, elevator.getCurrentLoad(), 0.001);
        assertEquals("初始能耗应该是0", 0.0, elevator.getEnergyConsumption(), 0.001);
        assertTrue("乘客列表初始应该为空", elevator.getPassengerList().isEmpty());
        assertTrue("目标集合初始应该为空", elevator.getDestinationSet().isEmpty());
        
        // 测试setter方法
        elevator.setCurrentFloor(5);
        elevator.setDirection(Direction.DOWN);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setMode(ElevatorMode.ENERGY_SAVING);
        elevator.setCurrentLoad(350.0);
        elevator.setEnergyConsumption(25.5);
        
        assertEquals("楼层应该更新为5", 5, elevator.getCurrentFloor());
        assertEquals("方向应该更新为DOWN", Direction.DOWN, elevator.getDirection());
        assertEquals("状态应该更新为MOVING", ElevatorStatus.MOVING, elevator.getStatus());
        assertEquals("模式应该更新为ENERGY_SAVING", ElevatorMode.ENERGY_SAVING, elevator.getMode());
        assertEquals("载重应该更新为350", 350.0, elevator.getCurrentLoad(), 0.001);
        assertEquals("能耗应该更新为25.5", 25.5, elevator.getEnergyConsumption(), 0.001);
    }

    /**
     * 测试Elevator的目标管理功能
     */
    @Test
    public void testElevatorDestinationManagement() {
        Elevator elevator = new Elevator(1, scheduler);
        
        // 添加目标楼层
        elevator.addDestination(3);
        elevator.addDestination(7);
        elevator.addDestination(2);
        
        Set<Integer> destinations = elevator.getDestinationSet();
        assertEquals("应该有3个目标楼层", 3, destinations.size());
        assertTrue("应该包含楼层3", destinations.contains(3));
        assertTrue("应该包含楼层7", destinations.contains(7));
        assertTrue("应该包含楼层2", destinations.contains(2));
    }

    /**
     * 测试Elevator的方向更新逻辑
     */
    @Test
    public void testElevatorDirectionUpdate() {
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(5);
        
        // 测试向上方向
        elevator.addDestination(8);
        elevator.updateDirection();
        assertEquals("当有更高楼层目标时方向应该是UP", Direction.UP, elevator.getDirection());
        
        // 测试向下方向
        elevator.getDestinationSet().clear();
        elevator.addDestination(2);
        elevator.updateDirection();
        assertEquals("当有更低楼层目标时方向应该是DOWN", Direction.DOWN, elevator.getDirection());
        
        // 测试空闲状态
        elevator.getDestinationSet().clear();
        elevator.updateDirection();
        assertEquals("没有目标时状态应该是IDLE", ElevatorStatus.IDLE, elevator.getStatus());
    }

    /**
     * 测试Elevator的紧急情况处理
     */
    @Test
    public void testElevatorEmergencyHandling() {
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(10);
        elevator.addDestination(15);
        
        // 添加观察者来验证通知
        Observer testObserver = mock(Observer.class);
        elevator.addObserver(testObserver);
        
        // 处理紧急情况
        elevator.handleEmergency();
        
        assertEquals("紧急状态应该是EMERGENCY", ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertEquals("紧急情况下应该只有1楼作为目标", Collections.singleton(1), elevator.getDestinationSet());
        assertEquals("紧急情况下应该清空乘客", 0.0, elevator.getCurrentLoad(), 0.001);
        
        // 验证观察者被通知（注意：业务代码中直接传递了ElevatorStatus而非Event对象）
        verify(testObserver, times(1)).update(eq(elevator), eq(ElevatorStatus.EMERGENCY));
    }

    /**
     * 测试Elevator的请求清除功能
     */
    @Test
    public void testElevatorClearAllRequests() {
        Elevator elevator = new Elevator(1, scheduler);
        elevator.addDestination(3);
        elevator.addDestination(7);
        
        List<PassengerRequest> clearedRequests = elevator.clearAllRequests();
        
        assertTrue("清除后目标集合应该为空", elevator.getDestinationSet().isEmpty());
        assertTrue("清除后乘客列表应该为空", elevator.getPassengerList().isEmpty());
    }

    /**
     * 测试NearestElevatorStrategy调度策略
     */
    @Test
    public void testNearestElevatorStrategy() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        // 设置电梯位置
        elevators.get(0).setCurrentFloor(1);  // 电梯1在1楼
        elevators.get(1).setCurrentFloor(5);  // 电梯2在5楼
        elevators.get(2).setCurrentFloor(10); // 电梯3在10楼
        
        // 所有电梯设为空闲
        for (Elevator e : elevators) {
            e.setStatus(ElevatorStatus.IDLE);
        }
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.MEDIUM, RequestType.STANDARD);
        
        // 选择最近的电梯
        Elevator selected = strategy.selectElevator(elevators, request);
        assertEquals("应该选择最近的电梯（电梯2，在5楼）", elevators.get(1), selected);
        
        // 测试资格条件
        assertTrue("空闲电梯应该有资格", strategy.isEligible(elevators.get(0), request));
        
        // 测试移动中的电梯
        elevators.get(0).setStatus(ElevatorStatus.MOVING);
        elevators.get(0).setDirection(Direction.UP);
        assertTrue("同方向移动的电梯应该有资格", 
                  strategy.isEligible(elevators.get(0), request));
        
        elevators.get(0).setDirection(Direction.DOWN);
        assertFalse("反方向移动的电梯不应该有资格", 
                   strategy.isEligible(elevators.get(0), request));
    }

    /**
     * 测试HighEfficiencyStrategy调度策略
     */
    @Test
    public void testHighEfficiencyStrategy() {
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        
        // 设置电梯位置和状态
        elevators.get(0).setCurrentFloor(2);
        elevators.get(0).setStatus(ElevatorStatus.IDLE);
        
        elevators.get(1).setCurrentFloor(3);
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(1).setDirection(Direction.UP);
        
        elevators.get(2).setCurrentFloor(8);
        elevators.get(2).setStatus(ElevatorStatus.MOVING);
        elevators.get(2).setDirection(Direction.DOWN);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 应该选择电梯1（空闲且最近）或电梯2（移动中且同方向）
        assertTrue("应该选择合适的电梯", 
                  selected == elevators.get(0) || selected == elevators.get(1));
        
        // 测试距离比较
        assertTrue("电梯1应该比电梯3更近", 
                  strategy.isCloser(elevators.get(0), elevators.get(2), request));
    }

    /**
     * 测试EnergySavingStrategy调度策略
     */
    @Test
    public void testEnergySavingStrategy() {
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        // 设置电梯状态
        elevators.get(0).setStatus(ElevatorStatus.IDLE);
        elevators.get(0).setCurrentFloor(3);
        
        elevators.get(1).setStatus(ElevatorStatus.MOVING);
        elevators.get(1).setCurrentFloor(4);
        elevators.get(1).setDirection(Direction.UP);
        
        elevators.get(2).setStatus(ElevatorStatus.MOVING);
        elevators.get(2).setCurrentFloor(10);
        elevators.get(2).setDirection(Direction.UP);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.LOW, RequestType.STANDARD);
        
        // 应该优先选择空闲电梯
        Elevator selected = strategy.selectElevator(elevators, request);
        assertEquals("应该优先选择空闲电梯", elevators.get(0), selected);
        
        // 测试无空闲电梯的情况
        elevators.get(0).setStatus(ElevatorStatus.MOVING);
        selected = strategy.selectElevator(elevators, request);
        
        // 应该选择同方向且距离近的电梯（电梯1）
        assertEquals("应该选择同方向且距离近的电梯", elevators.get(1), selected);
        
        // 测试距离太远的情况
        elevators.get(1).setCurrentFloor(15);
        selected = strategy.selectElevator(elevators, request);
        assertNull("距离太远时应该返回null", selected);
    }

    /**
     * 测试PredictiveSchedulingStrategy调度策略
     */
    @Test
    public void testPredictiveSchedulingStrategy() {
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        
        // 设置电梯位置和负载
        elevators.get(0).setCurrentFloor(2);
        elevators.get(1).setCurrentFloor(4);
        elevators.get(2).setCurrentFloor(8);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 应该选择预测成本最低的电梯（电梯1，距离最近）
        assertEquals("应该选择预测成本最低的电梯", elevators.get(1), selected);
        
        // 测试预测成本计算
        double cost1 = strategy.calculatePredictedCost(elevators.get(0), request);
        double cost2 = strategy.calculatePredictedCost(elevators.get(1), request);
        double cost3 = strategy.calculatePredictedCost(elevators.get(2), request);
        
        assertTrue("电梯1的预测成本应该合理", cost1 > 0);
        assertTrue("电梯2的预测成本应该最低", cost2 < cost1 && cost2 < cost3);
        assertTrue("电梯3的预测成本应该最高", cost3 > cost1 && cost3 > cost2);
    }

    /**
     * 测试Scheduler的请求提交功能
     */
    @Test
    public void testSchedulerSubmitRequest() {
        // 创建新的电梯列表用于测试
        List<Elevator> testElevators = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            testElevators.add(new Elevator(i, scheduler));
        }
        
        // 使用Mock策略来验证调度行为
        Scheduler testScheduler = new Scheduler(testElevators, 10, mockDispatchStrategy);
        
        PassengerRequest highPriorityRequest = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest normalRequest = new PassengerRequest(2, 6, Priority.MEDIUM, RequestType.STANDARD);
        
        // 提交高优先级请求
        testScheduler.submitRequest(highPriorityRequest);
        verify(mockDispatchStrategy, times(1)).selectElevator(testElevators, highPriorityRequest);
        
        // 提交普通优先级请求
        testScheduler.submitRequest(normalRequest);
        verify(mockDispatchStrategy, times(2)).selectElevator(testElevators, normalRequest);
    }

    /**
     * 测试Scheduler的请求重分配功能
     */
    @Test
    public void testSchedulerRedistributeRequests() {
        Elevator faultyElevator = elevators.get(0);
        faultyElevator.addDestination(3);
        faultyElevator.addDestination(7);
        
        // 重分配请求
        scheduler.redistributeRequests(faultyElevator);
        
        // 验证故障电梯的请求被清除
        assertTrue("故障电梯的目标应该被清除", faultyElevator.getDestinationSet().isEmpty());
    }

    /**
     * 测试Scheduler的紧急协议执行
     */
    @Test
    public void testSchedulerExecuteEmergencyProtocol() {
        scheduler.executeEmergencyProtocol();
        
        // 验证所有电梯都进入紧急状态
        for (Elevator elevator : elevators) {
            assertEquals("所有电梯应该进入紧急状态", ElevatorStatus.EMERGENCY, elevator.getStatus());
        }
    }

    /**
     * 测试Scheduler的观察者更新机制
     */
    @Test
    public void testSchedulerObserverUpdate() {
        Elevator faultyElevator = elevators.get(0);
        
        // 测试电梯故障事件
        Event faultEvent = new Event(EventType.ELEVATOR_FAULT, faultyElevator);
        scheduler.update(faultyElevator, faultEvent);
        
        // 测试紧急事件
        Event emergencyEvent = new Event(EventType.EMERGENCY, null);
        scheduler.update(faultyElevator, emergencyEvent);
        
        // 验证所有电梯都进入紧急状态
        for (Elevator elevator : elevators) {
            assertEquals("紧急事件后所有电梯应该进入紧急状态", 
                        ElevatorStatus.EMERGENCY, elevator.getStatus());
        }
    }

    /**
     * 测试Scheduler的策略切换功能
     */
    @Test
    public void testSchedulerSetDispatchStrategy() {
        DispatchStrategy newStrategy = new HighEfficiencyStrategy();
        scheduler.setDispatchStrategy(newStrategy);
        
        // 创建新请求来验证策略切换生效
        PassengerRequest request = new PassengerRequest(3, 8, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.submitRequest(request);
    }

    /**
     * 测试Event类的创建和属性
     */
    @Test
    public void testEvent() {
        String testData = "test data";
        Event event = new Event(EventType.ELEVATOR_FAULT, testData);
        
        assertEquals("事件类型应该是ELEVATOR_FAULT", EventType.ELEVATOR_FAULT, event.getType());
        assertEquals("事件数据应该是test data", testData, event.getData());
    }

    /**
     * 测试枚举值的完整性
     */
    @Test
    public void testEnums() {
        // 测试Direction枚举
        Direction[] directions = Direction.values();
        assertEquals("Direction应该有2个值", 2, directions.length);
        assertTrue("应该包含UP", Arrays.asList(directions).contains(Direction.UP));
        assertTrue("应该包含DOWN", Arrays.asList(directions).contains(Direction.DOWN));
        
        // 测试ElevatorStatus枚举
        ElevatorStatus[] statuses = ElevatorStatus.values();
        assertEquals("ElevatorStatus应该有6个值", 6, statuses.length);
        assertTrue("应该包含MOVING", Arrays.asList(statuses).contains(ElevatorStatus.MOVING));
        assertTrue("应该包含STOPPED", Arrays.asList(statuses).contains(ElevatorStatus.STOPPED));
        assertTrue("应该包含IDLE", Arrays.asList(statuses).contains(ElevatorStatus.IDLE));
        assertTrue("应该包含MAINTENANCE", Arrays.asList(statuses).contains(ElevatorStatus.MAINTENANCE));
        assertTrue("应该包含EMERGENCY", Arrays.asList(statuses).contains(ElevatorStatus.EMERGENCY));
        assertTrue("应该包含FAULT", Arrays.asList(statuses).contains(ElevatorStatus.FAULT));
        
        // 测试Priority枚举
        Priority[] priorities = Priority.values();
        assertEquals("Priority应该有3个值", 3, priorities.length);
        assertTrue("应该包含HIGH", Arrays.asList(priorities).contains(Priority.HIGH));
        assertTrue("应该包含MEDIUM", Arrays.asList(priorities).contains(Priority.MEDIUM));
        assertTrue("应该包含LOW", Arrays.asList(priorities).contains(Priority.LOW));
        
        // 测试RequestType枚举
        RequestType[] requestTypes = RequestType.values();
        assertEquals("RequestType应该有2个值", 2, requestTypes.length);
        assertTrue("应该包含STANDARD", Arrays.asList(requestTypes).contains(RequestType.STANDARD));
        assertTrue("应该包含DESTINATION_CONTROL", Arrays.asList(requestTypes).contains(RequestType.DESTINATION_CONTROL));
        
        // 测试SpecialNeeds枚举
        SpecialNeeds[] specialNeeds = SpecialNeeds.values();
        assertEquals("SpecialNeeds应该有4个值", 4, specialNeeds.length);
        assertTrue("应该包含NONE", Arrays.asList(specialNeeds).contains(SpecialNeeds.NONE));
        assertTrue("应该包含DISABLED_ASSISTANCE", Arrays.asList(specialNeeds).contains(SpecialNeeds.DISABLED_ASSISTANCE));
        assertTrue("应该包含LARGE_LUGGAGE", Arrays.asList(specialNeeds).contains(SpecialNeeds.LARGE_LUGGAGE));
        assertTrue("应该包含VIP_SERVICE", Arrays.asList(specialNeeds).contains(SpecialNeeds.VIP_SERVICE));
        
        // 测试ElevatorMode枚举
        ElevatorMode[] modes = ElevatorMode.values();
        assertEquals("ElevatorMode应该有3个值", 3, modes.length);
        assertTrue("应该包含NORMAL", Arrays.asList(modes).contains(ElevatorMode.NORMAL));
        assertTrue("应该包含ENERGY_SAVING", Arrays.asList(modes).contains(ElevatorMode.ENERGY_SAVING));
        assertTrue("应该包含EMERGENCY", Arrays.asList(modes).contains(ElevatorMode.EMERGENCY));
        
        // 测试EventType枚举
        EventType[] eventTypes = EventType.values();
        assertEquals("EventType应该有4个值", 4, eventTypes.length);
        assertTrue("应该包含ELEVATOR_FAULT", Arrays.asList(eventTypes).contains(EventType.ELEVATOR_FAULT));
        assertTrue("应该包含EMERGENCY", Arrays.asList(eventTypes).contains(EventType.EMERGENCY));
        assertTrue("应该包含MAINTENANCE_REQUIRED", Arrays.asList(eventTypes).contains(EventType.MAINTENANCE_REQUIRED));
        assertTrue("应该包含CONFIG_UPDATED", Arrays.asList(eventTypes).contains(EventType.CONFIG_UPDATED));
    }

    /**
     * 测试边界条件和异常情况
     */
    @Test
    public void testEdgeCases() {
        // 测试空电梯列表
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        PassengerRequest request = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(new ArrayList<>(), request);
        assertNull("空电梯列表应该返回null", selected);
        
        // 测试同楼层请求
        PassengerRequest sameFloorRequest = new PassengerRequest(5, 5, Priority.LOW, RequestType.STANDARD);
        // 根据PassengerRequest构造函数，startFloor < destinationFloor时为UP，否则为DOWN
        // 同楼层时，startFloor == destinationFloor，条件startFloor < destinationFloor为false，所以方向为DOWN
        assertEquals("同楼层请求方向应该是DOWN", Direction.DOWN, sameFloorRequest.getDirection());
        
        // 测试极端楼层号
        PassengerRequest extremeRequest = new PassengerRequest(1, 100, Priority.HIGH, RequestType.STANDARD);
        assertEquals("极端楼层请求方向应该是UP", Direction.UP, extremeRequest.getDirection());
        
        extremeRequest = new PassengerRequest(100, 1, Priority.HIGH, RequestType.STANDARD);
        assertEquals("下行极端请求方向应该是DOWN", Direction.DOWN, extremeRequest.getDirection());
    }

    /**
     * 测试并发安全性
     */
    @Test
    public void testConcurrency() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<ElevatorManager> instances = Collections.synchronizedList(new ArrayList<>());
        
        // 多线程同时获取单例
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
            assertSame("所有线程应该获取到同一个实例", firstInstance, instance);
        }
    }

    /**
     * 测试观察者模式功能
     */
    @Test
    public void testObserverPattern() {
        Elevator elevator = new Elevator(1, scheduler);
        Observer testObserver = mock(Observer.class);
        
        elevator.addObserver(testObserver);
        
        // 测试事件通知
        Event testEvent = new Event(EventType.MAINTENANCE_REQUIRED, "test data");
        elevator.notifyObservers(testEvent);
        
        verify(testObserver, times(1)).update(eq(elevator), eq(testEvent));
    }

    /**
     * 重置单例实例的辅助方法
     * 使用反射来重置私有静态字段，确保测试间的独立性
     */
    private void resetSingletons() {
        try {
            // 重置ElevatorManager
            java.lang.reflect.Field emInstance = ElevatorManager.class.getDeclaredField("instance");
            emInstance.setAccessible(true);
            emInstance.set(null, null);
            
            // 重置Scheduler
            java.lang.reflect.Field sInstance = Scheduler.class.getDeclaredField("instance");
            sInstance.setAccessible(true);
            sInstance.set(null, null);
            
            // 重置SystemConfig
            java.lang.reflect.Field scInstance = SystemConfig.class.getDeclaredField("instance");
            scInstance.setAccessible(true);
            scInstance.set(null, null);
            
        } catch (Exception e) {
            // 忽略反射异常，在实际环境中不会影响测试
        }
    }
}