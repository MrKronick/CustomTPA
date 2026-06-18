package ru.mcplugins.tpa;

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
import ru.mcplugins.tpa.manager.UpdateChecker;
import ru.mcplugins.tpa.placeholder.TPAExpansion;
import ru.mcplugins.tpa.util.AdventureProvider;

public final class TPAPlugin extends JavaPlugin implements Listener {

    private static TPAPlugin instance;
    private RequestManager requestManager;
    private ConfigManager configManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        AdventureProvider.create(this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configManager = new ConfigManager(this);
        requestManager = new RequestManager(this);
        updateChecker = new UpdateChecker(this, "customtpa");

        registerCommands();
        getServer().getPluginManager().registerEvents(new GUIListener(this, requestManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TPAExpansion(this, requestManager).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        updateChecker.notifyConsole();
        getLogger().info("CustomTPA v" + getDescription().getVersion() + " by MrKronick загружен!");
    }

    @Override
    public void onDisable() {
        if (requestManager != null) requestManager.clearAll();
        AdventureProvider.close();
        getLogger().info("CustomTPA выключен.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        configManager.setPlayerLanguage(player);
        if (player.hasPermission("customtpa.reload")) {
            updateChecker.notifyPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        configManager.removePlayerLanguage(event.getPlayer());
    }

    private void registerCommands() {
        TPACommand tpaCommand = new TPACommand(this, requestManager, configManager);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TPAHereCommand tpaHereCommand = new TPAHereCommand(this, requestManager, configManager);
        getCommand("tpahere").setExecutor(tpaHereCommand);
        getCommand("tpahere").setTabCompleter(tpaHereCommand);

        TPAcceptCommand tpAcceptCommand = new TPAcceptCommand(this, requestManager, configManager);
        getCommand("tpaccept").setExecutor(tpAcceptCommand);
        getCommand("tpaccept").setTabCompleter(tpAcceptCommand);

        TPADenyCommand tpDenyCommand = new TPADenyCommand(this, requestManager, configManager);
        getCommand("tpadeny").setExecutor(tpDenyCommand);
        getCommand("tpadeny").setTabCompleter(tpDenyCommand);

        getCommand("tpcancel").setExecutor(new TPACancelCommand(this, requestManager, configManager));
        getCommand("tpatoggle").setExecutor(new TPAToggleCommand(this, requestManager, configManager));
        getCommand("customtpa").setExecutor(new CustomTPACommand(this, configManager));
        getCommand("tpaauto").setExecutor(new TPAAutoCommand(this, requestManager, configManager));
    }

    public static TPAPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
}