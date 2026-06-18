package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;
import ru.mcplugins.tpa.util.MessageUtil;

import java.util.List;
import java.util.stream.Collectors;

public class TPAcceptCommand implements CommandExecutor, TabCompleter {
    private final TPAPlugin plugin;
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPAcceptCommand(TPAPlugin plugin, RequestManager requestManager, ConfigManager config) {
        this.plugin = plugin;
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!player.hasPermission("tpa.accept")) {
            MessageUtil.send(player, config.getMessage(player, "no-permission"));
            return true;
        }

        List<RequestManager.TPARequest> incoming = requestManager.getIncomingRequests(player.getUniqueId());

        if (args.length == 0) {
            if (incoming.isEmpty()) {
                MessageUtil.send(player, config.getMessage(player, "no-requests"));
                return true;
            }
            Component list = Component.join(
                net.kyori.adventure.text.JoinConfiguration.commas(true),
                incoming.stream()
                    .map(r -> {
                        String name = Bukkit.getOfflinePlayer(r.getSender()).getName();
                        return Component.text(name != null ? name : "Unknown")
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/tpaccept " + name));
                    })
                    .toArray(Component[]::new)
            );
            MessageUtil.send(player, config.getMessage(player, "prefix").append(Component.text("Запросы: ")).append(list));
            return true;
        }

        if (incoming.isEmpty()) {
            MessageUtil.send(player, config.getMessage(player, "no-requests"));
            return true;
        }

        RequestManager.TPARequest request;
        String name = args[0];
        request = incoming.stream()
                .filter(r -> Bukkit.getOfflinePlayer(r.getSender()).getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (request == null) {
            MessageUtil.send(player, MiniMessage.miniMessage().deserialize("<red>Запрос от " + name + " не найден.</red>"));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null || !senderPlayer.isOnline()) {
            MessageUtil.send(player, config.getMessage(player, "player-offline"));
            requestManager.removeRequest(request.getSender(), request.getTarget());
            return true;
        }

        requestManager.removeRequest(request.getSender(), request.getTarget());

        final Player teleporting = request.isHere() ? player : senderPlayer;
        final Player other = request.isHere() ? senderPlayer : player;
        acceptRequest(plugin, teleporting, other, config);
        return true;
    }

    public static void acceptRequest(TPAPlugin plugin, Player teleporting, Player other, ConfigManager config) {
        int delay = config.getInt("delay-seconds", 3);

        MessageUtil.send(teleporting,
            config.format(teleporting, other, "teleport-accepted")
                .replaceText(builder -> builder.matchLiteral("%seconds%").replacement(String.valueOf(delay)))
        );

        if (delay == 0) {
            teleporting.teleport(other.getLocation());
            MessageUtil.send(teleporting, config.format(teleporting, other, "request-accepted"));
            MessageUtil.send(other, config.format(other, teleporting, "request-accepted"));
            return;
        }

        final Location startLocation = teleporting.getLocation().clone();

        new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                if (!teleporting.isOnline() || !other.isOnline()) {
                    if (teleporting.isOnline())
                        MessageUtil.send(teleporting, config.getMessage(teleporting, "player-offline"));
                    if (other.isOnline())
                        MessageUtil.send(other, config.getMessage(other, "player-offline"));
                    cancel();
                    return;
                }

                if (hasMoved(teleporting, startLocation)) {
                    MessageUtil.send(teleporting, config.getMessage(teleporting, "teleport-moved"));
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    teleporting.teleport(other.getLocation());
                    MessageUtil.send(teleporting, config.format(teleporting, other, "request-accepted"));
                    MessageUtil.send(other, config.format(other, teleporting, "request-accepted"));
                    cancel();
                    return;
                }

                teleporting.playSound(teleporting.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

                String msgText = config.getRawMessage(teleporting, "teleport-delay")
                        .replace("%seconds%", String.valueOf(countdown));
                Component actionBarMsg = config.formatRaw(null, null, msgText);
                MessageUtil.sendActionBar(teleporting, actionBarMsg);

                countdown--;
            }

            private boolean hasMoved(Player p, Location start) {
                Location now = p.getLocation();
                return now.getWorld() != start.getWorld() ||
                        now.getBlockX() != start.getBlockX() ||
                        now.getBlockY() != start.getBlockY() ||
                        now.getBlockZ() != start.getBlockZ();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return null;
        if (args.length == 1) {
            return requestManager.getIncomingRequests(player.getUniqueId()).stream()
                    .map(r -> Bukkit.getOfflinePlayer(r.getSender()).getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}