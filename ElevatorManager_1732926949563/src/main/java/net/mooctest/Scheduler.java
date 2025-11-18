package net.mooctest;

import java.util.*;
import java.util.concurrent.locks.*;

public class Scheduler implements Observer {
    private static volatile Scheduler instance;
    private final List<Elevator> elevatorList;
    private final Map<Integer, Floor> floors;
    private DispatchStrategy dispatchStrategy;
    private final Queue<PassengerRequest> highPriorityQueue;
    private final ReentrantLock lock;

    public Scheduler(List<Elevator> elevatorList, int floorCount, DispatchStrategy strategy) {
        this.elevatorList = elevatorList;
        this.floors = new HashMap<>();
        for (int i = 1; i <= floorCount; i++) {
            floors.put(i, new Floor(i));
        }
        this.dispatchStrategy = strategy;
        this.highPriorityQueue = new LinkedList<>();
        this.lock = new ReentrantLock();
    }

    public static Scheduler getInstance(List<Elevator> elevatorList, int floorCount, DispatchStrategy strategy) {
        if (instance == null) {
            synchronized (Scheduler.class) {
                if (instance == null) {
                    instance = new Scheduler(elevatorList, floorCount, strategy);
                }
            }
        }
        return instance;
    }

    public static Scheduler getInstance() {
        if (instance == null) {
            synchronized (Scheduler.class) {
                if (instance == null) {
                    instance = new Scheduler(new ArrayList<>(), 0, new NearestElevatorStrategy());
                }
            }
        }
        return instance;
    }


    public void submitRequest(PassengerRequest request) {
        lock.lock();
        try {
            if (request.getPriority() == Priority.HIGH) {
                highPriorityQueue.add(request);
            } else {
                Floor floor = floors.get(request.getStartFloor());
                floor.addRequest(request);
            }
            dispatchElevator(request);
        } finally {
            lock.unlock();
        }
    }

    public void dispatchElevator(PassengerRequest request) {
        Elevator selectedElevator = dispatchStrategy.selectElevator(elevatorList, request);
        if (selectedElevator != null) {
            selectedElevator.addDestination(request.getStartFloor());
            System.out.println("Elevator " + selectedElevator.getId() + " dispatched for request: " + request);
        } else {
            System.out.println("No available elevators for request: " + request);
        }
    }

    public List<PassengerRequest> getRequestsAtFloor(int floorNumber, Direction direction) {
        Floor floor = floors.get(floorNumber);
        return floor.getRequests(direction);
    }

    @Override
    public void update(Observable o, Object arg) {
        Event event = (Event) arg;
        if (event.getType() == EventType.ELEVATOR_FAULT) {
            redistributeRequests((Elevator) o);
        } else if (event.getType() == EventType.EMERGENCY) {
            executeEmergencyProtocol();
        }
    }

    public void redistributeRequests(Elevator faultyElevator) {
        lock.lock();
        try {
            List<PassengerRequest> pendingRequests = faultyElevator.clearAllRequests();
            for (PassengerRequest request : pendingRequests) {
                dispatchElevator(request);
            }
        } finally {
            lock.unlock();
        }
    }

    public void executeEmergencyProtocol() {
        lock.lock();
        try {
            for (Elevator elevator : elevatorList) {
                elevator.handleEmergency();
            }
        } finally {
            lock.unlock();
        }
    }

    public void setDispatchStrategy(DispatchStrategy strategy) {
        lock.lock();
        try {
            this.dispatchStrategy = strategy;
        } finally {
            lock.unlock();
        }
    }


}
