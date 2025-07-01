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

public class GraceProtectionListener implements Listener {

    private final PlayerDifficultyStorage difficultyStorage;

    public GraceProtectionListener(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;
    }

    // ─────────────────────────────────────────────────────────────
    // 🛡️ Block Damage While Under Grace Period
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        // 📦 Mini Block: Only apply to players
        if (!(event.getEntity() instanceof Player player)) return;

        // 📦 Mini Block: Grace mode must be enabled in config
        if (!ConfigManager.enableGraceMode()) return;

        // 💬 Get this player's selected difficulty
        String difficulty = difficultyStorage.getDifficulty(player);

        // ⏳ Fetch grace time for that difficulty
        int graceSeconds = ConfigManager.getGraceTime(difficulty);

        // 🧮 Convert playtime ticks to seconds (20 ticks = 1 sec)
        long playTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int playSeconds = (int) (playTicks / 20);

        // 📦 Mini Block: If grace expired or grace = 0, allow damage
        if (graceSeconds <= 0 || playSeconds >= graceSeconds) return;

        // 🧪 If damage cause is listed in bypass types, block it
        EntityDamageEvent.DamageCause cause = event.getCause();
        List<String> protectedTypes = ConfigManager.getGraceBypassDamageTypes();

        if (protectedTypes.contains(cause.name())) {
            event.setCancelled(true); // 🛑 Block damage

            // 🧪 Debug log if enabled
            if (ConfigManager.isDebugMode()) {
                PickYourDifficulty.getInstance().getLogger().info(
                        "[PickYourDifficulty] Cancelled " + cause + " damage for " + player.getName() + " (under grace)"
                );
            }
        }
    }
}