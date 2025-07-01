// ╔════════════════════════════════════════════════════════════════════╗
// ║                    ⏳ CooldownTracker.java                         ║
// ║  Tracks difficulty change cooldowns on a per-player basis          ║
// ║  Uses in-memory map; persists cooldowns via cooldowns.yml          ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.storage;

import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.utils.StorageUtil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownTracker {

    // ╔════════════════════════════════════════════════════════════╗
    // ║                  🗺️ Internal Cooldown Map                  ║
    // ╚════════════════════════════════════════════════════════════╝

    /** Stores when each player last changed difficulty (epoch seconds) */
    private static final Map<UUID, Long> cooldownMap = new HashMap<>();

    // ╔════════════════════════════════════════════════════════════╗
    // ║                    ⏱️ Cooldown Logic                       ║
    // ╚════════════════════════════════════════════════════════════╝

    /**
     * Checks whether the given player is still on cooldown.
     *
     * @param uuid The player's UUID
     * @return true if cooldown is active; false otherwise
     */
    public static boolean isCooldownActive(UUID uuid) {
        if (!cooldownMap.containsKey(uuid)) return false;

        long lastChangeTime = cooldownMap.get(uuid);
        long now = System.currentTimeMillis() / 1000;

        // 🧮 Check if enough seconds have passed since last change
        return (now - lastChangeTime) < ConfigManager.changeCooldownSeconds();
    }

    /**
     * Gets the number of seconds remaining on the player's cooldown.
     *
     * @param uuid The player's UUID
     * @return Remaining cooldown seconds (or 0 if expired)
     */
    public static long getRemainingSeconds(UUID uuid) {
        if (!cooldownMap.containsKey(uuid)) return 0;

        long lastChangeTime = cooldownMap.get(uuid);
        long now = System.currentTimeMillis() / 1000;
        long cooldown = ConfigManager.changeCooldownSeconds();

        long elapsed = now - lastChangeTime;

        // 🧮 Subtract elapsed from cooldown; clamp to zero minimum
        return Math.max(0, cooldown - elapsed);
    }

    /**
     * Starts a cooldown timer for the player (using current timestamp).
     *
     * @param uuid The player's UUID
     */
    public static void setCooldownNow(UUID uuid) {
        long now = System.currentTimeMillis() / 1000;
        cooldownMap.put(uuid, now);
    }

    /**
     * Clears the cooldown entry for a specific player.
     *
     * @param uuid The player's UUID
     */
    public static void clearCooldown(UUID uuid) {
        cooldownMap.remove(uuid);
    }

    /**
     * ⚠️ Developer method: Clears ALL cooldowns (testing only).
     * Should not be used in production.
     */
    @SuppressWarnings("unused")
    public static void clearAll() {
        cooldownMap.clear();
    }

    // ╔════════════════════════════════════════════════════════════╗
    // ║           💾 Persistence to/from cooldowns.yml             ║
    // ╚════════════════════════════════════════════════════════════╝

    /**
     * Loads all cooldown data from cooldowns.yml and repopulates the map.
     */
    public static void loadFromDisk() {
        FileConfiguration config = StorageUtil.loadYaml("cooldowns.yml");

        cooldownMap.clear(); // start fresh

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long timestamp = config.getLong(key);
                cooldownMap.put(uuid, timestamp);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUID entries
            }
        }
    }

    /**
     * Saves current cooldownMap contents to cooldowns.yml.
     */
    public static void saveToDisk() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Long> entry : cooldownMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        StorageUtil.saveYaml(config, "cooldowns.yml");
    }
}