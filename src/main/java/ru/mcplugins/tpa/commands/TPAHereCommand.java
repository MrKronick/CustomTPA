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
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.util.MessageUtil;

import java.util.List;
import java.util.stream.Collectors;

public class TPAHereCommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;
    private final StatsManager statsManager;

    public TPAHereCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config, StatsManager statsManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }
        if (!player.hasPermission("tpa.tpahere")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }

        if (config.isWorldBlacklisted(player.getWorld().getName())) {
            MessageUtil.send(player, config.getMessage(player, "world-blacklisted-sender"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, Component.text("Usage: /tpahere <player>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(player, config.getMessage(player, "player-offline"));
            return true;
        }
        if (target.equals(player)) {
            MessageUtil.send(player, config.getMessage(player, "self-request"));
            return true;
        }
        if (requestManager.isToggled(target.getUniqueId())) {
            MessageUtil.send(player, config.getMessage(player, "toggle-blocked"));
            return true;
        }

        if (config.isWorldBlacklisted(target.getWorld().getName())) {
            MessageUtil.send(player, config.getMessage(player, "world-blacklisted-target"));
            return true;
        }

        int cooldown = config.getCooldown();
        if (!player.hasPermission("customtpa.bypass.cooldown") && requestManager.isOnCooldown(player.getUniqueId(), cooldown)) {
            long remaining = requestManager.getCooldownRemaining(player.getUniqueId(), cooldown);
            String msg = config.getRawMessage(player, "cooldown-active").replace("%seconds%", String.valueOf(remaining));
            MessageUtil.send(player, config.formatRaw(player, null, msg));
            return true;
        }

        if (requestManager.getRequest(player.getUniqueId(), target.getUniqueId()) != null) {
            MessageUtil.send(player, config.getMessage(player, "already-have-request"));
            return true;
        }

        RequestManager.TPARequest request = new RequestManager.TPARequest(
                player.getUniqueId(), target.getUniqueId(), true);
        requestManager.addRequest(request);
        requestManager.setLastRequestTime(player.getUniqueId());
        statsManager.addSent(player.getUniqueId());

        if (requestManager.isAutoAccept(target.getUniqueId())) {
            requestManager.removeRequest(request.getSender(), request.getTarget());
            Player teleporting = target;
            Player other = player;
            TPAcceptCommand.acceptRequest(plugin, teleporting, other, config, statsManager);
            return true;
        }

        sendSenderMessage(player, target);
        sendRequestMessage(target, player, true);
        return true;
    }

    private void sendSenderMessage(Player sender, Player target) {
        MessageUtil.send(sender, config.format(sender, target, "request-here-sent"));

        Component prompt = config.getMessage(sender, "cancel-prompt");
        Component cancelBtn = config.getMessage(sender, "cancel-button-text")
                .clickEvent(ClickEvent.runCommand("/tpcancel"))
                .hoverEvent(HoverEvent.showText(config.getMessage(sender, "cancel-hover")));
        MessageUtil.send(sender, prompt.append(cancelBtn));
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

        MessageUtil.send(receiver, base);
        MessageUtil.send(receiver, acceptPrompt.append(acceptBtn));
        MessageUtil.send(receiver, denyPrompt.append(denyBtn));
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