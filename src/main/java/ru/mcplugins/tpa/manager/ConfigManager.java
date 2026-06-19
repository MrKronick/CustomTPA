package ru.mcplugins.tpa.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languageCache = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern OTHER_PLACEHOLDER = Pattern.compile("%other_([^%]+)%");
    private static final Map<String, String> LEGACY_COLORS = new LinkedHashMap<>();

    private static final List<String> BUNDLED_LANGUAGES = Arrays.asList(
        "messages.yml",
        "messages_ru.yml",
        "messages_uk.yml",
        "messages_de.yml",
        "messages_fr.yml",
        "messages_es.yml",
        "messages_pt_br.yml",
        "messages_tr.yml",
        "messages_it.yml",
        "messages_pl.yml",
		"messages_zh_cn.yml",
		"messages_ja.yml",
		"messages_fi.yml",
		"messages_ko.yml",
		"messages_sv.yml",
		"messages_nl.yml",
		"messages_cs.yml",
		"messages_hu.yml",
		"messages_ro.yml"
    );

    static {
        LEGACY_COLORS.put("&0", "<black>");    LEGACY_COLORS.put("§0", "<black>");
        LEGACY_COLORS.put("&1", "<dark_blue>"); LEGACY_COLORS.put("§1", "<dark_blue>");
        LEGACY_COLORS.put("&2", "<dark_green>");LEGACY_COLORS.put("§2", "<dark_green>");
        LEGACY_COLORS.put("&3", "<dark_aqua>");LEGACY_COLORS.put("§3", "<dark_aqua>");
        LEGACY_COLORS.put("&4", "<dark_red>");  LEGACY_COLORS.put("§4", "<dark_red>");
        LEGACY_COLORS.put("&5", "<dark_purple>");LEGACY_COLORS.put("§5", "<dark_purple>");
        LEGACY_COLORS.put("&6", "<gold>");      LEGACY_COLORS.put("§6", "<gold>");
        LEGACY_COLORS.put("&7", "<gray>");      LEGACY_COLORS.put("§7", "<gray>");
        LEGACY_COLORS.put("&8", "<dark_gray>"); LEGACY_COLORS.put("§8", "<dark_gray>");
        LEGACY_COLORS.put("&9", "<blue>");      LEGACY_COLORS.put("§9", "<blue>");
        LEGACY_COLORS.put("&a", "<green>");     LEGACY_COLORS.put("§a", "<green>");
        LEGACY_COLORS.put("&b", "<aqua>");      LEGACY_COLORS.put("§b", "<aqua>");
        LEGACY_COLORS.put("&c", "<red>");       LEGACY_COLORS.put("§c", "<red>");
        LEGACY_COLORS.put("&d", "<light_purple>");LEGACY_COLORS.put("§d", "<light_purple>");
        LEGACY_COLORS.put("&e", "<yellow>");    LEGACY_COLORS.put("§e", "<yellow>");
        LEGACY_COLORS.put("&f", "<white>");     LEGACY_COLORS.put("§f", "<white>");

        LEGACY_COLORS.put("&l", "<bold>");      LEGACY_COLORS.put("§l", "<bold>");
        LEGACY_COLORS.put("&o", "<italic>");    LEGACY_COLORS.put("§o", "<italic>");
        LEGACY_COLORS.put("&n", "<underlined>");LEGACY_COLORS.put("§n", "<underlined>");
        LEGACY_COLORS.put("&m", "<strikethrough>");LEGACY_COLORS.put("§m", "<strikethrough>");
        LEGACY_COLORS.put("&k", "<obfuscated>");LEGACY_COLORS.put("§k", "<obfuscated>");
        LEGACY_COLORS.put("&r", "<reset>");     LEGACY_COLORS.put("§r", "<reset>");
    }

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        updateLanguageFiles();
        loadLanguages();
    }

    private void updateLanguageFiles() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File versionFile = new File(plugin.getDataFolder(), "version.txt");
        String currentVersion = plugin.getDescription().getVersion();
        boolean shouldOverwrite = true;

        if (versionFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
                String savedVersion = reader.readLine();
                if (currentVersion.equals(savedVersion)) {
                    shouldOverwrite = false;
                }
            } catch (IOException ignored) {}
        }

        if (shouldOverwrite) {
            for (String fileName : BUNDLED_LANGUAGES) {
                File outFile = new File(langFolder, fileName);
                try (InputStream in = plugin.getResource("lang/" + fileName)) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Не удалось обновить " + fileName + ": " + e.getMessage());
                }
            }
            try (FileWriter writer = new FileWriter(versionFile)) {
                writer.write(currentVersion);
            } catch (IOException ignored) {}
        } else {
            for (String fileName : BUNDLED_LANGUAGES) {
                File file = new File(langFolder, fileName);
                if (!file.exists()) {
                    plugin.saveResource("lang/" + fileName, false);
                } else {
                    try {
                        YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
                        Reader jarReader = new InputStreamReader(
                                plugin.getResource("lang/" + fileName), StandardCharsets.UTF_8);
                        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(jarReader);

                        boolean updated = false;
                        for (String key : defaults.getKeys(true)) {
                            if (!existing.contains(key)) {
                                existing.set(key, defaults.get(key));
                                updated = true;
                            }
                        }

                        if (updated) {
                            existing.save(file);
                            plugin.getLogger().info("Добавлены новые ключи в " + fileName);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка обновления " + fileName + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File[] files = langFolder.listFiles((dir, name) -> name.startsWith("messages") && name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String langCode = "en";
                if (fileName.contains("_")) {
                    langCode = fileName.substring(fileName.indexOf('_') + 1, fileName.lastIndexOf('.'));
                }
                languageCache.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }

        if (!languageCache.containsKey("en")) {
            plugin.getLogger().warning("Английский язык не найден, использую резервный");
        }
    }

    public void setPlayerLanguage(Player player) {
        String locale = player.getLocale();
        String langCode = locale.split("_")[0].toLowerCase();

        if (languageCache.containsKey(langCode)) {
            playerLanguages.put(player.getUniqueId(), langCode);
        } else {
            playerLanguages.put(player.getUniqueId(), "en");
        }
    }

    public void removePlayerLanguage(Player player) {
        playerLanguages.remove(player.getUniqueId());
    }

    public Component getMessage(Player player, String key) {
        String langCode = "en";
        if (player != null) {
            langCode = playerLanguages.getOrDefault(player.getUniqueId(), "en");
        }
        FileConfiguration messages = languageCache.get(langCode);
        if (messages == null) messages = languageCache.get("en");

        String raw = messages.getString(key, "<red>Missing message: " + key + "</red>");
        if (raw.isEmpty()) return Component.empty();
        return miniMessage.deserialize(convertLegacyToMiniMessage(raw));
    }

    public Component getMessage(String key) {
        return getMessage(null, key);
    }

    public String getRawMessage(Player player, String key) {
        String langCode = "en";
        if (player != null) {
            langCode = playerLanguages.getOrDefault(player.getUniqueId(), "en");
        }
        FileConfiguration messages = languageCache.get(langCode);
        if (messages == null) messages = languageCache.get("en");
        return messages.getString(key, "");
    }

    public String getRawMessage(String key) {
        return getRawMessage(null, key);
    }

    public int getInt(String key, int def) {
        FileConfiguration messages = languageCache.get("en");
        if (messages == null) return def;
        return messages.getInt(key, def);
    }

    public Component format(Player receiver, Player other, String key) {
        String raw = getRawMessage(receiver, key);
        if (raw.isEmpty()) return Component.empty();
        return formatRaw(receiver, other, raw);
    }

    public Component formatRaw(Player receiver, Player other, String raw) {
        if (raw.isEmpty()) return Component.empty();

        if (other != null) {
            raw = raw.replace("%other_name%", other.getName());
        } else {
            raw = raw.replace("%other_name%", "Unknown");
        }

        if (other != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Matcher matcher = OTHER_PLACEHOLDER.matcher(raw);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String placeholder = "%" + matcher.group(1) + "%";
                String value = PlaceholderAPI.setPlaceholders(other, placeholder);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(sb);
            raw = sb.toString();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && receiver != null) {
            raw = PlaceholderAPI.setPlaceholders(receiver, raw);
        }

        return miniMessage.deserialize(convertLegacyToMiniMessage(raw));
    }

    public static String convertLegacyToMiniMessage(String input) {
        if (input == null) return "";
        String result = input;

        Pattern gradientHexPattern = Pattern.compile("[§&]x([§&][0-9A-Fa-f]){6}");
        Matcher gradientMatcher = gradientHexPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (gradientMatcher.find()) {
            String match = gradientMatcher.group();
            String hex = match.replaceAll("[§&x]", "").replaceAll("[§&]", "");
            gradientMatcher.appendReplacement(sb, "<#" + hex + ">");
        }
        gradientMatcher.appendTail(sb);
        result = sb.toString();

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(result);
        sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        result = sb.toString();

        for (Map.Entry<String, String> entry : LEGACY_COLORS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public void reload() {
        languageCache.clear();
        updateLanguageFiles();
        loadLanguages();
        playerLanguages.clear();
    }
}