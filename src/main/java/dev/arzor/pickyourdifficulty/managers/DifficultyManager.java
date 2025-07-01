// ╔════════════════════════════════════════════════════════════════════╗
// ║                    🧩 DifficultyManager.java                       ║
// ║   Central logic for difficulty selection, validation, and perms    ║
// ║   Bridges config-defined difficulty data with runtime checks       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import org.bukkit.entity.Player;

import java.util.List;

// ─────────────────────────────────────────────────────────────
// 🎯 DifficultyManager — Core utility class for difficulty logic
// ─────────────────────────────────────────────────────────────
public class DifficultyManager {

    // ╔═══✅ Validate difficulty from config═════════════════════════════╗
    public static boolean isValidDifficulty(String difficultyKey) {
        // 💬 Case-insensitive check: does any config key match this input?
        return ConfigManager.getDifficultyNames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(difficultyKey));
    }

    // ╔═══📋 Get all defined difficulty keys═════════════════════════════╗
    public static List<String> getAllDifficulties() {
        // 💬 Direct passthrough to ConfigManager for raw difficulty keys
        return ConfigManager.getDifficultyNames();
    }

    // ╔═══🔡 Normalize to canonical difficulty key═══════════════════════╗
    public static String getCanonicalKey(String input) {
        // 💬 Return exact-case match from config (useful for display or lookup)
        return ConfigManager.getDifficultyNames().stream()
                .filter(name -> name.equalsIgnoreCase(input))
                .findFirst()
                .orElse(null); // 👻 null if not found
    }

    // ╔═══🔐 Permission check for selecting a difficulty═════════════════╗
    public static boolean cannotSelect(Player player, String difficulty) {
        // 💬 Permission format: pickyourdifficulty.difficulty.<name>
        String node = "pickyourdifficulty.difficulty." + difficulty.toLowerCase();

        // 💬 Uses PermissionUtil to resolve OP/admin override + perms
        return PermissionUtil.has(player, node);
    }

    // ╔═══🧾 Difficulty summary for tooltips or info═════════════════════╗
    public static String getDifficultySummary(String difficulty) {
        // 💬 Get configured grace + despawn time
        int grace = ConfigManager.getGraceTime(difficulty);
        int despawn = ConfigManager.getDespawnTime(difficulty);

        // 🧮 Return combined string: Grace: 60s | Despawn: 120s
        return "⏱️ Grace: " + grace + "s | 🧺 Despawn: " + despawn + "s";
    }

    // ╔═══🧭 Fallback difficulty logic═══════════════════════════════════╗
    public static String getFallbackDifficulty() {
        // 💬 Used when player has no selection or file is corrupted
        return ConfigManager.getFallbackDifficulty();
    }
}
