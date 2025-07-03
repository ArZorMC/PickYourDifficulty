// ╔════════════════════════════════════════════════════════════════════╗
// ║               ⏲️ HologramTaskManager.java                          ║
// ║  Background task for updating, expiring, and cleaning holograms    ║
// ║  - Skips unloaded chunks                                           ║
// ║  - Safely removes expired or picked-up items                       ║
// ║  - Uses BukkitRunnable loop with interval from config              ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

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
        if (intervalTicks <= 0) {
            PickYourDifficulty.debug("⏲️ HologramTaskManager not started: interval is set to 0 (disabled)");
            return;
        }

        PickYourDifficulty.debug("⏲️ HologramTaskManager starting with interval: " + intervalTicks + " ticks");

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
            PickYourDifficulty.debug("⛔ Stopping HologramTaskManager task (ID: " + taskId + ")");
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // 🧹 Remove all visual holograms (but leave persistent storage intact)
        PickYourDifficulty.debug("🧹 Removing all active holograms (visuals only)");
        HologramManager.removeAll();
    }

    // ─────────────────────────────────────────────────────────────
    // 🔁 Update All Tracked Items
    // ─────────────────────────────────────────────────────────────

    private static void updateAll() {
        long now = System.currentTimeMillis();

        // 🧠 Debug: Report how many items are currently tracked
        PickYourDifficulty.debug("🔄 Running hologram update loop for " + active.size() + " tracked items");

        // Loop through all tracked hologram entries
        Iterator<Map.Entry<UUID, HologramManager.TrackedHologram>> iterator = active.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HologramManager.TrackedHologram> entry = iterator.next();
            UUID itemId = entry.getKey();
            HologramManager.TrackedHologram tracked = entry.getValue();

            // 🔍 Try to find the matching item in the world
            Item item = findItemByUUID(itemId);

            // 📦 Remove if item no longer exists or was picked up
            if (item == null || item.isDead() || !item.isValid()) {
                PickYourDifficulty.debug("❌ Removing hologram: item no longer exists (UUID: " + itemId + ")");
                HologramManager.removeHologramFromUUID(itemId);
                iterator.remove();
                continue;
            }

            // 📭 Skip countdown if the chunk is not currently loaded
            Chunk chunk = item.getLocation().getChunk();
            if (!chunk.isLoaded()) {
                PickYourDifficulty.debug("📭 Skipping hologram update: chunk not loaded (UUID: " + itemId + ")");
                continue;
            }

            // 🧮 Compute remaining time until expiration
            long millisRemaining = tracked.expiresAtMillis() - now;
            int secondsLeft = (int) (millisRemaining / 1000L);

            // ⌛ Expired? Remove the item and hologram
            if (secondsLeft <= 0) {
                PickYourDifficulty.debug("⌛ Hologram expired: removing item (UUID: " + itemId + ")");
                HologramManager.removeHologramFromUUID(itemId);
                iterator.remove();
            } else {
                // 🔁 Still active? Update the hologram countdown
                PickYourDifficulty.debug("⏳ Updating hologram (UUID: " + itemId + ") — " + secondsLeft + "s remaining");
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
                    // 🧠 Check UUID match and return if found
                    if (entity instanceof Item item && item.getUniqueId().equals(uuid)) {
                        return item;
                    }
                }
            }
        }

        // 🚫 Not found — likely despawned or picked up
        return null;
    }
}