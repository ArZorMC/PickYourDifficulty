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

// ─────────────────────────────────────────────────────────────
// 📝 TextUtil — MiniMessage & PlaceholderAPI handling
// ─────────────────────────────────────────────────────────────
public class TextUtil {

    // ╔═══🎨 MiniMessage Engine═════════════════════════════════════════╗

    // 💬 Global MiniMessage instance (thread-safe singleton)
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // ╔═══🔍 PlaceholderAPI Detection═══════════════════════════════════╗

    // 💬 Checks if PlaceholderAPI is available on the server
    public static boolean isPlaceholderAPIEnabled() {
        boolean enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        // 🧪 Debug: log detection result
        PickYourDifficulty.debug("🔍 PlaceholderAPI enabled = " + enabled);

        return enabled;
    }

    // ╔═══🔁 Placeholder Resolution═════════════════════════════════════╗

    // 💬 Replaces both PlaceholderAPI placeholders and custom tags
    public static String replacePlaceholders(String text, Player player) {
        String result = text;

        // 📦 Phase 1: Try PlaceholderAPI first (if installed)
        if (isPlaceholderAPIEnabled()) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                // ❌ Catch any unexpected placeholder errors
                PickYourDifficulty.getInstance().getLogger().warning(
                        "❌ Failed to apply PlaceholderAPI: " + e.getMessage()
                );
            }
        }

        // 📦 Phase 2: Fallback <player> and <world> tags (even without PAPI)
        result = result
                .replace("<player>", player.getName())                    // 👤 Replace <player>
                .replace("<world>", player.getWorld().getName());         // 🌍 Replace <world>

        // 🧪 Debug: log final resolved string after all substitutions
        PickYourDifficulty.debug("🔁 Final placeholder output for " + player.getName() + " = " + result);

        return result;
    }

    // ╔═══🧾 mm() — MiniMessage to Component═════════════════════════════╗

    // 💬 Converts a MiniMessage string into a Component
    public static Component mm(String input) {
        return mm.deserialize(input);
    }

    // ╔═══📋 deserializeMiniMessageList() — List Conversion══════════════╗

    // 💬 Converts a list of MiniMessage strings into a list of Components
    public static List<Component> deserializeMiniMessageList(List<String> lines) {
        List<Component> components = new ArrayList<>();

        // 🎨 Convert each line separately to preserve per-line formatting
        for (String line : lines) {
            components.add(mm.deserialize(line));
        }

        return components;
    }

    // ╔═══🎨 parseLegacyString() — Legacy Format Conversion══════════════╗

    // 💬 Converts a MiniMessage-formatted string into a legacy (§) formatted line
    public static List<String> parseLegacyString(String message) {

        // 🧮 MiniMessage → Component → §-formatted legacy string
        String legacy = LegacyComponentSerializer.legacySection().serialize(mm.deserialize(message));

        // 🪄 Return it as a single-line list (for holograms, bossbars, etc.)
        return List.of(legacy);
    }
}