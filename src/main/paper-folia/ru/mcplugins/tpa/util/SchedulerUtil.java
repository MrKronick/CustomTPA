package ru.mcplugins.tpa.util;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SchedulerUtil {

    public static CancellableTask runTaskTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        GlobalRegionScheduler scheduler = Bukkit.getServer().getGlobalRegionScheduler();
        ScheduledTask scheduledTask = scheduler.runAtFixedRate(plugin, scheduled -> task.run(), delayTicks, periodTicks);
        return scheduledTask::cancel;
    }

    public interface CancellableTask {
        void cancel();
    }
}