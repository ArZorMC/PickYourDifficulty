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

// ─────────────────────────────────────────────────────────────
// ℹ️ CommandInfo — Displays player's current difficulty
// ─────────────────────────────────────────────────────────────
public class CommandInfo implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage instance for formatting rich chat components
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Info command triggered
        PickYourDifficulty.debug("/pyd info invoked by: " + sender.getName());

        // ╔═══🚫 Must Be Player═══════════════════════════════════════╗
        // This command cannot be run from console or command blocks
        if (!(sender instanceof Player player)) {
            PickYourDifficulty.debug("Blocked /pyd info — sender is not a player.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.players-only")));
            return true;
        }

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND player has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasInfoAccess(player)) {
            PickYourDifficulty.debug("Blocked /pyd info — " + player.getName() + " lacks permission.");

            // 🚫 Inform sender they don't have permission to use this command
            player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-info")));
            return true;
        }

        // ╔═══📥 Retrieve Player Difficulty═══════════════════════════╗
        // Pulls the raw difficulty key (e.g. "Normal", "Hardcore", etc.)
        String difficulty = PickYourDifficulty.getInstance()
                .getPlayerDataManager()
                .getDifficultyStorage()
                .getDifficulty(player.getUniqueId());

        PickYourDifficulty.debug(player.getName() + " has difficulty: " + difficulty);

        // ╔═══💬 Send Difficulty Label════════════════════════════════╗
        // Display the difficulty using the configured MiniMessage template
        player.sendMessage(mm.deserialize(
                MessagesManager.get("pyd.info-current").replace("<difficulty>", difficulty)
        ));

        // ╔═══📖 Send Difficulty Summary══════════════════════════════╗
        // Shows summary info like grace time, despawn time, etc.
        String summary = DifficultyManager.getDifficultySummary(difficulty);
        PickYourDifficulty.debug("Sending difficulty summary to " + player.getName() + ": " + summary);
        player.sendMessage(Component.text(summary));

        return true;
    }
}