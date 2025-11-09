package net.mooctest;

import java.util.*;

public class NotificationService {
    private static volatile NotificationService instance;
    private final List<NotificationChannel> channels;

    public NotificationService() {
        channels = new ArrayList<>();
        channels.add(new SMSChannel());
        channels.add(new EmailChannel());
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    public void sendNotification(Notification notification) {
        for (NotificationChannel channel : channels) {
            if (channel.supports(notification.getType())) {
                channel.send(notification);
            }
        }
    }

    public interface NotificationChannel {
        boolean supports(NotificationType type);
        void send(Notification notification);
    }

    public static class SMSChannel implements NotificationChannel {
        @Override
        public boolean supports(NotificationType type) {
            return type == NotificationType.EMERGENCY || type == NotificationType.MAINTENANCE;
        }

        @Override
        public void send(Notification notification) {
            System.out.println("Sending SMS notification: " + notification.getMessage() + " to " + notification.getRecipients());
        }
    }

    public static class EmailChannel implements NotificationChannel {
        @Override
        public boolean supports(NotificationType type) {
            return true;
        }

        @Override
        public void send(Notification notification) {
            System.out.println("Sending email notification: " + notification.getMessage() + " to " + notification.getRecipients());
        }
    }

    public enum NotificationType {
        EMERGENCY, MAINTENANCE, SYSTEM_UPDATE, INFORMATION
    }

    public static class Notification {
        private final NotificationType type;
        private final String message;
        private final List<String> recipients;

        public Notification(NotificationType type, String message, List<String> recipients) {
            this.type = type;
            this.message = message;
            this.recipients = recipients;
        }

        public NotificationType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getRecipients() {
            return recipients;
        }
    }
}
