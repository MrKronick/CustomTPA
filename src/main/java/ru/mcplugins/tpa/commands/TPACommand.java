package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.EconomyManager;
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.manager.StatsManager;
import ru.mcplugins.tpa.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class TPACommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;
    private final StatsManager statsManager;
    private final EconomyManager economyManager;

    public TPACommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config, StatsManager statsManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
        this.statsManager = statsManager;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("tpa.tpa")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }

        // Проверка фильтрации мира (отправитель)
        if (!player.hasPermission("customtpa.bypass.blacklist") && config.isWorldFiltered(player.getWorld().getName())) {
            MessageUtil.send(player, config.getMessage(player, "world-filtered-sender"));
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, Component.text("Usage: /tpa <player> or /tpa gui"));
            return true;
        }
        if (args[0].equalsIgnoreCase("gui")) {
            if (!player.hasPermission("tpa.gui")) {
                MessageUtil.send(player, config.getMessage(player, "no-permission"));
                return true;
            }
            ru.mcplugins.tpa.gui.GUIHandler.openGUI(player, requestManager, config);
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

        // Проверка фильтрации мира (цель)
        if (!player.hasPermission("customtpa.bypass.blacklist") && config.isWorldFiltered(target.getWorld().getName())) {
            MessageUtil.send(player, config.getMessage(player, "world-filtered-target"));
            return true;
        }

        int cooldown = config.getCooldown();
        if (!player.hasPermission("customtpa.bypass.cooldown") && requestManager.isOnCooldown(player.getUniqueId(), cooldown)) {
            long remaining = requestManager.getCooldownRemaining(player.getUniqueId(), cooldown);
            String msg = config.getRawMessage(player, "cooldown-active").replace("%seconds%", String.valueOf(remaining));
            MessageUtil.send(player, config.formatRaw(player, null, msg));
            return true;
        }

        int dailyLimit = config.getDailyLimit();
        if (!player.hasPermission("customtpa.bypass.limit") && requestManager.isDailyLimitReached(player.getUniqueId(), dailyLimit)) {
            MessageUtil.send(player, config.getMessage(player, "daily-limit-reached"));
            return true;
        }

        // ===== Экономика (Vault) =====
        if (economyManager.isEnabled() && !player.hasPermission("customtpa.bypass.cost")) {
            int cost = config.getTpaCost();
            if (cost > 0) {
                if (!economyManager.hasBalance(player, cost)) {
                    String msg = config.getRawMessage(player, "vault-insufficient-funds")
                            .replace("%cost%", economyManager.format(cost));
                    MessageUtil.send(player, config.formatRaw(player, null, msg));
                    return true;
                }
                economyManager.charge(player, cost);
                String msg = config.getRawMessage(player, "vault-charged")
                        .replace("%cost%", economyManager.format(cost));
                MessageUtil.send(player, config.formatRaw(player, null, msg));
            }
        }

        if (requestManager.getRequest(player.getUniqueId(), target.getUniqueId()) != null) {
            MessageUtil.send(player, config.getMessage(player, "already-have-request"));
            return true;
        }

        RequestManager.TPARequest request = new RequestManager.TPARequest(
                player.getUniqueId(), target.getUniqueId(), false);
        requestManager.addRequest(request);
        requestManager.setLastRequestTime(player.getUniqueId());
        requestManager.incrementDailyRequests(player.getUniqueId());
        statsManager.addSent(player.getUniqueId());
        plugin.logRequest(player.getName() + " sent a TPA request to " + target.getName());

        if (config.isRequestSoundEnabled()) {
            Sound sound = config.getRequestSound();
            if (sound != null) {
                target.playSound(target.getLocation(), sound, 1.0f, 1.0f);
            }
        }

        if (requestManager.isAutoAccept(target.getUniqueId())) {
            requestManager.removeRequest(request.getSender(), request.getTarget());
            Player teleporting = player;
            Player other = target;
            TPAcceptCommand.acceptRequest(plugin, teleporting, other, config, statsManager);
            return true;
        }

        sendSenderMessage(player, target);
        sendRequestMessage(target, player, false);
        return true;
    }

    private void sendSenderMessage(Player sender, Player target) {
        MessageUtil.send(sender, config.format(sender, target, "request-sent"));

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
            List<String> completions = new ArrayList<>();
            if (player.hasPermission("tpa.gui")) completions.add("gui");
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .forEach(completions::add);
            return completions;
        }
        return null;
    }
}