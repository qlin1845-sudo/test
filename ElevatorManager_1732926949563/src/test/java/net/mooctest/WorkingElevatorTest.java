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
 * 修复后的电梯系统测试类
 * 专注于基本功能验证和核心业务逻辑测试
 */
@RunWith(MockitoJUnitRunner.class)
public class FixedElevatorTest {

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
     * 测试SystemConfig配置管理
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
        
        // 测试下行请求
        PassengerRequest downRequest = new PassengerRequest(10, 3, Priority.MEDIUM, RequestType.DESTINATION_CONTROL);
        assertEquals("方向应该是DOWN", Direction.DOWN, downRequest.getDirection());
        
        // 测试同楼层请求（根据构造函数逻辑，同楼层时方向为DOWN）
        PassengerRequest sameFloorRequest = new PassengerRequest(5, 5, Priority.LOW, RequestType.STANDARD);
        assertEquals("同楼层请求方向应该是DOWN", Direction.DOWN, sameFloorRequest.getDirection());
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
        
        // 验证观察者被通知
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
     * 测试边界条件和异常情况
     */
    @Test
    public void testEdgeCases() {
        // 测试空电梯列表
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        PassengerRequest request = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(new ArrayList<>(), request);
        assertNull("空电梯列表应该返回null", selected);
        
        // 测试极端楼层号
        PassengerRequest extremeRequest = new PassengerRequest(1, 100, Priority.HIGH, RequestType.STANDARD);
        assertEquals("极端楼层请求方向应该是UP", Direction.UP, extremeRequest.getDirection());
        
        extremeRequest = new PassengerRequest(100, 1, Priority.HIGH, RequestType.STANDARD);
        assertEquals("下行极端请求方向应该是DOWN", Direction.DOWN, extremeRequest.getDirection());
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
        assertTrue("应该包含IDLE", Arrays.asList(statuses).contains(ElevatorStatus.IDLE));
        assertTrue("应该包含EMERGENCY", Arrays.asList(statuses).contains(ElevatorStatus.EMERGENCY));
        
        // 测试Priority枚举
        Priority[] priorities = Priority.values();
        assertEquals("Priority应该有3个值", 3, priorities.length);
        assertTrue("应该包含HIGH", Arrays.asList(priorities).contains(Priority.HIGH));
        assertTrue("应该包含MEDIUM", Arrays.asList(priorities).contains(Priority.MEDIUM));
        assertTrue("应该包含LOW", Arrays.asList(priorities).contains(Priority.LOW));
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
     * 重置单例实例的辅助方法
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