// ╔════════════════════════════════════════════════════════════════════╗
// ║                         ℹ️ CommandInfo.java                        ║
// ║   Handles /pyd info — displays the player's current difficulty     ║
// ║   Requires permission: pickyourdifficulty.info                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.DifficultyManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class CommandInfo implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage instance for formatting rich chat components */
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Handles the execution of /pyd info.
     * Displays the player's current difficulty and its full description.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ⛔ Command must be run by an actual player (not from console)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.players-only")));
            return true;
        }

        // 🔐 Check permission if permission system is enabled
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasInfoAccess(player)) {
            player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-info")));
            return true;
        }

        // 🗂️ Retrieve the player's saved difficulty key (e.g. "Normal")
        String difficulty = PickYourDifficulty.getInstance()
                .getPlayerDataManager()
                .getDifficultyStorage()
                .getDifficulty(player.getUniqueId());

        // 💬 Send formatted difficulty key with prefix (MiniMessage)
        player.sendMessage(mm.deserialize(
                MessagesManager.get("pyd.info-current").replace("<difficulty>", difficulty)
        ));

        // 📖 Send full descriptive summary (e.g. "Grace: 30s, Despawn: 60s...")
        player.sendMessage(Component.text(
                DifficultyManager.getDifficultySummary(difficulty)
        ));

        return true;
    }
}