package net.mooctest;

import java.util.List;

public class NearestElevatorStrategy implements DispatchStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
        Elevator nearestElevator = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (isEligible(elevator, request)) {
                int distance = Math.abs(elevator.getCurrentFloor() - request.getStartFloor());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestElevator = elevator;
                }
            }
        }

        return nearestElevator;
    }

    public boolean isEligible(Elevator elevator, PassengerRequest request) {
        return (elevator.getStatus() == ElevatorStatus.IDLE) ||
               (elevator.getStatus() == ElevatorStatus.MOVING &&
                elevator.getDirection() == request.getDirection());
    }
}
