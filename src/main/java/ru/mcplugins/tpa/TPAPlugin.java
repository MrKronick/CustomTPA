package ru.mcplugins.tpa;

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
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.manager.UpdateChecker;
import ru.mcplugins.tpa.placeholder.TPAExpansion;
import ru.mcplugins.tpa.util.MessageUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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
    private java.util.logging.Logger requestLogger;
    private Set<UUID> knownPlayers = new HashSet<>();
    private File knownPlayersFile;

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configManager = new ConfigManager(this);
        requestManager = new RequestManager(this);
        updateChecker = new UpdateChecker(this, "customtpa");
        statsManager = new StatsManager(this);

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

        YamlConfiguration playersConfig = new YamlConfiguration();
        for (UUID uuid : knownPlayers) {
            playersConfig.set(uuid.toString(), true);
        }
        try {
            playersConfig.save(knownPlayersFile);
        } catch (IOException e) {
            getLogger().warning("Could not save players.yml");
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
    }

    public void logRequest(String message) {
        if (requestLogger != null) {
            requestLogger.info(message);
        }
    }

    private void registerCommands() {
        TPACommand tpaCommand = new TPACommand(this, requestManager, configManager, statsManager);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TPAHereCommand tpaHereCommand = new TPAHereCommand(this, requestManager, configManager, statsManager);
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
    }

    public static TPAPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public StatsManager getStatsManager() { return statsManager; }
}