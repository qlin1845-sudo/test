package net.mooctest;

public class PassengerRequest {
    private final int startFloor;
    private final int destinationFloor;
    private final Direction direction;
    private final Priority priority;
    private final RequestType requestType;
    private final long timestamp;
    private final SpecialNeeds specialNeeds;

    public PassengerRequest(int startFloor, int destinationFloor, Priority priority, RequestType requestType) {
        this.startFloor = startFloor;
        this.destinationFloor = destinationFloor;
        this.direction = startFloor < destinationFloor ? Direction.UP : Direction.DOWN;
        this.priority = priority;
        this.requestType = requestType;
        this.timestamp = System.currentTimeMillis();
        this.specialNeeds = SpecialNeeds.NONE;
    }

    public int getStartFloor() {
        return startFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public Priority getPriority() {
        return priority;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SpecialNeeds getSpecialNeeds() {
        return specialNeeds;
    }

    @Override
    public String toString() {
        return String.format("Request [From %d to %d, Priority: %s, Type: %s, Special Needs: %s]",
                startFloor, destinationFloor, priority, requestType, specialNeeds);
    }
}
