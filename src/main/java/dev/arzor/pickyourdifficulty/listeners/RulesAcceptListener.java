// ╔════════════════════════════════════════════════════════════════════╗
// ║                   📜 RulesAcceptListener.java                      ║
// ║     Hooks into AcceptTheRules (via reflection) to auto-open GUI    ║
// ║   after rule acceptance, without requiring hard plugin dependency. ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

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
        if (!enabled) return;

        // 📦 Match event class name (reflection)
        if (event.getClass().getName().equals("me.rubix327.accepttherules.api.PlayerAcceptRulesEvent")) {
            try {
                // 🪪 Grab Player object via reflection
                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);

                // 🔐 Only open GUI if not already selected
                if (!dataManager.hasSelectedDifficulty(player)) {
                    guiManager.openDifficultyGUI(player);
                }

            } catch (Exception ex) {
                plugin.getLogger().warning("❌ Failed to handle AcceptTheRules event: " + ex.getMessage());
            }
        }
    }
}