package net.mooctest;

import java.util.*;

public class PluginManager {
    private final List<Plugin> plugins = new ArrayList<>();
    
    public void register(Plugin plugin) {
        if(plugin != null) plugins.add(plugin);
    }
    public List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }
    public void executeAll(UserManager userManager) {
        for(Plugin plugin : plugins) {
            plugin.execute(userManager);
        }
    }
}
