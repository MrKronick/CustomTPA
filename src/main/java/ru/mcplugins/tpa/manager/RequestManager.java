package ru.mcplugins.tpa.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.util.MessageUtil;
import ru.mcplugins.tpa.util.SchedulerUtil;

import java.util.*;
import java.util.stream.Collectors;

public class RequestManager {

    private final TPAPlugin plugin;
    private final List<TPARequest> activeRequests = Collections.synchronizedList(new ArrayList<>());
    private final Set<UUID> toggledOff = new HashSet<>();
    private final Set<UUID> autoAccept = new HashSet<>();
    private final Map<UUID, Long> lastRequestTime = new HashMap<>();

    public RequestManager(TPAPlugin plugin) {
        this.plugin = plugin;
        startExpiryTask();
    }

    private void startExpiryTask() {
        SchedulerUtil.runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            synchronized (activeRequests) {
                activeRequests.removeIf(req -> {
                    if (req.isExpired(now)) {
                        Player target = plugin.getServer().getPlayer(req.getTarget());
                        Player sender = plugin.getServer().getPlayer(req.getSender());
                        if (target != null && target.isOnline()) {
                            MessageUtil.send(target, plugin.getConfigManager().format(target, sender, "request-expired"));
                        }
                        if (sender != null && sender.isOnline()) {
                            MessageUtil.send(sender, plugin.getConfigManager().format(sender, target, "request-expired"));
                        }
                        return true;
                    }
                    return false;
                });
            }
        }, 20L, 20L);
    }

    public boolean isToggled(UUID uuid) { return toggledOff.contains(uuid); }
    public void setToggled(UUID uuid, boolean toggled) {
        if (toggled) toggledOff.add(uuid); else toggledOff.remove(uuid);
    }

    public boolean isAutoAccept(UUID uuid) { return autoAccept.contains(uuid); }
    public void setAutoAccept(UUID uuid, boolean value) {
        if (value) autoAccept.add(uuid); else autoAccept.remove(uuid);
    }

    public TPARequest getRequest(UUID sender, UUID target) {
        synchronized (activeRequests) {
            return activeRequests.stream()
                    .filter(r -> r.getSender().equals(sender) && r.getTarget().equals(target))
                    .findFirst().orElse(null);
        }
    }

    public void addRequest(TPARequest request) {
        synchronized (activeRequests) { activeRequests.add(request); }
    }

    public boolean removeRequest(UUID sender, UUID target) {
        synchronized (activeRequests) {
            return activeRequests.removeIf(r -> r.getSender().equals(sender) && r.getTarget().equals(target));
        }
    }

    public List<TPARequest> getIncomingRequests(UUID target) {
        synchronized (activeRequests) {
            return activeRequests.stream().filter(r -> r.getTarget().equals(target)).collect(Collectors.toList());
        }
    }

    public int getIncomingCount(UUID target) { return getIncomingRequests(target).size(); }

    public TPARequest getOutgoingRequest(UUID sender) {
        synchronized (activeRequests) {
            return activeRequests.stream().filter(r -> r.getSender().equals(sender)).findFirst().orElse(null);
        }
    }

    public void clearAll() { synchronized (activeRequests) { activeRequests.clear(); } }

    // Методы кулдауна
    public boolean isOnCooldown(UUID sender, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Long last = lastRequestTime.get(sender);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < (cooldownSeconds * 1000L);
    }

    public long getCooldownRemaining(UUID sender, int cooldownSeconds) {
        Long last = lastRequestTime.get(sender);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        return Math.max(0, remaining / 1000);
    }

    public void setLastRequestTime(UUID sender) {
        lastRequestTime.put(sender, System.currentTimeMillis());
    }

    public static class TPARequest {
        private final UUID sender, target;
        private final boolean isHere;
        private final long timestamp;
        public TPARequest(UUID sender, UUID target, boolean isHere) {
            this.sender = sender; this.target = target; this.isHere = isHere;
            this.timestamp = System.currentTimeMillis();
        }
        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public boolean isHere() { return isHere; }
        public long getTimestamp() { return timestamp; }
        public boolean isExpired(long now) { return (now - timestamp) > 60_000; }
    }
}