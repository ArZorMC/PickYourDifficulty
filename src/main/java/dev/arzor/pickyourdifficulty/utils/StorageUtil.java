// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🗃️ StorageUtil.java                         ║
// ║  Safely loads and saves YAML files under the plugin's data folder  ║
// ║  Used for persistence of cooldowns, player data, and config clones ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

// ─────────────────────────────────────────────────────────────
// 🗃️ StorageUtil — YAML load/save/delete logic
// ─────────────────────────────────────────────────────────────
public class StorageUtil {

    // ╔═══📂 File Path Resolution════════════════════════════════════════╗

    // 💬 Gets a reference to a file in the plugin's data folder
    public static File getFile(String filename) {
        return new File(PickYourDifficulty.getInstance().getDataFolder(), filename);
    }

    // ╔═══📥 Load YAML File═════════════════════════════════════════════╗

    // 💬 Loads a YAML file and creates it if missing
    public static FileConfiguration loadYaml(String filename) {
        File file = getFile(filename);

        // 📦 If the file doesn't exist, create it (and parent folder)
        if (!file.exists()) {
            try {
                boolean dirsMade = file.getParentFile().mkdirs();   // Ensure directory exists
                boolean fileCreated = file.createNewFile();         // Attempt to create the file

                // 🧪 Debug: log directory and file creation result
                PickYourDifficulty.debug("📁 Created file: " + filename +
                        " (dirsMade=" + dirsMade + ", fileCreated=" + fileCreated + ")");

            } catch (IOException e) {
                // ⚠️ Log file creation failure
                PickYourDifficulty.getInstance().getLogger().warning("❌ Failed to create " + filename + ": " + e.getMessage());
            }
        }

        // 📤 Load and return the config (new or existing)
        return YamlConfiguration.loadConfiguration(file);
    }

    // ╔═══💾 Save YAML File═════════════════════════════════════════════╗

    // 💬 Saves a FileConfiguration to disk
    public static void saveYaml(FileConfiguration config, String filename) {
        try {
            config.save(getFile(filename));

            // 🧪 Debug: log confirmed save
            PickYourDifficulty.debug("💾 Saved file: " + filename);

        } catch (IOException e) {
            // ⚠️ Log file saving failure
            PickYourDifficulty.getInstance().getLogger().warning("❌ Failed to save " + filename + ": " + e.getMessage());
        }
    }

    // ╔═══🗑️ Delete YAML File═══════════════════════════════════════════╗

    @SuppressWarnings("unused") // ⚠️ Dev/test usage only
    public static void deleteFile(String filename) {
        File file = getFile(filename);

        // 🔎 Only delete if file actually exists
        if (file.exists()) {
            boolean deleted = file.delete();

            // 🧪 Debug: log deletion result
            PickYourDifficulty.debug("🗑️ Deleted file: " + filename + " → success=" + deleted);
        }
    }
}
