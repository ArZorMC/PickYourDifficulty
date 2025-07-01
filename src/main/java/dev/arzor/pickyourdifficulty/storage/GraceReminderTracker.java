// ╔════════════════════════════════════════════════════════════════════╗
// ║              🧠 GraceReminderTracker.java                          ║
// ║   Tracks when each player was last sent a grace period reminder   ║
// ║   to avoid sending them too frequently (anti-spam mechanism)      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraceReminderTracker {

    // ─────────────────────────────────────────────────────────────
    // 🧭 Internal Tracking Map
    // ─────────────────────────────────────────────────────────────

    /** Stores last reminder timestamp per player (system milliseconds) */
    private static final Map<UUID, Long> lastReminderTimestamps = new HashMap<>();

    // ─────────────────────────────────────────────────────────────
    // ⏱️ Reminder Logic
    // ─────────────────────────────────────────────────────────────

    /**
     * Updates the stored timestamp to mark when a player was last reminded.
     *
     * @param uuid The UUID of the player to update
     */
    public static void updateReminder(UUID uuid) {
        // 💾 Save the current system time for this player
        lastReminderTimestamps.put(uuid, System.currentTimeMillis());
    }

    /**
     * Returns how many seconds have passed since the last reminder.
     * If no reminder has ever been sent, returns Long.MAX_VALUE.
     *
     * @param uuid The UUID of the player
     * @return Seconds since last reminder, or max value if never reminded
     */
    public static long getSecondsSinceLastReminder(UUID uuid) {
        Long last = lastReminderTimestamps.get(uuid);

        // 📭 If player has never received a reminder, use large value as default
        if (last == null) return Long.MAX_VALUE;

        // 🧮 Convert difference in milliseconds to seconds
        return (System.currentTimeMillis() - last) / 1000;
    }

    // ─────────────────────────────────────────────────────────────
    // 🧹 Cleanup Methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Removes the reminder timestamp for a single player.
     * Useful when a player logs out or is reset.
     *
     * @param uuid The UUID of the player to remove
     */
    public static void clear(UUID uuid) {
        lastReminderTimestamps.remove(uuid);
    }

    /**
     * Wipes all reminder timestamps.
     * Typically used during plugin reloads or resets.
     */
    public static void clearAll() {
        lastReminderTimestamps.clear();
    }
}
