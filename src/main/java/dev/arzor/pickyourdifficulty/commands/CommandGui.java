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

public class CommandGui implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 📦 Dependencies
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage instance for formatting MiniMessage-enabled strings */
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Executes the /pyd gui command to open the difficulty selection GUI.
     * Only accessible by players with permission, and subject to config rules.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ⛔ Block non-player senders (e.g., console)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.players-only")));
            return true;
        }

        // 🔐 Block players without permission (if permission checks are enabled)
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasGuiAccess(player)) {
            player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-gui")));
            return true;
        }

        // 🛑 Player already selected a difficulty AND switching is disabled
        if (!ConfigManager.allowDifficultyChange()
                && PickYourDifficulty.getInstance().getPlayerDataManager()
                .getDifficultyStorage().hasSelectedDifficulty(player)) {

            // 💬 Tell the player they can't change it again
            player.sendMessage(mm.deserialize(MessagesManager.get("gui.already-selected")));

            // 🔇 Play denied sound to signal the GUI is locked
            SoundManager.playDeniedSound(player, false);
            return true;
        }

        // ⏳ Cooldown active — block access to GUI temporarily
        long secondsLeft = CooldownTracker.getRemainingSeconds(player.getUniqueId());

        if (secondsLeft > 0) {
            // 🧮 Format the cooldown (e.g. 1m 12s) using helper utility
            String formatted = TimeFormatUtil.formatCooldown(secondsLeft);

            // 💬 Replace <time> placeholder in cooldown message
            String rawMessage = MessagesManager.get("gui.cooldown-wait").replace("<time>", formatted);

            // 🔕 Inform player and play cooldown-denied sound
            player.sendMessage(mm.deserialize(rawMessage));
            SoundManager.playDeniedSound(player, true);
            return true;
        }

        // ✅ All checks passed — open the difficulty selection GUI
        GUIManager.getInstance().openDifficultyGUI(player);

        // 📍 Optional: placeholder confirmation message
        player.sendMessage(mm.deserialize(MessagesManager.get("gui.open-placeholder")));
        return true;
    }
}
