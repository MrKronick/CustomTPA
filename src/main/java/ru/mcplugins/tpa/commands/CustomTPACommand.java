package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomTPACommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final ConfigManager config;

    public CustomTPACommand(TPAPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            Component prefix = sender instanceof Player
                    ? config.getMessage((Player) sender, "prefix")
                    : Component.text("[TPA] ");
            Component info = prefix.append(
                    Component.text("CustomTPA v" + plugin.getDescription().getVersion() + " by MrKronick"));
            if (sender instanceof Player) {
                MessageUtil.send((Player) sender, info);
            } else {
                sender.sendMessage("[TPA] CustomTPA v" + plugin.getDescription().getVersion() + " by MrKronick");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            config.reload();
            Component msg = sender instanceof Player
                    ? config.getMessage((Player) sender, "config-reloaded")
                    : Component.text("Configuration reloaded");
            if (sender instanceof Player) {
                MessageUtil.send((Player) sender, msg);
            } else {
                sender.sendMessage("Configuration reloaded");
            }
            return true;
        }
        Component usage = sender instanceof Player
                ? config.getMessage((Player) sender, "prefix").append(Component.text("Usage: /ctpa reload"))
                : Component.text("[TPA] Usage: /ctpa reload");
        if (sender instanceof Player) {
            MessageUtil.send((Player) sender, usage);
        } else {
            sender.sendMessage("[TPA] Usage: /ctpa reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return null;
    }
}