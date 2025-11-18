package net.mooctest;

public class ElevatorStatusReport {
    private final int elevatorId;
    private final int currentFloor;
    private final Direction direction;
    private final ElevatorStatus status;
    private final double speed;
    private final double currentLoad;
    private final int passengerCount;

    public ElevatorStatusReport(int elevatorId, int currentFloor, Direction direction, ElevatorStatus status, double speed, double currentLoad, int passengerCount) {
        this.elevatorId = elevatorId;
        this.currentFloor = currentFloor;
        this.direction = direction;
        this.status = status;
        this.speed = speed;
        this.currentLoad = currentLoad;
        this.passengerCount = passengerCount;
    }

    public int getElevatorId() {
        return elevatorId;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public ElevatorStatus getStatus() {
        return status;
    }

    public double getSpeed() {
        return speed;
    }

    public double getCurrentLoad() {
        return currentLoad;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    @Override
    public String toString() {
        return String.format(
                "ElevatorStatusReport[elevatorId=%d, currentFloor=%d, direction=%s, status=%s, speed=%.2f, currentLoad=%.2f, passengerCount=%d]",
                elevatorId, currentFloor, direction, status, speed, currentLoad, passengerCount
        );
    }
}
