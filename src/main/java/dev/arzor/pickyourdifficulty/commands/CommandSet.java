// ╔════════════════════════════════════════════════════════════════════╗
// ║                       🧩 CommandSet.java                           ║
// ║  Handles /pyd set <player> <difficulty> — assigns difficulty       ║
// ║  Requires permission: pickyourdifficulty.set                       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.DifficultyManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

// ─────────────────────────────────────────────────────────────
// 🧩 CommandSet — Force assign a difficulty to a player
// ─────────────────────────────────────────────────────────────
public class CommandSet implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    // 💬 MiniMessage instance for deserializing colorized text
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Toggle command triggered
        PickYourDifficulty.debug("/pyd set invoked by: " + sender.getName());

        // ╔═══🔐 Permission Check═════════════════════════════════════╗
        // Only allow access if permission enforcement is enabled AND sender has permission
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasSetPermission(sender)) {
            PickYourDifficulty.debug(sender.getName() + " tried to run /pyd set without permission.");

            // 🚫 Inform sender they don't have permission to use this command
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-set")));
            return true;
        }

        // ╔═══📏 Argument Validation═══════════════════════════════════╗

        // ✅ Ensure we received 3 args: /pyd set <player> <difficulty>
        if (args.length < 3) {
            PickYourDifficulty.debug("Invalid usage of /pyd set — missing arguments.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.usage")));
            return true;
        }

        // ╔═══🎯 Player & Difficulty Resolution════════════════════════╗

        String playerName = args[1];                  // 🧍 Player to set
        String difficultyArg = args[2].toLowerCase(); // 🎮 Difficulty key (lowercase for comparison)

        PickYourDifficulty.debug(sender.getName() + " is attempting to set difficulty for " + playerName + " to '" + difficultyArg + "'");

        // 🔍 Attempt to find the player — must be online to apply directly
        Player target = Bukkit.getPlayerExact(playerName);

        // ❌ If player doesn't exist or isn't online
        if (target == null) {
            PickYourDifficulty.debug("Target player '" + playerName + "' not found.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.player-not-found")
                    .replace("<player>", playerName)));
            return true;
        }

        // ⚠️ Validate difficulty key using DifficultyManager
        if (!DifficultyManager.isValidDifficulty(difficultyArg)) {
            PickYourDifficulty.debug("Invalid difficulty key provided: " + difficultyArg);
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.invalid-difficulty")
                    .replace("<difficulty>", difficultyArg)));
            return true;
        }

        // 🧼 Normalize the difficulty key into its canonical form (e.g., "easy" → "Easy")
        String canonical = DifficultyManager.getCanonicalKey(difficultyArg);
        PickYourDifficulty.debug("Canonical difficulty resolved as: " + canonical);

        // ╔═══🔒 Already Selected & Switching Disabled══════════════════════════════════════════════════════╗

        // 🛑 If switching is disabled and this player already chose a difficulty
        if (!ConfigManager.allowDifficultyChange()
                && PlayerDifficultyStorage.getInstance().hasSelected(target.getUniqueId())) {
            PickYourDifficulty.debug("Player '" + target.getName() + "' already selected a difficulty. Change not allowed.");
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.already-selected")
                    .replace("<player>", target.getName())));
            return true;
        }

        // ╔═══✅ Apply Difficulty & Cooldown════════════════════════════════════════════════════════════════╗

        // 💾 Save the difficulty to the storage file
        PickYourDifficulty.debug("Setting difficulty for " + target.getName() + " to " + canonical);
        PlayerDifficultyStorage.getInstance().setDifficulty(target.getUniqueId(), canonical);

        // ⏳ Start a new cooldown for this player
        PickYourDifficulty.debug("Resetting cooldown for " + target.getName());
        CooldownTracker.setCooldownNow(target.getUniqueId());

        PickYourDifficulty.debug("Difficulty assignment complete for " + target.getName());

        // 📣 Inform sender that it was successful
        sender.sendMessage(mm.deserialize(MessagesManager.get("set.success")
                .replace("<player>", target.getName())
                .replace("<difficulty>", canonical)));

        return true;
    }
}