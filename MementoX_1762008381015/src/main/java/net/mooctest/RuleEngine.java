package net.mooctest;

import java.util.*;

public class RuleEngine {
    public interface Rule {
        void apply(Note note, UserManager userManager);
    }
    private final List<Rule> rules = new ArrayList<>();

    public void addRule(Rule rule) {
        if(rule != null) rules.add(rule);
    }
    public void applyAll(Note note, UserManager userManager) {
        for(Rule rule : rules) {
            rule.apply(note, userManager);
        }
    }
    public List<Rule> getRules() { return Collections.unmodifiableList(rules); }
}
