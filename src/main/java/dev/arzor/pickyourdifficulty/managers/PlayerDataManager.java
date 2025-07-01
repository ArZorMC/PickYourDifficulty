// ╔════════════════════════════════════════════════════════════════════╗
// ║                   🧠 PlayerDataManager.java                        ║
// ║  Central access point for player difficulty + cooldown tracking    ║
// ║  Applies difficulty settings, enforces GUI cooldowns, and wraps    ║
// ║  access to PlayerDifficultyStorage                                 ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;

import org.bukkit.entity.Player;

/**
 * 📦 Centralized access to player difficulty data.
 * This currently wraps PlayerDifficultyStorage, but allows future logic expansions
 * (e.g., tracking selection state, cooldown checks, or cross-storage caching).
 */
// ⚠️ Not using Java `record` here — class encapsulates behavior, not just data.
@SuppressWarnings("ClassCanBeRecord")
public class PlayerDataManager {

    // ╔════════════════════════════════════════════════════════════╗
    // ║                  🧠 Stored Difficulty Data                 ║
    // ╚════════════════════════════════════════════════════════════╝
    private final PlayerDifficultyStorage difficultyStorage;

    // ─────────────────────────────────────────────────────────────
    // 🛠️ Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Constructs a new PlayerDataManager with linked storage and cooldowns.
     *
     * @param difficultyStorage The backend for player difficulty data
     */
    public PlayerDataManager(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;
    }

    // ─────────────────────────────────────────────────────────────
    // 📦 Difficulty Selection Logic
    // ─────────────────────────────────────────────────────────────

    /**
     * 🧩 Checks whether the player has already selected a difficulty.
     *
     * @param player The player to check
     * @return true if they have selected; false otherwise
     */
    public boolean hasSelectedDifficulty(Player player) {
        return difficultyStorage.getDifficulty(player) != null;
    }

    /**
     * 📥 Applies difficulty settings to a player and logs key details.
     *
     * @param player The player being processed
     */
    public void applyDifficulty(Player player) {
        // 📦 Get the player's difficulty from storage
        String difficulty = difficultyStorage.getDifficulty(player);

        // 🔁 If not found, fallback to default difficulty
        if (difficulty == null) {
            difficulty = DifficultyManager.getFallbackDifficulty();
        }

        // 🧮 Lookup config values for despawn and grace period
        int despawn = ConfigManager.getDespawnTime(difficulty);
        int grace = ConfigManager.getGraceTime(difficulty);

        // 🪵 Log what's being applied to this player
        PickYourDifficulty.getInstance().getLogger().info("[PickYourDifficulty] Applying difficulty to " + player.getName()
                + " → " + difficulty + " (despawn: " + despawn + "s, grace: " + grace + "s)");
    }

    // ─────────────────────────────────────────────────────────────
    // ⏳ GUI Cooldown Management
    // ─────────────────────────────────────────────────────────────

    /**
     * ⏱️ Checks if the player is still under cooldown and *cannot* open GUI.
     *
     * @param player The player to check
     * @return true if cooldown is active (GUI locked); false if allowed
     */
    public boolean isGuiCooldownActive(Player player) {
        // 🧮 If cooldown is > 0 and timer is running, player is blocked
        int cooldown = ConfigManager.changeCooldownSeconds();
        return cooldown > 0 && CooldownTracker.isCooldownActive(player.getUniqueId());
    }

    /**
     * ⏱️ Starts a cooldown timer for the player after switching difficulty.
     *
     * @param player The player to track
     */
    public void startGuiCooldown(Player player) {
        // 🕐 Starts tracking from current time
        CooldownTracker.setCooldownNow(player.getUniqueId());
    }

    /**
     * 📊 Returns how many seconds are left in the cooldown (if any).
     *
     * @param player The player to query
     * @return Seconds remaining, or 0 if no cooldown
     */
    public int getCooldownSecondsLeft(Player player) {
        // 🧮 Convert millis to whole seconds by casting
        return (int) CooldownTracker.getRemainingSeconds(player.getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────
    // 🔓 Raw Storage Access
    // ─────────────────────────────────────────────────────────────

    /**
     * 🧠 Exposes the wrapped storage system for advanced use.
     *
     * @return The PlayerDifficultyStorage instance
     */
    public PlayerDifficultyStorage getDifficultyStorage() {
        return difficultyStorage;
    }
}
