package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

import java.util.List;
import java.util.stream.Collectors;

public class TPACommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPACommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки.");
            return true;
        }
        if (!player.hasPermission("tpa.tpa")) {
            player.sendMessage(config.getMessage(player, "no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Используйте: /tpa <игрок>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("gui")) {
            if (!player.hasPermission("tpa.gui")) {
                player.sendMessage(config.getMessage(player, "no-permission"));
                return true;
            }
            ru.mcplugins.tpa.gui.GUIHandler.openGUI(player, requestManager, config);
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(config.getMessage(player, "player-offline"));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(config.getMessage(player, "self-request"));
            return true;
        }
        if (requestManager.isToggled(target.getUniqueId())) {
            player.sendMessage(config.getMessage(player, "toggle-blocked"));
            return true;
        }
        if (requestManager.getRequest(player.getUniqueId(), target.getUniqueId()) != null) {
            player.sendMessage(config.getMessage(player, "already-have-request"));
            return true;
        }

        RequestManager.TPARequest request = new RequestManager.TPARequest(
                player.getUniqueId(), target.getUniqueId(), false);
        requestManager.addRequest(request);

        sendSenderMessage(player, target);
        sendRequestMessage(target, player, false);
        return true;
    }

    private void sendSenderMessage(Player sender, Player target) {
        sender.sendMessage(config.format(sender, target, "request-sent"));

        Component prompt = config.getMessage(sender, "cancel-prompt");
        Component cancelBtn = config.getMessage(sender, "cancel-button-text")
                .clickEvent(ClickEvent.runCommand("/tpcancel"))
                .hoverEvent(HoverEvent.showText(config.getMessage(sender, "cancel-hover")));
        sender.sendMessage(prompt.append(cancelBtn));
    }

    private void sendRequestMessage(Player receiver, Player other, boolean isHere) {
        String key = isHere ? "request-here-received" : "request-received";
        Component base = config.format(receiver, other, key);

        Component acceptPrompt = config.getMessage(receiver, "accept-prompt");
        Component acceptBtn = config.getMessage(receiver, "accept-button-text")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + other.getName()))
                .hoverEvent(HoverEvent.showText(config.getMessage(receiver, "accept-hover")));

        Component denyPrompt = config.getMessage(receiver, "deny-prompt");
        Component denyBtn = config.getMessage(receiver, "deny-button-text")
                .clickEvent(ClickEvent.runCommand("/tpadeny " + other.getName()))
                .hoverEvent(HoverEvent.showText(config.getMessage(receiver, "deny-hover")));

        receiver.sendMessage(base);
        receiver.sendMessage(acceptPrompt.append(acceptBtn));
        receiver.sendMessage(denyPrompt.append(denyBtn));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return null;
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}