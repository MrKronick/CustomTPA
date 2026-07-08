package ru.mcplugins.tpa.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
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

    private FileConfiguration config;
    private List<String> filterList;
    private String filterMode;

    private static final List<String> BUNDLED_LANGUAGES = Arrays.asList(
        "messages.yml", "messages_ru.yml", "messages_uk.yml", "messages_de.yml",
        "messages_fr.yml", "messages_es.yml", "messages_pt_br.yml", "messages_tr.yml",
        "messages_it.yml", "messages_pl.yml", "messages_zh_cn.yml", "messages_ja.yml",
        "messages_fi.yml", "messages_ko.yml", "messages_sv.yml", "messages_nl.yml",
        "messages_cs.yml", "messages_hu.yml", "messages_ro.yml",
        "messages_nb.yml", "messages_da.yml", "messages_el.yml", "messages_id.yml",
        "messages_ar.yml", "messages_vi.yml",
        "messages_sk.yml", "messages_bg.yml", "messages_he.yml", "messages_th.yml", "messages_tl.yml",
        "messages_hi.yml", "messages_pt.yml", "messages_hr.yml", "messages_sl.yml",
        "messages_lt.yml", "messages_lv.yml", "messages_et.yml", "messages_sr.yml",
        "messages_mk.yml", "messages_ca.yml", "messages_bn.yml", "messages_ur.yml",
        "messages_fa.yml", "messages_ms.yml", "messages_sw.yml", "messages_af.yml",
        "messages_is.yml", "messages_gl.yml", "messages_eu.yml", "messages_eo.yml",
        "messages_pa.yml", "messages_mr.yml", "messages_te.yml", "messages_ta.yml",
        "messages_gu.yml", "messages_kn.yml", "messages_ml.yml", "messages_ne.yml",
        "messages_si.yml", "messages_my.yml", "messages_kk.yml", "messages_uz.yml",
        "messages_az.yml", "messages_ka.yml", "messages_hy.yml"
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
        loadConfig();
        updateLanguageFiles();
        loadLanguages();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        filterMode = config.getString("world-filter-mode", "blacklist").toLowerCase();
        filterList = config.getStringList("world-filter-list");
        if (filterList == null) filterList = Collections.emptyList();
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
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(versionFile))) {
                String savedVersion = reader.readLine();
                if (currentVersion.equals(savedVersion)) {
                    shouldOverwrite = false;
                }
            } catch (java.io.IOException ignored) {}
        }

        if (shouldOverwrite) {
            for (String fileName : BUNDLED_LANGUAGES) {
                File outFile = new File(langFolder, fileName);
                try (java.io.InputStream in = plugin.getResource("lang/" + fileName)) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning("Could not update " + fileName + ": " + e.getMessage());
                }
            }
            try (java.io.FileWriter writer = new java.io.FileWriter(versionFile)) {
                writer.write(currentVersion);
            } catch (java.io.IOException ignored) {}
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
                            plugin.getLogger().info("Language file updated: " + fileName + " (new keys added)");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error updating " + fileName + ": " + e.getMessage());
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
            plugin.getLogger().warning("English language not found, using fallback");
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

    public int getCooldown() {
        return config.getInt("cooldown", 0);
    }

    public int getDelay() {
        return config.getInt("delay", 3);
    }

    public Sound getCountdownSound() {
        String soundName = config.getString("countdown-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid countdown sound in config.yml: " + soundName + ". Using default.");
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }

    public int getRequestTimeout() {
        return config.getInt("request-timeout", 60);
    }

    public boolean isRequestSoundEnabled() {
        return config.getBoolean("request-sound.enabled", true);
    }

    public Sound getRequestSound() {
        String soundName = config.getString("request-sound.sound", "BLOCK_NOTE_BLOCK_PLING");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid request sound in config.yml: " + soundName);
            return null;
        }
    }

    public int getDailyLimit() {
        return config.getInt("daily-limit", 0);
    }

    public boolean isWarmWelcomeEnabled() {
        return config.getBoolean("warm-welcome.enabled", false);
    }

    public String getWarmWelcomeMessage() {
        return config.getString("warm-welcome.message", "");
    }

    public boolean isTeleportEffectsEnabled() {
        return config.getBoolean("teleport-effects.enabled", false);
    }

    public String getTeleportParticle() {
        return config.getString("teleport-effects.particle", "PORTAL");
    }

    public int getTeleportParticleCount() {
        return config.getInt("teleport-effects.count", 30);
    }

    public boolean isRequestLoggingEnabled() {
        return config.getBoolean("request-logging.enabled", false);
    }

    public String getRequestLogFile() {
        return config.getString("request-logging.file", "tpa.log");
    }

    public boolean isWorldFiltered(String worldName) {
        if (filterList.isEmpty()) return false;

        if (filterMode.equals("whitelist")) {
            return !filterList.contains(worldName);
        } else {
            return filterList.contains(worldName);
        }
    }

    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("update-checker", true);
    }

    public boolean isTpaBackEnabled() {
        return config.getBoolean("tpa-back.enabled", true);
    }

    public int getMaxLogDays() {
        return config.getInt("max-log-days", 7);
    }

    public boolean isForceTpaEnabled() {
        return config.getBoolean("force-tpa.enabled", true);
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

        Pattern gradientHexPattern = Pattern.compile("[§&]x([§&][0-9A-Fa-f]){6}");
        Matcher gradientMatcher = gradientHexPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (gradientMatcher.find()) {
            String match = gradientMatcher.group();
            String hex = match.replaceAll("[§&x]", "").replaceAll("[§&]", "");
            gradientMatcher.appendReplacement(sb, "<#" + hex + ">");
        }
        gradientMatcher.appendTail(sb);
        String result = sb.toString();

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
        loadConfig();
        updateLanguageFiles();
        loadLanguages();
    }
}