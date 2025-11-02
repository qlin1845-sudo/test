package net.mooctest;

import java.util.*;

public class Caretaker {
    private final List<Memento> history = new ArrayList<>();
    private int currentIndex = -1;

    public void save(Memento memento) {
        while(history.size() > currentIndex + 1) {
            history.remove(history.size()-1);
        }
        history.add(memento);
        currentIndex = history.size() - 1;
    }
    
    public Memento undo() throws MementoException {
        if(currentIndex <= 0) throw new MementoException("Cannot undo, no previous state available.");
        currentIndex--;
        return history.get(currentIndex);
    }
    
    public Memento redo() throws MementoException {
        if(currentIndex >= history.size() - 1) throw new MementoException("Cannot redo, no next state available.");
        currentIndex++;
        return history.get(currentIndex);
    }

    public Memento getCurrent() throws MementoException {
        if(currentIndex < 0 || currentIndex >= history.size()) throw new MementoException("No current memento available.");
        return history.get(currentIndex);
    }
    
    public List<Memento> getAllHistory() {
        return new ArrayList<>(history);
    }
    
    public void clear() {
        history.clear();
        currentIndex = -1;
    }
}
