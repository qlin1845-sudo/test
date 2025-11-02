package net.mooctest;

import java.util.*;

public class PermissionManager {
    private final Map<User, Permission> userPermissionMap = new HashMap<>();
    
    public void grantPermission(User user, Permission permission) {
        if(user != null && permission != null) {
            userPermissionMap.put(user, permission);
        }
    }
    public void revokePermission(User user) {
        if(user != null) userPermissionMap.remove(user);
    }
    public Permission getPermission(User user) {
        return userPermissionMap.get(user);
    }
    public boolean canEdit(User user) {
        Permission p = getPermission(user);
        return p == Permission.OWNER || p == Permission.EDIT;
    }
    public boolean canView(User user) {
        return getPermission(user) != null;
    }
    public Set<User> listCollaborators() {
        return new HashSet<>(userPermissionMap.keySet());
    }
}
