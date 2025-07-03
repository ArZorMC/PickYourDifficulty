// ╔════════════════════════════════════════════════════════════════════╗
// ║              🧠 GraceReminderTracker.java                          ║
// ║   Tracks when each player was last sent a grace period reminder   ║
// ║   to avoid sending them too frequently (anti-spam mechanism)      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.storage;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// 🧠 GraceReminderTracker — Anti-Spam Reminder System
// ─────────────────────────────────────────────────────────────
public class GraceReminderTracker {

    // ╔═══🗺️ Internal Timestamp Map══════════════════════════════════════╗
    // Stores: Player UUID → last reminder timestamp (milliseconds)
    private static final Map<UUID, Long> lastReminderTimestamps = new HashMap<>();

    // ╔═══📌 updateReminder() — Mark current time as last reminder═══════╗
    public static void updateReminder(UUID uuid) {
        long now = System.currentTimeMillis(); // Current time in ms
        lastReminderTimestamps.put(uuid, now); // Save new timestamp

        // 🧪 Debug: log when reminders are marked
        PickYourDifficulty.debug("🔔 Updated grace reminder timestamp for " + uuid + " → " + now + "ms");
    }

    // ╔═══⏱️ getSecondsSinceLastReminder() — Time since last reminder════╗
    public static long getSecondsSinceLastReminder(UUID uuid) {
        Long last = lastReminderTimestamps.get(uuid);

        // 📭 No reminder ever sent — treat as infinite delay
        if (last == null) {
            PickYourDifficulty.debug("📭 No previous reminder found for " + uuid + " → returning Long.MAX_VALUE");
            return Long.MAX_VALUE;
        }

        long now = System.currentTimeMillis(); // Current time in ms
        long diffMillis = now - last;          // 🧮 Milliseconds since last reminder
        long seconds = diffMillis / 1000;      // 🧮 Convert to seconds (1s = 1000ms)

        // 🧪 Debug: show time since last reminder
        PickYourDifficulty.debug("⏱️ Time since last reminder for " + uuid +
                " → " + seconds + "s (" + diffMillis + "ms)");

        return seconds;
    }

    // ╔═══🧼 clear() — Remove reminder for single player═════════════════╗
    public static void clear(UUID uuid) {
        lastReminderTimestamps.remove(uuid);

        PickYourDifficulty.debug("❌ Cleared grace reminder timestamp for " + uuid);
    }

    // ╔═══💣 clearAll() — ⚠️ Dev wipe of all reminder timestamps══════════╗
    public static void clearAll() {
        lastReminderTimestamps.clear();

        PickYourDifficulty.debug("🧹 Cleared all grace reminder timestamps");
    }
}
