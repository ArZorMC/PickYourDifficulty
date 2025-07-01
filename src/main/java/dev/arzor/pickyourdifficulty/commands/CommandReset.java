// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🧩 CommandReset.java                           ║
// ║  Handles /pyd reset <player> — clears difficulty + cooldown        ║
// ║  Requires permission: pickyourdifficulty.reset                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

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

public class CommandReset implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage formatter for colorized message output */
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Executes /pyd reset <player> — clears their difficulty and cooldown.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ╔═══⛔ Permission Check═════════════════════════════════════════════════════════════════════════════╗

        // 💬 If permission enforcement is enabled and the sender lacks reset permission...
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasResetPermission(sender)) {
            // 🚫 Inform the sender that they don't have access to this command
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-reset")));
            return true;
        }

        // ╔═══📏 Argument Check══════════════════════════════════════════════════════════════════════════════╗

        // 💬 Must specify a player name (e.g., /pyd reset ArZor)
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("reset.usage")));
            return true;
        }

        // ╔═══🎯 Target Player Resolution════════════════════════════════════════════════════════════════════╗

        // 🔍 Get offline player by name (works even if they're not online)
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetUUID = target.getUniqueId();

        // ❓ Check if this player has selected a difficulty before
        if (!PlayerDifficultyStorage.getInstance().hasSelected(targetUUID)) {
            sender.sendMessage(MessagesManager.format("admin.no-difficulty"));
            return true;
        }

        // ╔═══🧹 Clear Difficulty + Cooldown═════════════════════════════════════════════════════════════════╗

        // 💬 If the player is online, and we can get the Player object, clear directly
        if (target.isOnline()) {
            if (target.getPlayer() != null) {
                PlayerDifficultyStorage.getInstance().clearDifficulty(target.getPlayer());
            } else {
                // 🛡️ Fallback — shouldn't happen, but avoids a null pointer exception
                PlayerDifficultyStorage.getInstance().clearDifficulty(targetUUID);
            }
        } else {
            // 📦 Offline — clear difficulty directly using UUID
            PlayerDifficultyStorage.getInstance().clearDifficulty(targetUUID);
        }

        // ⏱️ Also clear their cooldown if one exists
        CooldownTracker.clearCooldown(targetUUID);

        // ╔═══✅ Confirmation Message════════════════════════════════════════════════════════════════════════╗

        // 💬 Let the sender know it was successful
        sender.sendMessage(mm.deserialize(MessagesManager.get("reset.success")
                .replace("<player>", target.getName() != null ? target.getName() : targetUUID.toString())));

        return true;
    }
}