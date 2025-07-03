// ╔════════════════════════════════════════════════════════════════════╗
// ║                 📣 GraceReminderListener.java                      ║
// ║ Reminds players they are under grace (on login or interval)        ║
// ║ Modes controlled by config: "onLogin", "interval", or "both"       ║
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

// ─────────────────────────────────────────────────────────────
// 📣 GraceReminderListener — Sends grace reminders to players
// ─────────────────────────────────────────────────────────────
// This listener handles:
//  • Reminders on join
//  • Repeating interval reminders
//
// 🔁 Mode is configurable: "onLogin", "interval", or "both"
// ⏰ Respects reminder cooldown via GraceReminderTracker
// 🌍 Skips players in excluded worlds
public class GraceReminderListener implements Listener {

    private final PlayerDifficultyStorage difficultyStorage;

    public GraceReminderListener(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;

        // 📦 Mini Block: Only start repeating task if grace is enabled and interval reminders are active
        if (ConfigManager.enableGraceMode()
                && (reminderMode().equals("interval") || reminderMode().equals("both"))) {
            PickYourDifficulty.debug("GraceReminderListener → Starting interval task for grace reminders.");
            startIntervalTask();
        } else {
            PickYourDifficulty.debug("GraceReminderListener → Not starting interval task (config says not needed).");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🚪 onPlayerJoin — Send reminder if mode includes "onLogin"
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ConfigManager.enableGraceMode()) {         // ⛔ Grace system disabled
            PickYourDifficulty.debug("JoinReminder: Skipped — grace mode disabled.");
            return;
        }

        if (ConfigManager.disableReminder()) {          // ⛔ Reminders globally disabled
            PickYourDifficulty.debug("JoinReminder: Skipped — reminders globally disabled.");
            return;
        }

        if (reminderMode().equalsIgnoreCase("interval")) { // 💡 Not in login mode
            PickYourDifficulty.debug("JoinReminder: Skipped — mode set to interval only.");
            return;
        }

        Player player = event.getPlayer();

        // 📦 Mini Block: Check if world is excluded
        if (shouldIgnoreWorld(player)) {
            PickYourDifficulty.debug("JoinReminder: Skipped for " + player.getName() + " — world excluded.");
            return;
        }

        // ✅ Send reminder only if player is still under grace
        if (isInGrace(player)) {
            PickYourDifficulty.debug("JoinReminder: Sending to " + player.getName());
            sendReminder(player);
        } else {
            PickYourDifficulty.debug("JoinReminder: Skipped for " + player.getName() + " — grace expired.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ♻️ startIntervalTask — Repeating reminder based on config interval
    // ─────────────────────────────────────────────────────────────
    private void startIntervalTask() {
        if (ConfigManager.disableReminder()) return; // ⛔ Redundant safety check

        // 🕐 Get the interval delay in seconds from config and convert to ticks
        int intervalSeconds = ConfigManager.getGraceReminderIntervalSeconds();
        long intervalTicks = intervalSeconds * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shouldIgnoreWorld(player)) {
                        PickYourDifficulty.debug("IntervalReminder: Skipped for " + player.getName() + " — world excluded.");
                        continue;
                    }

                    // 📦 Mini Block: Send reminder only if player is still under grace
                    if (!isInGrace(player)) {
                        PickYourDifficulty.debug("IntervalReminder: Skipped for " + player.getName() + " — grace expired.");
                        continue;
                    }

                    // 🕒 Check last reminder time
                    UUID uuid = player.getUniqueId();
                    long secondsElapsed = GraceReminderTracker.getSecondsSinceLastReminder(uuid);

                    PickYourDifficulty.debug("IntervalReminder: Checking " + player.getName()
                            + " → last reminder " + secondsElapsed + "s ago (interval = " + intervalSeconds + "s)");

                    // 📦 Mini Block: Send reminder if interval exceeded
                    if (secondsElapsed >= intervalSeconds) {
                        PickYourDifficulty.debug("IntervalReminder: Sending reminder to " + player.getName());
                        sendReminder(player);
                    }
                }
            }

            // 🕐 Initial delay = 20 ticks (1s), repeat using config interval
        }.runTaskTimer(PickYourDifficulty.getInstance(), 20L, intervalTicks);
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

        PickYourDifficulty.debug("GraceCheck: " + player.getName() + " → grace=" + graceTotal + "s, played=" + playSeconds + "s");

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
