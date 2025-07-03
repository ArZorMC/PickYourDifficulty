// ╔════════════════════════════════════════════════════════════════════╗
// ║                        📖 CommandHelp.java                         ║
// ║   Handles /pyd help — shows available commands interactively       ║
// ║   Filters by permission; OPs and wildcard users see everything     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
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

// ─────────────────────────────────────────────────────────────
// 📖 CommandHelp — Displays interactive command list
// ─────────────────────────────────────────────────────────────
public class CommandHelp implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage parser for hover/clickable rich text
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Help command received
        PickYourDifficulty.debug("/pyd help invoked by: " + sender.getName());

        // ╔═══🚫 Must Be Player═══════════════════════════════════════╗
        // This command cannot be run from console or command blocks
        if (!(sender instanceof Player player)) {
            PickYourDifficulty.debug("Blocked /pyd help — sender is not a player.");
            sender.sendMessage(MessagesManager.format("error.players-only"));
            return true;
        }

        // ╔═══🔓 Determine Permission Visibility══════════════════════╗
        // OPs and players with wildcard permissions can see hover tooltips
        boolean showPermissions = player.isOp()
                || PermissionUtil.has(player, "*")
                || PermissionUtil.has(player, "pickyourdifficulty.*");

        PickYourDifficulty.debug("Help menu shown to: " + player.getName() + " | showPermissions=" + showPermissions);

        // ╔═══🧾 Send Header═══════════════════════════════════════════╗
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

    // 💬 Sends a formatted help line to the player, with optional permission hover
    private void sendEntry(Player player, String key, boolean showPermissions) {

        // 📥 Grab permission node required for this command
        String permission = MessagesManager.get("help.commands." + key + ".permission");

        // ❌ Skip if the player has no permission AND we're not showing hidden commands
        if (!PermissionUtil.hasAny(player, List.of(permission)) && !showPermissions) {
            PickYourDifficulty.debug("Skipping help entry for /pyd " + key + " — no permission and showPermissions=false");
            return;
        }

        // 📥 Load the text to display and the suggested command for click
        String text = MessagesManager.get("help.commands." + key + ".text");       // Line text (MiniMessage)
        String suggest = MessagesManager.get("help.commands." + key + ".suggest"); // Suggested command on click

        PickYourDifficulty.debug("Adding help entry: /pyd " + key + " → suggests: '" + suggest + "'");

        // ✅ Build a clickable message using MiniMessage
        Component entry = mm.deserialize(text)
                .clickEvent(ClickEvent.suggestCommand(suggest));

        // 💡 If hover is enabled, show required permission on hover
        if (showPermissions) {
            entry = entry.hoverEvent(HoverEvent.showText(Component.text("Permission: " + permission)));
        }

        // 📤 Send the final interactive entry to the player
        player.sendMessage(entry);
    }
}
