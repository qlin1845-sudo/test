package net.mooctest;

import java.util.*;

public class LabelSuggestionService {
    public List<Label> suggestLabels(Note note, Collection<Label> allLabels) {
        List<Label> suggestions = new ArrayList<>();
        String content = note.getContent().toLowerCase();
        for(Label label : allLabels) {
            if(content.contains(label.getName().toLowerCase())) {
                suggestions.add(label);
            }
        }
        return suggestions;
    }
}
