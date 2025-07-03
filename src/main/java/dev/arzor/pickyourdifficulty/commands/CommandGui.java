// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🧩 CommandGui.java                          ║
// ║   Handles /pyd gui — opens the difficulty selection interface      ║
// ║   Requires permission: pickyourdifficulty.gui                      ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.GUIManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.managers.SoundManager;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;
import dev.arzor.pickyourdifficulty.utils.TimeFormatUtil;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

// ─────────────────────────────────────────────────────────────
// 🧩 CommandGui — Handles /pyd gui logic and access checks
// ─────────────────────────────────────────────────────────────
public class CommandGui implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 📦 Dependencies
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage instance for deserializing formatted messages
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Received command execution request
        PickYourDifficulty.debug("Command /pyd gui invoked by: " + sender.getName());

        // ╔═══🚫 Must Be Player═══════════════════════════════════════╗
        // This command cannot be used from console or command blocks
        if (!(sender instanceof Player player)) {
            PickYourDifficulty.debug("Command blocked — sender is not a player.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.players-only")));
            return true;
        }

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND player has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasGuiAccess(player)) {
            PickYourDifficulty.debug(player.getName() + " lacks permission to open GUI.");

            // 🚫 Inform sender they don't have permission to use this command
            player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-gui")));
            return true;
        }

        // ╔═══🛑 Difficulty Already Selected & Locked═════════════════╗
        // If switching difficulties is disallowed, and they already picked one, deny access
        if (!ConfigManager.allowDifficultyChange()
                && PickYourDifficulty.getInstance().getPlayerDataManager()
                .getDifficultyStorage().hasSelectedDifficulty(player)) {

            // 💬 Tell the player they can't change it again
            PickYourDifficulty.debug(player.getName() + " attempted to open GUI, but switching is disabled and already selected.");
            player.sendMessage(mm.deserialize(MessagesManager.get("gui.already-selected")));

            // 🔇 Play denied sound to signal the GUI is locked
            SoundManager.playDeniedSound(player, false);
            return true;
        }

        // ╔═══⏳ Cooldown Handling═════════════════════════════════════╗
        // Get remaining cooldown in seconds for this player
        long secondsLeft = CooldownTracker.getRemainingSeconds(player.getUniqueId());

        if (secondsLeft > 0) {
            PickYourDifficulty.debug(player.getName() + " is under cooldown — " + secondsLeft + "s remaining.");

            // 🧮 Convert raw seconds to friendly text format (e.g. "1m 12s")
            // This helps players understand how long they must wait before retrying.
            // TimeFormatUtil internally breaks the time into minutes + seconds and returns a human-readable string.
            String formatted = TimeFormatUtil.formatCooldown(secondsLeft);

            // 💬 Replace <time> placeholder in cooldown message
            String rawMessage = MessagesManager.get("gui.cooldown-wait").replace("<time>", formatted);

            // 🔕 Inform player and play cooldown-denied sound variant
            player.sendMessage(mm.deserialize(rawMessage));
            SoundManager.playDeniedSound(player, true);
            return true;
        }

        // ╔═══✅ All checks passed — open GUI══════════════════════════╗

        PickYourDifficulty.debug(player.getName() + " passed all checks — opening GUI.");
        GUIManager.getInstance().openDifficultyGUI(player);

        // 💬 Optional placeholder message (can be used in messages.yml to confirm GUI opened)
        player.sendMessage(mm.deserialize(MessagesManager.get("gui.open-placeholder")));
        return true;
    }
}
