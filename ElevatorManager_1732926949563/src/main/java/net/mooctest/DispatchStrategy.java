package net.mooctest;

import java.util.List;

public interface DispatchStrategy {
    /**
     * 选择一个电梯来满足乘客请求
     * @param elevators 可用的电梯列表
     * @param request 乘客请求
     * @return 选定的电梯，若没有合适的电梯返回 null
     */
    Elevator selectElevator(List<Elevator> elevators, PassengerRequest request);
}
