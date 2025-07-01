// ╔════════════════════════════════════════════════════════════════════╗
// ║               ⏲️ HologramTaskManager.java                          ║
// ║  Background task for updating, expiring, and cleaning holograms    ║
// ║  - Skips unloaded chunks                                           ║
// ║  - Safely removes expired or picked-up items                       ║
// ║  - Uses BukkitRunnable loop with interval from config              ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HologramTaskManager {

    // ─────────────────────────────────────────────────────────────
    // 🧠 Active Tracking References
    // ─────────────────────────────────────────────────────────────

    // Pull a live reference to all tracked holograms from the manager
    private static final Map<UUID, HologramManager.TrackedHologram> active = HologramManager.getTrackedData();

    // Stores the task ID so we can stop it later
    private static int taskId = -1;

    // ─────────────────────────────────────────────────────────────
    // ▶ Start the Update Loop
    // ─────────────────────────────────────────────────────────────

    public static void start(JavaPlugin plugin) {
        int intervalTicks = ConfigManager.getHologramUpdateInterval();

        // 🛑 If update interval is zero or disabled, skip launching task
        if (intervalTicks <= 0) return;

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                updateAll();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks).getTaskId();
    }

    // ─────────────────────────────────────────────────────────────
    // ⛔ Stop the Update Task
    // ─────────────────────────────────────────────────────────────

    public static void stop() {
        // Cancel the task if it’s running
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // 🧹 Remove all visual holograms (but leave persistent storage intact)
        HologramManager.removeAll();
    }

    // ─────────────────────────────────────────────────────────────
    // 🔁 Update All Tracked Items
    // ─────────────────────────────────────────────────────────────

    private static void updateAll() {
        long now = System.currentTimeMillis();

        // Loop through all tracked items
        Iterator<Map.Entry<UUID, HologramManager.TrackedHologram>> iterator = active.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HologramManager.TrackedHologram> entry = iterator.next();
            UUID itemId = entry.getKey();
            HologramManager.TrackedHologram tracked = entry.getValue();

            // 🔍 Try to find the matching item in the world
            Item item = findItemByUUID(itemId);

            // 📦 Skip if item is gone or invalid
            if (item == null || item.isDead() || !item.isValid()) {
                HologramManager.removeHologramFromUUID(itemId);
                iterator.remove();
                continue;
            }

            // 📦 Skip countdown if the chunk is not currently loaded
            Chunk chunk = item.getLocation().getChunk();
            if (!chunk.isLoaded()) continue;

            // 🧮 Compute remaining time until expiration
            long millisRemaining = tracked.expiresAtMillis() - now;
            int secondsLeft = (int) (millisRemaining / 1000L);

            // ⌛ If time’s up, remove it entirely
            if (secondsLeft <= 0) {
                HologramManager.removeHologramFromUUID(itemId);
                iterator.remove();
            } else {
                // 🔁 Otherwise update the visual countdown
                HologramManager.updateHologram(item);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 Locate a Live Item Entity by UUID
    // ─────────────────────────────────────────────────────────────

    private static Item findItemByUUID(UUID uuid) {
        // Search through every loaded world and chunk for matching entity
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Item item && item.getUniqueId().equals(uuid)) {
                        return item;
                    }
                }
            }
        }

        // 🚫 Not found — may have despawned or been picked up
        return null;
    }
}