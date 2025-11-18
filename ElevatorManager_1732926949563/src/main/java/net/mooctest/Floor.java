package net.mooctest;

import java.util.*;
import java.util.concurrent.locks.*;

public class Floor {
    private final int floorNumber;
    private final Map<Direction, Queue<PassengerRequest>> requestQueues;
    private final ReentrantLock lock;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.requestQueues = new EnumMap<>(Direction.class);
        requestQueues.put(Direction.UP, new LinkedList<>());
        requestQueues.put(Direction.DOWN, new LinkedList<>());
        this.lock = new ReentrantLock();
    }

    public void addRequest(PassengerRequest request) {
        lock.lock();
        try {
            requestQueues.get(request.getDirection()).add(request);
        } finally {
            lock.unlock();
        }
    }

    public List<PassengerRequest> getRequests(Direction direction) {
        lock.lock();
        try {
            List<PassengerRequest> requests = new ArrayList<>(requestQueues.get(direction));
            requestQueues.get(direction).clear();
            return requests;
        } finally {
            lock.unlock();
        }
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}
