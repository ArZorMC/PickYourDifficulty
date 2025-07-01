// ╔════════════════════════════════════════════════════════════════════╗
// ║                 📣 GraceReminderListener.java                      ║
// ║ Reminds players they are under grace (on login or interval)       ║
// ║ Modes controlled by config: "onLogin", "interval", or "both"      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.storage.GraceReminderTracker;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GraceReminderListener implements Listener {

    private final PlayerDifficultyStorage difficultyStorage;

    public GraceReminderListener(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;

        // 📦 Mini Block: Only start repeating task if grace is enabled and interval reminders are active
        if (ConfigManager.enableGraceMode()
                && (reminderMode().equals("interval") || reminderMode().equals("both"))) {
            startIntervalTask();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🚪 Join Listener — Send Reminder on Login
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ConfigManager.enableGraceMode()) return;         // ⛔ Grace system disabled
        if (ConfigManager.disableReminder()) return;          // ⛔ Reminders globally disabled
        if (reminderMode().equalsIgnoreCase("interval")) return; // 💡 Not in login mode

        Player player = event.getPlayer();

        // 📦 Mini Block: Check if world is excluded or player is not in grace
        if (shouldIgnoreWorld(player)) return;
        if (isInGrace(player)) return;

        sendReminder(player);
    }

    // ─────────────────────────────────────────────────────────────
    // ♻️ Interval Task — Run Every Minute
    // ─────────────────────────────────────────────────────────────
    private void startIntervalTask() {
        if (ConfigManager.disableReminder()) return; // ⛔ Redundant safety check

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shouldIgnoreWorld(player)) continue;

                    // 📦 Mini Block: Send reminder only if player is still under grace
                    if (isInGrace(player)) continue;

                    // 🕒 Check last reminder time
                    UUID uuid = player.getUniqueId();
                    long secondsElapsed = GraceReminderTracker.getSecondsSinceLastReminder(uuid);
                    int interval = ConfigManager.getGraceReminderIntervalSeconds();

                    // 📦 Mini Block: Send reminder if interval exceeded
                    if (secondsElapsed >= interval) {
                        sendReminder(player);
                    }
                }
            }
        }.runTaskTimer(PickYourDifficulty.getInstance(), 20L, 20L * 60); // 🕐 Run every 60 seconds
    }

    // ─────────────────────────────────────────────────────────────
    // 📬 Send Reminder Message
    // ─────────────────────────────────────────────────────────────
    private void sendReminder(Player player) {
        Component msg = MessagesManager.get("grace-active", player);
        player.sendMessage(msg);

        // 🧾 Update reminder tracker timestamp
        GraceReminderTracker.updateReminder(player.getUniqueId());

        // 🧪 Optional debug logging
        if (ConfigManager.isDebugMode()) {
            PickYourDifficulty.getInstance().getLogger().info(
                    "[PickYourDifficulty] Grace reminder sent to " + player.getName()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 Grace Check — Is Player Still In Grace?
    // ─────────────────────────────────────────────────────────────
    private boolean isInGrace(Player player) {
        String difficulty = difficultyStorage.getDifficulty(player);
        int graceTotal = ConfigManager.getGraceTime(difficulty);

        // 🧮 Convert play ticks to seconds (20 ticks = 1 second)
        long playTicks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
        int playSeconds = (int) (playTicks / 20);

        return graceTotal > 0 && playSeconds < graceTotal;
    }

    // ─────────────────────────────────────────────────────────────
    // 🌍 World Exclusion Check — Skip Reminder if World Is Ignored
    // ─────────────────────────────────────────────────────────────
    private boolean shouldIgnoreWorld(Player player) {
        return ConfigManager.getDisabledOverrideWorlds().stream()
                .anyMatch(world -> world.equalsIgnoreCase(player.getWorld().getName()));
    }

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Config Mode Lookup
    // ─────────────────────────────────────────────────────────────
    private String reminderMode() {
        return ConfigManager.getGraceReminderMode().toLowerCase();
    }
}
