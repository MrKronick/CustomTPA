package ru.mcplugins.tpa.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.RequestManager;

public class TPAExpansion extends PlaceholderExpansion {

    private final TPAPlugin plugin;
    private final RequestManager requestManager;

    public TPAExpansion(TPAPlugin plugin, RequestManager requestManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
    }

    @Override
    public String getIdentifier() {
        return "tpa";
    }

    @Override
    public String getAuthor() {
        return "MrKronick";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "incoming":
                return String.valueOf(requestManager.getIncomingCount(player.getUniqueId()));
            case "has_incoming":
                return requestManager.getIncomingCount(player.getUniqueId()) > 0 ? "yes" : "no";
            case "outgoing":
                return requestManager.getOutgoingRequest(player.getUniqueId()) != null ? "yes" : "no";
            case "sent":
                int[] stats = TPAPlugin.getInstance().getStatsManager().getStats(player.getUniqueId());
                return String.valueOf(stats[0]);
            case "accepted":
                int[] stats2 = TPAPlugin.getInstance().getStatsManager().getStats(player.getUniqueId());
                return String.valueOf(stats2[1]);
            case "denied":
                int[] stats3 = TPAPlugin.getInstance().getStatsManager().getStats(player.getUniqueId());
                return String.valueOf(stats3[2]);
            default:
                return null;
        }
    }
}