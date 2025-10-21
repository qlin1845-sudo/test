package net.mooctest;

import java.util.*;
import java.util.concurrent.*;

public class MaintenanceManager implements EventBus.EventListener {
    private static volatile MaintenanceManager instance;
    private final Queue<MaintenanceTask> taskQueue;
    private final List<MaintenanceRecord> maintenanceRecords;
    private final ExecutorService executorService;

    public MaintenanceManager() {
        this.taskQueue = new LinkedList<>();
        this.maintenanceRecords = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this::processTasks);
    }

    public static MaintenanceManager getInstance() {
        if (instance == null) {
            synchronized (MaintenanceManager.class) {
                if (instance == null) {
                    instance = new MaintenanceManager();
                }
            }
        }
        return instance;
    }

    @Override
    public void onEvent(EventBus.Event event) {
        if (event.getType() == EventType.ELEVATOR_FAULT) {
            Elevator elevator = (Elevator) event.getData();
            scheduleMaintenance(elevator);
        }
    }

    public void scheduleMaintenance(Elevator elevator) {
        MaintenanceTask task = new MaintenanceTask(elevator.getId(), System.currentTimeMillis(), "Fault repair");
        taskQueue.add(task);
        notifyMaintenancePersonnel(task);
    }

    public void processTasks() {
        while (true) {
            MaintenanceTask task = taskQueue.poll();
            if (task != null) {
                performMaintenance(task);
            }
            try {
                Thread.sleep(1000); // Sleep to reduce busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void performMaintenance(MaintenanceTask task) {
        System.out.println("Performing maintenance on Elevator " + task.getElevatorId());
        recordMaintenanceResult(task.getElevatorId(), "Maintenance complete.");
    }

    public void recordMaintenanceResult(int elevatorId, String result) {
        maintenanceRecords.add(new MaintenanceRecord(elevatorId, System.currentTimeMillis(), result));
    }

    public void notifyMaintenancePersonnel(MaintenanceTask task) {
        System.out.println("Notification: Maintenance required for Elevator " + task.getElevatorId());
    }

    public static class MaintenanceTask {
        private final int elevatorId;
        private final long scheduledTime;
        private final String description;

        public MaintenanceTask(int elevatorId, long scheduledTime, String description) {
            this.elevatorId = elevatorId;
            this.scheduledTime = scheduledTime;
            this.description = description;
        }

        public int getElevatorId() {
            return elevatorId;
        }

        public long getScheduledTime() {
            return scheduledTime;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class MaintenanceRecord {
        private final int elevatorId;
        private final long maintenanceTime;
        private final String result;

        public MaintenanceRecord(int elevatorId, long maintenanceTime, String result) {
            this.elevatorId = elevatorId;
            this.maintenanceTime = maintenanceTime;
            this.result = result;
        }

        public int getElevatorId() {
            return elevatorId;
        }

        public long getMaintenanceTime() {
            return maintenanceTime;
        }

        public String getResult() {
            return result;
        }
    }
}
