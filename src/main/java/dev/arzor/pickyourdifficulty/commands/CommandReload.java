// ╔════════════════════════════════════════════════════════════════════╗
// ║                    🧩 CommandReload.java                           ║
// ║   Handles /pyd reload — reloads config and messages                ║
// ║   Requires permission: pickyourdifficulty.reload                   ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.managers.ReloadManager;

import dev.arzor.pickyourdifficulty.storage.GraceReminderTracker;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public class CommandReload implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage instance for formatting colored output */
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Executes /pyd reload — re-initializes config + message files.
     * Only works if the sender has reload permission.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ╔═══⛔ Permission check═════════════════════════════════════════════════════════════════════════════╗

        // 💬 If permission enforcement is on AND sender lacks reload rights...
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasReloadPermission(sender)) {
            // 🚫 Inform sender they don't have permission
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
            return true;
        }

        // ╔═══🧪 Dev Mode Reset — Rebuild Reloadables═════════════════════════════════════════════════════════╗

        // 🛠️ If dev mode is active, re-register Config and MessagesManager from scratch
        if (ConfigManager.devModeAlwaysShow()) {
            ReloadManager.clearAll(); // 🧼 Clear previous reloadable objects
            ReloadManager.register(new ConfigManager());       // 📄 Reload config logic
            ReloadManager.register(new MessagesManager());     // 💬 Reload message templates
        }

        // ╔═══🔁 Reload All Components════════════════════════════════════════════════════════════════════════╗

        // ♻️ Go through every Reloadable and run its reload logic (e.g., reading config/messages again)
        ReloadManager.reloadAll();

        // ╔═══🧽 Grace Reminder Reset═════════════════════════════════════════════════════════════════════════╗

        // 🧹 Clear all tracked grace reminders so they restart from fresh state
        GraceReminderTracker.clearAll();

        // ✅ Notify sender that reload completed
        sender.sendMessage(MessagesManager.get("command.reload-success"));

        return true;
    }
}
