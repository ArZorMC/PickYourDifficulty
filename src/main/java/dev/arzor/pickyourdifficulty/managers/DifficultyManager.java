// ╔════════════════════════════════════════════════════════════════════╗
// ║                    🧩 DifficultyManager.java                       ║
// ║   Central logic for difficulty selection, validation, and perms    ║
// ║   Bridges config-defined difficulty data with runtime checks       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
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
        boolean valid = ConfigManager.getDifficultyNames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(difficultyKey));

        // 🧪 Debug: show result of validity check
        PickYourDifficulty.debug("Validating difficulty key: '" + difficultyKey + "' → " + valid);

        return valid;
    }

    // ╔═══📋 Get all defined difficulty keys═════════════════════════════╗
    public static List<String> getAllDifficulties() {

        // 💬 Direct passthrough to ConfigManager for raw difficulty keys
        List<String> names = ConfigManager.getDifficultyNames();

        // 🧪 Debug: log loaded keys
        PickYourDifficulty.debug("Loaded difficulty keys from config: " + names);

        return names;
    }

    // ╔═══🔡 Normalize to canonical difficulty key═══════════════════════╗
    public static String getCanonicalKey(String input) {

        // 💬 Match the case-insensitive input against known difficulty keys
        // 🔁 Return the exact-cased config key if found (e.g., "Hardcore" instead of "hardcore")
        String canonical = ConfigManager.getDifficultyNames().stream()
                .filter(name -> name.equalsIgnoreCase(input))
                .findFirst()
                .orElse(null); // 👻 Return null if no match found

        // 🧪 Debug: log canonical resolution result
        PickYourDifficulty.debug("Resolving canonical difficulty key: input = '" + input + "', resolved = " + canonical);

        return canonical;
    }

    // ╔═══🔐 Permission check for selecting a difficulty═════════════════╗
    public static boolean cannotSelect(Player player, String difficulty) {

        // 🔐 Permission node format: pickyourdifficulty.difficulty.<name>
        String node = "pickyourdifficulty.difficulty." + difficulty.toLowerCase();

        // 📛 Uses PermissionUtil to resolve OP/admin bypass and actual permission
        boolean hasPermission = PermissionUtil.has(player, node);

        // 🧪 Debug: log permission check result
        PickYourDifficulty.debug("Checking if " + player.getName() + " can select '" + difficulty + "' → " + hasPermission + " (perm: " + node + ")");

        return hasPermission;
    }

    // ╔═══🧾 Difficulty summary for tooltips or info═════════════════════╗
    public static String getDifficultySummary(String difficulty) {

        // 📊 Get config-defined grace and despawn time for this difficulty
        int grace = ConfigManager.getGraceTime(difficulty);     // ⏱️ in seconds
        int despawn = ConfigManager.getDespawnTime(difficulty); // 🧺 in seconds

        // 🧮 Return formatted summary string
        String summary = "⏱️ Grace: " + grace + "s | 🧺 Despawn: " + despawn + "s";

        // 🧪 Debug: log formatted summary
        PickYourDifficulty.debug("Generated summary for '" + difficulty + "': " + summary);

        return summary;
    }

    // ╔═══🧭 Fallback difficulty logic═══════════════════════════════════╗
    public static String getFallbackDifficulty() {

        // 🌐 Used when a player has no difficulty saved or config is missing/corrupted
        String fallback = ConfigManager.getFallbackDifficulty();

        // 🧪 Debug: log fallback result
        PickYourDifficulty.debug("Resolved fallback difficulty: " + fallback);

        return fallback;
    }
}
