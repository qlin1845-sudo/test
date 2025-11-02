package net.mooctest;

import java.util.*;

public class Label {
    private final String name;
    private final Label parent;
    private final List<Label> children;

    public Label(String name) {
        if(name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Label name cannot be null or empty");
        this.name = name.trim();
        this.parent = null;
        this.children = Collections.emptyList();
    }

    public Label(String name, Label parent) {
        if(name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Label name cannot be null or empty");
        this.name = name.trim();
        this.parent = parent;
        this.children = new ArrayList<>();
        if (parent instanceof Label && parent.children instanceof ArrayList) {
            parent.children.add(this);
        }
    }
    
    public String getName() {
        return name;
    }
    public Label getParent() { return parent; }
    public List<Label> getChildren() { return Collections.unmodifiableList(children); }

    public String getFullPath() {
        if (parent == null) return getName();
        return (parent instanceof Label ? ((Label) parent).getFullPath() : parent.getName()) + "/" + getName();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return name.equals(label.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
