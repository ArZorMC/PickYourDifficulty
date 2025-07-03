// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🔐 PermissionUtil.java                         ║
// ║  Utility methods for clean permission checks across the plugin     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

// ─────────────────────────────────────────────────────────────
// 🔐 PermissionUtil — Central permission handling logic
// ─────────────────────────────────────────────────────────────
public class PermissionUtil {

    // ╔═══✅ Basic Permission Check (player only)════════════════════════╗

    // 💬 Checks if a player has a specific permission or is OP
    public static boolean has(Player player, String node) {
        boolean result = player.hasPermission(node) || player.isOp();

        // 🧪 Debug: show permission result
        PickYourDifficulty.debug("🔐 Permission check for " + player.getName() +
                " → " + node + " = " + result + " (OP: " + player.isOp() + ")");
        return result;
    }

    // ╔═══✅ Basic Permission Check (generic sender)═════════════════════╗

    // 💬 Checks if a command sender (player or console) has a specific permission
    public static boolean has(CommandSender sender, String node) {
        boolean result = sender.hasPermission(node);

        // 🧪 Debug: show sender permission check
        PickYourDifficulty.debug("🔐 Permission check for sender " + sender.getName() +
                " → " + node + " = " + result);
        return result;
    }

    // ╔═══🔀 Multi-Permission Check═════════════════════════════════════╗

    // 💬 Checks if a player has *any* of a collection of permission nodes
    public static boolean hasAny(Player player, Collection<String> nodes) {

        // 🛡️ OPs bypass all permission checks
        if (player.isOp()) {
            // 🧪 Debug: show if OP bypass granted
            PickYourDifficulty.debug("🔐 Multi-permission check → " + player.getName() +
                    " is OP → bypass granted");
            return true;
        }

        for (String node : nodes) {
            if (player.hasPermission(node)) {
                // 🧪 Debug: show which permission matched
                PickYourDifficulty.debug("🔐 Multi-permission match → " + player.getName() +
                        " has " + node);
                return true;
            }
        }

        // 🧪 Debug: none matched
        PickYourDifficulty.debug("🔐 Multi-permission check → " + player.getName() +
                " has no matching permissions");
        return false;
    }

    // ╔═══🛡️ Admin-Level Access═════════════════════════════════════════╗

    // 💬 Checks if a player has admin access (via permission or OP)
    @SuppressWarnings("unused")
    public static boolean isAdmin(Player player) {
        boolean result = player.hasPermission("pickyourdifficulty.admin") || player.isOp();

        // 🧪 Debug: log admin check
        PickYourDifficulty.debug("🛡️ Admin check for " + player.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a command sender (player or console) has admin access
    public static boolean isAdmin(CommandSender sender) {
        boolean result = sender.hasPermission("pickyourdifficulty.admin") || sender.isOp();

        // 🧪 Debug: log sender admin check
        PickYourDifficulty.debug("🛡️ Admin check for sender " + sender.getName() + " → " + result);
        return result;
    }

    // ╔═══🎮 Feature-Specific Permissions═══════════════════════════════╗

    // 💬 Checks if a player is allowed to open the difficulty GUI
    public static boolean hasGuiAccess(Player player) {
        boolean result = has(player, "pickyourdifficulty.gui");

        // 🧪 Debug: log GUI access check
        PickYourDifficulty.debug("🧭 GUI access check for " + player.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a player is allowed to view info/debug output
    public static boolean hasInfoAccess(Player player) {
        boolean result = has(player, "pickyourdifficulty.info");

        // 🧪 Debug: log info permission check
        PickYourDifficulty.debug("ℹ️ Info access check for " + player.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a sender is allowed to reload the plugin
    public static boolean hasReloadPermission(CommandSender sender) {
        boolean result = sender.hasPermission("pickyourdifficulty.reload") || sender.isOp();

        // 🧪 Debug: log reload permission check
        PickYourDifficulty.debug("♻️ Reload permission check for " + sender.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a sender is allowed to reset another player’s difficulty
    public static boolean hasResetPermission(CommandSender sender) {
        boolean result = sender.hasPermission("pickyourdifficulty.reset") || sender.isOp();

        // 🧪 Debug: log reset permission check
        PickYourDifficulty.debug("🔁 Reset permission check for " + sender.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a sender is allowed to force-set another player’s difficulty
    public static boolean hasSetPermission(CommandSender sender) {
        boolean result = sender.hasPermission("pickyourdifficulty.set") || sender.isOp();

        // 🧪 Debug: log set permission check
        PickYourDifficulty.debug("✳️ Set permission check for " + sender.getName() + " → " + result);
        return result;
    }

    // 💬 Checks if a player is allowed to toggle holograms
    public static boolean hasHologramTogglePermission(Player player) {
        boolean result = has(player, "pickyourdifficulty.toggleholograms");

        // 🧪 Debug: log hologram toggle check
        PickYourDifficulty.debug("👓 Hologram toggle permission check for " + player.getName() + " → " + result);
        return result;
    }
}