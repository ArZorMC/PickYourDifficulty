// ╔════════════════════════════════════════════════════════════════════╗
// ║                          📝 TextUtil.java                          ║
// ║   Utility class for MiniMessage and basic placeholder resolution   ║
// ║   Includes PlaceholderAPI support + fallback replacements          ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {

    // ─────────────────────────────────────────────────────────────
    // 🎨 MiniMessage Setup
    // ─────────────────────────────────────────────────────────────

    /** Global MiniMessage instance (safe to reuse) */
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // ─────────────────────────────────────────────────────────────
    // 🔍 PlaceholderAPI Detection
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks if PlaceholderAPI is installed and enabled.
     *
     * @return true if PlaceholderAPI is available
     */
    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    // ─────────────────────────────────────────────────────────────
    // 🔁 Placeholder Replacement Logic
    // ─────────────────────────────────────────────────────────────

    /**
     * Replaces placeholders in a raw string.
     * Applies both PlaceholderAPI (if available) and custom tags like <player>.
     *
     * @param text   Raw input with placeholders
     * @param player The player context
     * @return Final string with all placeholders resolved
     */
    public static String replacePlaceholders(String text, Player player) {
        String result = text;

        // 📦 Phase 1: Use PlaceholderAPI if present
        if (isPlaceholderAPIEnabled()) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                PickYourDifficulty.getInstance().getLogger().warning(
                        "[PickYourDifficulty] Failed to apply PlaceholderAPI: " + e.getMessage()
                );
            }
        }

        // 📦 Phase 2: Built-in tag replacements
        result = result
                .replace("<player>", player.getName())                    // Replace <player> tag
                .replace("<world>", player.getWorld().getName());        // Replace <world> tag

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // 🧾 MiniMessage to Component Conversions
    // ─────────────────────────────────────────────────────────────

    /**
     * Parses a MiniMessage string into a Component.
     *
     * @param input MiniMessage-formatted string
     * @return Adventure Component result
     */
    public static Component mm(String input) {
        return mm.deserialize(input);
    }

    /**
     * Converts a list of MiniMessage strings into a list of Components.
     *
     * @param lines List of MiniMessage lines
     * @return List of Adventure Components
     */
    public static List<Component> deserializeMiniMessageList(List<String> lines) {
        List<Component> components = new ArrayList<>();

        // 💬 Deserialize each MiniMessage line
        for (String line : lines) {
            components.add(mm.deserialize(line));
        }

        return components;
    }

    // ─────────────────────────────────────────────────────────────
    // 🎨 Legacy Fallback Format
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts a MiniMessage string to a legacy-formatted string.
     * Useful for hologram APIs like DecentHolograms that don’t use Components.
     *
     * @param message MiniMessage-formatted input
     * @return List with one legacy-colored line
     */
    public static List<String> parseLegacyString(String message) {
        // 🧮 Convert MiniMessage → Component → legacy string using section (§) color codes
        String legacy = LegacyComponentSerializer.legacySection().serialize(mm.deserialize(message));
        return List.of(legacy);
    }
}