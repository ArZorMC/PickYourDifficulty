// ╔════════════════════════════════════════════════════════════════════╗
// ║               🛡️ GraceProtectionListener.java                      ║
// ║ Cancels damage for players under grace protection.                 ║
// ║ Damage causes are defined in config under graceMode.bypass...      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

// ─────────────────────────────────────────────────────────────
// 🛡️ GraceProtectionListener — Cancels damage if under grace
// ─────────────────────────────────────────────────────────────
// This listener checks whether a player is within their grace
// period and blocks certain damage types accordingly.
//
// ✅ Grace durations are based on difficulty setting
// ❌ If grace has expired or is zero, damage is allowed
// 🔥 Blocked damage types are defined in config under:
//    graceMode.bypassDamageTypes
public class GraceProtectionListener implements Listener {

    // 🧠 Storage access to retrieve player difficulty
    private final PlayerDifficultyStorage difficultyStorage;

    public GraceProtectionListener(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;
    }

    // ─────────────────────────────────────────────────────────────
    // 🛡️ Block Damage While Under Grace Period
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        // 📦 Target Check: Only apply to players
        if (!(event.getEntity() instanceof Player player)) return;

        // 🧪 Debug: Start of damage check
        PickYourDifficulty.debug("DamageEvent: " + player.getName() + " took " + event.getDamage() + " from " + event.getCause());

        // 📦 Grace Mode Enabled?
        if (!ConfigManager.enableGraceMode()) {
            PickYourDifficulty.debug(" - Skipped: Grace mode not enabled.");
            return;
        }

        // 💬 Get this player's selected difficulty
        String difficulty = difficultyStorage.getDifficulty(player);
        PickYourDifficulty.debug(" - Difficulty: " + difficulty);

        // ⏳ Fetch grace time for that difficulty
        int graceSeconds = ConfigManager.getGraceTime(difficulty);
        PickYourDifficulty.debug(" - Grace duration: " + graceSeconds + "s");

        // 🧮 Convert playtime ticks to seconds (20 ticks = 1 sec)
        long playTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int playSeconds = (int) (playTicks / 20);
        PickYourDifficulty.debug(" - Playtime so far: " + playSeconds + "s");

        // 📦 If grace expired or grace = 0, allow damage
        if (graceSeconds <= 0) {
            PickYourDifficulty.debug(" - Skipped: Grace period is zero.");
            return;
        }

        if (playSeconds >= graceSeconds) {
            PickYourDifficulty.debug(" - Skipped: Grace has expired.");
            return;
        }

        // 📦 Block Specific Damage Causes While Under Grace
        EntityDamageEvent.DamageCause cause = event.getCause();
        List<String> protectedTypes = ConfigManager.getGraceBypassDamageTypes();

        // 🧠 These are the causes we want to CANCEL during grace period
        if (protectedTypes.contains(cause.name())) {
            event.setCancelled(true); // 🛑 Block damage

            // 🧪 Log the cancel for debugging
            PickYourDifficulty.debug(" - Cancelled: " + cause + " damage blocked (under grace)");

            // 📣 Broadcast to console (or log)
            PickYourDifficulty.getInstance().getLogger().info(
                    "[PickYourDifficulty] Cancelled " + cause + " damage for " + player.getName() + " (under grace)"
            );
        } else {
            PickYourDifficulty.debug(" - Allowed: " + cause + " not in grace bypass list.");
        }
    }
}