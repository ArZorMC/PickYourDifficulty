// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🔐 PermissionUtil.java                         ║
// ║  Utility methods for clean permission checks across the plugin     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PermissionUtil {

    // ─────────────────────────────────────────────────────────────
    // ✅ Core Permission Check Logic
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks if a player has a specific permission or is an operator.
     *
     * @param player The player to check
     * @param node   The permission node
     * @return true if the player has the permission or is OP
     */
    public static boolean has(Player player, String node) {
        return player.hasPermission(node) || player.isOp();
    }

    /**
     * Checks if a command sender (player or console) has a specific permission.
     *
     * @param sender The command sender to check
     * @param node   The permission node
     * @return true if the sender has the permission
     */
    public static boolean has(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    /**
     * Checks if a player has *any* of the given permission nodes.
     * Also returns true if the player is OP.
     *
     * @param player The player to check
     * @param nodes  A collection of permission nodes
     * @return true if any permission is matched
     */
    public static boolean hasAny(Player player, Collection<String> nodes) {
        if (player.isOp()) return true;

        for (String node : nodes) {
            if (player.hasPermission(node)) return true;
        }

        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // 🛡️ Admin-Level Access Check
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks if a player has admin-level access.
     * Includes explicit permission and OP fallback.
     *
     * @param player The player to check
     * @return true if admin
     */
    @SuppressWarnings("unused")
    public static boolean isAdmin(Player player) {
        return player.hasPermission("pickyourdifficulty.admin") || player.isOp();
    }

    /**
     * Checks if any command sender has admin access.
     * This is used where sender may not be a player.
     *
     * @param sender The command sender
     * @return true if admin
     */
    public static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("pickyourdifficulty.admin") || sender.isOp();
    }

    // ─────────────────────────────────────────────────────────────
    // 🎮 Specific Feature Permissions
    // ─────────────────────────────────────────────────────────────

    /** Checks if a player is allowed to open the difficulty GUI. */
    public static boolean hasGuiAccess(Player player) {
        return has(player, "pickyourdifficulty.gui");
    }

    /** Checks if a player can view their difficulty info. */
    public static boolean hasInfoAccess(Player player) {
        return has(player, "pickyourdifficulty.info");
    }

    /** Checks if a sender can reload the plugin. */
    public static boolean hasReloadPermission(CommandSender sender) {
        return sender.hasPermission("pickyourdifficulty.reload") || sender.isOp();
    }

    /** Checks if a sender can reset another player’s difficulty. */
    public static boolean hasResetPermission(CommandSender sender) {
        return sender.hasPermission("pickyourdifficulty.reset") || sender.isOp();
    }

    /** Checks if a sender can force-set a player’s difficulty. */
    public static boolean hasSetPermission(CommandSender sender) {
        return sender.hasPermission("pickyourdifficulty.set") || sender.isOp();
    }

    /** Checks if a player is allowed to toggle holograms. */
    public static boolean hasHologramTogglePermission(Player player) {
        return has(player, "pickyourdifficulty.toggleholograms");
    }
}
