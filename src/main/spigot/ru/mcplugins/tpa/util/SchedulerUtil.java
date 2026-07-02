package ru.mcplugins.tpa.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SchedulerUtil {

    public static CancellableTask runTaskTimer(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        BukkitRunnable bukkitTask = new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        };
        bukkitTask.runTaskTimer(plugin, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    public interface CancellableTask {
        void cancel();
    }
}