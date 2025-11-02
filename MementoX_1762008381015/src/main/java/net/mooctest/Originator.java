package net.mooctest;

public interface Originator {
    Memento createMemento();
    void restoreMemento(Memento memento) throws MementoException;
}
