// ╔════════════════════════════════════════════════════════════════════╗
// ║                   🧩 PlaceholderRegistrar.java                     ║
// ║  Registers PlaceholderAPI expansions if enabled and available      ║
// ║  This wraps all logic for safe integration with PAPI               ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.placeholders;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlaceholderRegistrar {

    // ─────────────────────────────────────────────────────────────
    // 🚀 Registration Method
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers placeholders with PlaceholderAPI if:
     * 1. Enabled in the plugin's config.yml
     * 2. PlaceholderAPI is installed and enabled on the server
     *
     * @param playerDataManager Active player data manager instance
     */
    public static void register(PlayerDataManager playerDataManager) {

        // 📦 Exit early if PAPI integration is not enabled
        if (!ConfigManager.enablePlaceholderAPI()) return;
        if (!ConfigManager.registerPlaceholders()) return;

        // 🔍 Attempt to locate the PlaceholderAPI plugin
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            PickYourDifficulty.getInstance().getLogger().warning(
                    "[PickYourDifficulty] PlaceholderAPI is not installed or not enabled!"
            );
            return;
        }

        // ✅ All checks passed — register the expansion
        new DifficultyPlaceholder(playerDataManager).register();

        // 📝 Confirm successful registration in console
        PickYourDifficulty.getInstance().getLogger().info(
                "[PickYourDifficulty] Registered %pickyourdifficulty_*% placeholders with PlaceholderAPI."
        );
    }
}