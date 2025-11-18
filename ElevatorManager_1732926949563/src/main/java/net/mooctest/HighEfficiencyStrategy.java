package net.mooctest;

import java.util.List;

public class HighEfficiencyStrategy implements DispatchStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
        Elevator selectedElevator = null;

        for (Elevator elevator : elevators) {
            if (elevator.getStatus() == ElevatorStatus.IDLE || elevator.getDirection() == request.getDirection()) {
                if (selectedElevator == null || isCloser(elevator, selectedElevator, request)) {
                    selectedElevator = elevator;
                }
            }
        }

        return selectedElevator;
    }

    public boolean isCloser(Elevator candidate, Elevator current, PassengerRequest request) {
        return Math.abs(candidate.getCurrentFloor() - request.getStartFloor()) <
               Math.abs(current.getCurrentFloor() - request.getStartFloor());
    }
}
