package ru.mcplugins.tpa.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

import java.util.UUID;

public class GUIListener implements Listener {

    private final TPAPlugin plugin;
    private final RequestManager requestManager;

    public GUIListener(TPAPlugin plugin, RequestManager requestManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ConfigManager config = plugin.getConfigManager();
        if (e.getView().title().equals(config.getMessage(player, "gui-title"))) {
            e.setCancelled(true);
            handleRequestGUI(e, player);
        }
    }

    private void handleRequestGUI(InventoryClickEvent e, Player player) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ConfigManager config = plugin.getConfigManager();

        if (clicked.getType() == Material.PLAYER_HEAD && e.getSlot() >= 9 && e.getSlot() <= 17) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta.hasOwner()) {
                UUID senderUUID = meta.getOwningPlayer().getUniqueId();
                RequestManager.TPARequest req = requestManager.getRequest(senderUUID, player.getUniqueId());
                if (req != null) {
                    GUIHandler.setSelected(player.getUniqueId(), senderUUID);
                    GUIHandler.openGUI(player, requestManager, config);
                }
            }
            return;
        }

        if (clicked.getType() == Material.LIME_CONCRETE) {
            UUID selected = GUIHandler.getSelected(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(config.getMessage(player, "no-selection"));
                return;
            }
            RequestManager.TPARequest req = requestManager.getRequest(selected, player.getUniqueId());
            if (req == null) {
                player.sendMessage(config.getMessage(player, "request-expired"));
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }
            Player sender = Bukkit.getPlayer(req.getSender());
            if (sender == null || !sender.isOnline()) {
                player.sendMessage(config.getMessage(player, "player-offline"));
                requestManager.removeRequest(req.getSender(), req.getTarget());
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }
            if (req.isHere()) {
                player.teleport(sender.getLocation());
            } else {
                sender.teleport(player.getLocation());
            }
            requestManager.removeRequest(req.getSender(), req.getTarget());
            player.sendMessage(config.getMessage(player, "request-accepted"));
            sender.sendMessage(config.getMessage(sender, "request-accepted"));
            GUIHandler.clearSelected(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            UUID selected = GUIHandler.getSelected(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(config.getMessage(player, "no-selection"));
                return;
            }
            RequestManager.TPARequest req = requestManager.getRequest(selected, player.getUniqueId());
            if (req == null) {
                player.sendMessage(config.getMessage(player, "request-expired"));
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }
            Player sender = Bukkit.getPlayer(req.getSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(config.getMessage(sender, "request-denied"));
            }
            requestManager.removeRequest(req.getSender(), req.getTarget());
            player.sendMessage(config.getMessage(player, "request-denied"));
            GUIHandler.clearSelected(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            GUIHandler.openGUI(player, requestManager, config);
        }
    }
}