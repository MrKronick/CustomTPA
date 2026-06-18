package ru.mcplugins.tpa.util;

import org.bukkit.plugin.java.JavaPlugin;

public class AdventureProvider {
    private static Object audience;

    public static void create(JavaPlugin plugin) {
        // Paper не использует BukkitAudiences
    }

    public static Object get() {
        return audience;
    }

    public static void close() {
        audience = null;
    }
}