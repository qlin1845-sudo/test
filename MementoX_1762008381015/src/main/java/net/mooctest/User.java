package net.mooctest;

import java.util.*;

public class User {
    private final String name;
    private final List<Note> notes = new ArrayList<>();
    private final Map<Note, HistoryManager> notesHistory = new HashMap<>();

    public User(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Username can't be null or empty");
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public List<Note> getNotes() {
        return new ArrayList<>(notes);
    }

    public void addNote(Note note) {
        if (note != null && !notes.contains(note)) {
            notes.add(note);
            notesHistory.put(note, new HistoryManager(note));
        }
    }

    public void removeNote(Note note) {
        notes.remove(note);
        notesHistory.remove(note);
    }

    public HistoryManager getHistoryManager(Note note) {
        return notesHistory.get(note);
    }
}
