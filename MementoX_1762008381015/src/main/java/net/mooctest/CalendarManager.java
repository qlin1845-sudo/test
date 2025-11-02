package net.mooctest;

import java.util.*;
import java.text.*;

public class CalendarManager {
    private final Map<String, List<Note>> dateToNotes = new HashMap<>();
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");

    public void addNoteByDate(Note note, Date date) {
        String day = DAY_FORMAT.format(date);
        dateToNotes.computeIfAbsent(day, k -> new ArrayList<>()).add(note);
    }

    public List<Note> getNotesByDay(Date date) {
        String day = DAY_FORMAT.format(date);
        return dateToNotes.getOrDefault(day, Collections.emptyList());
    }

    public List<Note> getNotesByMonth(Date date) {
        String month = MONTH_FORMAT.format(date);
        List<Note> notes = new ArrayList<>();
        for(String key : dateToNotes.keySet()) {
            if(key.startsWith(month)) {
                notes.addAll(dateToNotes.get(key));
            }
        }
        return notes;
    }

    // Inlined Reminder class
    public static class Reminder {
        private final Note note;
        private final Date remindTime;
        private boolean triggered;

        public Reminder(Note note, Date remindTime) {
            if (note == null || remindTime == null) throw new IllegalArgumentException("null arg");
            this.note = note;
            this.remindTime = (Date) remindTime.clone();
            this.triggered = false;
        }
        public Note getNote() { return note; }
        public Date getRemindTime() { return (Date) remindTime.clone(); }
        public boolean isTriggered() { return triggered; }
        public void setTriggered(boolean t) { this.triggered = t; }
    }
}
