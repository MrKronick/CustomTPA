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
    public String getIdentifier() { return "tpa"; }
    @Override
    public String getAuthor() { return "mcplugins"; }
    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        if (params.equals("incoming")) return String.valueOf(requestManager.getIncomingCount(player.getUniqueId()));
        if (params.equals("has_incoming")) return requestManager.getIncomingCount(player.getUniqueId()) > 0 ? "true" : "false";
        if (params.equals("outgoing")) return requestManager.getOutgoingRequest(player.getUniqueId()) != null ? "true" : "false";
        return null;
    }
}