// ╔════════════════════════════════════════════════════════════════════╗
// ║                     ⏱️ TimeFormatUtil.java                         ║
// ║  Formats seconds into readable cooldown strings using config or    ║
// ║  fallback logic. Useful for holograms, GUIs, or chat displays.     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;

// ─────────────────────────────────────────────────────────────
// ⏱️ TimeFormatUtil — Cooldown formatting helpers
// ─────────────────────────────────────────────────────────────
public class TimeFormatUtil {

    // ╔═══🧮 Config-Based Cooldown Format══════════════════════════════╗

    // 💬 Formats seconds using placeholders defined in config
    public static String formatCooldown(long totalSeconds) {

        // 🧮 Convert total seconds into hours, minutes, seconds
        long hours = totalSeconds / 3600;               // Total full hours
        long minutes = (totalSeconds % 3600) / 60;      // Remaining full minutes
        long seconds = totalSeconds % 60;               // Remaining seconds after hours and minutes

        // 📦 Load format string from config (e.g. "<hours>h <minutes>m <seconds>s")
        String format = ConfigManager.getCooldownFormat();

        // 🔧 Replace <hours>, <minutes>, <seconds> with actual values
        String result = applyFormatPlaceholders(format, hours, minutes, seconds);

        // 🧪 Debug: log full conversion trace
        PickYourDifficulty.debug("⏱️ formatCooldown(" + totalSeconds + "s) → " + result);

        return result;
    }

    // ╔═══🧭 Simple Fallback Formatter══════════════════════════════════╗

    // 💬 Fallback formatting for cooldowns (e.g., "2h 4m 15s")
    public static String formatSimple(long totalSeconds) {

        // 🧮 Break total seconds into hours/minutes/seconds
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // 🔧 Build minimal string (skipping zero units)
        String result = buildSimpleString(hours, minutes, seconds);

        // 🧪 Debug: confirm trimmed fallback output
        PickYourDifficulty.debug("⏱️ formatSimple(" + totalSeconds + "s) → " + result);

        return result;
    }

    // ╔═══🧩 Internal Helpers═══════════════════════════════════════════╗

    // 💬 Replaces placeholders like <hours>, <minutes>, <seconds> in string using actual values
    private static String applyFormatPlaceholders(String format, long hours, long minutes, long seconds) {
        return format
                .replace("<hours>", String.valueOf(hours))
                .replace("<minutes>", String.valueOf(minutes))
                .replace("<seconds>", String.valueOf(seconds));
    }

    // 💬 Builds a minimal human-readable trimmed cooldown time string, skipping zeroes (e.g. "5m 12s" or "4h")
    private static String buildSimpleString(long hours, long minutes, long seconds) {
        StringBuilder sb = new StringBuilder();

        // Only include units that have non-zero values
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s"); // Always show something

        return sb.toString().trim(); // Remove trailing space
    }
}