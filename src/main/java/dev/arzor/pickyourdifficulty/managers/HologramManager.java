// ╔════════════════════════════════════════════════════════════════════╗
// ║                    🪧 HologramManager.java                         ║
// ║  Manages spawn/update/removal of despawn timer holograms above     ║
// ║  dropped items using DecentHolograms. Includes permission-based    ║
// ║  viewing and persistent YAML tracking for reload-safe recovery.    ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.utils.TextUtil;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

// ╔════════════════════════════════════════════════════════════════════╗
// ║                         📊 HologramManager                         ║
// ║    Handles creation, update, deletion, persistence, and toggles    ║
// ╚════════════════════════════════════════════════════════════════════╝
public class HologramManager {

    // ─────────────────────────────────────────────────────────────
    // 🗃️ Storage and Tracking
    // ─────────────────────────────────────────────────────────────

    private static final Map<UUID, Hologram> hologramMap = new HashMap<>();
    private static final Map<UUID, TrackedHologram> trackedData = new HashMap<>();
    private static final Set<UUID> hologramsDisabled = new HashSet<>();

    private static final File hologramDataFile = new File(PickYourDifficulty.getInstance().getDataFolder(), "holograms.yml");
    private static final YamlConfiguration hologramData = YamlConfiguration.loadConfiguration(hologramDataFile);

    private static final File toggleFile = new File(PickYourDifficulty.getInstance().getDataFolder(), "holograms_toggles.yml");
    private static final YamlConfiguration toggleData = YamlConfiguration.loadConfiguration(toggleFile);

    // ─────────────────────────────────────────────────────────────
    // 🧱 Data Class
    // ─────────────────────────────────────────────────────────────

    public record TrackedHologram(UUID itemId, long expiresAtMillis) {}

    // ─────────────────────────────────────────────────────────────
    // 🎯 Create New Hologram
    // ─────────────────────────────────────────────────────────────

    public static void createHologram(Item item, int despawnSeconds) {
        UUID itemId = item.getUniqueId();
        Location location = item.getLocation().add(0, 0.5, 0); // 📍 Float slightly above

        // 🧮 Calculate expiration time
        long now = System.currentTimeMillis();
        long expiresAt = now + (despawnSeconds * 1000L);

        // 🆔 Generate unique hologram ID
        String hologramId = "pyd_" + itemId.toString().replace("-", "");

        // 🖋️ Format the hologram line using placeholder
        String raw = ConfigManager.getHologramFormat();
        String formatted = raw.replace("<despawnTime>", String.valueOf(despawnSeconds));

        // 🔒 Only show if player has permission (if required)
        boolean requirePerm = ConfigManager.hologramsRequirePermission();
        Hologram hologram = DHAPI.createHologram(hologramId, location, requirePerm, TextUtil.parseLegacyString(formatted));

        // 🧠 Track in memory + save to file
        hologramMap.put(itemId, hologram);
        trackedData.put(itemId, new TrackedHologram(itemId, expiresAt));
        hologramData.set(itemId + ".expiresAt", expiresAt);
        saveHologramFile();

        // 🐛 Debug
        PickYourDifficulty.debug("📌 Created hologram for item " + itemId + " — expires in " + despawnSeconds + "s");
    }

    // ─────────────────────────────────────────────────────────────
    // 🔁 Update Hologram Time Remaining
    // ─────────────────────────────────────────────────────────────

    public static void updateHologram(Item item) {
        UUID id = item.getUniqueId();
        Hologram hologram = hologramMap.get(item.getUniqueId());
        TrackedHologram data = trackedData.get(item.getUniqueId());
        if (hologram == null || data == null) return;

        // 🧮 Calculate seconds left until despawn
        long now = System.currentTimeMillis();
        long secondsLeft = (data.expiresAtMillis() - now) / 1000;
        if (secondsLeft < 0) secondsLeft = 0;

        // 🔁 Update line text
        String raw = ConfigManager.getHologramFormat();
        String updated = raw.replace("<despawnTime>", String.valueOf(secondsLeft));
        DHAPI.setHologramLine(hologram, 0, updated);

        // 🐛 Debug
        PickYourDifficulty.debug("🔄 Updated hologram for item " + id + " — " + secondsLeft + "s remaining");
    }

    // ─────────────────────────────────────────────────────────────
    // ❌ Remove Individual Hologram
    // ─────────────────────────────────────────────────────────────

    public static void removeHologram(Item item) {
        removeHologramFromUUID(item.getUniqueId());
    }

    public static void removeHologramFromUUID(UUID id) {
        Hologram hologram = hologramMap.remove(id);
        trackedData.remove(id);
        hologramData.set(id.toString(), null);

        if (hologram != null) {
            hologram.delete();
            PickYourDifficulty.debug("🗑️ Removed hologram for item " + id);
        }

        saveHologramFile();
    }

    // ─────────────────────────────────────────────────────────────
    // ❌ Remove All Holograms
    // ─────────────────────────────────────────────────────────────

    public static void removeAll() {
        for (Hologram holo : hologramMap.values()) {
            holo.delete();
        }

        hologramMap.clear();
        trackedData.clear();

        // 🧹 Clear YAML entries
        hologramData.getKeys(false).forEach(key -> hologramData.set(key, null));
        saveHologramFile();

        // 🐛 Debug
        PickYourDifficulty.debug("🧹 Removed all active holograms and cleared storage");
    }

    // ─────────────────────────────────────────────────────────────
    // 🔁 Restore Holograms on Plugin Reload
    // ─────────────────────────────────────────────────────────────

    public static void restoreAll() {
        long now = System.currentTimeMillis();

        for (String key : hologramData.getKeys(false)) {
            try {
                UUID itemId = UUID.fromString(key);
                long expiresAt = hologramData.getLong(key + ".expiresAt");

                if (expiresAt <= now) {
                    // ⌛ Skip expired holograms
                    hologramData.set(key, null);
                    continue;
                }

                trackedData.put(itemId, new TrackedHologram(itemId, expiresAt));
                PickYourDifficulty.debug("📦 Restored hologram tracking for item " + itemId + " (expires in " + ((expiresAt - now) / 1000) + "s)");

                // 💡 Hologram will be spawned later via HologramTaskManager
            } catch (Exception e) {
                PickYourDifficulty.getInstance().getLogger().warning("[PickYourDifficulty] Skipping invalid hologram entry: " + key);
            }
        }

        saveHologramFile();
    }

    // ─────────────────────────────────────────────────────────────
    // 👁️ Player Toggle Controls
    // ─────────────────────────────────────────────────────────────

    public static boolean isHidden(Player player) {
        UUID id = player.getUniqueId();

        // 🧠 Default to config value if not explicitly toggled
        return toggleData.contains("toggles." + id)
                ? toggleData.getBoolean("toggles." + id)
                : !ConfigManager.hologramsDefaultEnabled(); // 💡 Default from config
    }

    public static void setHidden(Player player, boolean hidden) {
        UUID id = player.getUniqueId();

        if (hidden) {
            hologramsDisabled.add(id);
        } else {
            hologramsDisabled.remove(id);
        }

        toggleData.set("toggles." + id, hidden);
        saveToggleFile();

        PickYourDifficulty.debug("👁️ Set hologram toggle for " + player.getName() + ": " + (hidden ? "HIDDEN" : "VISIBLE"));
    }

    public static boolean toggleHidden(Player player) {
        // 💡 Flip current visibility state
        boolean nowHidden = !isHidden(player);

        // 📥 Apply the new state
        setHidden(player, nowHidden);

        // 🧪 Debug: log toggle action
        PickYourDifficulty.debug("🔁 Toggled hologram view for " + player.getName() + " → " + (nowHidden ? "HIDDEN" : "VISIBLE"));
        return nowHidden;
    }

    public static Set<UUID> getHiddenPlayers() {
        return hologramsDisabled;
    }

    // ─────────────────────────────────────────────────────────────
    // 💾 File Persistence Helpers
    // ─────────────────────────────────────────────────────────────

    private static void saveHologramFile() {
        try {
            hologramData.save(hologramDataFile);
        } catch (IOException e) {
            PickYourDifficulty.getInstance().getLogger().log(Level.SEVERE, "Failed to save holograms.yml", e);
        }
    }

    private static void saveToggleFile() {
        try {
            toggleData.save(toggleFile);
        } catch (IOException e) {
            PickYourDifficulty.getInstance().getLogger().log(Level.SEVERE, "Failed to save holograms_toggles.yml", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔎 Accessors for TaskManager and Debug
    // ─────────────────────────────────────────────────────────────

    public static Map<UUID, TrackedHologram> getTrackedData() {
        return trackedData;
    }

    public static Map<UUID, Hologram> getHologramMap() {
        return hologramMap;
    }
}
