// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🧩 CommandReset.java                           ║
// ║  Handles /pyd reset <player> — clears difficulty + cooldown        ║
// ║  Requires permission: pickyourdifficulty.reset                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// 🧩 CommandReset — Clears player difficulty and cooldown
// ─────────────────────────────────────────────────────────────
public class CommandReset implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage formatter for sending formatted colored messages
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Toggle command triggered
        PickYourDifficulty.debug("/pyd reset invoked by: " + sender.getName());

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND sender has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasResetPermission(sender)) {
            PickYourDifficulty.debug(sender.getName() + " attempted to run /pyd reset without permission.");

            // 🚫 Inform sender they don't have permission to use this command
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-reset")));
            return true;
        }

        // ╔═══📏 Argument Check══════════════════════════════════════════════════════════════════════════════╗

        // 💬 Must specify a player name (e.g., /pyd reset ArZor)
        if (args.length < 2) {
            PickYourDifficulty.debug("Missing player argument for /pyd reset. Showing usage.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("reset.usage")));
            return true;
        }

        // ╔═══🎯 Target Player Resolution════════════════════════════════════════════════════════════════════╗

        // 🔍 Resolve the player name to an OfflinePlayer (even if they're not online)
        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        PickYourDifficulty.debug(sender.getName() + " is attempting to reset player: " + targetName + " (" + targetUUID + ")");

        // ❓ Check if this player has selected a difficulty before
        if (!PlayerDifficultyStorage.getInstance().hasSelected(targetUUID)) {
            PickYourDifficulty.debug("Reset aborted — player " + targetName + " has no selected difficulty.");
            sender.sendMessage(MessagesManager.format("admin.no-difficulty"));
            return true;
        }

        // ╔═══🧹 Clear Difficulty + Cooldown═════════════════════════════════════════════════════════════════╗

        // 👤 If the player is online, try clearing their difficulty using the Player object
        if (target.isOnline()) {
            PickYourDifficulty.debug("Player " + targetName + " is online — clearing live difficulty.");
            if (target.getPlayer() != null) {
                PlayerDifficultyStorage.getInstance().clearDifficulty(target.getPlayer());
            } else {
                // 🛡️ Fallback — shouldn't happen, but avoids a null pointer exception
                PickYourDifficulty.debug("Warning: getPlayer() returned null — falling back to UUID-based clear.");
                PlayerDifficultyStorage.getInstance().clearDifficulty(targetUUID);
            }
        } else {
            // 🌐 Player is offline — clear using UUID-based storage
            PickYourDifficulty.debug("Player " + targetName + " is offline — clearing by UUID.");
            PlayerDifficultyStorage.getInstance().clearDifficulty(targetUUID);
        }

        // ⏱️ Also clear their cooldown if one exists
        PickYourDifficulty.debug("Clearing cooldown for player: " + targetName);
        CooldownTracker.clearCooldown(targetUUID);

        // ╔═══✅ Confirmation Message════════════════════════════════════════════════════════════════════════╗

        // 📣 Inform the sender that the reset was successful
        PickYourDifficulty.debug("Reset complete for player: " + targetName);
        sender.sendMessage(mm.deserialize(MessagesManager.get("reset.success")
                .replace("<player>", target.getName() != null ? target.getName() : targetUUID.toString())));

        return true;
    }
}