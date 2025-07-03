// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🧩 JoinListener.java                         ║
// ║   Handles player join logic: auto GUI open + difficulty reload     ║
// ║   Also clears reminders and cooldowns on quit                      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.GUIManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.GraceReminderTracker;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// 👋 JoinListener — GUI + Grace Handling on Join/Quit
// ─────────────────────────────────────────────────────────────
// This listener handles:
//  • GUI opening logic (auto or dev mode)
//  • Difficulty application + grace reminders
//  • Quit cleanup of cooldown and grace memory
public class JoinListener implements Listener {

    private final GUIManager guiManager;
    private final PlayerDataManager dataManager;

    // ─────────────────────────────────────────────────────────────
    // 🧱 Constructor — Inject GUI and Data Managers
    // ─────────────────────────────────────────────────────────────
    public JoinListener(GUIManager guiManager, PlayerDataManager dataManager) {
        this.guiManager = guiManager;
        this.dataManager = dataManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 🚪 Player Join — Show GUI or Apply Difficulty
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 🧪 Debug: Log the join event with UUID
        PickYourDifficulty.debug("Player joined: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");

        // 📦 Already Selected Difficulty → Apply + Welcome
        if (dataManager.hasSelectedDifficulty(player)) {
            PickYourDifficulty.debug("Difficulty already selected for " + player.getName() + " — applying difficulty.");

            // ✅ Apply saved difficulty effects
            dataManager.applyDifficulty(player);

            // 💬 Send welcome message (if enabled in config)
            String difficulty = dataManager.getDifficultyStorage().getDifficulty(player);
            if (ConfigManager.showWelcomeOnJoin() && difficulty != null && !difficulty.isEmpty()) {
                player.sendMessage(MessagesManager.format(difficulty));
                PickYourDifficulty.debug("Welcome message sent for difficulty: " + difficulty);
            }

            // 🛡️ Grace Reminder (if grace time > 0)
            int graceTime = ConfigManager.getGraceTime(difficulty);
            if (graceTime > 0) {
                player.sendMessage(MessagesManager.get("grace-active", player));
                PickYourDifficulty.debug("Grace reminder sent to " + player.getName() + " (" + graceTime + "s)");
            }

            return;
        }

        // 📦 Dev Mode GUI Override → Force GUI open
        if (ConfigManager.devModeAlwaysShow()) {
            PickYourDifficulty.debug("Dev mode active — forcing GUI open for " + player.getName());
            guiManager.openDifficultyGUI(player);
            return;
        }

        // 📦 Auto-Open Disabled → Skip
        if (!ConfigManager.autoOpenIfUnchosen()) {
            PickYourDifficulty.debug("Auto-open disabled — skipping GUI for " + player.getName());
            return;
        }

        // 📦 GUI Cooldown Active → Skip
        if (dataManager.isGuiCooldownActive(player)) {
            PickYourDifficulty.debug("GUI cooldown active — skipping GUI for " + player.getName());
            return;
        }

        // 🪟 Open the difficulty GUI for first-time chooser
        PickYourDifficulty.debug("Opening difficulty GUI for new player: " + player.getName());
        guiManager.openDifficultyGUI(player);
    }

    // ─────────────────────────────────────────────────────────────
    // 🚪 Player Quit — Clean Up Memory Cache
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();

        GraceReminderTracker.clear(uuid);        // 🧼 Clear grace tracker
        CooldownTracker.clearCooldown(uuid);     // 🧼 Clear cooldowns

        PickYourDifficulty.debug("Player quit: " + name + " — cleared grace + cooldown cache.");
    }
}