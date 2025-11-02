package net.mooctest;

public class NoteMemento extends Memento {
    private final String noteContent;
    
    public NoteMemento(String noteContent) {
        super();
        this.noteContent = noteContent;
    }
    @Override
    public String getState() {
        return noteContent;
    }
}
