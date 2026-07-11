package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.util.MessageUtil;

public class StatsCommand implements CommandExecutor {
    private final TPAPlugin plugin;
    private final ConfigManager config;
    private final StatsManager statsManager;

    public StatsCommand(TPAPlugin plugin, ConfigManager config, StatsManager statsManager) {
        this.plugin = plugin;
        this.config = config;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        int[] s = statsManager.getStats(player.getUniqueId());
        player.sendMessage("");
        MessageUtil.send(player, config.getMessage(player, "stats-header"));
        MessageUtil.send(player, config.formatRaw(player, null,
                config.getRawMessage(player, "stats-format")
                        .replace("%sent%", String.valueOf(s[0]))
                        .replace("%accepted%", String.valueOf(s[1]))
                        .replace("%denied%", String.valueOf(s[2]))
        ));
        player.sendMessage("");
        return true;
    }
}