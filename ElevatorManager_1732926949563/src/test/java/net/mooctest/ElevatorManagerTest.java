package net.mooctest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/*
 * 测试代码基于JUnit 4，若eclipse提示未找到Junit 5的测试用例，请在Run Configurations中设置Test Runner为Junit 4。请不要使用Junit 5
 * 语法编写测试代码
 */

public class ElevatorManagerTest {

    // ==================== 枚举类测试 ====================
    
    /**
     * 测试Direction枚举 - 验证所有方向值
     */
    @Test(timeout = 4000)
    public void testDirectionEnum() {
        // 测试Direction枚举的所有值
        Direction[] directions = Direction.values();
        assertEquals(2, directions.length);
        assertEquals(Direction.UP, Direction.valueOf("UP"));
        assertEquals(Direction.DOWN, Direction.valueOf("DOWN"));
        assertNotEquals(Direction.UP, Direction.DOWN);
    }
    
    /**
     * 测试ElevatorStatus枚举 - 验证所有电梯状态
     */
    @Test(timeout = 4000)
    public void testElevatorStatusEnum() {
        // 测试ElevatorStatus枚举的所有值
        ElevatorStatus[] statuses = ElevatorStatus.values();
        assertEquals(6, statuses.length);
        assertEquals(ElevatorStatus.MOVING, ElevatorStatus.valueOf("MOVING"));
        assertEquals(ElevatorStatus.STOPPED, ElevatorStatus.valueOf("STOPPED"));
        assertEquals(ElevatorStatus.IDLE, ElevatorStatus.valueOf("IDLE"));
        assertEquals(ElevatorStatus.MAINTENANCE, ElevatorStatus.valueOf("MAINTENANCE"));
        assertEquals(ElevatorStatus.EMERGENCY, ElevatorStatus.valueOf("EMERGENCY"));
        assertEquals(ElevatorStatus.FAULT, ElevatorStatus.valueOf("FAULT"));
    }
    
    /**
     * 测试Priority枚举 - 验证所有优先级
     */
    @Test(timeout = 4000)
    public void testPriorityEnum() {
        // 测试Priority枚举的所有值
        Priority[] priorities = Priority.values();
        assertEquals(3, priorities.length);
        assertEquals(Priority.HIGH, Priority.valueOf("HIGH"));
        assertEquals(Priority.MEDIUM, Priority.valueOf("MEDIUM"));
        assertEquals(Priority.LOW, Priority.valueOf("LOW"));
    }
    
    /**
     * 测试RequestType枚举 - 验证所有请求类型
     */
    @Test(timeout = 4000)
    public void testRequestTypeEnum() {
        // 测试RequestType枚举的所有值
        RequestType[] types = RequestType.values();
        assertEquals(2, types.length);
        assertEquals(RequestType.STANDARD, RequestType.valueOf("STANDARD"));
        assertEquals(RequestType.DESTINATION_CONTROL, RequestType.valueOf("DESTINATION_CONTROL"));
    }
    
    /**
     * 测试SpecialNeeds枚举 - 验证所有特殊需求类型
     */
    @Test(timeout = 4000)
    public void testSpecialNeedsEnum() {
        // 测试SpecialNeeds枚举的所有值
        SpecialNeeds[] needs = SpecialNeeds.values();
        assertEquals(4, needs.length);
        assertEquals(SpecialNeeds.NONE, SpecialNeeds.valueOf("NONE"));
        assertEquals(SpecialNeeds.DISABLED_ASSISTANCE, SpecialNeeds.valueOf("DISABLED_ASSISTANCE"));
        assertEquals(SpecialNeeds.LARGE_LUGGAGE, SpecialNeeds.valueOf("LARGE_LUGGAGE"));
        assertEquals(SpecialNeeds.VIP_SERVICE, SpecialNeeds.valueOf("VIP_SERVICE"));
    }
    
    /**
     * 测试ElevatorMode枚举 - 验证所有电梯模式
     */
    @Test(timeout = 4000)
    public void testElevatorModeEnum() {
        // 测试ElevatorMode枚举的所有值
        ElevatorMode[] modes = ElevatorMode.values();
        assertEquals(3, modes.length);
        assertEquals(ElevatorMode.NORMAL, ElevatorMode.valueOf("NORMAL"));
        assertEquals(ElevatorMode.ENERGY_SAVING, ElevatorMode.valueOf("ENERGY_SAVING"));
        assertEquals(ElevatorMode.EMERGENCY, ElevatorMode.valueOf("EMERGENCY"));
    }
    
    /**
     * 测试EventType枚举 - 验证所有事件类型
     */
    @Test(timeout = 4000)
    public void testEventTypeEnum() {
        // 测试EventType枚举的所有值
        EventType[] types = EventType.values();
        assertEquals(4, types.length);
        assertEquals(EventType.ELEVATOR_FAULT, EventType.valueOf("ELEVATOR_FAULT"));
        assertEquals(EventType.EMERGENCY, EventType.valueOf("EMERGENCY"));
        assertEquals(EventType.MAINTENANCE_REQUIRED, EventType.valueOf("MAINTENANCE_REQUIRED"));
        assertEquals(EventType.CONFIG_UPDATED, EventType.valueOf("CONFIG_UPDATED"));
    }

    // ==================== PassengerRequest类测试 ====================
    
    /**
     * 测试PassengerRequest构造函数 - 向上请求
     */
    @Test(timeout = 4000)
    public void testPassengerRequestUpDirection() {
        // 测试向上方向的乘客请求
        PassengerRequest request = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        assertEquals(1, request.getStartFloor());
        assertEquals(5, request.getDestinationFloor());
        assertEquals(Direction.UP, request.getDirection());
        assertEquals(Priority.HIGH, request.getPriority());
        assertEquals(RequestType.STANDARD, request.getRequestType());
        assertEquals(SpecialNeeds.NONE, request.getSpecialNeeds());
        assertTrue(request.getTimestamp() > 0);
        assertNotNull(request.toString());
        assertTrue(request.toString().contains("From 1 to 5"));
    }
    
    /**
     * 测试PassengerRequest构造函数 - 向下请求
     */
    @Test(timeout = 4000)
    public void testPassengerRequestDownDirection() {
        // 测试向下方向的乘客请求
        PassengerRequest request = new PassengerRequest(10, 2, Priority.MEDIUM, RequestType.DESTINATION_CONTROL);
        assertEquals(10, request.getStartFloor());
        assertEquals(2, request.getDestinationFloor());
        assertEquals(Direction.DOWN, request.getDirection());
        assertEquals(Priority.MEDIUM, request.getPriority());
        assertEquals(RequestType.DESTINATION_CONTROL, request.getRequestType());
        assertTrue(request.toString().contains("Priority: MEDIUM"));
    }
    
    /**
     * 测试PassengerRequest - 同层请求（边界情况）
     */
    @Test(timeout = 4000)
    public void testPassengerRequestSameFloor() {
        // 测试同一楼层的请求（边界情况）
        PassengerRequest request = new PassengerRequest(5, 5, Priority.LOW, RequestType.STANDARD);
        assertEquals(5, request.getStartFloor());
        assertEquals(5, request.getDestinationFloor());
        assertEquals(Direction.DOWN, request.getDirection()); // 相同楼层会被判定为DOWN
    }

    // ==================== Floor类测试 ====================
    
    /**
     * 测试Floor构造函数和基本功能
     */
    @Test(timeout = 4000)
    public void testFloorConstruction() {
        // 测试楼层对象的创建
        Floor floor = new Floor(5);
        assertEquals(5, floor.getFloorNumber());
    }
    
    /**
     * 测试Floor添加和获取请求 - UP方向
     */
    @Test(timeout = 4000)
    public void testFloorAddAndGetRequestsUp() {
        // 测试向上方向的请求添加和获取
        Floor floor = new Floor(3);
        PassengerRequest request1 = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(3, 10, Priority.LOW, RequestType.STANDARD);
        
        floor.addRequest(request1);
        floor.addRequest(request2);
        
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        assertEquals(2, requests.size());
        
        // 获取后队列应该被清空
        List<PassengerRequest> emptyRequests = floor.getRequests(Direction.UP);
        assertEquals(0, emptyRequests.size());
    }
    
    /**
     * 测试Floor添加和获取请求 - DOWN方向
     */
    @Test(timeout = 4000)
    public void testFloorAddAndGetRequestsDown() {
        // 测试向下方向的请求添加和获取
        Floor floor = new Floor(8);
        PassengerRequest request = new PassengerRequest(8, 2, Priority.MEDIUM, RequestType.STANDARD);
        
        floor.addRequest(request);
        
        List<PassengerRequest> requests = floor.getRequests(Direction.DOWN);
        assertEquals(1, requests.size());
        assertEquals(request, requests.get(0));
    }
    
    /**
     * 测试Floor获取空请求列表
     */
    @Test(timeout = 4000)
    public void testFloorGetEmptyRequests() {
        // 测试获取空的请求列表
        Floor floor = new Floor(1);
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        assertNotNull(requests);
        assertEquals(0, requests.size());
    }

    // ==================== Event类测试 ====================
    
    /**
     * 测试Event构造函数和getter方法
     */
    @Test(timeout = 4000)
    public void testEventConstruction() {
        // 测试事件对象的创建
        String testData = "Test emergency data";
        Event event = new Event(EventType.EMERGENCY, testData);
        
        assertEquals(EventType.EMERGENCY, event.getType());
        assertEquals(testData, event.getData());
    }
    
    /**
     * 测试Event - null数据
     */
    @Test(timeout = 4000)
    public void testEventWithNullData() {
        // 测试空数据的事件
        Event event = new Event(EventType.MAINTENANCE_REQUIRED, null);
        assertEquals(EventType.MAINTENANCE_REQUIRED, event.getType());
        assertNull(event.getData());
    }

    // ==================== EventBus类测试 ====================
    
    /**
     * 测试EventBus单例模式
     */
    @Test(timeout = 4000)
    public void testEventBusSingleton() throws Exception {
        // 测试EventBus单例模式
        EventBus instance1 = EventBus.getInstance();
        EventBus instance2 = EventBus.getInstance();
        assertSame(instance1, instance2);
        
        // 重置单例以测试懒加载
        resetSingleton(EventBus.class);
        EventBus newInstance = EventBus.getInstance();
        assertNotNull(newInstance);
    }
    
    /**
     * 测试EventBus订阅和发布
     */
    @Test(timeout = 4000)
    public void testEventBusSubscribeAndPublish() throws Exception {
        // 测试事件订阅和发布机制
        resetSingleton(EventBus.class);
        EventBus eventBus = EventBus.getInstance();
        
        final boolean[] listenerCalled = {false};
        final EventBus.Event[] receivedEvent = {null};
        
        EventBus.EventListener listener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                listenerCalled[0] = true;
                receivedEvent[0] = event;
            }
        };
        
        eventBus.subscribe(EventType.EMERGENCY, listener);
        
        EventBus.Event testEvent = new EventBus.Event(EventType.EMERGENCY, "Test data");
        eventBus.publish(testEvent);
        
        assertTrue(listenerCalled[0]);
        assertNotNull(receivedEvent[0]);
        assertEquals(EventType.EMERGENCY, receivedEvent[0].getType());
        assertEquals("Test data", receivedEvent[0].getData());
    }
    
    /**
     * 测试EventBus发布无订阅者的事件
     */
    @Test(timeout = 4000)
    public void testEventBusPublishNoSubscribers() throws Exception {
        // 测试发布没有订阅者的事件
        resetSingleton(EventBus.class);
        EventBus eventBus = EventBus.getInstance();
        
        EventBus.Event testEvent = new EventBus.Event(EventType.CONFIG_UPDATED, "No subscribers");
        eventBus.publish(testEvent); // 不应该抛出异常
    }
    
    /**
     * 测试EventBus多个订阅者
     */
    @Test(timeout = 4000)
    public void testEventBusMultipleSubscribers() throws Exception {
        // 测试多个订阅者接收同一事件
        resetSingleton(EventBus.class);
        EventBus eventBus = EventBus.getInstance();
        
        final int[] callCount = {0};
        
        EventBus.EventListener listener1 = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                callCount[0]++;
            }
        };
        
        EventBus.EventListener listener2 = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                callCount[0]++;
            }
        };
        
        eventBus.subscribe(EventType.ELEVATOR_FAULT, listener1);
        eventBus.subscribe(EventType.ELEVATOR_FAULT, listener2);
        
        EventBus.Event testEvent = new EventBus.Event(EventType.ELEVATOR_FAULT, "Fault");
        eventBus.publish(testEvent);
        
        assertEquals(2, callCount[0]);
    }
    
    /**
     * 测试EventBus内部Event类
     */
    @Test(timeout = 4000)
    public void testEventBusInnerEventClass() {
        // 测试EventBus内部的Event类
        EventBus.Event event = new EventBus.Event(EventType.EMERGENCY, "Data");
        assertEquals(EventType.EMERGENCY, event.getType());
        assertEquals("Data", event.getData());
    }

    // ==================== SystemConfig类测试 ====================
    
    /**
     * 测试SystemConfig单例模式
     */
    @Test(timeout = 4000)
    public void testSystemConfigSingleton() throws Exception {
        // 测试SystemConfig单例模式
        SystemConfig config1 = SystemConfig.getInstance();
        SystemConfig config2 = SystemConfig.getInstance();
        assertSame(config1, config2);
    }
    
    /**
     * 测试SystemConfig默认值
     */
    @Test(timeout = 4000)
    public void testSystemConfigDefaults() throws Exception {
        // 测试系统配置的默认值
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        assertEquals(20, config.getFloorCount());
        assertEquals(4, config.getElevatorCount());
        assertEquals(800.0, config.getMaxLoad(), 0.01);
    }
    
    /**
     * 测试SystemConfig设置楼层数 - 有效值
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetFloorCountValid() throws Exception {
        // 测试设置有效的楼层数
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        config.setFloorCount(30);
        assertEquals(30, config.getFloorCount());
    }
    
    /**
     * 测试SystemConfig设置楼层数 - 无效值（零）
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetFloorCountInvalidZero() throws Exception {
        // 测试设置无效的楼层数（零）
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        int originalCount = config.getFloorCount();
        config.setFloorCount(0);
        assertEquals(originalCount, config.getFloorCount()); // 应该保持不变
    }
    
    /**
     * 测试SystemConfig设置楼层数 - 无效值（负数）
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetFloorCountInvalidNegative() throws Exception {
        // 测试设置无效的楼层数（负数）
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        int originalCount = config.getFloorCount();
        config.setFloorCount(-5);
        assertEquals(originalCount, config.getFloorCount()); // 应该保持不变
    }
    
    /**
     * 测试SystemConfig设置电梯数 - 有效值
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetElevatorCountValid() throws Exception {
        // 测试设置有效的电梯数
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        config.setElevatorCount(8);
        assertEquals(8, config.getElevatorCount());
    }
    
    /**
     * 测试SystemConfig设置电梯数 - 无效值
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetElevatorCountInvalid() throws Exception {
        // 测试设置无效的电梯数
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        int originalCount = config.getElevatorCount();
        config.setElevatorCount(0);
        assertEquals(originalCount, config.getElevatorCount());
        
        config.setElevatorCount(-3);
        assertEquals(originalCount, config.getElevatorCount());
    }
    
    /**
     * 测试SystemConfig设置最大负载 - 有效值
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetMaxLoadValid() throws Exception {
        // 测试设置有效的最大负载
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        config.setMaxLoad(1000.0);
        assertEquals(1000.0, config.getMaxLoad(), 0.01);
    }
    
    /**
     * 测试SystemConfig设置最大负载 - 无效值
     */
    @Test(timeout = 4000)
    public void testSystemConfigSetMaxLoadInvalid() throws Exception {
        // 测试设置无效的最大负载
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        double originalLoad = config.getMaxLoad();
        config.setMaxLoad(0.0);
        assertEquals(originalLoad, config.getMaxLoad(), 0.01);
        
        config.setMaxLoad(-100.0);
        assertEquals(originalLoad, config.getMaxLoad(), 0.01);
    }

    // ==================== ElevatorManager类测试 ====================
    
    /**
     * 测试ElevatorManager单例模式
     */
    @Test(timeout = 4000)
    public void testElevatorManagerSingleton() throws Exception {
        // 测试ElevatorManager单例模式
        resetSingleton(ElevatorManager.class);
        ElevatorManager manager1 = ElevatorManager.getInstance();
        ElevatorManager manager2 = ElevatorManager.getInstance();
        assertSame(manager1, manager2);
    }
    
    /**
     * 测试ElevatorManager注册电梯
     */
    @Test(timeout = 4000)
    public void testElevatorManagerRegisterElevator() throws Exception {
        // 测试注册电梯
        resetSingleton(ElevatorManager.class);
        resetSingleton(Scheduler.class);
        ElevatorManager manager = ElevatorManager.getInstance();
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        manager.registerElevator(elevator);
        
        Elevator retrieved = manager.getElevatorById(1);
        assertSame(elevator, retrieved);
    }
    
    /**
     * 测试ElevatorManager获取不存在的电梯
     */
    @Test(timeout = 4000)
    public void testElevatorManagerGetNonExistentElevator() throws Exception {
        // 测试获取不存在的电梯
        resetSingleton(ElevatorManager.class);
        ElevatorManager manager = ElevatorManager.getInstance();
        
        Elevator elevator = manager.getElevatorById(999);
        assertNull(elevator);
    }
    
    /**
     * 测试ElevatorManager获取所有电梯
     */
    @Test(timeout = 4000)
    public void testElevatorManagerGetAllElevators() throws Exception {
        // 测试获取所有电梯
        resetSingleton(ElevatorManager.class);
        resetSingleton(Scheduler.class);
        ElevatorManager manager = ElevatorManager.getInstance();
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator1 = new Elevator(1, mockScheduler);
        Elevator elevator2 = new Elevator(2, mockScheduler);
        
        manager.registerElevator(elevator1);
        manager.registerElevator(elevator2);
        
        Collection<Elevator> elevators = manager.getAllElevators();
        assertEquals(2, elevators.size());
        assertTrue(elevators.contains(elevator1));
        assertTrue(elevators.contains(elevator2));
    }
    
    /**
     * 测试ElevatorManager构造函数
     */
    @Test(timeout = 4000)
    public void testElevatorManagerConstructor() {
        // 测试直接构造ElevatorManager对象
        ElevatorManager manager = new ElevatorManager();
        assertNotNull(manager);
        assertNotNull(manager.getAllElevators());
        assertEquals(0, manager.getAllElevators().size());
    }

    // ==================== Elevator类测试 ====================
    
    /**
     * 测试Elevator构造函数和初始状态
     */
    @Test(timeout = 4000)
    public void testElevatorConstruction() throws Exception {
        // 测试电梯对象的创建和初始状态
        resetSingleton(SystemConfig.class);
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        assertEquals(1, elevator.getId());
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals(0.0, elevator.getEnergyConsumption(), 0.01);
        assertEquals(ElevatorMode.NORMAL, elevator.getMode());
        assertEquals(0.0, elevator.getCurrentLoad(), 0.01);
        assertTrue(elevator.getMaxLoad() > 0);
        assertNotNull(elevator.getPassengerList());
        assertEquals(0, elevator.getPassengerList().size());
        assertNotNull(elevator.getDestinationSet());
        assertNotNull(elevator.getLock());
        assertNotNull(elevator.getCondition());
        assertNotNull(elevator.getScheduler());
        assertNotNull(elevator.getObservers());
    }
    
    /**
     * 测试Elevator设置和获取当前楼层
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetCurrentFloor() {
        // 测试设置和获取当前楼层
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(5);
        assertEquals(5, elevator.getCurrentFloor());
        
        elevator.setCurrentFloor(10);
        assertEquals(10, elevator.getCurrentFloor());
    }
    
    /**
     * 测试Elevator设置和获取方向
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetDirection() {
        // 测试设置和获取电梯方向
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setDirection(Direction.DOWN);
        assertEquals(Direction.DOWN, elevator.getDirection());
        
        elevator.setDirection(Direction.UP);
        assertEquals(Direction.UP, elevator.getDirection());
    }
    
    /**
     * 测试Elevator设置和获取状态
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetStatus() {
        // 测试设置和获取电梯状态
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setStatus(ElevatorStatus.MOVING);
        assertEquals(ElevatorStatus.MOVING, elevator.getStatus());
        
        elevator.setStatus(ElevatorStatus.MAINTENANCE);
        assertEquals(ElevatorStatus.MAINTENANCE, elevator.getStatus());
    }
    
    /**
     * 测试Elevator设置和获取能量消耗
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetEnergyConsumption() {
        // 测试设置和获取能量消耗
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setEnergyConsumption(50.5);
        assertEquals(50.5, elevator.getEnergyConsumption(), 0.01);
    }
    
    /**
     * 测试Elevator设置和获取当前负载
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetCurrentLoad() {
        // 测试设置和获取当前负载
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentLoad(350.0);
        assertEquals(350.0, elevator.getCurrentLoad(), 0.01);
    }
    
    /**
     * 测试Elevator设置和获取模式
     */
    @Test(timeout = 4000)
    public void testElevatorSetAndGetMode() {
        // 测试设置和获取电梯模式
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setMode(ElevatorMode.ENERGY_SAVING);
        assertEquals(ElevatorMode.ENERGY_SAVING, elevator.getMode());
        
        elevator.setMode(ElevatorMode.EMERGENCY);
        assertEquals(ElevatorMode.EMERGENCY, elevator.getMode());
    }
    
    /**
     * 测试Elevator添加目的地
     */
    @Test(timeout = 4000)
    public void testElevatorAddDestination() {
        // 测试添加目的地楼层
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.addDestination(5);
        assertTrue(elevator.getDestinationSet().contains(5));
        
        elevator.addDestination(10);
        assertTrue(elevator.getDestinationSet().contains(10));
        assertEquals(2, elevator.getDestinationSet().size());
    }
    
    /**
     * 测试Elevator更新方向 - 向上
     */
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionUp() {
        // 测试更新方向为向上
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(3);
        elevator.addDestination(8);
        elevator.updateDirection();
        
        assertEquals(Direction.UP, elevator.getDirection());
    }
    
    /**
     * 测试Elevator更新方向 - 向下
     */
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionDown() {
        // 测试更新方向为向下
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(10);
        elevator.addDestination(2);
        elevator.updateDirection();
        
        assertEquals(Direction.DOWN, elevator.getDirection());
    }
    
    /**
     * 测试Elevator更新方向 - 空目的地
     */
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionEmpty() {
        // 测试空目的地时更新方向
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.updateDirection();
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
    }
    
    /**
     * 测试Elevator卸载乘客
     */
    @Test(timeout = 4000)
    public void testElevatorUnloadPassengers() {
        // 测试卸载到达目的地的乘客
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(1, 8, Priority.LOW, RequestType.STANDARD);
        
        elevator.getPassengerList(); // 触发列表创建
        elevator.setCurrentFloor(5);
        
        // 使用反射添加乘客
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(request1);
            passengerList.add(request2);
        } catch (Exception e) {
            fail("反射失败");
        }
        
        elevator.unloadPassengers();
        
        List<PassengerRequest> remaining = elevator.getPassengerList();
        assertEquals(1, remaining.size());
        assertEquals(8, remaining.get(0).getDestinationFloor());
    }
    
    /**
     * 测试Elevator装载乘客
     */
    @Test(timeout = 4000)
    public void testElevatorLoadPassengers() {
        // 测试装载乘客
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        elevator.setDirection(Direction.UP);
        
        elevator.loadPassengers();
        
        verify(mockScheduler).getRequestsAtFloor(3, Direction.UP);
    }
    
    /**
     * 测试Elevator装载乘客 - 超载限制
     */
    @Test(timeout = 4000)
    public void testElevatorLoadPassengersOverload() {
        // 测试超载限制
        Scheduler mockScheduler = mock(Scheduler.class);
        List<PassengerRequest> requests = new ArrayList<>();
        // 添加多个请求以测试负载限制
        for (int i = 0; i < 20; i++) {
            requests.add(new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD));
        }
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(requests);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(1);
        elevator.setDirection(Direction.UP);
        
        elevator.loadPassengers();
        
        // 应该受到最大负载限制
        assertTrue(elevator.getCurrentLoad() <= elevator.getMaxLoad());
    }
    
    /**
     * 测试Elevator清除所有请求
     */
    @Test(timeout = 4000)
    public void testElevatorClearAllRequests() {
        // 测试清除所有请求
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.addDestination(5);
        elevator.addDestination(10);
        
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD));
        } catch (Exception e) {
            fail("反射失败");
        }
        
        List<PassengerRequest> cleared = elevator.clearAllRequests();
        
        assertNotNull(cleared);
        assertEquals(0, elevator.getPassengerList().size());
        assertEquals(0, elevator.getDestinationSet().size());
    }
    
    /**
     * 测试Elevator处理紧急情况
     */
    @Test(timeout = 4000)
    public void testElevatorHandleEmergency() {
        // 测试处理紧急情况
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.addDestination(5);
        elevator.addDestination(10);
        
        elevator.handleEmergency();
        
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertTrue(elevator.getDestinationSet().contains(1)); // 应该包含1楼
        assertEquals(0, elevator.getPassengerList().size());
    }
    
    /**
     * 测试Elevator添加观察者
     */
    @Test(timeout = 4000)
    public void testElevatorAddObserver() {
        // 测试添加观察者
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        Observer mockObserver = mock(Observer.class);
        elevator.addObserver(mockObserver);
        
        assertEquals(1, elevator.getObservers().size());
        assertTrue(elevator.getObservers().contains(mockObserver));
    }
    
    /**
     * 测试Elevator通知观察者
     */
    @Test(timeout = 4000)
    public void testElevatorNotifyObservers() {
        // 测试通知观察者
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        Observer mockObserver = mock(Observer.class);
        elevator.addObserver(mockObserver);
        
        Event testEvent = new Event(EventType.EMERGENCY, "Test");
        elevator.notifyObservers(testEvent);
        
        verify(mockObserver).update(elevator, testEvent);
    }

    // ==================== Scheduler类测试 ====================
    
    /**
     * 测试Scheduler构造函数
     */
    @Test(timeout = 4000)
    public void testSchedulerConstruction() throws Exception {
        // 测试调度器的创建
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy = new NearestElevatorStrategy();
        
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        assertNotNull(scheduler);
    }
    
    /**
     * 测试Scheduler单例模式 - 带参数
     */
    @Test(timeout = 4000)
    public void testSchedulerSingletonWithParams() throws Exception {
        // 测试带参数的单例获取
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy = new NearestElevatorStrategy();
        
        Scheduler scheduler1 = Scheduler.getInstance(elevators, 10, strategy);
        Scheduler scheduler2 = Scheduler.getInstance(elevators, 10, strategy);
        
        assertSame(scheduler1, scheduler2);
    }
    
    /**
     * 测试Scheduler单例模式 - 无参数
     */
    @Test(timeout = 4000)
    public void testSchedulerSingletonNoParams() throws Exception {
        // 测试无参数的单例获取
        resetSingleton(Scheduler.class);
        Scheduler scheduler = Scheduler.getInstance();
        assertNotNull(scheduler);
    }
    
    /**
     * 测试Scheduler提交高优先级请求
     */
    @Test(timeout = 4000)
    public void testSchedulerSubmitHighPriorityRequest() throws Exception {
        // 测试提交高优先级请求
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        assertTrue(elevator.getDestinationSet().contains(3));
    }
    
    /**
     * 测试Scheduler提交普通优先级请求
     */
    @Test(timeout = 4000)
    public void testSchedulerSubmitNormalPriorityRequest() throws Exception {
        // 测试提交普通优先级请求
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        assertTrue(elevator.getDestinationSet().contains(3));
    }
    
    /**
     * 测试Scheduler获取楼层请求
     */
    @Test(timeout = 4000)
    public void testSchedulerGetRequestsAtFloor() throws Exception {
        // 测试获取指定楼层的请求
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        List<PassengerRequest> requests = scheduler.getRequestsAtFloor(3, Direction.UP);
        assertNotNull(requests);
    }
    
    /**
     * 测试Scheduler更新分派策略
     */
    @Test(timeout = 4000)
    public void testSchedulerSetDispatchStrategy() throws Exception {
        // 测试更新分派策略
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy1 = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy1);
        
        DispatchStrategy strategy2 = new HighEfficiencyStrategy();
        scheduler.setDispatchStrategy(strategy2);
        // 策略已更新，不会抛出异常
    }
    
    /**
     * 测试Scheduler调度电梯 - 无可用电梯
     */
    @Test(timeout = 4000)
    public void testSchedulerDispatchElevatorNoAvailable() throws Exception {
        // 测试无可用电梯时的调度
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        scheduler.dispatchElevator(request); // 不应抛出异常
    }
    
    /**
     * 测试Scheduler重新分配请求
     */
    @Test(timeout = 4000)
    public void testSchedulerRedistributeRequests() throws Exception {
        // 测试重新分配故障电梯的请求
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator1 = new Elevator(1, mockScheduler);
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        elevator1.addDestination(5);
        scheduler.redistributeRequests(elevator1);
        
        assertEquals(0, elevator1.getDestinationSet().size());
    }
    
    /**
     * 测试Scheduler执行紧急协议
     */
    @Test(timeout = 4000)
    public void testSchedulerExecuteEmergencyProtocol() throws Exception {
        // 测试执行紧急协议
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator1 = new Elevator(1, mockScheduler);
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        scheduler.executeEmergencyProtocol();
        
        assertEquals(ElevatorStatus.EMERGENCY, elevator1.getStatus());
        assertEquals(ElevatorStatus.EMERGENCY, elevator2.getStatus());
    }
    
    /**
     * 测试Scheduler作为Observer更新 - 电梯故障
     */
    @Test(timeout = 4000)
    public void testSchedulerUpdateElevatorFault() throws Exception {
        // 测试接收电梯故障事件
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        Event event = new Event(EventType.ELEVATOR_FAULT, elevator);
        scheduler.update(elevator, event);
        
        assertEquals(0, elevator.getDestinationSet().size());
    }
    
    /**
     * 测试Scheduler作为Observer更新 - 紧急事件
     */
    @Test(timeout = 4000)
    public void testSchedulerUpdateEmergency() throws Exception {
        // 测试接收紧急事件
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        Event event = new Event(EventType.EMERGENCY, "Emergency situation");
        scheduler.update(elevator, event);
        
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
    }

    // ==================== 策略模式测试 ====================
    
    /**
     * 测试NearestElevatorStrategy - 选择最近的空闲电梯
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyIdle() {
        // 测试选择最近的空闲电梯
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setCurrentFloor(1);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setCurrentFloor(10);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertSame(elevator1, selected);
    }
    
    /**
     * 测试NearestElevatorStrategy - 选择同方向的电梯
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategySameDirection() {
        // 测试选择同方向的电梯
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(2);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertSame(elevator, selected);
    }
    
    /**
     * 测试NearestElevatorStrategy - 无合适电梯
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyNoEligible() {
        // 测试无合适电梯的情况
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.MAINTENANCE);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试NearestElevatorStrategy - isEligible方法
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyIsEligible() {
        // 测试isEligible方法的各种情况
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setStatus(ElevatorStatus.IDLE);
        PassengerRequest request = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        assertTrue(strategy.isEligible(elevator1, request));
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setStatus(ElevatorStatus.MOVING);
        elevator2.setDirection(Direction.UP);
        assertTrue(strategy.isEligible(elevator2, request));
        
        Elevator elevator3 = new Elevator(3, mockScheduler);
        elevator3.setStatus(ElevatorStatus.MOVING);
        elevator3.setDirection(Direction.DOWN);
        PassengerRequest downRequest = new PassengerRequest(8, 3, Priority.HIGH, RequestType.STANDARD);
        assertTrue(strategy.isEligible(elevator3, downRequest));
        
        Elevator elevator4 = new Elevator(4, mockScheduler);
        elevator4.setStatus(ElevatorStatus.MAINTENANCE);
        assertFalse(strategy.isEligible(elevator4, request));
    }
    
    /**
     * 测试HighEfficiencyStrategy - 选择最近的电梯
     */
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategy() {
        // 测试高效策略选择最近的电梯
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setCurrentFloor(1);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setCurrentFloor(8);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
    }
    
    /**
     * 测试HighEfficiencyStrategy - isCloser方法
     */
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyIsCloser() {
        // 测试isCloser方法
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setCurrentFloor(3);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setCurrentFloor(10);
        
        PassengerRequest request = new PassengerRequest(5, 8, Priority.HIGH, RequestType.STANDARD);
        
        assertTrue(strategy.isCloser(elevator1, elevator2, request));
        assertFalse(strategy.isCloser(elevator2, elevator1, request));
    }
    
    /**
     * 测试HighEfficiencyStrategy - 同方向优先
     */
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategySameDirection() {
        // 测试同方向的电梯优先
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertSame(elevator, selected);
    }
    
    /**
     * 测试HighEfficiencyStrategy - 空列表
     */
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyEmptyList() {
        // 测试空电梯列表
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试EnergySavingStrategy - 选择空闲电梯
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategyIdle() {
        // 测试选择空闲电梯
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setStatus(ElevatorStatus.MOVING);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertSame(elevator2, selected);
    }
    
    /**
     * 测试EnergySavingStrategy - 选择同方向且距离近的电梯
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategySameDirectionNear() {
        // 测试选择同方向且距离近的电梯
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertSame(elevator, selected);
    }
    
    /**
     * 测试EnergySavingStrategy - 无合适电梯（距离远）
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategyNoSuitableFar() {
        // 测试无合适电梯（距离太远）
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(1);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试EnergySavingStrategy - 无合适电梯（方向不同）
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategyNoSuitableDirection() {
        // 测试无合适电梯（方向不同）
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.DOWN);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试PredictiveSchedulingStrategy - 选择最佳电梯
     */
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategy() {
        // 测试预测调度策略选择最佳电梯
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setCurrentFloor(1);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setCurrentFloor(5);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertSame(elevator2, selected); // elevator2距离更近
    }
    
    /**
     * 测试PredictiveSchedulingStrategy - calculatePredictedCost方法
     */
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyCalculateCost() {
        // 测试预测成本计算
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        
        PassengerRequest request = new PassengerRequest(8, 12, Priority.HIGH, RequestType.STANDARD);
        double cost = strategy.calculatePredictedCost(elevator, request);
        
        assertTrue(cost > 0);
        assertEquals(5.0, cost, 0.1); // 距离5
    }
    
    /**
     * 测试PredictiveSchedulingStrategy - 带负载的成本计算
     */
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyWithLoad() {
        // 测试带负载的预测成本
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setCurrentLoad(400); // 增加负载
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        double cost = strategy.calculatePredictedCost(elevator, request);
        
        assertTrue(cost >= 0);
    }
    
    /**
     * 测试PredictiveSchedulingStrategy - 空列表
     */
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyEmptyList() {
        // 测试空电梯列表
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }

    // ==================== LogManager类测试 ====================
    
    /**
     * 测试LogManager单例模式
     */
    @Test(timeout = 4000)
    public void testLogManagerSingleton() throws Exception {
        // 测试LogManager单例模式
        resetSingleton(LogManager.class);
        LogManager manager1 = LogManager.getInstance();
        LogManager manager2 = LogManager.getInstance();
        assertSame(manager1, manager2);
    }
    
    /**
     * 测试LogManager记录电梯事件
     */
    @Test(timeout = 4000)
    public void testLogManagerRecordElevatorEvent() throws Exception {
        // 测试记录电梯事件
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        logManager.recordElevatorEvent(1, "Elevator started");
        logManager.recordElevatorEvent(2, "Elevator stopped");
        
        // 验证日志已记录（通过查询验证）
        long now = System.currentTimeMillis();
        List<LogManager.SystemLog> logs = logManager.queryLogs("Elevator 1", 0, now);
        assertNotNull(logs);
    }
    
    /**
     * 测试LogManager记录调度器事件
     */
    @Test(timeout = 4000)
    public void testLogManagerRecordSchedulerEvent() throws Exception {
        // 测试记录调度器事件
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        logManager.recordSchedulerEvent("Request dispatched");
        
        long now = System.currentTimeMillis();
        List<LogManager.SystemLog> logs = logManager.queryLogs("Scheduler", 0, now);
        assertNotNull(logs);
    }
    
    /**
     * 测试LogManager记录通用事件
     */
    @Test(timeout = 4000)
    public void testLogManagerRecordEvent() throws Exception {
        // 测试记录通用事件
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        logManager.recordEvent("TestSource", "Test message");
        
        long now = System.currentTimeMillis();
        List<LogManager.SystemLog> logs = logManager.queryLogs("TestSource", 0, now);
        assertEquals(1, logs.size());
        assertEquals("TestSource", logs.get(0).getSource());
        assertEquals("Test message", logs.get(0).getMessage());
    }
    
    /**
     * 测试LogManager查询日志 - 无匹配
     */
    @Test(timeout = 4000)
    public void testLogManagerQueryLogsNoMatch() throws Exception {
        // 测试查询无匹配的日志
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        List<LogManager.SystemLog> logs = logManager.queryLogs("NonExistent", 0, System.currentTimeMillis());
        assertNotNull(logs);
        assertEquals(0, logs.size());
    }
    
    /**
     * 测试LogManager查询日志 - 时间范围过滤
     */
    @Test(timeout = 4000)
    public void testLogManagerQueryLogsTimeRange() throws Exception {
        // 测试时间范围过滤
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        long startTime = System.currentTimeMillis();
        logManager.recordEvent("Test", "Message 1");
        Thread.sleep(10);
        long midTime = System.currentTimeMillis();
        Thread.sleep(10);
        logManager.recordEvent("Test", "Message 2");
        long endTime = System.currentTimeMillis();
        
        List<LogManager.SystemLog> logs1 = logManager.queryLogs("Test", startTime, midTime);
        List<LogManager.SystemLog> logs2 = logManager.queryLogs("Test", midTime, endTime);
        
        assertNotNull(logs1);
        assertNotNull(logs2);
    }
    
    /**
     * 测试LogManager.SystemLog内部类
     */
    @Test(timeout = 4000)
    public void testSystemLogClass() {
        // 测试SystemLog内部类
        long timestamp = System.currentTimeMillis();
        LogManager.SystemLog log = new LogManager.SystemLog("Source", "Message", timestamp);
        
        assertEquals("Source", log.getSource());
        assertEquals("Message", log.getMessage());
        assertEquals(timestamp, log.getTimestamp());
    }

    // ==================== AnalyticsEngine类测试 ====================
    
    /**
     * 测试AnalyticsEngine单例模式
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineSingleton() throws Exception {
        // 测试AnalyticsEngine单例模式
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine1 = AnalyticsEngine.getInstance();
        AnalyticsEngine engine2 = AnalyticsEngine.getInstance();
        assertSame(engine1, engine2);
    }
    
    /**
     * 测试AnalyticsEngine处理状态报告
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineProcessStatusReport() throws Exception {
        // 测试处理电梯状态报告
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        ElevatorStatusReport report = new ElevatorStatusReport(1, 5, Direction.UP, ElevatorStatus.MOVING, 2.0, 350.0, 5);
        engine.processStatusReport(report);
        
        // 验证报告已处理（不抛出异常）
    }
    
    /**
     * 测试AnalyticsEngine更新楼层乘客数
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineUpdateFloorPassengerCount() throws Exception {
        // 测试更新楼层乘客数
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        engine.updateFloorPassengerCount(1, 10);
        engine.updateFloorPassengerCount(2, 15);
        engine.updateFloorPassengerCount(3, 8);
        
        // 验证更新成功（不抛出异常）
    }
    
    /**
     * 测试AnalyticsEngine判断高峰时段 - 是高峰
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineIsPeakHoursTrue() throws Exception {
        // 测试高峰时段判断（是）
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        // 添加足够多的乘客以触发高峰判断
        engine.updateFloorPassengerCount(1, 20);
        engine.updateFloorPassengerCount(2, 20);
        engine.updateFloorPassengerCount(3, 15);
        
        assertTrue(engine.isPeakHours());
    }
    
    /**
     * 测试AnalyticsEngine判断高峰时段 - 不是高峰
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineIsPeakHoursFalse() throws Exception {
        // 测试高峰时段判断（否）
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        engine.updateFloorPassengerCount(1, 5);
        engine.updateFloorPassengerCount(2, 3);
        
        assertFalse(engine.isPeakHours());
    }
    
    /**
     * 测试AnalyticsEngine生成性能报告
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineGeneratePerformanceReport() throws Exception {
        // 测试生成性能报告
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        AnalyticsEngine.Report report = engine.generatePerformanceReport();
        
        assertNotNull(report);
        assertEquals("System Performance Report", report.getTitle());
        assertTrue(report.getGeneratedTime() > 0);
    }
    
    /**
     * 测试AnalyticsEngine.Report内部类
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineReportClass() {
        // 测试Report内部类
        long timestamp = System.currentTimeMillis();
        AnalyticsEngine.Report report = new AnalyticsEngine.Report("Test Report", timestamp);
        
        assertEquals("Test Report", report.getTitle());
        assertEquals(timestamp, report.getGeneratedTime());
    }

    // ==================== ElevatorStatusReport类测试 ====================
    
    /**
     * 测试ElevatorStatusReport构造函数和getter方法
     */
    @Test(timeout = 4000)
    public void testElevatorStatusReport() {
        // 测试电梯状态报告的创建和访问
        ElevatorStatusReport report = new ElevatorStatusReport(1, 5, Direction.UP, ElevatorStatus.MOVING, 2.5, 420.0, 6);
        
        assertEquals(1, report.getElevatorId());
        assertEquals(5, report.getCurrentFloor());
        assertEquals(Direction.UP, report.getDirection());
        assertEquals(ElevatorStatus.MOVING, report.getStatus());
        assertEquals(2.5, report.getSpeed(), 0.01);
        assertEquals(420.0, report.getCurrentLoad(), 0.01);
        assertEquals(6, report.getPassengerCount());
    }
    
    /**
     * 测试ElevatorStatusReport的toString方法
     */
    @Test(timeout = 4000)
    public void testElevatorStatusReportToString() {
        // 测试toString方法
        ElevatorStatusReport report = new ElevatorStatusReport(2, 10, Direction.DOWN, ElevatorStatus.STOPPED, 0.0, 280.0, 4);
        
        String str = report.toString();
        assertNotNull(str);
        assertTrue(str.contains("elevatorId=2"));
        assertTrue(str.contains("currentFloor=10"));
        assertTrue(str.contains("direction=DOWN"));
        assertTrue(str.contains("status=STOPPED"));
    }

    // ==================== NotificationService类测试 ====================
    
    /**
     * 测试NotificationService单例模式
     */
    @Test(timeout = 4000)
    public void testNotificationServiceSingleton() throws Exception {
        // 测试NotificationService单例模式
        resetSingleton(NotificationService.class);
        NotificationService service1 = NotificationService.getInstance();
        NotificationService service2 = NotificationService.getInstance();
        assertSame(service1, service2);
    }
    
    /**
     * 测试NotificationService发送紧急通知
     */
    @Test(timeout = 4000)
    public void testNotificationServiceSendEmergency() throws Exception {
        // 测试发送紧急通知
        resetSingleton(NotificationService.class);
        NotificationService service = NotificationService.getInstance();
        
        List<String> recipients = Arrays.asList("admin@test.com", "security@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.EMERGENCY,
            "Emergency situation",
            recipients
        );
        
        service.sendNotification(notification); // 不应抛出异常
    }
    
    /**
     * 测试NotificationService发送维护通知
     */
    @Test(timeout = 4000)
    public void testNotificationServiceSendMaintenance() throws Exception {
        // 测试发送维护通知
        resetSingleton(NotificationService.class);
        NotificationService service = NotificationService.getInstance();
        
        List<String> recipients = Arrays.asList("maintenance@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.MAINTENANCE,
            "Maintenance required",
            recipients
        );
        
        service.sendNotification(notification);
    }
    
    /**
     * 测试NotificationService发送系统更新通知
     */
    @Test(timeout = 4000)
    public void testNotificationServiceSendSystemUpdate() throws Exception {
        // 测试发送系统更新通知
        resetSingleton(NotificationService.class);
        NotificationService service = NotificationService.getInstance();
        
        List<String> recipients = Arrays.asList("users@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.SYSTEM_UPDATE,
            "System update available",
            recipients
        );
        
        service.sendNotification(notification);
    }
    
    /**
     * 测试NotificationService发送信息通知
     */
    @Test(timeout = 4000)
    public void testNotificationServiceSendInformation() throws Exception {
        // 测试发送信息通知
        resetSingleton(NotificationService.class);
        NotificationService service = NotificationService.getInstance();
        
        List<String> recipients = Arrays.asList("info@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.INFORMATION,
            "Information message",
            recipients
        );
        
        service.sendNotification(notification);
    }
    
    /**
     * 测试NotificationService.Notification类
     */
    @Test(timeout = 4000)
    public void testNotificationClass() {
        // 测试Notification类
        List<String> recipients = Arrays.asList("test@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.EMERGENCY,
            "Test message",
            recipients
        );
        
        assertEquals(NotificationService.NotificationType.EMERGENCY, notification.getType());
        assertEquals("Test message", notification.getMessage());
        assertEquals(recipients, notification.getRecipients());
    }
    
    /**
     * 测试NotificationService.NotificationType枚举
     */
    @Test(timeout = 4000)
    public void testNotificationTypeEnum() {
        // 测试NotificationType枚举
        NotificationService.NotificationType[] types = NotificationService.NotificationType.values();
        assertEquals(4, types.length);
        assertEquals(NotificationService.NotificationType.EMERGENCY, NotificationService.NotificationType.valueOf("EMERGENCY"));
        assertEquals(NotificationService.NotificationType.MAINTENANCE, NotificationService.NotificationType.valueOf("MAINTENANCE"));
        assertEquals(NotificationService.NotificationType.SYSTEM_UPDATE, NotificationService.NotificationType.valueOf("SYSTEM_UPDATE"));
        assertEquals(NotificationService.NotificationType.INFORMATION, NotificationService.NotificationType.valueOf("INFORMATION"));
    }
    
    /**
     * 测试SMSChannel支持的通知类型
     */
    @Test(timeout = 4000)
    public void testSMSChannelSupports() throws Exception {
        // 测试SMS通道支持的通知类型
        NotificationService.SMSChannel channel = new NotificationService.SMSChannel();
        
        assertTrue(channel.supports(NotificationService.NotificationType.EMERGENCY));
        assertTrue(channel.supports(NotificationService.NotificationType.MAINTENANCE));
        assertFalse(channel.supports(NotificationService.NotificationType.SYSTEM_UPDATE));
        assertFalse(channel.supports(NotificationService.NotificationType.INFORMATION));
    }
    
    /**
     * 测试SMSChannel发送通知
     */
    @Test(timeout = 4000)
    public void testSMSChannelSend() {
        // 测试SMS通道发送通知
        NotificationService.SMSChannel channel = new NotificationService.SMSChannel();
        
        List<String> recipients = Arrays.asList("123456789");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.EMERGENCY,
            "Emergency",
            recipients
        );
        
        channel.send(notification); // 不应抛出异常
    }
    
    /**
     * 测试EmailChannel支持所有通知类型
     */
    @Test(timeout = 4000)
    public void testEmailChannelSupports() {
        // 测试Email通道支持所有类型
        NotificationService.EmailChannel channel = new NotificationService.EmailChannel();
        
        assertTrue(channel.supports(NotificationService.NotificationType.EMERGENCY));
        assertTrue(channel.supports(NotificationService.NotificationType.MAINTENANCE));
        assertTrue(channel.supports(NotificationService.NotificationType.SYSTEM_UPDATE));
        assertTrue(channel.supports(NotificationService.NotificationType.INFORMATION));
    }
    
    /**
     * 测试EmailChannel发送通知
     */
    @Test(timeout = 4000)
    public void testEmailChannelSend() {
        // 测试Email通道发送通知
        NotificationService.EmailChannel channel = new NotificationService.EmailChannel();
        
        List<String> recipients = Arrays.asList("test@test.com");
        NotificationService.Notification notification = new NotificationService.Notification(
            NotificationService.NotificationType.INFORMATION,
            "Info",
            recipients
        );
        
        channel.send(notification); // 不应抛出异常
    }

    // ==================== ThreadPoolManager类测试 ====================
    
    /**
     * 测试ThreadPoolManager单例模式
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagerSingleton() throws Exception {
        // 测试ThreadPoolManager单例模式
        resetSingleton(ThreadPoolManager.class);
        ThreadPoolManager manager1 = ThreadPoolManager.getInstance();
        ThreadPoolManager manager2 = ThreadPoolManager.getInstance();
        assertSame(manager1, manager2);
    }
    
    /**
     * 测试ThreadPoolManager提交任务
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagerSubmitTask() throws Exception {
        // 测试提交任务到线程池
        resetSingleton(ThreadPoolManager.class);
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        
        final boolean[] taskExecuted = {false};
        Runnable task = new Runnable() {
            @Override
            public void run() {
                taskExecuted[0] = true;
            }
        };
        
        manager.submitTask(task);
        Thread.sleep(100); // 等待任务执行
        
        assertTrue(taskExecuted[0]);
    }
    
    /**
     * 测试ThreadPoolManager构造函数
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagerConstructor() {
        // 测试ThreadPoolManager构造函数
        ThreadPoolManager manager = new ThreadPoolManager();
        assertNotNull(manager);
    }

    // ==================== MaintenanceManager类测试 ====================
    
    /**
     * 测试MaintenanceManager单例模式
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerSingleton() throws Exception {
        // 测试MaintenanceManager单例模式
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager1 = MaintenanceManager.getInstance();
        MaintenanceManager manager2 = MaintenanceManager.getInstance();
        assertSame(manager1, manager2);
    }
    
    /**
     * 测试MaintenanceManager调度维护
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerScheduleMaintenance() throws Exception {
        // 测试调度维护任务
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        manager.scheduleMaintenance(elevator);
        // 验证任务已调度（不抛出异常）
    }
    
    /**
     * 测试MaintenanceManager执行维护
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerPerformMaintenance() throws Exception {
        // 测试执行维护
        resetSingleton(MaintenanceManager.class);
        resetSingleton(LogManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        MaintenanceManager.MaintenanceTask task = new MaintenanceManager.MaintenanceTask(1, System.currentTimeMillis(), "Routine check");
        manager.performMaintenance(task);
        // 验证维护已执行（不抛出异常）
    }
    
    /**
     * 测试MaintenanceManager记录维护结果
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerRecordMaintenanceResult() throws Exception {
        // 测试记录维护结果
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        manager.recordMaintenanceResult(1, "Maintenance completed successfully");
        // 验证结果已记录（不抛出异常）
    }
    
    /**
     * 测试MaintenanceManager通知维护人员
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerNotifyMaintenancePersonnel() throws Exception {
        // 测试通知维护人员
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        MaintenanceManager.MaintenanceTask task = new MaintenanceManager.MaintenanceTask(1, System.currentTimeMillis(), "Urgent repair");
        manager.notifyMaintenancePersonnel(task);
        // 验证通知已发送（不抛出异常）
    }
    
    /**
     * 测试MaintenanceManager处理电梯故障事件
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerOnEvent() throws Exception {
        // 测试处理电梯故障事件
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        EventBus.Event event = new EventBus.Event(EventType.ELEVATOR_FAULT, elevator);
        manager.onEvent(event);
        // 验证事件已处理（不抛出异常）
    }
    
    /**
     * 测试MaintenanceManager处理非故障事件
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerOnEventNonFault() throws Exception {
        // 测试处理非故障事件
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        EventBus.Event event = new EventBus.Event(EventType.CONFIG_UPDATED, "Config");
        manager.onEvent(event); // 不应触发维护调度
    }
    
    /**
     * 测试MaintenanceTask内部类
     */
    @Test(timeout = 4000)
    public void testMaintenanceTaskClass() {
        // 测试MaintenanceTask内部类
        long timestamp = System.currentTimeMillis();
        MaintenanceManager.MaintenanceTask task = new MaintenanceManager.MaintenanceTask(1, timestamp, "Test task");
        
        assertEquals(1, task.getElevatorId());
        assertEquals(timestamp, task.getScheduledTime());
        assertEquals("Test task", task.getDescription());
    }
    
    /**
     * 测试MaintenanceRecord内部类
     */
    @Test(timeout = 4000)
    public void testMaintenanceRecordClass() {
        // 测试MaintenanceRecord内部类
        long timestamp = System.currentTimeMillis();
        MaintenanceManager.MaintenanceRecord record = new MaintenanceManager.MaintenanceRecord(1, timestamp, "Success");
        
        assertEquals(1, record.getElevatorId());
        assertEquals(timestamp, record.getMaintenanceTime());
        assertEquals("Success", record.getResult());
    }

    // ==================== SecurityMonitor类测试 ====================
    
    /**
     * 测试SecurityMonitor单例模式
     */
    @Test(timeout = 4000)
    public void testSecurityMonitorSingleton() throws Exception {
        // 测试SecurityMonitor单例模式
        resetSingleton(SecurityMonitor.class);
        resetSingleton(EventBus.class);
        resetSingleton(LogManager.class);
        resetSingleton(NotificationService.class);
        resetSingleton(Scheduler.class);
        
        SecurityMonitor monitor1 = SecurityMonitor.getInstance();
        SecurityMonitor monitor2 = SecurityMonitor.getInstance();
        assertSame(monitor1, monitor2);
    }
    
    /**
     * 测试SecurityMonitor处理紧急事件
     */
    @Test(timeout = 4000)
    public void testSecurityMonitorHandleEmergency() throws Exception {
        // 测试处理紧急情况
        resetSingleton(SecurityMonitor.class);
        resetSingleton(EventBus.class);
        resetSingleton(LogManager.class);
        resetSingleton(NotificationService.class);
        resetSingleton(Scheduler.class);
        
        SecurityMonitor monitor = SecurityMonitor.getInstance();
        monitor.handleEmergency("Fire alarm");
        // 验证紧急情况已处理（不抛出异常）
    }
    
    /**
     * 测试SecurityMonitor接收紧急事件
     */
    @Test(timeout = 4000)
    public void testSecurityMonitorOnEventEmergency() throws Exception {
        // 测试接收紧急事件通知
        resetSingleton(SecurityMonitor.class);
        resetSingleton(EventBus.class);
        resetSingleton(LogManager.class);
        resetSingleton(NotificationService.class);
        resetSingleton(Scheduler.class);
        
        SecurityMonitor monitor = SecurityMonitor.getInstance();
        
        EventBus.Event event = new EventBus.Event(EventType.EMERGENCY, "Emergency data");
        monitor.onEvent(event);
        // 验证事件已处理（不抛出异常）
    }
    
    /**
     * 测试SecurityMonitor接收非紧急事件
     */
    @Test(timeout = 4000)
    public void testSecurityMonitorOnEventNonEmergency() throws Exception {
        // 测试接收非紧急事件
        resetSingleton(SecurityMonitor.class);
        resetSingleton(EventBus.class);
        resetSingleton(LogManager.class);
        resetSingleton(NotificationService.class);
        resetSingleton(Scheduler.class);
        
        SecurityMonitor monitor = SecurityMonitor.getInstance();
        
        EventBus.Event event = new EventBus.Event(EventType.CONFIG_UPDATED, "Config");
        monitor.onEvent(event); // 不应触发紧急处理
    }
    
    /**
     * 测试SecurityEvent内部类
     */
    @Test(timeout = 4000)
    public void testSecurityEventClass() {
        // 测试SecurityEvent内部类
        long timestamp = System.currentTimeMillis();
        Object data = "Test data";
        SecurityMonitor.SecurityEvent event = new SecurityMonitor.SecurityEvent("Test description", timestamp, data);
        
        assertEquals("Test description", event.getDescription());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(data, event.getData());
    }

    // ==================== 并发和边界测试 ====================
    
    /**
     * 测试多线程访问单例 - ElevatorManager
     */
    @Test(timeout = 4000)
    public void testElevatorManagerConcurrency() throws Exception {
        // 测试多线程并发访问ElevatorManager单例
        resetSingleton(ElevatorManager.class);
        
        final ElevatorManager[] instances = new ElevatorManager[10];
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    instances[index] = ElevatorManager.getInstance();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 所有实例应该是同一个
        for (int i = 1; i < 10; i++) {
            assertSame(instances[0], instances[i]);
        }
    }
    
    /**
     * 测试多线程访问单例 - EventBus
     */
    @Test(timeout = 4000)
    public void testEventBusConcurrency() throws Exception {
        // 测试多线程并发访问EventBus单例
        resetSingleton(EventBus.class);
        
        final EventBus[] instances = new EventBus[10];
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    instances[index] = EventBus.getInstance();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        for (int i = 1; i < 10; i++) {
            assertSame(instances[0], instances[i]);
        }
    }
    
    /**
     * 测试Floor并发添加请求
     */
    @Test(timeout = 4000)
    public void testFloorConcurrentAddRequests() throws Exception {
        // 测试多线程并发添加请求到Floor
        final Floor floor = new Floor(5);
        final int threadCount = 5;
        final int requestsPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < requestsPerThread; j++) {
                        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
                        floor.addRequest(request);
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        assertEquals(threadCount * requestsPerThread, requests.size());
    }
    
    /**
     * 测试Elevator边界条件 - 最小楼层
     */
    @Test(timeout = 4000)
    public void testElevatorMinFloor() {
        // 测试电梯在最小楼层的行为
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(1);
        assertEquals(1, elevator.getCurrentFloor());
    }
    
    /**
     * 测试Elevator边界条件 - 最大楼层
     */
    @Test(timeout = 4000)
    public void testElevatorMaxFloor() {
        // 测试电梯在最大楼层的行为
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(100);
        assertEquals(100, elevator.getCurrentFloor());
    }
    
    /**
     * 测试PassengerRequest时间戳唯一性
     */
    @Test(timeout = 4000)
    public void testPassengerRequestTimestampUniqueness() throws Exception {
        // 测试乘客请求时间戳的生成
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        Thread.sleep(2);
        PassengerRequest request2 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        
        assertTrue(request2.getTimestamp() >= request1.getTimestamp());
    }

    // ==================== 集成测试 ====================
    
    /**
     * 测试完整的电梯调度流程
     */
    @Test(timeout = 4000)
    public void testCompleteElevatorDispatchFlow() throws Exception {
        // 测试完整的电梯调度流程
        resetSingleton(Scheduler.class);
        resetSingleton(ElevatorManager.class);
        
        List<Elevator> elevators = new ArrayList<>();
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator1.setStatus(ElevatorStatus.IDLE);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        ElevatorManager manager = ElevatorManager.getInstance();
        manager.registerElevator(elevator1);
        manager.registerElevator(elevator2);
        
        PassengerRequest request = new PassengerRequest(3, 10, Priority.HIGH, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        assertTrue(elevator1.getDestinationSet().size() > 0 || elevator2.getDestinationSet().size() > 0);
    }
    
    /**
     * 测试事件总线完整流程
     */
    @Test(timeout = 4000)
    public void testEventBusCompleteFlow() throws Exception {
        // 测试事件总线的完整流程
        resetSingleton(EventBus.class);
        EventBus eventBus = EventBus.getInstance();
        
        final List<EventBus.Event> receivedEvents = new ArrayList<>();
        
        EventBus.EventListener listener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                receivedEvents.add(event);
            }
        };
        
        eventBus.subscribe(EventType.EMERGENCY, listener);
        eventBus.subscribe(EventType.ELEVATOR_FAULT, listener);
        
        EventBus.Event event1 = new EventBus.Event(EventType.EMERGENCY, "Emergency 1");
        EventBus.Event event2 = new EventBus.Event(EventType.ELEVATOR_FAULT, "Fault 1");
        
        eventBus.publish(event1);
        eventBus.publish(event2);
        
        assertEquals(2, receivedEvents.size());
    }
    
    /**
     * 测试系统配置影响电梯创建
     */
    @Test(timeout = 4000)
    public void testSystemConfigAffectsElevator() throws Exception {
        // 测试系统配置对电梯的影响
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        config.setMaxLoad(1000.0);
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        assertEquals(1000.0, elevator.getMaxLoad(), 0.01);
    }

    // ==================== 额外的高覆盖率测试 ====================
    
    /**
     * 测试Elevator的move方法 - 向上移动并到达目的地
     */
    @Test(timeout = 4000)
    public void testElevatorMoveUpWithDestination() throws Exception {
        // 测试向上移动并到达目的地
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        elevator.setDirection(Direction.UP);
        elevator.addDestination(4);
        elevator.setStatus(ElevatorStatus.MOVING);
        
        double initialEnergy = elevator.getEnergyConsumption();
        elevator.move();
        
        assertEquals(4, elevator.getCurrentFloor());
        assertTrue(elevator.getEnergyConsumption() > initialEnergy);
        assertFalse(elevator.getDestinationSet().contains(4));
    }
    
    /**
     * 测试Elevator的move方法 - 向下移动
     */
    @Test(timeout = 4000)
    public void testElevatorMoveDown() throws Exception {
        // 测试向下移动
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setDirection(Direction.DOWN);
        elevator.addDestination(3);
        elevator.setStatus(ElevatorStatus.MOVING);
        
        elevator.move();
        
        assertEquals(4, elevator.getCurrentFloor());
    }
    
    /**
     * 测试Elevator的move方法 - 不包含当前楼层
     */
    @Test(timeout = 4000)
    public void testElevatorMoveNoDestinationAtCurrentFloor() throws Exception {
        // 测试移动但当前楼层不是目的地
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        elevator.setDirection(Direction.UP);
        elevator.addDestination(6);
        elevator.setStatus(ElevatorStatus.MOVING);
        
        elevator.move();
        
        assertEquals(4, elevator.getCurrentFloor());
        assertTrue(elevator.getDestinationSet().contains(6));
    }
    
    /**
     * 测试Elevator的move方法 - 最后一个目的地
     */
    @Test(timeout = 4000)
    public void testElevatorMoveLastDestination() throws Exception {
        // 测试到达最后一个目的地后变为IDLE
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(4);
        elevator.setDirection(Direction.UP);
        elevator.addDestination(5);
        elevator.setStatus(ElevatorStatus.MOVING);
        
        elevator.move();
        
        assertEquals(5, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals(0, elevator.getDestinationSet().size());
    }
    
    /**
     * 测试Elevator的moveToFirstFloor方法 - 从上方移动到1楼
     */
    @Test(timeout = 4000)
    public void testElevatorMoveToFirstFloorFromAbove() throws Exception {
        // 测试从高楼层紧急下降到1楼
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(3);
        elevator.setDirection(Direction.DOWN);
        
        double initialEnergy = elevator.getEnergyConsumption();
        elevator.moveToFirstFloor();
        
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        assertTrue(elevator.getEnergyConsumption() > initialEnergy);
    }
    
    /**
     * 测试Elevator的moveToFirstFloor方法 - 已在1楼
     */
    @Test(timeout = 4000)
    public void testElevatorMoveToFirstFloorAlreadyAtFirst() throws Exception {
        // 测试已经在1楼时调用moveToFirstFloor
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(1);
        
        elevator.moveToFirstFloor();
        
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
    }
    
    /**
     * 测试Elevator的moveToFirstFloor方法 - 向上方向
     */
    @Test(timeout = 4000)
    public void testElevatorMoveToFirstFloorWithUpDirection() throws Exception {
        // 测试向上方向时紧急下降到1楼
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(2);
        elevator.setDirection(Direction.UP);
        
        elevator.moveToFirstFloor();
        
        assertEquals(1, elevator.getCurrentFloor());
    }
    
    /**
     * 测试Elevator的openDoor方法
     */
    @Test(timeout = 4000)
    public void testElevatorOpenDoor() throws Exception {
        // 测试打开门的流程
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setDirection(Direction.UP);
        
        elevator.openDoor();
        
        assertEquals(ElevatorStatus.STOPPED, elevator.getStatus());
    }
    
    /**
     * 测试Elevator的openDoor方法 - 有乘客需要卸载
     */
    @Test(timeout = 4000)
    public void testElevatorOpenDoorWithPassengers() throws Exception {
        // 测试有乘客需要卸载时打开门
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setDirection(Direction.UP);
        
        // 添加乘客
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(new PassengerRequest(3, 5, Priority.HIGH, RequestType.STANDARD));
        } catch (Exception e) {
            fail("反射失败");
        }
        
        elevator.openDoor();
        
        assertEquals(0, elevator.getPassengerList().size());
    }
    
    /**
     * 测试Elevator更新方向 - 当前楼层等于最小目的地
     */
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionAtMinDestination() {
        // 测试当前楼层等于最小目的地时的方向更新
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        elevator.setCurrentFloor(5);
        elevator.addDestination(5);
        elevator.addDestination(8);
        elevator.updateDirection();
        
        // 最小值是5，当前也是5，应该向上
        assertEquals(Direction.UP, elevator.getDirection());
    }
    
    /**
     * 测试Elevator loadPassengers - 达到最大负载
     */
    @Test(timeout = 4000)
    public void testElevatorLoadPassengersAtMaxLoad() {
        // 测试达到最大负载时的装载行为
        Scheduler mockScheduler = mock(Scheduler.class);
        List<PassengerRequest> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD));
        }
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(requests);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(1);
        elevator.setDirection(Direction.UP);
        elevator.setCurrentLoad(750.0); // 接近最大负载800
        
        elevator.loadPassengers();
        
        // 应该只能装载很少的乘客
        assertTrue(elevator.getCurrentLoad() <= elevator.getMaxLoad());
    }
    
    /**
     * 测试NearestElevatorStrategy - 方向不匹配
     */
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyDirectionMismatch() {
        // 测试方向不匹配的电梯不会被选择
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.DOWN);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试HighEfficiencyStrategy - 不符合条件的电梯
     */
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyNoMatch() {
        // 测试没有符合条件的电梯
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.MAINTENANCE);
        elevator.setDirection(Direction.DOWN);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    /**
     * 测试EnergySavingStrategy - 边界距离5
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategyBoundaryDistance() {
        // 测试边界距离正好是5
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(5);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected); // 距离正好是5，不满足 < 5
    }
    
    /**
     * 测试EnergySavingStrategy - 边界距离4
     */
    @Test(timeout = 4000)
    public void testEnergySavingStrategyBoundaryDistance4() {
        // 测试边界距离小于5
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(6);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected); // 距离是4，满足 < 5
    }
    
    /**
     * 测试Scheduler update方法 - 其他事件类型
     */
    @Test(timeout = 4000)
    public void testSchedulerUpdateOtherEventType() throws Exception {
        // 测试其他类型的事件
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        Event event = new Event(EventType.MAINTENANCE_REQUIRED, "Maintenance");
        scheduler.update(elevator, event);
        
        // 不应该触发任何特殊处理
    }
    
    /**
     * 测试Elevator的run方法 - 模拟线程执行
     */
    @Test(timeout = 4000)
    public void testElevatorRunMethod() throws Exception {
        // 测试电梯线程的运行
        Scheduler mockScheduler = mock(Scheduler.class);
        when(mockScheduler.getRequestsAtFloor(anyInt(), any(Direction.class)))
            .thenReturn(new ArrayList<>());
        
        final Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setCurrentFloor(1);
        
        Thread elevatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 添加目的地并唤醒
                    elevator.addDestination(2);
                    Thread.sleep(100);
                    // 中断线程
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
        
        elevatorThread.start();
        Thread.sleep(200);
        elevatorThread.interrupt();
        elevatorThread.join(500);
    }
    
    /**
     * 测试Elevator通知观察者 - ElevatorStatus作为参数
     */
    @Test(timeout = 4000)
    public void testElevatorNotifyObserversWithStatus() {
        // 测试使用ElevatorStatus作为参数通知观察者
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        Observer mockObserver = mock(Observer.class);
        elevator.addObserver(mockObserver);
        
        elevator.notifyObservers(ElevatorStatus.EMERGENCY);
        
        verify(mockObserver).update(elevator, ElevatorStatus.EMERGENCY);
    }
    
    /**
     * 测试多个Observer - 都收到通知
     */
    @Test(timeout = 4000)
    public void testElevatorMultipleObservers() {
        // 测试多个观察者都能收到通知
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        Observer mockObserver1 = mock(Observer.class);
        Observer mockObserver2 = mock(Observer.class);
        Observer mockObserver3 = mock(Observer.class);
        
        elevator.addObserver(mockObserver1);
        elevator.addObserver(mockObserver2);
        elevator.addObserver(mockObserver3);
        
        Event testEvent = new Event(EventType.EMERGENCY, "Test");
        elevator.notifyObservers(testEvent);
        
        verify(mockObserver1).update(elevator, testEvent);
        verify(mockObserver2).update(elevator, testEvent);
        verify(mockObserver3).update(elevator, testEvent);
    }
    
    /**
     * 测试PredictiveSchedulingStrategy - 多个电梯选择成本最低的
     */
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyMultipleElevators() {
        // 测试在多个电梯中选择成本最低的
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        
        Elevator elevator1 = new Elevator(1, mockScheduler);
        elevator1.setCurrentFloor(1);
        elevator1.setCurrentLoad(0);
        
        Elevator elevator2 = new Elevator(2, mockScheduler);
        elevator2.setCurrentFloor(10);
        elevator2.setCurrentLoad(500); // 高负载
        
        Elevator elevator3 = new Elevator(3, mockScheduler);
        elevator3.setCurrentFloor(4);
        elevator3.setCurrentLoad(100);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        elevators.add(elevator3);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        // elevator3应该被选中，因为距离近且负载低
        assertSame(elevator3, selected);
    }
    
    /**
     * 测试LogManager查询日志 - 边界时间
     */
    @Test(timeout = 4000)
    public void testLogManagerQueryLogsBoundaryTime() throws Exception {
        // 测试边界时间的日志查询
        resetSingleton(LogManager.class);
        LogManager logManager = LogManager.getInstance();
        
        long timestamp = System.currentTimeMillis();
        logManager.recordEvent("Test", "Message at exact time");
        
        // 查询正好在时间戳上的日志
        List<LogManager.SystemLog> logs = logManager.queryLogs("Test", timestamp, timestamp);
        assertNotNull(logs);
    }
    
    /**
     * 测试AnalyticsEngine - 边界值50
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineBoundaryValue50() throws Exception {
        // 测试边界值50
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        engine.updateFloorPassengerCount(1, 50);
        
        assertFalse(engine.isPeakHours()); // 正好50不算高峰
    }
    
    /**
     * 测试AnalyticsEngine - 边界值51
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineBoundaryValue51() throws Exception {
        // 测试边界值51
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        engine.updateFloorPassengerCount(1, 51);
        
        assertTrue(engine.isPeakHours()); // 51算高峰
    }
    
    /**
     * 测试NotificationService构造函数初始化通道
     */
    @Test(timeout = 4000)
    public void testNotificationServiceConstructor() {
        // 测试构造函数
        NotificationService service = new NotificationService();
        assertNotNull(service);
    }
    
    /**
     * 测试ThreadPoolManager shutdown方法
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagerShutdown() throws Exception {
        // 测试关闭线程池
        ThreadPoolManager manager = new ThreadPoolManager();
        manager.shutdown();
        // 验证关闭成功（不抛出异常）
    }
    
    /**
     * 测试ThreadPoolManager shutdown - 等待超时
     */
    @Test(timeout = 4000)
    public void testThreadPoolManagerShutdownTimeout() throws Exception {
        // 测试关闭超时的情况
        ThreadPoolManager manager = new ThreadPoolManager();
        
        // 提交一个长时间运行的任务
        manager.submitTask(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        Thread.sleep(50);
        manager.shutdown();
    }
    
    /**
     * 测试Scheduler dispatchElevator - 有选中的电梯
     */
    @Test(timeout = 4000)
    public void testSchedulerDispatchElevatorWithSelection() throws Exception {
        // 测试有电梯被选中时的调度
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.dispatchElevator(request);
        
        assertTrue(elevator.getDestinationSet().contains(3));
    }
    
    /**
     * 测试Elevator clearAllRequests - 返回待处理请求
     */
    @Test(timeout = 4000)
    public void testElevatorClearAllRequestsReturnsRequests() {
        // 测试清除请求返回待处理的请求列表
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(1, 8, Priority.MEDIUM, RequestType.STANDARD);
        
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(request1);
            passengerList.add(request2);
        } catch (Exception e) {
            fail("反射失败");
        }
        
        elevator.addDestination(5);
        elevator.addDestination(8);
        
        List<PassengerRequest> cleared = elevator.clearAllRequests();
        
        assertEquals(2, cleared.size());
        assertTrue(cleared.contains(request1));
        assertTrue(cleared.contains(request2));
    }
    
    /**
     * 测试MaintenanceManager processTasks方法
     */
    @Test(timeout = 4000)
    public void testMaintenanceManagerProcessTasks() throws Exception {
        // 测试处理维护任务队列
        resetSingleton(MaintenanceManager.class);
        MaintenanceManager manager = MaintenanceManager.getInstance();
        
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        manager.scheduleMaintenance(elevator);
        Thread.sleep(100); // 等待任务处理
    }
    
    /**
     * 测试Floor并发 - 不同方向的请求
     */
    @Test(timeout = 4000)
    public void testFloorConcurrentDifferentDirections() throws Exception {
        // 测试并发添加不同方向的请求
        final Floor floor = new Floor(5);
        final int threadCount = 3;
        Thread[] threads = new Thread[threadCount * 2];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
                    floor.addRequest(request);
                }
            });
            
            threads[i + threadCount] = new Thread(new Runnable() {
                @Override
                public void run() {
                    PassengerRequest request = new PassengerRequest(5, 2, Priority.LOW, RequestType.STANDARD);
                    floor.addRequest(request);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        List<PassengerRequest> upRequests = floor.getRequests(Direction.UP);
        List<PassengerRequest> downRequests = floor.getRequests(Direction.DOWN);
        
        assertEquals(threadCount, upRequests.size());
        assertEquals(threadCount, downRequests.size());
    }
    
    /**
     * 测试SystemConfig - 多个设置组合
     */
    @Test(timeout = 4000)
    public void testSystemConfigMultipleSettings() throws Exception {
        // 测试多个配置的组合设置
        resetSingleton(SystemConfig.class);
        SystemConfig config = SystemConfig.getInstance();
        
        config.setFloorCount(25);
        config.setElevatorCount(6);
        config.setMaxLoad(900.0);
        
        assertEquals(25, config.getFloorCount());
        assertEquals(6, config.getElevatorCount());
        assertEquals(900.0, config.getMaxLoad(), 0.01);
    }
    
    /**
     * 测试ElevatorStatusReport - 所有状态值
     */
    @Test(timeout = 4000)
    public void testElevatorStatusReportAllStatuses() {
        // 测试所有可能的状态值
        ElevatorStatusReport report1 = new ElevatorStatusReport(1, 1, Direction.UP, ElevatorStatus.IDLE, 0.0, 0.0, 0);
        assertEquals(ElevatorStatus.IDLE, report1.getStatus());
        
        ElevatorStatusReport report2 = new ElevatorStatusReport(2, 5, Direction.DOWN, ElevatorStatus.MAINTENANCE, 0.0, 100.0, 1);
        assertEquals(ElevatorStatus.MAINTENANCE, report2.getStatus());
        
        ElevatorStatusReport report3 = new ElevatorStatusReport(3, 3, Direction.UP, ElevatorStatus.FAULT, 1.0, 200.0, 2);
        assertEquals(ElevatorStatus.FAULT, report3.getStatus());
    }
    
    /**
     * 测试PassengerRequest - 所有优先级和类型的组合
     */
    @Test(timeout = 4000)
    public void testPassengerRequestAllCombinations() {
        // 测试所有优先级和请求类型的组合
        PassengerRequest req1 = new PassengerRequest(1, 5, Priority.LOW, RequestType.STANDARD);
        assertEquals(Priority.LOW, req1.getPriority());
        assertEquals(RequestType.STANDARD, req1.getRequestType());
        
        PassengerRequest req2 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.DESTINATION_CONTROL);
        assertEquals(Priority.HIGH, req2.getPriority());
        assertEquals(RequestType.DESTINATION_CONTROL, req2.getRequestType());
        
        PassengerRequest req3 = new PassengerRequest(1, 5, Priority.MEDIUM, RequestType.STANDARD);
        assertEquals(Priority.MEDIUM, req3.getPriority());
    }
    
    /**
     * 测试Elevator unloadPassengers - 多个乘客部分卸载
     */
    @Test(timeout = 4000)
    public void testElevatorUnloadPassengersPartial() {
        // 测试部分乘客卸载
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        PassengerRequest request1 = new PassengerRequest(1, 3, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(2, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request3 = new PassengerRequest(1, 3, Priority.LOW, RequestType.STANDARD);
        
        elevator.setCurrentFloor(3);
        
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(request1);
            passengerList.add(request2);
            passengerList.add(request3);
        } catch (Exception e) {
            fail("反射失败");
        }
        
        elevator.unloadPassengers();
        
        List<PassengerRequest> remaining = elevator.getPassengerList();
        assertEquals(1, remaining.size());
        assertEquals(5, remaining.get(0).getDestinationFloor());
    }
    
    /**
     * 测试Elevator - 负载计算的准确性
     */
    @Test(timeout = 4000)
    public void testElevatorLoadCalculation() {
        // 测试负载计算
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        
        PassengerRequest request1 = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(1, 8, Priority.HIGH, RequestType.STANDARD);
        
        elevator.setCurrentFloor(3);
        
        try {
            Field passengerListField = Elevator.class.getDeclaredField("passengerList");
            passengerListField.setAccessible(true);
            List<PassengerRequest> passengerList = (List<PassengerRequest>) passengerListField.get(elevator);
            passengerList.add(request1);
            passengerList.add(request2);
        } catch (Exception e) {
            fail("反射失败");
        }
        
        elevator.unloadPassengers();
        
        // 2个乘客，每人70kg
        assertEquals(140.0, elevator.getCurrentLoad(), 0.01);
    }
    
    /**
     * 测试AnalyticsEngine - 零乘客数
     */
    @Test(timeout = 4000)
    public void testAnalyticsEngineZeroPassengers() throws Exception {
        // 测试零乘客数
        resetSingleton(AnalyticsEngine.class);
        resetSingleton(LogManager.class);
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        engine.updateFloorPassengerCount(1, 0);
        engine.updateFloorPassengerCount(2, 0);
        
        assertFalse(engine.isPeakHours());
    }
    
    /**
     * 测试EventBus subscribe - 同一监听器订阅多次
     */
    @Test(timeout = 4000)
    public void testEventBusSubscribeMultipleTimes() throws Exception {
        // 测试同一监听器多次订阅
        resetSingleton(EventBus.class);
        EventBus eventBus = EventBus.getInstance();
        
        final int[] callCount = {0};
        EventBus.EventListener listener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                callCount[0]++;
            }
        };
        
        eventBus.subscribe(EventType.EMERGENCY, listener);
        eventBus.subscribe(EventType.EMERGENCY, listener); // 重复订阅
        
        EventBus.Event testEvent = new EventBus.Event(EventType.EMERGENCY, "Test");
        eventBus.publish(testEvent);
        
        assertEquals(2, callCount[0]); // 应该被调用两次
    }
    
    /**
     * 测试Scheduler - LOW优先级请求
     */
    @Test(timeout = 4000)
    public void testSchedulerSubmitLowPriorityRequest() throws Exception {
        // 测试提交低优先级请求
        resetSingleton(Scheduler.class);
        List<Elevator> elevators = new ArrayList<>();
        Scheduler mockScheduler = mock(Scheduler.class);
        Elevator elevator = new Elevator(1, mockScheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        DispatchStrategy strategy = new NearestElevatorStrategy();
        Scheduler scheduler = new Scheduler(elevators, 10, strategy);
        
        PassengerRequest request = new PassengerRequest(3, 8, Priority.LOW, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        assertTrue(elevator.getDestinationSet().contains(3));
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 重置单例实例的辅助方法
     * 使用反射将单例的instance字段设置为null
     */
    private void resetSingleton(Class<?> clazz) throws Exception {
        Field instanceField = clazz.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
