// ╔════════════════════════════════════════════════════════════════════╗
// ║                   📜 RulesAcceptListener.java                      ║
// ║     Hooks into AcceptTheRules (via reflection) to auto-open GUI    ║
// ║   after rule acceptance, without requiring hard plugin dependency. ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.GUIManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

// ─────────────────────────────────────────────────────────────
// 📜 RulesAcceptListener — Handles AcceptTheRules hook
// ─────────────────────────────────────────────────────────────
// This listener:
//  • Registers dynamically if AcceptTheRules is installed
//  • Listens reflectively for PlayerAcceptRulesEvent
//  • Opens the difficulty GUI after rules are accepted
public class RulesAcceptListener implements Listener {

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║                  🔧 Core Dependencies Injected                     ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private final JavaPlugin plugin;
    private final GUIManager guiManager;
    private final PlayerDataManager dataManager;
    private final boolean enabled;

    // ─────────────────────────────────────────────────────────────
    // 🧪 Constructor — Only enables listener if plugin + config match
    // ─────────────────────────────────────────────────────────────
    public RulesAcceptListener(JavaPlugin plugin, GUIManager guiManager, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.dataManager = dataManager;

        // 🔎 Enable integration only if plugin is present + config allows it
        this.enabled = ConfigManager.autoOpenAfterRules() && isAcceptTheRulesPresent();

        if (enabled) {
            Bukkit.getPluginManager().registerEvents(this, plugin);

            // 🧪 Debug: Listener registered successfully
            PickYourDifficulty.debug("RulesAcceptListener enabled (AcceptTheRules detected + config enabled).");
        } else {
            // 🧪 Debug: Listener not registered due to missing plugin or config
            PickYourDifficulty.debug("RulesAcceptListener disabled (plugin not detected or config disabled).");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 Reflection Check — Is AcceptTheRules installed?
    // ─────────────────────────────────────────────────────────────
    private boolean isAcceptTheRulesPresent() {
        try {
            Class.forName("me.rubix327.accepttherules.api.PlayerAcceptRulesEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🧭 Event Catch-All — Handle AcceptTheRules if present
    // ─────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPossibleAcceptRules(Event event) {
        // 🚫 Skip if integration wasn't enabled
        if (!enabled) return;

        // 📦 Match event class name (via reflection)
        if (event.getClass().getName().equals("me.rubix327.accepttherules.api.PlayerAcceptRulesEvent")) {

            // 🧪 Debug: Event detected
            PickYourDifficulty.debug("AcceptTheRules event detected via reflection.");

            try {
                // 🪪 Extract Player object via reflective call
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);

                // 🧪 Debug: Player confirmed
                PickYourDifficulty.debug("Player accepted rules: " + player.getName());

                // 🔐 Only open GUI if not already selected
                if (!dataManager.hasSelectedDifficulty(player)) {
                    guiManager.openDifficultyGUI(player);

                    // 🧪 Debug: GUI opened
                    PickYourDifficulty.debug("Difficulty GUI opened for " + player.getName());
                } else {
                    // 🧪 Debug: Player already has a difficulty
                    PickYourDifficulty.debug("No GUI opened — " + player.getName() + " already selected a difficulty.");
                }

            } catch (Exception ex) {
                // ❌ Handle unexpected reflection failures
                plugin.getLogger().warning("❌ Failed to handle AcceptTheRules event: " + ex.getMessage());

                // 🧪 Robust debug logging
                plugin.getLogger().log(Level.SEVERE, "Stack trace for AcceptTheRules failure:", ex);
            }
        }
    }
}