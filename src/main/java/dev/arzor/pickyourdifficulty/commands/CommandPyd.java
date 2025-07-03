// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🎮 CommandPyd.java                          ║
// ║   Main handler for /pyd and all subcommands                        ║
// ║   Routes to: gui, help, info, reload, reset, set, toggleholograms  ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

// ─────────────────────────────────────────────────────────────
// 🎮 CommandPyd — Root command router for /pyd
// ─────────────────────────────────────────────────────────────
public class CommandPyd implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 📦 Debug: Show the base command and first argument
        PickYourDifficulty.debug("/pyd invoked by: " + sender.getName() + " | args=" + String.join(" ", args));

        // ╔═══🧭 /pyd or /pyd gui — open difficulty GUI══════════════════════════════════════════════════════╗
        // If no argument is passed or the argument is "gui", open the GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            PickYourDifficulty.debug("Routing to: CommandGui");
            return new CommandGui().onCommand(sender, command, label, args);
        }

        // ╔═══🆘 /pyd help — show interactive help═══════════════════════════════════════════════════════════╗
        // Displays clickable help messages filtered by permissions
        if (args[0].equalsIgnoreCase("help")) {
            PickYourDifficulty.debug("Routing to: CommandHelp");
            return new CommandHelp().onCommand(sender, command, label, args);
        }

        // ╔═══ℹ️ /pyd info — show player difficulty═════════════════════════════════════════════════════════╗
        // Shows the player’s current difficulty and its description
        if (args[0].equalsIgnoreCase("info")) {
            PickYourDifficulty.debug("Routing to: CommandInfo");
            return new CommandInfo().onCommand(sender, command, label, args);
        }

        // ╔═══🔄 /pyd reload — reload plugin settings═══════════════════════════════════════════════════════╗
        // Reloads the plugin’s configuration and resets internal caches
        if (args[0].equalsIgnoreCase("reload")) {
            PickYourDifficulty.debug("Routing to: CommandReload");
            return new CommandReload().onCommand(sender, command, label, args);
        }

        // ╔═══🔁 /pyd reset <player> — reset player difficulty══════════════════════════════════════════════╗
        // Admin resets another player's difficulty selection
        if (args[0].equalsIgnoreCase("reset")) {
            PickYourDifficulty.debug("Routing to: CommandReset");
            return new CommandReset().onCommand(sender, command, label, args);
        }

        // ╔═══🧩 /pyd set <player> <difficulty> — set manually═══════════════════════════════════════════════╗
        // Admin assigns a difficulty to a player manually
        if (args[0].equalsIgnoreCase("set")) {
            PickYourDifficulty.debug("Routing to: CommandSet");
            return new CommandSet().onCommand(sender, command, label, args);
        }

        // ╔═══👁️ /pyd toggleholograms — toggle hologram visibility══════════════════════════════════════════╗
        // Allows a player to toggle visibility of despawn timer holograms
        if (args[0].equalsIgnoreCase("toggleholograms")) {
            PickYourDifficulty.debug("Routing to: CommandToggleHolograms");
            return new CommandToggleHolograms().onCommand(sender, command, label, args);
        }

        // ╔═══❓ Unknown subcommand — show error═════════════════════════════════════════════════════════════╗
        // Unrecognized subcommand — show a friendly error message
        PickYourDifficulty.debug("Unknown subcommand: " + args[0]);
        sender.sendMessage(MessagesManager.format("pyd.unknown-subcommand"));
        return true;
    }
}