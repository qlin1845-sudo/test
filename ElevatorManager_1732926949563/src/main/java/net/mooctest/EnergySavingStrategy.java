package net.mooctest;

import java.util.List;

public class EnergySavingStrategy implements DispatchStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
        for (Elevator elevator : elevators) {
            if (elevator.getStatus() == ElevatorStatus.IDLE) {
                return elevator; // 优先选择空闲的电梯
            }
        }

        // 若无空闲电梯，则选择在该方向上接近的电梯
        for (Elevator elevator : elevators) {
            if (elevator.getDirection() == request.getDirection() &&
                Math.abs(elevator.getCurrentFloor() - request.getStartFloor()) < 5) {
                return elevator;
            }
        }

        return null; // 若无合适电梯，则返回 null
    }
}
