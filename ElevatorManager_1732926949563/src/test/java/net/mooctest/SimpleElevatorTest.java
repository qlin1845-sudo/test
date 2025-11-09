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
 * 简化的电梯系统测试类
 * 用于验证基本编译和运行
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleElevatorTest {

    @Mock
    private Observer mockObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBasicElevatorCreation() {
        Scheduler scheduler = Scheduler.getInstance();
        Elevator elevator = new Elevator(1, scheduler);
        
        assertNotNull("电梯应该成功创建", elevator);
        assertEquals("电梯ID应该是1", 1, elevator.getId());
        assertEquals("初始楼层应该是1", 1, elevator.getCurrentFloor());
    }

    @Test
    public void testPassengerRequest() {
        PassengerRequest request = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        
        assertNotNull("乘客请求应该成功创建", request);
        assertEquals("起始楼层应该是1", 1, request.getStartFloor());
        assertEquals("目标楼层应该是5", 5, request.getDestinationFloor());
        assertEquals("方向应该是UP", Direction.UP, request.getDirection());
    }

    @Test
    public void testFloor() {
        Floor floor = new Floor(5);
        
        assertNotNull("楼层应该成功创建", floor);
        assertEquals("楼层号应该是5", 5, floor.getFloorNumber());
    }

    @Test
    public void testSystemConfig() {
        SystemConfig config = SystemConfig.getInstance();
        
        assertNotNull("系统配置应该成功创建", config);
        assertTrue("最大载重应该大于0", config.getMaxLoad() > 0);
        assertTrue("楼层数应该大于0", config.getFloorCount() > 0);
        assertTrue("电梯数应该大于0", config.getElevatorCount() > 0);
    }

    @Test
    public void testNearestStrategy() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        
        assertNotNull("策略对象应该成功创建", strategy);
        
        List<Elevator> elevators = new ArrayList<>();
        elevators.add(new Elevator(1, Scheduler.getInstance()));
        
        PassengerRequest request = new PassengerRequest(2, 5, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull("应该能选择到电梯", selected);
    }

    @After
    public void tearDown() {
        // 清理单例实例
        try {
            java.lang.reflect.Field emInstance = ElevatorManager.class.getDeclaredField("instance");
            emInstance.setAccessible(true);
            emInstance.set(null, null);
            
            java.lang.reflect.Field sInstance = Scheduler.class.getDeclaredField("instance");
            sInstance.setAccessible(true);
            sInstance.set(null, null);
            
            java.lang.reflect.Field scInstance = SystemConfig.class.getDeclaredField("instance");
            scInstance.setAccessible(true);
            scInstance.set(null, null);
            
        } catch (Exception e) {
            // 忽略反射异常
        }
    }
}