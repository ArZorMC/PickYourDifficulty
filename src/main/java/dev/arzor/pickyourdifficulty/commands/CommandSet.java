// ╔════════════════════════════════════════════════════════════════════╗
// ║                       🧩 CommandSet.java                           ║
// ║  Handles /pyd set <player> <difficulty> — assigns difficulty       ║
// ║  Requires permission: pickyourdifficulty.set                       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

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

public class CommandSet implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 🧰 Utilities
    // ─────────────────────────────────────────────────────────────

    /** MiniMessage instance for rich text output */
    private final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    /**
     * Executes /pyd set <player> <difficulty>
     * Allows admins to assign a difficulty to a player manually.
     */
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // ╔═══⛔ Permission Check═════════════════════════════════════════════════════════════════════════════╗

        // 🛑 Require proper permission if permission checks are enabled
        if (ConfigManager.requireCommandPermissions() && !PermissionUtil.hasSetPermission(sender)) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission-set")));
            return true;
        }

        // ╔═══📏 Argument Validation═════════════════════════════════════════════════════════════════════════╗

        // 💬 Expect: /pyd set <player> <difficulty>
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.usage")));
            return true;
        }

        // ╔═══🎯 Player & Difficulty Resolution═════════════════════════════════════════════════════════════╗

        String playerName = args[1];
        String difficultyArg = args[2].toLowerCase();

        // 🔍 Try to find the online player
        Player target = Bukkit.getPlayerExact(playerName);

        // ❌ Player not found
        if (target == null) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.player-not-found")
                    .replace("<player>", playerName)));
            return true;
        }

        // 🧪 Difficulty must be valid
        if (!DifficultyManager.isValidDifficulty(difficultyArg)) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.invalid-difficulty")
                    .replace("<difficulty>", difficultyArg)));
            return true;
        }

        // 🎨 Normalize difficulty key (get canonical version from aliases)
        String canonical = DifficultyManager.getCanonicalKey(difficultyArg);

        // ╔═══🔒 Already Selected & Switching Disabled══════════════════════════════════════════════════════╗

        // 🚫 If difficulty change is disabled and player already selected one...
        if (!ConfigManager.allowDifficultyChange()
                && PlayerDifficultyStorage.getInstance().hasSelected(target.getUniqueId())) {
            sender.sendMessage(mm.deserialize(MessagesManager.get("set.already-selected")
                    .replace("<player>", target.getName())));
            return true;
        }

        // ╔═══✅ Apply Difficulty & Cooldown════════════════════════════════════════════════════════════════╗

        // 💾 Set difficulty
        PlayerDifficultyStorage.getInstance().setDifficulty(target.getUniqueId(), canonical);

        // ⏳ Reset their cooldown timer
        CooldownTracker.setCooldownNow(target.getUniqueId());

        // 📢 Confirm to the sender
        sender.sendMessage(mm.deserialize(MessagesManager.get("set.success")
                .replace("<player>", target.getName())
                .replace("<difficulty>", canonical)));

        return true;
    }
}