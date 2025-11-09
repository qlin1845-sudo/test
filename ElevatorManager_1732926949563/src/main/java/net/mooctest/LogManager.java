package net.mooctest;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class LogManager {
    private static volatile LogManager instance;
    private final List<SystemLog> logs;

    public LogManager() {
        logs = new CopyOnWriteArrayList<>();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }

    public void recordElevatorEvent(int elevatorId, String event) {
        logs.add(new SystemLog("Elevator " + elevatorId, event, System.currentTimeMillis()));
    }

    public void recordSchedulerEvent(String event) {
        logs.add(new SystemLog("Scheduler", event, System.currentTimeMillis()));
    }
    public void recordEvent(String source, String message) {
        logs.add(new SystemLog(source, message, System.currentTimeMillis()));
    }

    public List<SystemLog> queryLogs(String source, long startTime, long endTime) {
        return logs.stream()
                .filter(log -> log.getSource().equals(source)
                        && log.getTimestamp() >= startTime
                        && log.getTimestamp() <= endTime)
                .collect(Collectors.toList());
    }

    public static class SystemLog {
        private final String source;
        private final String message;
        private final long timestamp;

        public SystemLog(String source, String message, long timestamp) {
            this.source = source;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
