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

// ─────────────────────────────────────────────────────────────
// 🧠 PlayerDifficultyStorage — Runtime difficulty state tracker
// ─────────────────────────────────────────────────────────────
public class PlayerDifficultyStorage {

    // ╔═══🔁 Singleton Access═════════════════════════════════════════════╗

    // 🧩 Static singleton instance for global access
    private static final PlayerDifficultyStorage instance = new PlayerDifficultyStorage();

    // 🪪 Accessor for singleton instance
    public static PlayerDifficultyStorage getInstance() {
        return instance;
    }

    // 🔒 Prevent external instantiation
    private PlayerDifficultyStorage() {}

    // ╔═══🗺️ Internal Difficulty Map═════════════════════════════════════╗

    // 🗺️ Maps player UUID to selected difficulty key from config
    private final Map<UUID, String> difficultyMap = new HashMap<>();

    // ╔═══🔍 Get Difficulty — fallback if not set════════════════════════════╗

    public String getDifficulty(Player player) {
        UUID uuid = player.getUniqueId();

        // 🪂 Fallback to default if player has no set difficulty
        String difficulty = difficultyMap.getOrDefault(uuid, ConfigManager.getFallbackDifficulty());

        // 🧪 Debug output if enabled
        PickYourDifficulty.debug("🔍 Retrieved difficulty for " + player.getName() + " → " + difficulty);
        return difficulty;
    }

    public String getDifficulty(OfflinePlayer offlinePlayer) {
        UUID uuid = offlinePlayer.getUniqueId();
        String difficulty = difficultyMap.getOrDefault(uuid, ConfigManager.getFallbackDifficulty());

        PickYourDifficulty.debug("🔍 Retrieved difficulty for offline player " + uuid + " → " + difficulty);
        return difficulty;
    }

    public String getDifficulty(UUID uuid) {
        String difficulty = difficultyMap.getOrDefault(uuid, ConfigManager.getFallbackDifficulty());

        PickYourDifficulty.debug("🔍 Retrieved difficulty for UUID " + uuid + " → " + difficulty);
        return difficulty;
    }

    // ╔═══✅ Check if Player Has Selected═══════════════════════════════════╗
    public boolean hasSelectedDifficulty(Player player) {
        // 📌 True if the player's  UUID exists in memory
        return difficultyMap.containsKey(player.getUniqueId());
    }

    public boolean hasSelected(UUID uuid) {
        return difficultyMap.containsKey(uuid);
    }

    // ╔═══📝 Set Difficulty═════════════════════════════════════════════════╗

    public void setDifficulty(Player player, String difficultyKey) {
        UUID uuid = player.getUniqueId();

        // 💾 Save to in-memory map
        difficultyMap.put(player.getUniqueId(), difficultyKey);

        // 📣 Console log for server owners (always shown)
        PickYourDifficulty.getInstance().getLogger().info(
                "[PickYourDifficulty] Saved difficulty for " + player.getName() + " → " + difficultyKey
        );

        // 🧪 Debug trace
        PickYourDifficulty.debug("💾 Updated difficulty for " + player.getName() + " (" + uuid + ") → " + difficultyKey);
    }

    public void setDifficulty(UUID uuid, String difficultyKey) {
        difficultyMap.put(uuid, difficultyKey);

        PickYourDifficulty.debug("💾 Updated difficulty for UUID " + uuid + " → " + difficultyKey);
    }

    // ╔═══🧽 Clear Difficulty═══════════════════════════════════════════════╗

    public void clearDifficulty(Player player) {
        UUID uuid = player.getUniqueId();
        difficultyMap.remove(uuid);

        PickYourDifficulty.debug("❌ Cleared difficulty for player " + player.getName() + " (" + uuid + ")");
    }

    public void clearDifficulty(UUID uuid) {
        difficultyMap.remove(uuid);

        PickYourDifficulty.debug("❌ Cleared difficulty for UUID " + uuid);
    }

    // ╔═══📦 getAllDifficultyData() — For debug/export══════════════════════╗

    public Map<UUID, String> getAllDifficultyData() {
        // 🛡️ Return a defensive copy to prevent external mutation
        return new HashMap<>(difficultyMap);
    }

    // 🔁 Difficulty effects (grace, despawn, etc.) are applied by:
    //    PlayerDataManager#applyDifficulty — this class stores state only.

    // ╔════════════════════════════════════════════════════════════╗
    // 💾 Persistence to Disk (playerdata.yml)
    // ╚════════════════════════════════════════════════════════════╝

    public void loadFromDisk() {
        // 📂 Load YAML config from disk
        FileConfiguration config = StorageUtil.loadYaml("playerdata.yml");

        // 🧹 Clear previous entries before reloading
        difficultyMap.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String difficulty = config.getString(key);

                // 🛑 Skip null entries
                if (difficulty != null) {
                    difficultyMap.put(uuid, difficulty);

                    PickYourDifficulty.debug("📥 Loaded difficulty from disk for " + uuid + " → " + difficulty);
                }

            } catch (IllegalArgumentException e) {
                // 🧯 Skip malformed UUID entries
                PickYourDifficulty.debug("⚠️ Skipped invalid UUID in playerdata.yml: " + key);
            }
        }
    }

    public void saveToDisk() {
        // 🧾 Create fresh YAML structure to store difficulty data
        FileConfiguration config = new YamlConfiguration();

        // 💾 Dump all in-memory difficulty entries
        for (Map.Entry<UUID, String> entry : difficultyMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        // 💽 Write to disk using utility
        StorageUtil.saveYaml(config, "playerdata.yml");

        PickYourDifficulty.debug("📤 Saved " + difficultyMap.size() + " difficulty entries to disk (playerdata.yml)");
    }
}