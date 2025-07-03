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

// ─────────────────────────────────────────────────────────────
// 🧭 Placeholder Registrar — Connects with PlaceholderAPI
// ─────────────────────────────────────────────────────────────
public class PlaceholderRegistrar {

    // ╔═══🚀 Register Placeholders if Integration is Enabled═════════════════════════╗
    public static void register(PlayerDataManager playerDataManager) {

        // 📦 Step 1: Check if PlaceholderAPI integration is enabled in config
        if (!ConfigManager.enablePlaceholderAPI()) {
            PickYourDifficulty.debug("🔌 PlaceholderAPI integration is disabled in config — skipping registration.");
            return;
        }

        // 📦 Step 2: Check if placeholders should be registered
        if (!ConfigManager.registerPlaceholders()) {
            PickYourDifficulty.debug("🛑 Placeholder registration is disabled in config — skipping.");
            return;
        }


        // 📦 Step 3: Verify PlaceholderAPI plugin is installed and enabled
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

        // ❌ Fail if not found or not enabled
        if (papi == null || !papi.isEnabled()) {
            PickYourDifficulty.getInstance().getLogger().warning(
                    "[PickYourDifficulty] ⚠️ PlaceholderAPI not found or not enabled — placeholders will not be registered."
            );

            PickYourDifficulty.debug("❌ PlaceholderAPI missing or disabled. Detected plugin = " + papi);
            return;
        }

        // 📦 Step 4: Register our placeholder expansion
        new DifficultyPlaceholder(playerDataManager).register();

        // 🧪 Debug: Confirm registration
        PickYourDifficulty.debug("✅ Registered %pickyourdifficulty_*% placeholders with PlaceholderAPI.");

        // 📣 Always log success to console for visibility
        PickYourDifficulty.getInstance().getLogger().info(
                "[PickYourDifficulty] ✅ PlaceholderAPI integration complete — placeholders registered."
        );
    }
}