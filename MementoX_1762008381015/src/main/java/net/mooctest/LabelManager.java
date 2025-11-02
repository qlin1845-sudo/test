package net.mooctest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LabelManager {
    private final Map<Label, Set<Note>> labelNoteMap = new HashMap<>();

    public void addLabelToNote(Label label, Note note) {
        labelNoteMap.computeIfAbsent(label, k -> new HashSet<>()).add(note);
        note.addLabel(label);
    }
    
    public void removeLabelFromNote(Label label, Note note) {
        Set<Note> notes = labelNoteMap.get(label);
        if(notes != null) {
            notes.remove(note);
            note.removeLabel(label);
            if(notes.isEmpty()) {
                labelNoteMap.remove(label);
            }
        }
    }

    public Set<Note> getNotesByLabel(Label label) {
        return labelNoteMap.getOrDefault(label, new HashSet<>());
    }

    public Set<Label> getAllLabels() {
        return new HashSet<>(labelNoteMap.keySet());
    }
}
