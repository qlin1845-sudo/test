package net.mooctest;

import java.util.*;

public class SearchService {
    public List<Note> searchByLabel(User user, Label label) {
        List<Note> res = new ArrayList<>();
        for (Note note : user.getNotes()) {
            if (note.getLabels().contains(label)) {
                res.add(note);
            }
        }
        return res;
    }

    public List<Note> searchByKeyword(User user, String keyword) {
        List<Note> res = new ArrayList<>();
        if(keyword == null) return res;
        for (Note note : user.getNotes()) {
            if(note.getContent().contains(keyword)) {
                res.add(note);
            }
        }
        return res;
    }

    public List<Note> searchByKeywordAllUsers(Collection<User> users, String keyword) {
        List<Note> res = new ArrayList<>();
        if(keyword == null) return res;
        for (User user : users) {
            res.addAll(searchByKeyword(user, keyword));
        }
        return res;
    }

    // merged from AdvancedSearchService
    public List<Note> fuzzySearch(User user, String keyword) {
        List<Note> result = new ArrayList<>();
        if (keyword == null || keyword.isEmpty()) return result;
        String lower = keyword.toLowerCase();
        for (Note note : user.getNotes()) {
            if (note.getContent().toLowerCase().contains(lower)) {
                result.add(note);
            }
        }
        return result;
    }

    public String highlight(String content, String keyword) {
        if (content == null || keyword == null) return content;
        return content.replaceAll("(?i)" + java.util.regex.Pattern.quote(keyword), "[[$0]]");
    }
}
