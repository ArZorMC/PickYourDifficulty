// ╔════════════════════════════════════════════════════════════════════╗
// ║             🪧 CommandToggleHolograms.java                         ║
// ║   Allows players to toggle visibility of despawn timer holograms   ║
// ║       Honors permissions and configurable MiniMessage output       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.HologramManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

// ─────────────────────────────────────────────────────────────
// 🪧 CommandToggleHolograms — /pyd toggleholograms
// ─────────────────────────────────────────────────────────────
public class CommandToggleHolograms implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 📦 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage parser for deserializing text from messages.yml
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Toggle command triggered
        PickYourDifficulty.debug("/pyd toggleholograms invoked by: " + sender.getName());

        // ╔═══🚫 Must Be Player═══════════════════════════════════════╗
        // This command cannot be used from console or command blocks
        if (!(sender instanceof Player player)) {
            PickYourDifficulty.debug("Blocked /pyd toggleholograms — sender is not a player.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("toggle.players-only")));
            return true;
        }

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND player has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasHologramTogglePermission(player)) {
            PickYourDifficulty.debug("Blocked /pyd toggleholograms — " + player.getName() + " lacks permission.");

            // 🚫 Inform sender they don't have permission to use this command
            player.sendMessage(mm.deserialize(MessagesManager.get("toggle.no-permission")));
            return true;
        }

        // ╔═══✅ Toggle hologram visibility═════════════════════════════════════════════════════════════════╗

        // 🔄 Flip visibility for this player
        boolean nowHidden = HologramManager.toggleHidden(player);
        PickYourDifficulty.debug("Toggled hologram visibility for " + player.getName() + " → nowHidden = " + nowHidden);

        // 📩 Determine which message to show
        String messageKey = nowHidden ? "toggle.success-off" : "toggle.success-on";

        // 📨 Send updated status message
        player.sendMessage(mm.deserialize(MessagesManager.get(messageKey)));

        return true;
    }
}