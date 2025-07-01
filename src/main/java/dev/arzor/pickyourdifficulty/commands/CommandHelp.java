// ╔════════════════════════════════════════════════════════════════════╗
// ║                        📖 CommandHelp.java                         ║
// ║   Handles /pyd help — shows available commands interactively       ║
// ║   Filters by permission; OPs and wildcard users see everything     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.List;

public class CommandHelp implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage parser for formatting hover/clickable rich text */
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Handles the execution of /pyd help
     * Displays available commands interactively based on permission.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ⛔ This command must be run by a player (not console)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessagesManager.format("error.players-only"));
            return true;
        }

        // ✅ Determine if the player should see permission hovers
        // We enable this if the player is OP or has wildcard access
        boolean showPermissions = player.isOp()
                || PermissionUtil.has(player, "*")
                || PermissionUtil.has(player, "pickyourdifficulty.*");

        // 🧾 Send help menu header (from messages.yml)
        player.sendMessage(MessagesManager.format("help.header"));

        // ╔═══📌 Command Entries════════════════════════════════════════════════════════════════════════════╗
        // Each help entry includes click-to-suggest and optional hover permission
        sendEntry(player, "gui", showPermissions);               // 🧭 /pyd gui
        sendEntry(player, "info", showPermissions);              // ℹ️ /pyd info
        sendEntry(player, "reload", showPermissions);            // 🔁 /pyd reload
        sendEntry(player, "reset", showPermissions);             // 🧹 /pyd reset <player>
        sendEntry(player, "set", showPermissions);               // 🎯 /pyd set <player> <difficulty>
        sendEntry(player, "toggleholograms", showPermissions);   // 👁️ /pyd toggleholograms

        return true;
    }

    // ─────────────────────────────────────────────────────────────
    // 📌 Help Entry Display
    // ─────────────────────────────────────────────────────────────

    /**
     * Sends a single help entry to the player with rich text features.
     * Will not send if the player lacks permission and showPermissions is false.
     *
     * @param player          Player to receive the help line
     * @param key             Config key for this help entry (e.g., "gui")
     * @param showPermissions Whether to add permission hover text
     */
    private void sendEntry(Player player, String key, boolean showPermissions) {
        String permission = MessagesManager.get("help.commands." + key + ".permission");

        // ❌ Don't show the command if the player can't use it, and we're not showing all permissions
        if (!PermissionUtil.hasAny(player, List.of(permission)) && !showPermissions) return;

        String text = MessagesManager.get("help.commands." + key + ".text");       // Line text (MiniMessage)
        String suggest = MessagesManager.get("help.commands." + key + ".suggest"); // Suggested command on click

        // ✅ Build the message with click-to-suggest behavior
        Component entry = mm.deserialize(text)
                .clickEvent(ClickEvent.suggestCommand(suggest));

        // 💡 If permissions are visible, show them in hover tooltip
        if (showPermissions) {
            entry = entry.hoverEvent(HoverEvent.showText(Component.text("Permission: " + permission)));
        }

        // 📤 Send the entry to the player
        player.sendMessage(entry);
    }
}
