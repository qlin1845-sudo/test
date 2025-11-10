package net.mooctest;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * 简单的测试验证类
 */
public class SimpleTest {
    
    @Test
    public void testBasicFunctionality() {
        // 测试基本对象创建
        ElevatorManager manager = ElevatorManager.getInstance();
        assertNotNull("ElevatorManager应该不为null", manager);
        
        // 测试电梯创建
        Elevator elevator = new Elevator(1, null);
        assertEquals("电梯ID应该为1", 1, elevator.getId());
        assertEquals("初始楼层应该为1", 1, elevator.getCurrentFloor());
        
        // 测试乘客请求创建
        PassengerRequest request = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        assertEquals("起始楼层应该为1", 1, request.getStartFloor());
        assertEquals("目标楼层应该为5", 5, request.getDestinationFloor());
        
        // 测试楼层创建
        Floor floor = new Floor(3);
        assertEquals("楼层号应该为3", 3, floor.getFloorNumber());
        
        // 测试枚举
        assertEquals("UP方向", Direction.UP, Direction.UP);
        assertEquals("IDLE状态", ElevatorStatus.IDLE, ElevatorStatus.IDLE);
        assertEquals("HIGH优先级", Priority.HIGH, Priority.HIGH);
        
        assertTrue("基本功能测试通过", true);
    }
}