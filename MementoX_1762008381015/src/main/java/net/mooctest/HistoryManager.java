package net.mooctest;

import java.util.*;

public class HistoryManager {
    private final Originator originator;
    private final Map<String, List<Memento>> branches = new HashMap<>();
    private String currentBranch = "main";
    private int currentIndex = -1;

    public HistoryManager(Originator originator) {
        this.originator = originator;
        branches.put(currentBranch, new ArrayList<>());
        save();
    }

    // Linear history API (backward compatible)
    public void save() {
        List<Memento> list = branches.get(currentBranch);
        while (list.size() > currentIndex + 1) list.remove(list.size() - 1);
        list.add(originator.createMemento());
        currentIndex = list.size() - 1;
    }

    public void undo() throws MementoException {
        if (currentIndex <= 0) throw new MementoException("Cannot undo, no previous state available.");
        currentIndex--;
        originator.restoreMemento(branches.get(currentBranch).get(currentIndex));
    }

    public void redo() throws MementoException {
        List<Memento> list = branches.get(currentBranch);
        if (currentIndex >= list.size() - 1) throw new MementoException("Cannot redo, no next state available.");
        currentIndex++;
        originator.restoreMemento(list.get(currentIndex));
    }

    public List<Memento> getHistory() {
        return new ArrayList<>(branches.get(currentBranch));
    }

    public void clearHistory() {
        branches.get(currentBranch).clear();
        currentIndex = -1;
    }

    public void createBranch(String name) {
        if (!branches.containsKey(name)) {
            branches.put(name, new ArrayList<>());
        }
    }

    public void switchBranch(String name) throws MementoException {
        if (!branches.containsKey(name)) throw new MementoException("Branch not found: " + name);
        currentBranch = name;
        currentIndex = branches.get(name).size() - 1;
        if (currentIndex >= 0) {
            originator.restoreMemento(branches.get(currentBranch).get(currentIndex));
        }
    }

    public String getCurrentBranch() { return currentBranch; }

    public List<String> getAllBranches() { return new ArrayList<>(branches.keySet()); }
}
