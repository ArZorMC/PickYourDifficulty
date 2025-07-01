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

public final class PickYourDifficulty extends JavaPlugin {

    // ─────────────────────────────────────────────────────────────
    // 🧱 Singleton Plugin Instance and Core Managers
    // ─────────────────────────────────────────────────────────────

    private static PickYourDifficulty instance;

    private PlayerDifficultyStorage difficultyStorage;
    private PlayerDataManager playerDataManager;
    private GUIManager guiManager;

    // ─────────────────────────────────────────────────────────────
    // 🔃 Plugin Lifecycle — onLoad()
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        // 💾 Store static instance for global access
        instance = this;
    }

    // ─────────────────────────────────────────────────────────────
    // ▶️ Plugin Lifecycle — onEnable()
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {

        // ╔═══📦 Load Configuration Files═════════════════════════════╗
        MessagesManager.init(this);
        ConfigManager.init(this);

        // ╔═══🔧 Initialize Core Managers═════════════════════════════╗
        difficultyStorage = PlayerDifficultyStorage.getInstance();
        difficultyStorage.loadFromDisk();

        playerDataManager = new PlayerDataManager(difficultyStorage);
        guiManager = GUIManager.getInstance();

        // ╔═══🧠 Load Cooldown Tracking Data══════════════════════════╗
        CooldownTracker.loadFromDisk();

        // ╔═══🎧 Register Event Listeners═════════════════════════════╗
        // 👋 Join and GUI setup
        getServer().getPluginManager().registerEvents(new JoinListener(guiManager, playerDataManager), this);

        // 🧱 Core gameplay logic
        getServer().getPluginManager().registerEvents(new DeathDropListener(), this);
        getServer().getPluginManager().registerEvents(new DespawnTimerListener(difficultyStorage), this);
        getServer().getPluginManager().registerEvents(new GraceReminderListener(difficultyStorage), this);
        getServer().getPluginManager().registerEvents(new GraceProtectionListener(difficultyStorage), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(), this);

        // 🖱️ GUI click handling
        getServer().getPluginManager().registerEvents(new GUIClickListener(guiManager, playerDataManager, getLogger()), this);
        getServer().getPluginManager().registerEvents(new ConfirmGUIClickListener(guiManager), this);

        // 📜 AcceptTheRules hook — registers itself if plugin is present
        new RulesAcceptListener(this, guiManager, playerDataManager);

        // ╔═══💬 Register Commands════════════════════════════════════╗
        PluginCommand pydCmd = getCommand("pyd");
        if (pydCmd != null) {
            pydCmd.setExecutor(new CommandPyd());
            // 🎮 /pyd — Main player-facing command for difficulty GUI and settings
        }

        PluginCommand debugCmd = getCommand("pyddebug");
        if (debugCmd != null) {
            debugCmd.setExecutor(new CommandPydDebug());
            // 🐞 /pyddebug — Developer/debugging tools and data inspection
        }

        // ╔═══🔤 PlaceholderAPI Integration═══════════════════════════╗
        if (ConfigManager.enablePlaceholderAPI()) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                dev.arzor.pickyourdifficulty.placeholders.PlaceholderRegistrar.register(playerDataManager);
                getLogger().info("🔤 PlaceholderAPI detected – Placeholder support enabled.");
            } else {
                getLogger().warning("⚠️ PlaceholderAPI is enabled in config but not installed. Skipping placeholder support.");
            }
        }

        // ╔═══📜 AcceptTheRules Integration═══════════════════════════╗
        if (ConfigManager.autoOpenAfterRules()) {
            if (Bukkit.getPluginManager().isPluginEnabled("AcceptTheRules")) {
                getLogger().info("📜 AcceptTheRules detected - Rules support enabled.");
            } else {
                getLogger().warning("⚠️ AcceptTheRules is enabled in config but not installed. Skipping rules support.");
            }
        }

        // ╔═══📱 Geyser Integration (proxy-assumed) ══════════════════╗
        if (ConfigManager.enableGeyserSupport()) {
            getLogger().info("📱 Geyser support is ENABLED in config. Assuming Geyser is installed on the proxy.");
            getLogger().info("    📢 If using Geyser, ensure it is installed on your PROXY (e.g., Velocity or BungeeCord),");
            getLogger().info("    as it will not appear in this server's plugin list.");
        }

        // ╔═══🔮 DecentHolograms Integration══════════════════════════╗
        if (ConfigManager.hologramsEnabled()) {
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
                getLogger().info("🔮 DecentHolograms detected – Hologram support enabled.");
            } else {
                getLogger().warning("⚠️ DecentHolograms is enabled in config but not installed. Skipping hologram support.");
            }
        }

        // ╔═══💡 Restore Despawn Holograms════════════════════════════╗
        HologramManager.restoreAll();

        // ╔═══⏲️ Start Hologram Update Task═══════════════════════════╗
        HologramTaskManager.start(this);

        getLogger().info("✅ PickYourDifficulty has been enabled.  Ready for players!");
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

        getLogger().info("❌ PickYourDifficulty has been disabled.");
    }

    // ─────────────────────────────────────────────────────────────
    // 🧭 Getters for Managers and Instance
    // ─────────────────────────────────────────────────────────────

    public static PickYourDifficulty getInstance() {
        return instance;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PlayerDifficultyStorage getPlayerDifficultyStorage() {
        return difficultyStorage;
    }
}
