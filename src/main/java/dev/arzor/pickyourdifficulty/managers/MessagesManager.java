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
            PickYourDifficulty.debug("📨 messages.yml not found — generating default copy.");
        }

        // 🧪 Load all messages into memory
        messages = YamlConfiguration.loadConfiguration(file);
        PickYourDifficulty.debug("📨 messages.yml loaded with " + messages.getKeys(true).size() + " keys");
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
            plugin.getLogger().warning("⚠ Missing message key: '" + path + "'");
            plugin.getLogger().warning("⚠ Available keys: " + messages.getKeys(true));
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

        PickYourDifficulty.debug("📨 Formatting message: " + path);
        return mm.deserialize(raw);
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format with Custom Placeholders
    // ─────────────────────────────────────────────────────────────

    public static Component format(String path, Map<String, String> placeholders) {
        String raw = get(path);

        // 🧩 Replace prefix first
        if (raw.contains("<prefix>")) {
            raw = raw.replace("<prefix>", get("prefix"));
        }

        // 🔁 Replace any other custom placeholders (e.g., <graceTime>)
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        PickYourDifficulty.debug("📨 Formatting message with placeholders: " + path + " → " + placeholders);
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

        // ⏳ Remaining = total - elapsed (minimum 0)
        int graceRemaining = Math.max(0, totalGrace - secondsPlayed);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("difficulty", difficulty);
        placeholders.put("graceTime", String.valueOf(graceRemaining));
        placeholders.put("graceTimeTotal", String.valueOf(totalGrace));

        PickYourDifficulty.debug("📨 Formatting player grace message: " + path +
                " (played: " + secondsPlayed + "s, remaining: " + graceRemaining + "s, difficulty: " + difficulty + ")");

        return format(path, placeholders);
    }

    // ─────────────────────────────────────────────────────────────
    // 💬 Format with Cooldown + Player Context
    // ─────────────────────────────────────────────────────────────

    // 💬 Dynamically formats a message with:
    //    • <player>       → Player’s name
    //    • <prefix>       → Global prefix from messages.yml
    //    • <cooldowntime> → Cooldown time in seconds (e.g., "30s")
    public static Component format(String path, Player player, int cooldownSeconds) {
        String raw = get(path);

        // 🧩 Replace known tokens first
        if (raw.contains("<prefix>")) {
            raw = raw.replace("<prefix>", get("prefix"));
        }

        if (raw.contains("<cooldowntime>")) {
            raw = raw.replace("<cooldowntime>", cooldownSeconds + "s");
        }

        // 🔁 Replace PlaceholderAPI and <player> tags
        raw = TextUtil.replacePlaceholders(raw, player);

        PickYourDifficulty.debug("📨 Formatting cooldown message for " + player.getName() + ": " +
                cooldownSeconds + "s → " + path);

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
        PickYourDifficulty.debug("♻️ MessagesManager.reload() completed");
    }
}
