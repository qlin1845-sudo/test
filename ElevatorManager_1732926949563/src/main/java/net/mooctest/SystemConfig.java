package net.mooctest;

public class SystemConfig {
    private static volatile SystemConfig instance;
    private int floorCount;
    private int elevatorCount;
    private double maxLoad; // Maximum load per elevator in kg

    public SystemConfig() {
        // Default configuration
        this.floorCount = 20;
        this.elevatorCount = 4;
        this.maxLoad = 800; // Default maximum load to 800kg
    }

    public static SystemConfig getInstance() {
        if (instance == null) {
            synchronized (SystemConfig.class) {
                if (instance == null) {
                    instance = new SystemConfig();
                }
            }
        }
        return instance;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(int floorCount) {
        if (floorCount > 0) {
            this.floorCount = floorCount;
        }
    }

    public int getElevatorCount() {
        return elevatorCount;
    }

    public void setElevatorCount(int elevatorCount) {
        if (elevatorCount > 0) {
            this.elevatorCount = elevatorCount;
        }
    }

    public double getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(double maxLoad) {
        if (maxLoad > 0) {
            this.maxLoad = maxLoad;
        }
    }
}
