package net.mooctest;

import java.util.*;

public class ElevatorManager {
    private static volatile ElevatorManager instance;
    private final Map<Integer, Elevator> elevatorMap;

    public ElevatorManager() {
        elevatorMap = new HashMap<>();
    }

    public static ElevatorManager getInstance() {
        if (instance == null) {
            synchronized (ElevatorManager.class) {
                if (instance == null) {
                    instance = new ElevatorManager();
                }
            }
        }
        return instance;
    }

    public void registerElevator(Elevator elevator) {
        elevatorMap.put(elevator.getId(), elevator);
    }

    public Elevator getElevatorById(int id) {
        return elevatorMap.get(id);
    }

    public Collection<Elevator> getAllElevators() {
        return elevatorMap.values();
    }
}
