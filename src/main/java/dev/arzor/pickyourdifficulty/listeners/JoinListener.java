// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🧩 JoinListener.java                         ║
// ║   Handles player join logic: auto GUI open + difficulty reload     ║
// ║   Also clears reminders and cooldowns on quit                      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

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

        // 📦 Mini Block: Already selected difficulty? Apply it.
        if (dataManager.hasSelectedDifficulty(player)) {
            dataManager.applyDifficulty(player);

            // 💬 Send welcome message (if enabled in config)
            if (ConfigManager.showWelcomeOnJoin()) {
                String difficulty = dataManager.getDifficultyStorage().getDifficulty(player);
                if (difficulty != null && !difficulty.isEmpty()) {
                    player.sendMessage(MessagesManager.format(difficulty));
                }
            }

            // 💬 Grace Reminder — only if grace > 0
            String difficulty = dataManager.getDifficultyStorage().getDifficulty(player);
            int graceTime = ConfigManager.getGraceTime(difficulty);
            if (graceTime > 0) {
                player.sendMessage(MessagesManager.get("grace-active", player));
            }

            return;
        }

        // 📦 Mini Block: Force GUI open if dev mode enabled
        if (ConfigManager.devModeAlwaysShow()) {
            guiManager.openDifficultyGUI(player);
            return;
        }

        // 📦 Mini Block: Skip if auto-open feature is disabled
        if (!ConfigManager.autoOpenIfUnchosen()) return;

        // 📦 Mini Block: Skip if player is still under cooldown
        if (dataManager.isGuiCooldownActive(player)) return;

        // 🪟 Open the difficulty GUI for first-time chooser
        guiManager.openDifficultyGUI(player);
    }

    // ─────────────────────────────────────────────────────────────
    // 🚪 Player Quit — Clean Up Memory Cache
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        GraceReminderTracker.clear(uuid);        // 🧼 Clear grace tracker
        CooldownTracker.clearCooldown(uuid);     // 🧼 Clear cooldowns
    }
}