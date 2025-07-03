// ╔════════════════════════════════════════════════════════════════════╗
// ║                🎮 PickYourDifficulty Plugin Main Class             ║
// ║   Initializes config, commands, integrations, and listeners        ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty;

import dev.arzor.pickyourdifficulty.commands.CommandPyd;
import dev.arzor.pickyourdifficulty.commands.CommandPydDebug;
import dev.arzor.pickyourdifficulty.listeners.*;
import dev.arzor.pickyourdifficulty.managers.*;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

// ─────────────────────────────────────────────────────────────
// 🎮 Main plugin entry point
// ─────────────────────────────────────────────────────────────
public final class PickYourDifficulty extends JavaPlugin {

    // ─────────────────────────────────────────────────────────────
    // 🧱 Singleton Plugin Instance and Core Managers
    // ─────────────────────────────────────────────────────────────

    // 💾 Holds the active instance of this plugin for global access
    private static PickYourDifficulty instance;

    // 🧠 Stores persistent player difficulty data (disk + memory)
    private PlayerDifficultyStorage difficultyStorage;

    // 🔁 Tracks player session data, cooldowns, etc
    private PlayerDataManager playerDataManager;

    // 🖼️ Manages difficulty selection and confirmation GUIs
    private GUIManager guiManager;

    // ─────────────────────────────────────────────────────────────
    // 🔃 Plugin Lifecycle — onLoad()
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        // 💬 Save this instance for static access across plugin
        instance = this;
    }

    /// ─────────────────────────────────────────────────────────────
    // ▶️ Plugin Lifecycle — onEnable()
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {

        // ╔═══📦 Load Config & Messages════════════════════════════════╗
        // 🧾 Initialize messages first so we can send localized errors if config fails
        MessagesManager.init(this);

        // ⚙️ Load config.yml and apply defaults
        ConfigManager.init(this);

        // ╔═══🧠 Load Player Data═══════════════════════════════════════╗
        // 💾 Load previously stored difficulties from disk
        difficultyStorage = PlayerDifficultyStorage.getInstance();
        difficultyStorage.loadFromDisk();

        // 🧑‍💻 Build session manager on top of stored data
        playerDataManager = new PlayerDataManager(difficultyStorage);

        // 🎨 Load GUI templates and prepare menus
        guiManager = GUIManager.getInstance();

        // ╔═══⏳ Load Cooldowns═════════════════════════════════════════╗
        CooldownTracker.loadFromDisk();

        // ╔═══🎧 Register Event Listeners═════════════════════════════╗

        // 👋 Handle join + GUI open
        getServer().getPluginManager().registerEvents(new JoinListener(guiManager, playerDataManager), this);

        // 💀 Track deaths and save item drops
        getServer().getPluginManager().registerEvents(new DeathDropListener(), this);

        // ⏲️ Show hologram timers for dropped items
        getServer().getPluginManager().registerEvents(new DespawnTimerListener(difficultyStorage), this);

        // 🛡️ Warn when grace period is ending
        getServer().getPluginManager().registerEvents(new GraceReminderListener(difficultyStorage), this);

        // 🛡️ Prevent damage during grace period
        getServer().getPluginManager().registerEvents(new GraceProtectionListener(difficultyStorage), this);

        // 🎒 Store dropped items before despawn
        getServer().getPluginManager().registerEvents(new ItemPickupListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(), this);

        // 🖱️ GUI click handling (main + confirm)
        getServer().getPluginManager().registerEvents(new GUIClickListener(guiManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new ConfirmGUIClickListener(guiManager), this);

        // 📜 AcceptTheRules integration auto-registers listener
        new RulesAcceptListener(this, guiManager, playerDataManager);

        // ╔═══💬 Register Commands═════════════════════════════════════╗

        // 🧭 /pyd — Opens GUI, sets difficulty
        PluginCommand pydCmd = getCommand("pyd");
        if (pydCmd != null) {
            pydCmd.setExecutor(new CommandPyd());
        }

        // 🧪 /pyddebug — Shows debug info for devs
        PluginCommand debugCmd = getCommand("pyddebug");
        if (debugCmd != null) {
            debugCmd.setExecutor(new CommandPydDebug());
        }

        // ╔═══🔌 External Integrations═════════════════════════════════╗

        // 🔤 PlaceholderAPI (placeholder registration)
        if (ConfigManager.enablePlaceholderAPI()) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                dev.arzor.pickyourdifficulty.placeholders.PlaceholderRegistrar.register(playerDataManager);
                getLogger().info("🔤 PlaceholderAPI detected – Placeholder support enabled.");
            } else {
                getLogger().warning("⚠️ PlaceholderAPI is enabled in config but not installed. Skipping placeholder support.");
            }
        }

        // 📜 AcceptTheRules (auto GUI on rule accept)
        if (ConfigManager.autoOpenAfterRules()) {
            if (Bukkit.getPluginManager().isPluginEnabled("AcceptTheRules")) {
                getLogger().info("📜 AcceptTheRules detected - Rules support enabled.");
            } else {
                getLogger().warning("⚠️ AcceptTheRules is enabled in config but not installed. Skipping rules support.");
            }
        }

        // 📱 Geyser (Bedrock support via proxy)
        if (ConfigManager.enableGeyserSupport()) {
            getLogger().info("📱 Geyser support is ENABLED in config. Assuming Geyser is installed on the proxy.");
            getLogger().info("    📢 If using Geyser, ensure it is installed on your PROXY (e.g., Velocity or BungeeCord),");
            getLogger().info("    as it will not appear in this server's plugin list.");
        }

        // 🔮 DecentHolograms (visual timers)
        if (ConfigManager.hologramsEnabled()) {
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
                getLogger().info("🔮 DecentHolograms detected – Hologram support enabled.");
            } else {
                getLogger().warning("⚠️ DecentHolograms is enabled in config but not installed. Skipping hologram support.");
            }
        }

        // ╔═══💡 Visual Restore + Task Start═══════════════════════════╗

        // 🔄 Restore dropped item holograms from memory
        HologramManager.restoreAll();

        // 🔁 Start recurring update task
        HologramTaskManager.start(this);

        // ✅ Final enable log
        getLogger().info("✅ PickYourDifficulty has been enabled. Ready for players!");
    }

    // ─────────────────────────────────────────────────────────────
    // ⏹️ Plugin Lifecycle — onDisable()
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onDisable() {

        // 🧹 Stop hologram updates and clean visuals
        HologramTaskManager.stop();

        // 💾 Persist cooldowns and difficulty storage to disk
        CooldownTracker.saveToDisk();
        PlayerDifficultyStorage.getInstance().saveToDisk();

        // ❌ Final disable log
        getLogger().info("❌ PickYourDifficulty has been disabled.");
    }

    // ─────────────────────────────────────────────────────────────
    // 🧭 Getters for Managers and Instance
    // ─────────────────────────────────────────────────────────────

    // 💬 Access plugin instance from anywhere
    public static PickYourDifficulty getInstance() {
        return instance;
    }

    // 💬 Access GUI manager for menu display
    public GUIManager getGuiManager() {
        return guiManager;
    }

    // 💬 Access session + stats manager
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    // 💬 Access difficulty storage system
    public PlayerDifficultyStorage getPlayerDifficultyStorage() {
        return difficultyStorage;
    }

    // ─────────────────────────────────────────────────────────────
    // 🪵 Debug Logger — Global debug print controlled by config
    // ─────────────────────────────────────────────────────────────

    // 💬 Logs debug output to console if debug mode is enabled
    // ✅ Usage: PickYourDifficulty.debug("Something happened!");
    // 🪵 Prefix is auto-added for consistency/
    public static void debug(String message) {
        if (ConfigManager.isDebugMode()) {
            instance.getLogger().info("[PickYourDifficulty] [DEBUG] " + message);
        }
    }
}
