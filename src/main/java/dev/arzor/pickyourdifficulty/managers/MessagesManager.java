// ╔════════════════════════════════════════════════════════════════════╗
// ║                    📨 MessagesManager.java                         ║
// ║     Loads and formats messages from messages.yml using MiniMessage ║
// ║     Supports custom placeholders and player-specific values        ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.interfaces.Reloadable;
import dev.arzor.pickyourdifficulty.utils.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager implements Reloadable {

    // ─────────────────────────────────────────────────────────────
    // 📂 Internal File References
    // ─────────────────────────────────────────────────────────────

    private static FileConfiguration messages;
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static PickYourDifficulty plugin;

    // ─────────────────────────────────────────────────────────────
    // 🔁 Register with ReloadManager
    // ─────────────────────────────────────────────────────────────

    static {
        ReloadManager.register(new MessagesManager());
    }

    // ─────────────────────────────────────────────────────────────
    // 🧰 Init Method — Load or Create messages.yml
    // ─────────────────────────────────────────────────────────────

    public static void init(PickYourDifficulty plugin) {
        MessagesManager.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "messages.yml");

        // 📦 Create messages.yml if it doesn’t exist
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);
    }

    // ─────────────────────────────────────────────────────────────
// 📨 Get Raw String by Path — with logging if missing
// ─────────────────────────────────────────────────────────────

    public static @Nonnull String get(String path) {
        // 🛡️ Ensure prefix is applied consistently
        if (!path.startsWith("messages.")) {
            path = "messages." + path;
        }

        String value = messages.getString(path);

        // ❗ Warn if message path is missing in messages.yml
        if (value == null) {
            PickYourDifficulty.getInstance().getLogger().warning("⚠ Missing message key: '" + path + "'");
            PickYourDifficulty.getInstance().getLogger().warning("⚠ Available keys: " + messages.getKeys(true));
            return "<red>Missing message: " + path + "</red>";
        }

        return value;
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format Basic Message (No Placeholders)
    // ─────────────────────────────────────────────────────────────

    public static Component format(String path) {
        String raw = get(path);

        // 💬 Replace <prefix> if used in message
        if (raw.contains("<prefix>")) {
            raw = raw.replace("<prefix>", get("prefix"));
        }

        return mm.deserialize(raw);
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format with Custom Placeholders
    // ─────────────────────────────────────────────────────────────

    public static Component format(String path, Map<String, String> placeholders) {
        String raw = get(path);

        if (raw.contains("<prefix>")) {
            raw = raw.replace("<prefix>", get("prefix"));
        }

        // 🔁 Replace custom placeholders like <graceTime>
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        return mm.deserialize(raw);
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format for Player with Grace Time Context
    // ─────────────────────────────────────────────────────────────

    public static Component get(String path, Player player) {
        // ⛏️ Get player difficulty from storage
        String difficulty = PickYourDifficulty.getInstance()
                .getPlayerDataManager()
                .getDifficultyStorage()
                .getDifficulty(player);

        // 🔢 Get configured total grace time
        int totalGrace = ConfigManager.getGraceTime(difficulty);

        // 🧮 Convert PLAY_ONE_MINUTE from ticks to seconds
        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int secondsPlayed = (int) (ticks / 20); // 🧮 20 ticks = 1 second

        // ⏳ Remaining = total - elapsed (min 0)
        int graceRemaining = Math.max(0, totalGrace - secondsPlayed);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("difficulty", difficulty);
        placeholders.put("graceTime", String.valueOf(graceRemaining));
        placeholders.put("graceTimeTotal", String.valueOf(totalGrace));

        return format(path, placeholders);
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format with Cooldown + Player Context
    // ─────────────────────────────────────────────────────────────

    /**
     * Formats a message with player context and cooldown time placeholder.
     * Replaces:
     * - <player>       → player name
     * - <prefix>       → global prefix
     * - <cooldowntime> → cooldown time in seconds (e.g., "30s")
     */
    public static Component format(String path, Player player, int cooldownSeconds) {
        String raw = get(path);

        if (raw.contains("<prefix>")) {
            raw = raw.replace("<prefix>", get("prefix"));
        }

        if (raw.contains("<cooldowntime>")) {
            raw = raw.replace("<cooldowntime>", cooldownSeconds + "s");
        }

        // 🔁 Replace <player> and any PlaceholderAPI values
        raw = TextUtil.replacePlaceholders(raw, player);

        return mm.deserialize(raw);
    }

    // ─────────────────────────────────────────────────────────────
    // 🔄 Reload messages.yml on demand
    // ─────────────────────────────────────────────────────────────

    @Override
    public void reload() {
        File file = new File(PickYourDifficulty.getInstance().getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("✅ Loaded message keys: " + messages.getKeys(true));

    }
}
