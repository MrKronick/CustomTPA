package ru.mcplugins.tpa.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String currentVersion;
    private final String modrinthProjectId;
    private String latestVersion = null;

    public UpdateChecker(JavaPlugin plugin, String modrinthProjectId) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.modrinthProjectId = modrinthProjectId;
    }

    public void checkNow(java.util.function.Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + modrinthProjectId + "/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "CustomTPA-UpdateChecker");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();
                if (!versions.isEmpty()) {
                    JsonElement latest = versions.get(0).getAsJsonObject().get("version_number");
                    latestVersion = latest.getAsString();
                    return isUpdateAvailable();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
            return false;
        }).thenAccept(callback);
    }

    public boolean isUpdateAvailable() {
        if (latestVersion == null) return false;
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
            int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latest = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (latest > current) return true;
            if (latest < current) return false;
        }
        return false;
    }

    public void notifyPlayer(Player player) {
        checkNow(hasUpdate -> {
            if (hasUpdate) {
                Component message = Component.text()
                        .append(Component.text("✨ New version available ", TextColor.color(0xFFAA00)))
                        .append(Component.text("CustomTPA " + latestVersion + "! ", TextColor.color(0x55FFFF)))
                        .append(Component.text("Click here to open Modrinth", TextColor.color(0x55FF55))
                                .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/customtpa")))
                        .build();
                player.sendMessage(message);
            }
        });
    }

    public void notifyConsole() {
        checkNow(hasUpdate -> {
            if (hasUpdate) {
                plugin.getLogger().info("========================================");
                plugin.getLogger().info("A new version of CustomTPA is available: " + latestVersion);
                plugin.getLogger().info("Download at https://modrinth.com/plugin/customtpa");
                plugin.getLogger().info("========================================");
            }
        });
    }
}