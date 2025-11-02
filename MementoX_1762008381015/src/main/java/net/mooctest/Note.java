package net.mooctest;

import java.util.HashSet;
import java.util.Set;

public class Note implements Originator {
    private String content;
    private final Set<Label> labels = new HashSet<>();

    public Note(String content) {
        this.content = content == null ? "" : content;
    }

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }
    
    public Set<Label> getLabels() {
        return new HashSet<>(labels);
    }

    public void addLabel(Label label) {
        if (label != null) labels.add(label);
    }

    public void removeLabel(Label label) {
        labels.remove(label);
    }

    @Override
    public Memento createMemento() {
        return new NoteMemento(content);
    }

    @Override
    public void restoreMemento(Memento memento) throws MementoException {
        if (!(memento instanceof NoteMemento)) {
            throw new MementoException("Wrong memento type for Note");
        }
        this.content = ((NoteMemento) memento).getState();
    }
}
