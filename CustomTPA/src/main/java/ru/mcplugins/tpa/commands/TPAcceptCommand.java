package ru.mcplugins.tpa.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

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
            player.sendMessage(config.getMessage(player, "no-permission"));
            return true;
        }

        List<RequestManager.TPARequest> incoming = requestManager.getIncomingRequests(player.getUniqueId());

        if (args.length == 0) {
            if (incoming.isEmpty()) {
                player.sendMessage(config.getMessage(player, "no-requests"));
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
            player.sendMessage(config.getMessage(player, "prefix").append(Component.text("Запросы: ")).append(list));
            return true;
        }

        if (incoming.isEmpty()) {
            player.sendMessage(config.getMessage(player, "no-requests"));
            return true;
        }

        RequestManager.TPARequest request;
        String name = args[0];
        request = incoming.stream()
                .filter(r -> Bukkit.getOfflinePlayer(r.getSender()).getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (request == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Запрос от " + name + " не найден.</red>"));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null || !senderPlayer.isOnline()) {
            player.sendMessage(config.getMessage(player, "player-offline"));
            requestManager.removeRequest(request.getSender(), request.getTarget());
            return true;
        }

        requestManager.removeRequest(request.getSender(), request.getTarget());

        int delay = config.getInt("delay-seconds", 3);
        boolean isHere = request.isHere();

        final Player teleporting = isHere ? player : senderPlayer;
        final Player other = isHere ? senderPlayer : player;

        teleporting.sendMessage(
            config.format(teleporting, other, "teleport-accepted")
                .replaceText(builder -> builder.matchLiteral("%seconds%").replacement(String.valueOf(delay)))
        );

        if (delay == 0) {
            if (isHere) player.teleport(senderPlayer.getLocation());
            else senderPlayer.teleport(player.getLocation());
            teleporting.sendMessage(config.format(teleporting, other, "request-accepted"));
            other.sendMessage(config.format(other, teleporting, "request-accepted"));
            return true;
        }

        final Location startLocation = teleporting.getLocation().clone();

        new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                if (!teleporting.isOnline() || !other.isOnline()) {
                    if (teleporting.isOnline())
                        teleporting.sendMessage(config.getMessage(teleporting, "player-offline"));
                    if (other.isOnline())
                        other.sendMessage(config.getMessage(other, "player-offline"));
                    cancel();
                    return;
                }

                if (hasMoved(teleporting, startLocation)) {
                    teleporting.sendMessage(config.getMessage(teleporting, "teleport-moved"));
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    if (isHere) player.teleport(senderPlayer.getLocation());
                    else senderPlayer.teleport(player.getLocation());
                    teleporting.sendMessage(config.format(teleporting, other, "request-accepted"));
                    other.sendMessage(config.format(other, teleporting, "request-accepted"));
                    cancel();
                    return;
                }

                String msgText = config.getRawMessage(teleporting, "teleport-delay")
                        .replace("%seconds%", String.valueOf(countdown));
                Component actionBarMsg = config.formatRaw(null, null, msgText);
                teleporting.sendActionBar(actionBarMsg);

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

        return true;
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