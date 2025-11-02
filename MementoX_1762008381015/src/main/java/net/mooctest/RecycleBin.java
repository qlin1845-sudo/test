package net.mooctest;

import java.util.*;

public class RecycleBin {
    private final Set<Note> bin = new HashSet<>();

    public void recycle(Note note) {
        if(note != null) bin.add(note);
    }
    public boolean restore(Note note) {
        return bin.remove(note);
    }
    public void clear() {
        bin.clear();
    }
    public boolean isInBin(Note note) {
        return bin.contains(note);
    }
    public Set<Note> listDeletedNotes() {
        return new HashSet<>(bin);
    }
}
