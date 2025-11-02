package net.mooctest;

import java.util.Date;
import java.util.UUID;

public abstract class Memento {
    private final String id;
    private final Date timestamp;

    public Memento() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
    }

    public String getId() {
        return id;
    }

    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    public abstract Object getState();
}
