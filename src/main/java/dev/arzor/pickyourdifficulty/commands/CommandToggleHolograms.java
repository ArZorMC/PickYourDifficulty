// ╔════════════════════════════════════════════════════════════════════╗
// ║             🪧 CommandToggleHolograms.java                         ║
// ║   Allows players to toggle visibility of despawn timer holograms   ║
// ║       Honors permissions and configurable MiniMessage output       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

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

public class CommandToggleHolograms implements CommandExecutor {

    // 🧵 MiniMessage parser for deserializing text from messages.yml
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ╔═══🙅‍♂️ Player-only check════════════════════════════════════════════════════════════════════════╗
        if (!(sender instanceof Player player)) {
            // ❌ Only players can toggle holograms — not console or command blocks
            sender.sendMessage(mm.deserialize(MessagesManager.get("toggle.players-only")));
            return true;
        }

        // ╔═══🔐 Permission check═══════════════════════════════════════════════════════════════════════════╗
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasHologramTogglePermission(player)) {
            // ❌ Player doesn't have toggle permission
            player.sendMessage(mm.deserialize(MessagesManager.get("toggle.no-permission")));
            return true;
        }

        // ╔═══✅ Toggle hologram visibility═════════════════════════════════════════════════════════════════╗

        // 🔄 Flip visibility for this player
        boolean nowHidden = HologramManager.toggleHidden(player);

        // 📩 Determine which message to show
        String messageKey = nowHidden ? "toggle.success-off" : "toggle.success-on";

        // 📨 Send updated status message
        player.sendMessage(mm.deserialize(MessagesManager.get(messageKey)));
        return true;
    }
}