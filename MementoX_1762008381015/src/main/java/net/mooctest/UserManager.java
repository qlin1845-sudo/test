package net.mooctest;

import java.util.*;

public class UserManager {
    private final Map<String, User> users = new HashMap<>();

    public User registerUser(String name) {
        if (users.containsKey(name)) {
            throw new IllegalArgumentException("User already exists");
        }
        User user = new User(name);
        users.put(name, user);
        return user;
    }

    public User getUser(String name) {
        return users.get(name);
    }

    public void removeUser(String name) {
        users.remove(name);
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }
}
