package ru.mcplugins.tpa.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final TPAPlugin plugin;
    private final Map<UUID, int[]> stats = new HashMap<>(); // [sent, accepted, denied]
    private final File file;
    private FileConfiguration data;

    public StatsManager(TPAPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml");
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int sent = data.getInt(key + ".sent", 0);
                int accepted = data.getInt(key + ".accepted", 0);
                int denied = data.getInt(key + ".denied", 0);
                stats.put(uuid, new int[]{sent, accepted, denied});
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, int[]> entry : stats.entrySet()) {
            String uuid = entry.getKey().toString();
            int[] arr = entry.getValue();
            data.set(uuid + ".sent", arr[0]);
            data.set(uuid + ".accepted", arr[1]);
            data.set(uuid + ".denied", arr[2]);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml");
        }
    }

    public void addSent(UUID uuid) {
        int[] arr = stats.computeIfAbsent(uuid, k -> new int[3]);
        arr[0]++;
    }

    public void addAccepted(UUID uuid) {
        int[] arr = stats.computeIfAbsent(uuid, k -> new int[3]);
        arr[1]++;
    }

    public void addDenied(UUID uuid) {
        int[] arr = stats.computeIfAbsent(uuid, k -> new int[3]);
        arr[2]++;
    }

    public int[] getStats(UUID uuid) {
        return stats.getOrDefault(uuid, new int[3]);
    }
}