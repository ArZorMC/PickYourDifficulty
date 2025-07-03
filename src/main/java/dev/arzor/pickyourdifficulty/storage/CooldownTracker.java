// ╔════════════════════════════════════════════════════════════════════╗
// ║                    ⏳ CooldownTracker.java                         ║
// ║  Tracks difficulty change cooldowns on a per-player basis          ║
// ║  Uses in-memory map; persists cooldowns via cooldowns.yml          ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.storage;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.utils.StorageUtil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// 🧠 CooldownTracker — Memory + Disk for Difficulty Lock Timer
// ─────────────────────────────────────────────────────────────
public class CooldownTracker {

    // ╔═══🗺️ Internal Cooldown Map═══════════════════════════════════════╗
    // Stores: Player UUID → Epoch seconds of last difficulty change
    private static final Map<UUID, Long> cooldownMap = new HashMap<>();

    // ╔═══❄️ isCooldownActive() — Check if a player is on cooldown═══════╗
    public static boolean isCooldownActive(UUID uuid) {

        // 🧼 No record = no cooldown
        if (!cooldownMap.containsKey(uuid)) return false;

        // 🕓 When did the player last change difficulty?
        long lastChangeTime = cooldownMap.get(uuid);

        // ⏱️ Get current time in epoch seconds
        long now = System.currentTimeMillis() / 1000;

        // 🧮 Elapsed time = now - last change
        long elapsed = now - lastChangeTime;

        // 🔁 How long the cooldown lasts (from config)
        long cooldown = ConfigManager.changeCooldownSeconds();

        // ✅ Still cooling down if elapsed < cooldown
        boolean active = elapsed < cooldown;

        // 🧪 Debug output if enabled
        PickYourDifficulty.debug("⌛ Cooldown check for " + uuid + ": " +
                (active ? "ACTIVE" : "EXPIRED") + " (" + elapsed + "s elapsed, cooldown = " + cooldown + "s)");

        return active;
    }

    // ╔═══⏱️ getRemainingSeconds() — Time Left on Cooldown════════════════╗
    public static long getRemainingSeconds(UUID uuid) {

        // 🧼 No entry? No time remaining.
        if (!cooldownMap.containsKey(uuid)) return 0;

        long lastChangeTime = cooldownMap.get(uuid);
        long now = System.currentTimeMillis() / 1000;
        long cooldown = ConfigManager.changeCooldownSeconds();

        // 🧮 Calculate time remaining by subtract elapsed from cooldown; clamp to zero minimum
        long elapsed = now - lastChangeTime;
        long remaining = Math.max(0, cooldown - elapsed);

        PickYourDifficulty.debug("⏱️ Remaining cooldown for " + uuid + ": " + remaining +
                "s (elapsed: " + elapsed + "s)");

        return remaining;
    }

    // ╔═══🎯 setCooldownNow() — Start cooldown from current time═══════════╗
    public static void setCooldownNow(UUID uuid) {
        long now = System.currentTimeMillis() / 1000;
        cooldownMap.put(uuid, now);

        PickYourDifficulty.debug("📌 Set cooldown for " + uuid + " at time " + now);
    }

    // ╔═══🧼 clearCooldown() — Remove cooldown for a specific player═══════╗
    public static void clearCooldown(UUID uuid) {
        cooldownMap.remove(uuid);

        PickYourDifficulty.debug("❌ Cleared cooldown for " + uuid);
    }

    // ╔═══💣 clearAll() — ⚠️ Dev-only nuke method to clear all cooldowns════╗
    @SuppressWarnings("unused")
    public static void clearAll() {
        cooldownMap.clear();

        PickYourDifficulty.debug("💥 Cleared all cooldowns (dev use only)");
    }

    // ╔═══💾 Load Cooldowns from Disk═════════════════════════════════════╗
    public static void loadFromDisk() {
        FileConfiguration config = StorageUtil.loadYaml("cooldowns.yml");

        cooldownMap.clear(); // start fresh
        int loaded = 0;

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long timestamp = config.getLong(key);
                cooldownMap.put(uuid, timestamp);
                loaded++;
            } catch (IllegalArgumentException ignored) {
                // 🧼 Skip any invalid entries that aren’t valid UUIDs
            }
        }

        PickYourDifficulty.debug("💾 Loaded " + loaded + " cooldown entries from cooldowns.yml");
    }

    // ╔═══💾 Save Cooldowns to Disk═══════════════════════════════════════╗
    public static void saveToDisk() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Long> entry : cooldownMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        StorageUtil.saveYaml(config, "cooldowns.yml");

        PickYourDifficulty.debug("💾 Saved " + cooldownMap.size() + " cooldown entries to cooldowns.yml");
    }
}