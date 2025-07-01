// ╔════════════════════════════════════════════════════════════════════╗
// ║                     ⏱️ TimeFormatUtil.java                         ║
// ║  Formats seconds into readable cooldown strings using config or    ║
// ║  fallback logic. Useful for holograms, GUIs, or chat displays.     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.managers.ConfigManager;

public class TimeFormatUtil {

    // ─────────────────────────────────────────────────────────────
    // 🧮 Format using <hours> <minutes> <seconds> from config string
    // ─────────────────────────────────────────────────────────────

    /**
     * Formats total seconds into a human-readable string using
     * the configured format from `cooldown-format`.
     * Example config: "<hours>h <minutes>m <seconds>s"
     *
     * @param totalSeconds Total duration in seconds
     * @return Formatted string with placeholders resolved
     */
    public static String formatCooldown(long totalSeconds) {

        // 🧮 Break seconds into hours, minutes, seconds
        long hours = totalSeconds / 3600;               // Total full hours
        long minutes = (totalSeconds % 3600) / 60;      // Remaining full minutes
        long seconds = totalSeconds % 60;               // Remaining seconds after hours and minutes

        // 📦 Pull the placeholder format string from config
        String format = ConfigManager.getCooldownFormat();

        // 💬 Replace placeholders with actual values
        return format
                .replace("<hours>", String.valueOf(hours))
                .replace("<minutes>", String.valueOf(minutes))
                .replace("<seconds>", String.valueOf(seconds));
    }

    // ─────────────────────────────────────────────────────────────
    // 🧭 Simple Format (no config required)
    // ─────────────────────────────────────────────────────────────

    /**
     * Formats total seconds into a simplified time string,
     * like "2h 5m 0s" or "14m 22s", skipping empty parts.
     *
     * @param totalSeconds Total time in seconds
     * @return Clean, trimmed string (always shows something)
     */
    public static String formatSimple(long totalSeconds) {

        // 🧮 Same math as above — break into parts
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        // 💬 Only append if value > 0 to avoid clutter
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");

        // 📦 Always include seconds if nothing else exists
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        // 💬 Trim trailing space and return
        return sb.toString().trim();
    }
}