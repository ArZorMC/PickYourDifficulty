// ╔════════════════════════════════════════════════════════════════════╗
// ║               🧠 PlayerDifficultyStorage.java                      ║
// ║  Tracks which difficulty each player has selected (in-memory).    ║
// ║  Source of truth for difficulty state; persists to YAML on disk.  ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.storage;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.utils.StorageUtil;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDifficultyStorage {

    // ─────────────────────────────────────────────────────────────
    // 🧩 Singleton Instance
    // ─────────────────────────────────────────────────────────────

    /** Static instance of this class (used globally via getInstance) */
    private static final PlayerDifficultyStorage instance = new PlayerDifficultyStorage();

    /** Returns the singleton instance */
    public static PlayerDifficultyStorage getInstance() {
        return instance;
    }

    /** Private constructor to prevent instantiation elsewhere */
    private PlayerDifficultyStorage() {}

    // ─────────────────────────────────────────────────────────────
    // 🗺️ Difficulty Map
    // ─────────────────────────────────────────────────────────────

    /** Internal memory map storing UUID → difficulty key */
    private final Map<UUID, String> difficultyMap = new HashMap<>();

    // ─────────────────────────────────────────────────────────────
    // 🔍 Get Difficulty
    // ─────────────────────────────────────────────────────────────

    /**
     * Gets the selected difficulty for a player, falling back if unset.
     *
     * @param player The online Player
     * @return Difficulty string key
     */
    public String getDifficulty(Player player) {
        return difficultyMap.getOrDefault(player.getUniqueId(), ConfigManager.getFallbackDifficulty());
    }

    /**
     * Gets the selected difficulty for an OfflinePlayer.
     *
     * @param offlinePlayer The offline player
     * @return Difficulty string key
     */
    public String getDifficulty(OfflinePlayer offlinePlayer) {
        return difficultyMap.getOrDefault(offlinePlayer.getUniqueId(), ConfigManager.getFallbackDifficulty());
    }

    /**
     * Gets the difficulty by raw UUID.
     *
     * @param uuid The player UUID
     * @return Difficulty string key
     */
    public String getDifficulty(UUID uuid) {
        return difficultyMap.getOrDefault(uuid, ConfigManager.getFallbackDifficulty());
    }

    // ─────────────────────────────────────────────────────────────
    // ✅ Check if Player Has Selected
    // ─────────────────────────────────────────────────────────────

    /** Checks if a player has manually selected a difficulty */
    public boolean hasSelectedDifficulty(Player player) {
        return difficultyMap.containsKey(player.getUniqueId());
    }

    /** Checks if a UUID is registered with a difficulty */
    public boolean hasSelected(UUID uuid) {
        return difficultyMap.containsKey(uuid);
    }

    // ─────────────────────────────────────────────────────────────
    // 📝 Set Difficulty
    // ─────────────────────────────────────────────────────────────

    /**
     * Sets a player's difficulty and logs the change.
     *
     * @param player        The online player
     * @param difficultyKey The difficulty string key
     */
    public void setDifficulty(Player player, String difficultyKey) {
        difficultyMap.put(player.getUniqueId(), difficultyKey);

        // 📢 Log the change for auditing
        PickYourDifficulty.getInstance().getLogger().info("[PickYourDifficulty] Saved difficulty for "
                + player.getName() + " → " + difficultyKey);
    }

    /**
     * Sets a difficulty for a player by UUID (offline/admin use).
     *
     * @param uuid          The player UUID
     * @param difficultyKey The difficulty string key
     */
    public void setDifficulty(UUID uuid, String difficultyKey) {
        difficultyMap.put(uuid, difficultyKey);
    }

    // ─────────────────────────────────────────────────────────────
    // 🧽 Clear Difficulty
    // ─────────────────────────────────────────────────────────────

    /** Clears a player’s difficulty setting (e.g., /pyd reset) */
    public void clearDifficulty(Player player) {
        difficultyMap.remove(player.getUniqueId());
    }

    /** Clears a difficulty by UUID (offline use) */
    public void clearDifficulty(UUID uuid) {
        difficultyMap.remove(uuid);
    }

    // ─────────────────────────────────────────────────────────────
    // 🧪 Debug & Export Tools
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a defensive copy of the full internal map.
     * Used for debug output, statistics, or export.
     */
    public Map<UUID, String> getAllDifficultyData() {
        return new HashMap<>(difficultyMap); // 🛡️ Prevent external mutation
    }

    // 🔁 Difficulty effects (grace, despawn, etc.) are applied by:
    //    PlayerDataManager#applyDifficulty — this class stores state only.

    // ─────────────────────────────────────────────────────────────
    // 💾 Persistence — Load / Save
    // ─────────────────────────────────────────────────────────────

    /**
     * Loads difficulty data from disk (playerdata.yml)
     * Invalid UUID strings are ignored silently.
     */
    public void loadFromDisk() {
        FileConfiguration config = StorageUtil.loadYaml("playerdata.yml");
        difficultyMap.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String difficulty = config.getString(key);

                if (difficulty != null) {
                    difficultyMap.put(uuid, difficulty);
                }
            } catch (IllegalArgumentException ignored) {
                // 🧯 Skip malformed UUID keys
            }
        }
    }

    /**
     * Saves difficulty data to disk (playerdata.yml)
     */
    public void saveToDisk() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, String> entry : difficultyMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        StorageUtil.saveYaml(config, "playerdata.yml");
    }
}