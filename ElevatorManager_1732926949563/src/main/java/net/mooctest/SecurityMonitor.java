package net.mooctest;

import java.util.*;
import java.util.concurrent.*;

public class SecurityMonitor implements EventBus.EventListener {
    private static volatile SecurityMonitor instance;
    private final List<SecurityEvent> securityEvents;
    private final ExecutorService executorService;
    private final LogManager logManager;

    public SecurityMonitor() {
        this.securityEvents = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.logManager = LogManager.getInstance();
        EventBus.getInstance().subscribe(EventType.EMERGENCY, this);
    }

    public static SecurityMonitor getInstance() {
        if (instance == null) {
            synchronized (SecurityMonitor.class) {
                if (instance == null) {
                    instance = new SecurityMonitor();
                }
            }
        }
        return instance;
    }

    @Override
    public void onEvent(EventBus.Event event) {
        if (event.getType() == EventType.EMERGENCY) {
            handleEmergency(event.getData());
        }
    }

    public void handleEmergency(Object data) {
        SecurityEvent securityEvent = new SecurityEvent("Emergency situation", System.currentTimeMillis(), data);
        securityEvents.add(securityEvent);
        logManager.recordEvent("SecurityMonitor", "Handling emergency: " + data);
        NotificationService.getInstance().sendNotification(
                new NotificationService.Notification(
                        NotificationService.NotificationType.EMERGENCY,
                        "Emergency situation detected: " + data,
                        Arrays.asList("security@building.com")
                )
        );
        Scheduler.getInstance().executeEmergencyProtocol();
    }

    public static class SecurityEvent {
        private final String description;
        private final long timestamp;
        private final Object data;

        public SecurityEvent(String description, long timestamp, Object data) {
            this.description = description;
            this.timestamp = timestamp;
            this.data = data;
        }

        public String getDescription() {
            return description;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Object getData() {
            return data;
        }
    }
}
