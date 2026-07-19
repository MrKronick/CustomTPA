package ru.mcplugins.tpa;

import org.bstats.bukkit.Metrics;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mcplugins.tpa.commands.*;
import ru.mcplugins.tpa.gui.GUIListener;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.EconomyManager;
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.manager.UpdateChecker;
import ru.mcplugins.tpa.placeholder.TPAExpansion;
import ru.mcplugins.tpa.util.MessageUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public final class TPAPlugin extends JavaPlugin implements Listener {

    private static TPAPlugin instance;
    private RequestManager requestManager;
    private ConfigManager configManager;
    private UpdateChecker updateChecker;
    private StatsManager statsManager;
    private EconomyManager economyManager;
    private java.util.logging.Logger requestLogger;
    private Set<UUID> knownPlayers = new HashSet<>();
    private File knownPlayersFile;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        printLogo();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configManager = new ConfigManager(this);
        requestManager = new RequestManager(this);
        updateChecker = new UpdateChecker(this, "customtpa");
        statsManager = new StatsManager(this);
        economyManager = new EconomyManager(this);

        try {
            int pluginId = 32220;
            new Metrics(this, pluginId);
        } catch (Exception | NoClassDefFoundError e) {
            getLogger().warning("bStats could not be initialized: " + e.getMessage());
        }

        if (configManager.isRequestLoggingEnabled()) {
            try {
                File logFile = new File(getDataFolder(), configManager.getRequestLogFile());
                FileHandler fh = new FileHandler(logFile.getAbsolutePath(), true);
                fh.setFormatter(new SimpleFormatter());
                requestLogger = java.util.logging.Logger.getLogger("CustomTPA-Requests");
                requestLogger.addHandler(fh);
                requestLogger.setUseParentHandlers(false);
            } catch (IOException e) {
                getLogger().warning("Could not set up request logger: " + e.getMessage());
            }
        }

        knownPlayersFile = new File(getDataFolder(), "players.yml");
        if (knownPlayersFile.exists()) {
            YamlConfiguration playersConfig = YamlConfiguration.loadConfiguration(knownPlayersFile);
            for (String key : playersConfig.getKeys(false)) {
                try {
                    knownPlayers.add(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        registerCommands();
        getServer().getPluginManager().registerEvents(new GUIListener(this, requestManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TPAExpansion(this, requestManager).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        updateChecker.notifyConsole();
        getLogger().info("CustomTPA v" + getDescription().getVersion() + " by MrKronick loaded!");
    }

    @Override
    public void onDisable() {
        if (requestManager != null) requestManager.clearAll();
        if (statsManager != null) statsManager.save();

        if (knownPlayersFile != null) {
            YamlConfiguration playersConfig = new YamlConfiguration();
            for (UUID uuid : knownPlayers) {
                playersConfig.set(uuid.toString(), true);
            }
            try {
                playersConfig.save(knownPlayersFile);
            } catch (IOException e) {
                getLogger().warning("Could not save players.yml");
            }
        }

        if (requestLogger != null) {
            requestLogger.info("CustomTPA disabled.");
        }
        getLogger().info("CustomTPA disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        configManager.setPlayerLanguage(player);
        if (player.hasPermission("customtpa.reload")) {
            updateChecker.notifyPlayer(player);
        }
        if (configManager.isWarmWelcomeEnabled() && !knownPlayers.contains(player.getUniqueId())) {
            MessageUtil.send(player, configManager.getMessage(player, "warm-welcome-message"));
            knownPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        configManager.removePlayerLanguage(event.getPlayer());
        lastLocations.remove(event.getPlayer().getUniqueId());
    }

    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public Location consumeLastLocation(Player player) {
        return lastLocations.remove(player.getUniqueId());
    }

    public void logRequest(String message) {
        if (requestLogger != null) {
            requestLogger.info(message);
        }
    }

    private void registerCommands() {
        TPACommand tpaCommand = new TPACommand(this, requestManager, configManager, statsManager, economyManager);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TPAHereCommand tpaHereCommand = new TPAHereCommand(this, requestManager, configManager, statsManager, economyManager);
        getCommand("tpahere").setExecutor(tpaHereCommand);
        getCommand("tpahere").setTabCompleter(tpaHereCommand);

        TPAcceptCommand tpAcceptCommand = new TPAcceptCommand(this, requestManager, configManager, statsManager);
        getCommand("tpaccept").setExecutor(tpAcceptCommand);
        getCommand("tpaccept").setTabCompleter(tpAcceptCommand);

        TPADenyCommand tpDenyCommand = new TPADenyCommand(this, requestManager, configManager, statsManager);
        getCommand("tpadeny").setExecutor(tpDenyCommand);
        getCommand("tpadeny").setTabCompleter(tpDenyCommand);

        getCommand("tpcancel").setExecutor(new TPACancelCommand(this, requestManager, configManager));
        getCommand("tpatoggle").setExecutor(new TPAToggleCommand(this, requestManager, configManager));

        CustomTPACommand ctpaCommand = new CustomTPACommand(this, configManager);
        getCommand("customtpa").setExecutor(ctpaCommand);
        getCommand("customtpa").setTabCompleter(ctpaCommand);

        getCommand("tpaauto").setExecutor(new TPAAutoCommand(this, requestManager, configManager));
        getCommand("tpastats").setExecutor(new StatsCommand(this, configManager, statsManager));
        getCommand("tpaback").setExecutor(new TPABackCommand(this, configManager));
        getCommand("tpaforce").setExecutor(new TPAForceCommand(this, requestManager, configManager));
    }

    private void printLogo() {
        String reset = "\u001B[0m";
        String bold = "\u001B[1m";

        String[] colors = {
            "\u001B[38;5;226m",
            "\u001B[38;5;220m",
            "\u001B[38;5;214m",
            "\u001B[38;5;205m",
            "\u001B[38;5;135m",
            "\u001B[38;5;55m"
        };

        String[] lines = {
            "   ██████╗██╗   ██╗███████╗████████╗ ██████╗ ███╗   ███╗████████╗██████╗  █████╗ ",
            "  ██╔════╝██║   ██║██╔════╝╚══██╔══╝██╔═══██╗████╗ ████║╚══██╔══╝██╔══██╗██╔══██╗",
            "  ██║     ██║   ██║███████╗   ██║   ██║   ██║██╔████╔██║   ██║   ██████╔╝███████║",
            "  ██║     ██║   ██║╚════██║   ██║   ██║   ██║██║╚██╔╝██║   ██║   ██╔═══╝ ██╔══██║",
            "  ╚██████╗╚██████╔╝███████║   ██║   ╚██████╔╝██║ ╚═╝ ██║   ██║   ██║     ██║  ██║",
            "   ╚═════╝ ╚═════╝ ╚══════╝   ╚═╝    ╚═════╝ ╚═╝     ╚═╝   ╚═╝   ╚═╝     ╚═╝  ╚═╝"
        };

        StringBuilder logo = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            logo.append(colors[i]).append(lines[i]).append(reset).append("\n");
        }

        String cyan = "\u001B[36m";
        String gold = "\u001B[33m";

        logo.append("\n");
        logo.append(cyan).append(bold).append("   ═══════════════════════════════════════════════════════════════").append(reset).append("\n");
        logo.append(cyan).append(bold).append("   ✦ Teleport Requests Plugin v").append(getDescription().getVersion()).append(" ✦ 70 Languages ✦ by ").append(gold).append("MrKronick").append(reset).append(cyan).append(bold).append(" ✦").append(reset).append("\n");
        logo.append(cyan).append(bold).append("   ═══════════════════════════════════════════════════════════════").append(reset).append("\n");
        logo.append(cyan).append("   ✦ Running on ").append(getServer().getName()).append(" ").append(getServer().getVersion().split("-")[0]).append(reset).append("\n");
        logo.append(cyan).append("   ✦ Website: https://customtpa.mrkronick.ru").append(reset).append("\n");
        logo.append(cyan).append(bold).append("   ═══════════════════════════════════════════════════════════════").append(reset);

        getLogger().info("\n" + logo.toString());
    }

    public static TPAPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
