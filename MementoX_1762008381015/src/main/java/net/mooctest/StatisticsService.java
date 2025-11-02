package net.mooctest;

import java.util.*;

public class StatisticsService {
    public Map<Label, Integer> labelUsage(Collection<User> users) {
        Map<Label, Integer> map = new HashMap<>();
        for(User user : users) {
            for(Note note : user.getNotes()) {
                for(Label label : note.getLabels()) {
                    map.put(label, map.getOrDefault(label, 0) + 1);
                }
            }
        }
        return map;
    }

    public int noteCount(Collection<User> users) {
        int total = 0;
        for(User user : users) {
            total += user.getNotes().size();
        }
        return total;
    }
}
