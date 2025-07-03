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

// ─────────────────────────────────────────────────────────────
// 🧠 PlayerDataManager — Runtime access to player difficulty data
// ─────────────────────────────────────────────────────────────
// ⚠️ Not using Java `record` here — class encapsulates behavior, not just data.
@SuppressWarnings("ClassCanBeRecord")
public class PlayerDataManager {

    // 🗂️ Backing storage system for difficulty per player
    private final PlayerDifficultyStorage difficultyStorage;

    // ╔═══🛠️ Constructor════════════════════════════════════════════════╗
    public PlayerDataManager(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;

        // 🧪 Debug: confirm manager initialized with storage
        PickYourDifficulty.debug("🧠 PlayerDataManager initialized with PlayerDifficultyStorage backend");
    }

    // ╔═══🔍 Check if player has selected a difficulty══════════════════╗
    public boolean hasSelectedDifficulty(Player player) {
        String difficulty = difficultyStorage.getDifficulty(player);
        boolean selected = (difficulty != null);

        // 🧪 Debug: show selection state
        PickYourDifficulty.debug("🔍 hasSelectedDifficulty(): " + player.getName()
                + " → " + (selected ? "✅ Yes (" + difficulty + ")" : "❌ No selection"));

        return selected;
    }

    // ╔═══📥 Apply difficulty settings from config══════════════════════╗
    public void applyDifficulty(Player player) {
        // 📦 Get stored difficulty for this player
        String difficulty = difficultyStorage.getDifficulty(player);

        // 📦 Use fallback if none selected
        if (difficulty == null) {
            difficulty = DifficultyManager.getFallbackDifficulty();
            PickYourDifficulty.debug("📥 No stored difficulty for " + player.getName()
                    + " — using fallback: " + difficulty);
        }

        // 🧮 Lookup config values
        int despawn = ConfigManager.getDespawnTime(difficulty);
        int grace = ConfigManager.getGraceTime(difficulty);

        // 🧪 Debug: log applied values
        PickYourDifficulty.debug("🎯 Applying difficulty to " + player.getName()
                + " → " + difficulty + " (despawn: " + despawn + "s, grace: " + grace + "s)");
    }

    // ╔═══⏳ Check if GUI cooldown is active════════════════════════════╗
    public boolean isGuiCooldownActive(Player player) {
        int cooldown = ConfigManager.changeCooldownSeconds();

        // 💬 Check if cooldown is configured and currently active
        boolean active = cooldown > 0 && CooldownTracker.isCooldownActive(player.getUniqueId());

        // 🧪 Debug: cooldown status
        PickYourDifficulty.debug("⏳ isGuiCooldownActive(): " + player.getName()
                + " → " + (active ? "⛔ Active" : "✅ OK"));

        return active;
    }

    // ╔═══🕐 Start GUI cooldown timer═══════════════════════════════════╗
    public void startGuiCooldown(Player player) {
        CooldownTracker.setCooldownNow(player.getUniqueId());

        // 🧪 Debug: log cooldown start
        PickYourDifficulty.debug("🕐 Started GUI cooldown for " + player.getName());
    }

    // ╔═══📊 Get remaining cooldown time════════════════════════════════╗
    public int getCooldownSecondsLeft(Player player) {
        long seconds = CooldownTracker.getRemainingSeconds(player.getUniqueId());

        // 🧮 Convert result to int (safe cast)
        int result = (int) seconds;

        // 🧪 Debug: remaining time
        PickYourDifficulty.debug("⏱️ getCooldownSecondsLeft(): " + player.getName() + " → " + result + "s");

        return result;
    }

    // ╔═══🧠 Access raw storage backend═════════════════════════════════╗
    public PlayerDifficultyStorage getDifficultyStorage() {
        // 🧪 Debug: log access (optional)
        PickYourDifficulty.debug("🧠 getDifficultyStorage() called");
        return difficultyStorage;
    }
}
