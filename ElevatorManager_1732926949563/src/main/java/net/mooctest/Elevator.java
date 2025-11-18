package net.mooctest;

import java.util.*;
import java.util.concurrent.locks.*;

public class Elevator extends Observable implements Runnable {
    private final int id;
    private volatile int currentFloor;
    private volatile Direction direction;
    private volatile ElevatorStatus status;
    private final List<PassengerRequest> passengerList;
    private final Set<Integer> destinationSet;
    private final ReentrantLock lock;
    private final Condition condition;
    private final double maxLoad; // Maximum load in kg
    private volatile double currentLoad;
    private final Scheduler scheduler;
    private final List<Observer> observers;
    private volatile double energyConsumption;
    private volatile ElevatorMode mode;

    public Elevator(int id, Scheduler scheduler) {
        this.id = id;
        this.currentFloor = 1;
        this.direction = Direction.UP;
        this.status = ElevatorStatus.IDLE;
        this.passengerList = new ArrayList<>();
        this.destinationSet = new TreeSet<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.maxLoad = SystemConfig.getInstance().getMaxLoad();
        this.scheduler = scheduler;
        this.energyConsumption = 0.0;
        this.mode = ElevatorMode.NORMAL;
        this.observers = new ArrayList<>();
        this.currentLoad = 0.0;
    }

    public int getId() {
        return id;
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

    public List<PassengerRequest> getPassengerList() {
        return new ArrayList<>(passengerList);
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers(Event event) {
        for (Observer observer : observers) {
            observer.update(this, event);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                lock.lock();
                while (status == ElevatorStatus.IDLE || destinationSet.isEmpty()) {
                    condition.await();
                }
                if (status == ElevatorStatus.EMERGENCY) {
                    moveToFirstFloor();
                } else {
                    move();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }
    }

    public void move() throws InterruptedException {
        status = ElevatorStatus.MOVING;
        updateDirection();

        if (direction == Direction.UP) {
            currentFloor++;
        } else if (direction == Direction.DOWN) {
            currentFloor--;
        }

        energyConsumption += 1.0; // Example energy consumption calculation

        if (destinationSet.contains(currentFloor)) {
            openDoor();
            destinationSet.remove(currentFloor);
        }

        if (destinationSet.isEmpty()) {
            status = ElevatorStatus.IDLE;
        }
    }

    public void openDoor() throws InterruptedException {
        status = ElevatorStatus.STOPPED;
        unloadPassengers();
        loadPassengers();
        Thread.sleep(500); // Simulate door open time
    }

    public void unloadPassengers() {
        passengerList.removeIf(request -> request.getDestinationFloor() == currentFloor);
        currentLoad = passengerList.size() * 70; // Assuming each passenger weighs 70 kg
    }

    public void loadPassengers() {
        List<PassengerRequest> requests = scheduler.getRequestsAtFloor(currentFloor, direction);
        for (PassengerRequest request : requests) {
            if (currentLoad < maxLoad) {
                passengerList.add(request);
                destinationSet.add(request.getDestinationFloor());
                currentLoad += 70;
            }
        }
    }

    public void updateDirection() {
        if (destinationSet.isEmpty()) {
            status = ElevatorStatus.IDLE;
        } else if (Collections.min(destinationSet) > currentFloor) {
            direction = Direction.UP;
        } else {
            direction = Direction.DOWN;
        }
    }

    public void addDestination(int floor) {
        lock.lock();
        try {
            destinationSet.add(floor);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void moveToFirstFloor() throws InterruptedException {
        while (currentFloor != 1) {
            if (direction == Direction.UP) currentFloor++;
            else currentFloor--;

            energyConsumption += 1.0;
            Thread.sleep(800); // Fast travel in emergency
        }
        status = ElevatorStatus.IDLE;
    }

    public void handleEmergency() {
        lock.lock();
        try {
            status = ElevatorStatus.EMERGENCY;
            destinationSet.clear();
            passengerList.clear();
            destinationSet.add(1);
            condition.signalAll();
            notifyObservers(ElevatorStatus.EMERGENCY);
        } finally {
            lock.unlock();
        }
    }

    public List<PassengerRequest> clearAllRequests() {
        lock.lock();
        try {
            List<PassengerRequest> pendingRequests = new ArrayList<>(passengerList);
            passengerList.clear();
            destinationSet.clear();
            condition.signalAll();
            return pendingRequests;
        } finally {
            lock.unlock();
        }
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setStatus(ElevatorStatus status) {
        this.status = status;
    }

    public Set<Integer> getDestinationSet() {
        return destinationSet;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Condition getCondition() {
        return condition;
    }

    public double getMaxLoad() {
        return maxLoad;
    }

    public double getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(double currentLoad) {
        this.currentLoad = currentLoad;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public List<Observer> getObservers() {
        return observers;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public ElevatorMode getMode() {
        return mode;
    }

    public void setMode(ElevatorMode mode) {
        this.mode = mode;
    }
}
