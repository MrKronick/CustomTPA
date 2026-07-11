package ru.mcplugins.tpa.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.mcplugins.tpa.TPAPlugin;

public class EconomyManager {

    private final TPAPlugin plugin;
    private Economy economy;

    public EconomyManager(TPAPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found — economy features disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault found but no economy service registered.");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Vault economy successfully hooked!");
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) return true;
        if (amount <= 0) return true;
        return economy.getBalance(player) >= amount;
    }

    public boolean charge(Player player, double amount) {
        if (!isEnabled() || amount <= 0) return true;
        if (economy.getBalance(player) < amount) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }

    public String format(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }
}