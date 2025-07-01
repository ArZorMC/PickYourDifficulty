// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🗃️ StorageUtil.java                         ║
// ║  Safely loads and saves YAML files under the plugin's data folder  ║
// ║  Used for persistence of cooldowns, player data, and config clones ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class StorageUtil {

    // ─────────────────────────────────────────────────────────────
    // 📂 File Path Resolution
    // ─────────────────────────────────────────────────────────────

    /**
     * Gets a reference to a YAML file inside the plugin's data folder.
     *
     * @param filename File name (e.g., "cooldowns.yml")
     * @return File object pointing to plugin data folder
     */
    public static File getFile(String filename) {
        // 💬 Combine plugin data folder with the filename to get full path
        return new File(PickYourDifficulty.getInstance().getDataFolder(), filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 📥 Load YAML
    // ─────────────────────────────────────────────────────────────

    /**
     * Loads a YAML file into a FileConfiguration.
     *
     * @param filename File name (e.g., "cooldowns.yml")
     * @return FileConfiguration object
     */
    public static FileConfiguration loadYaml(String filename) {
        File file = getFile(filename);

        // 📦 If the file doesn't exist, create it (and parent folder)
        if (!file.exists()) {
            try {
                boolean dirsMade = file.getParentFile().mkdirs();   // Ensure directory exists
                boolean fileCreated = file.createNewFile();         // Attempt to create the file

                // 🐛 Debug logging if enabled in config
                if (ConfigManager.isDebugMode()) {
                    PickYourDifficulty.getInstance().getLogger().info("📁 Created new file: " + filename +
                            " (dirsMade=" + dirsMade + ", fileCreated=" + fileCreated + ")");
                }

            } catch (IOException e) {
                // ⚠️ Log file creation failure
                PickYourDifficulty.getInstance().getLogger().warning("❌ Failed to create " + filename + ": " + e.getMessage());
            }
        }

        // ✅ Return loaded config from file (even if it was just created)
        return YamlConfiguration.loadConfiguration(file);
    }

    // ─────────────────────────────────────────────────────────────
    // 💾 Save YAML
    // ─────────────────────────────────────────────────────────────

    /**
     * Saves a FileConfiguration back to disk.
     *
     * @param config   FileConfiguration to save
     * @param filename File name to save to
     */
    public static void saveYaml(FileConfiguration config, String filename) {
        try {
            // 💬 Save the config object to disk using Bukkit API
            config.save(getFile(filename));
        } catch (IOException e) {
            // ⚠️ Log file saving failure
            PickYourDifficulty.getInstance().getLogger().warning("❌ Failed to save " + filename + ": " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🗑️ Delete YAML (dev/testing use only)
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a file inside the plugin's data folder if it exists.
     *
     * @param filename File name to delete
     */
    @SuppressWarnings("unused") // Reserved for dev/test commands
    public static void deleteFile(String filename) {
        File file = getFile(filename);

        // 🔎 Only delete if file already exists
        if (file.exists()) {
            boolean deleted = file.delete();

            // 🐛 Optional debug logging
            if (ConfigManager.isDebugMode()) {
                PickYourDifficulty.getInstance().getLogger().info("🗑️ Attempted to delete " + filename + ": " + deleted);
            }
        }
    }
}
