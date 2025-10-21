package net.mooctest;

import java.util.*;
import java.util.concurrent.*;

public class EventBus {
    private static volatile EventBus instance;
    private final Map<EventType, List<EventListener>> listeners;

    public EventBus() {
        listeners = new ConcurrentHashMap<>();
    }

    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    public void subscribe(EventType eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void publish(Event event) {
        List<EventListener> eventListeners = listeners.get(event.getType());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }



    public static class Event {
        private final EventType type;
        private final Object data;

        public Event(EventType type, Object data) {
            this.type = type;
            this.data = data;
        }

        public EventType getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    public interface EventListener {
        void onEvent(Event event);
    }
}
