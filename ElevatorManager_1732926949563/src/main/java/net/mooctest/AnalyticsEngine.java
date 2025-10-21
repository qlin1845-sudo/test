package net.mooctest;

import java.util.*;
import java.util.concurrent.*;

public class AnalyticsEngine {
    private static volatile AnalyticsEngine instance;
    private final List<ElevatorStatusReport> statusReports;
    private final Map<Integer, Integer> floorPassengerCounts;
    private final LogManager logManager;

    public AnalyticsEngine() {
        this.statusReports = new CopyOnWriteArrayList<>();
        this.floorPassengerCounts = new ConcurrentHashMap<>();
        this.logManager = LogManager.getInstance();
    }

    public static AnalyticsEngine getInstance() {
        if (instance == null) {
            synchronized (AnalyticsEngine.class) {
                if (instance == null) {
                    instance = new AnalyticsEngine();
                }
            }
        }
        return instance;
    }

    public void processStatusReport(ElevatorStatusReport report) {
        statusReports.add(report);
    }

    public void updateFloorPassengerCount(int floorNumber, int count) {
        floorPassengerCounts.put(floorNumber, count);
    }

    public boolean isPeakHours() {
        int totalWaitingPassengers = floorPassengerCounts.values().stream().mapToInt(Integer::intValue).sum();
        return totalWaitingPassengers > 50;
    }

    public Report generatePerformanceReport() {
        return new Report("System Performance Report", System.currentTimeMillis());
    }

    public static class Report {
        private final String title;
        private final long generatedTime;

        public Report(String title, long generatedTime) {
            this.title = title;
            this.generatedTime = generatedTime;
        }

        public String getTitle() {
            return title;
        }

        public long getGeneratedTime() {
            return generatedTime;
        }
    }
}
