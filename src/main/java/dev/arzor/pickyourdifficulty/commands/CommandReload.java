// ╔════════════════════════════════════════════════════════════════════╗
// ║                    🧩 CommandReload.java                           ║
// ║   Handles /pyd reload — reloads config and messages                ║
// ║   Requires permission: pickyourdifficulty.reload                   ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
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

// ─────────────────────────────────────────────────────────────
// 🧩 CommandReload — Reloads plugin config and messages
// ─────────────────────────────────────────────────────────────
public class CommandReload implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage instance for formatting colored output
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Toggle command triggered
        PickYourDifficulty.debug("/pyd reload invoked by: " + sender.getName());

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND sender has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasReloadPermission(sender)) {
            PickYourDifficulty.debug(sender.getName() + " attempted to reload without permission.");

            // 🚫 Inform sender they don't have permission to use this command
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
            return true;
        }

        // ╔═══🧪 Dev Mode Reset — Rebuild Reloadables═══════════════════╗
        // If devModeAlwaysShow is enabled, rebuild Reloadables to ensure they are re-registered fresh
        if (ConfigManager.devModeAlwaysShow()) {
            PickYourDifficulty.debug("Dev mode is enabled — clearing and re-registering reloadables.");

            // 🧼 Wipe all previously registered reloadables
            ReloadManager.clearAll();

            // 📄 Register config and message manager instances again
            ReloadManager.register(new ConfigManager());
            ReloadManager.register(new MessagesManager());
        } else {
            PickYourDifficulty.debug("Dev mode is OFF — skipping reloadable rebuild.");
        }

        // ╔═══🔁 Reload All Components═══════════════════════════════════╗
        // Triggers reload() method on all registered Reloadable components
        PickYourDifficulty.debug("Reloading all registered Reloadable components...");
        ReloadManager.reloadAll();

        // ╔═══🧽 Grace Reminder Reset════════════════════════════════════╗
        // Clears active grace reminders so they restart using new timings (if config changed)
        PickYourDifficulty.debug("Clearing GraceReminderTracker...");
        GraceReminderTracker.clearAll();

        // 💬 Let the sender know the reload succeeded
        PickYourDifficulty.debug("Reload completed successfully — notifying sender.");
        sender.sendMessage(MessagesManager.get("command.reload-success"));

        return true;
    }
}
