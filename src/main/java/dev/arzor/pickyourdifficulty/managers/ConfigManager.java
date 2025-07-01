// ╔════════════════════════════════════════════════════════════════════╗
// ║                     📘 ConfigManager.java                          ║
// ║    Wraps access to config.yml for PickYourDifficulty plugin        ║
// ║    Provides typed access to structured config settings.            ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.interfaces.Reloadable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager implements Reloadable {

    // ╔═══📦 Plugin Config Instance════════════════════════════════════════╗

    /** Reference to the plugin instance */
    private static final JavaPlugin plugin = PickYourDifficulty.getInstance();

    /** Cached config instance */
    private static FileConfiguration config;

    // ╔═══🔁 Config Initialization & Reload═══════════════════════════════╗

    static {
        ReloadManager.register(new ConfigManager()); // ⏺️ Register this static class for reloads
    }

    /** Initializes and loads the default config */
    public static void init(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    /** Reloads the config from disk */
    @Override
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // ╔═══🧭 General Settings═══════════════════════════════════════════════╗

    /** Whether to auto-open GUI after AcceptTheRules completion */
    public static boolean autoOpenAfterRules() {
        return config.getBoolean("autoOpenAfterRules", true);
    }

    /** Whether to auto-open GUI if player has never chosen a difficulty */
    public static boolean autoOpenIfUnchosen() {
        return config.getBoolean("autoOpenIfUnchosen", true);
    }

    // ╔═══💡 Fallbacks + Defaults══════════════════════════════════════╗

    /** Returns fallback difficulty if none selected */
    public static String getFallbackDifficulty() {
        return config.getString("fallbackDifficulty", "normal");
    }

    // ╔═══🔒 Difficulty Selection Rules═════════════════════════════════════╗

    /** Whether difficulty is locked once chosen */
    public static boolean lockInDifficulty() {
        return config.getBoolean("difficultySelection.lockedOnceChosen", false);
    }

    /** Whether difficulty changes are allowed */
    public static boolean allowDifficultyChange() {
        return config.getBoolean("difficultySelection.allowRechoose", true);
    }

    /** Cooldown time in seconds before switching is allowed again */
    public static int changeCooldownSeconds() {
        return config.getInt("difficultySelection.cooldownSeconds", 86400);
    }

    /** MiniMessage format string for cooldown time display */
    public static String getCooldownFormat() {
        return config.getString("difficultySelection.cooldownFormat", "<hours>h <minutes>m <seconds>s");
    }

    /** Whether confirmation GUI is required before applying */
    public static boolean requireConfirmation() {
        return config.getBoolean("difficultySelection.require-confirmation", true);
    }

    // ╔═══⏳ Despawn Timer Behavior═════════════════════════════════════════╗

    /** Whether despawn timer applies only to death drops */
    public static boolean despawnOnlyAffectsDeathDrops() {
        return config.getBoolean("despawnBehavior.onlyAffectsDeathDrops", true);
    }

    /** Prevent downgrading despawn timers if higher difficulty picks up */
    public static boolean preventDespawnTimerDowngrade() {
        return config.getBoolean("despawnBehavior.preventTimerDowngrade", true);
    }


    // ╔═══🎮 Difficulty Presets════════════════════════════════════════╗

    /** Returns all difficulty keys defined in config */
    public static List<String> getDifficultyNames() {
        ConfigurationSection section = config.getConfigurationSection("difficulties");
        return section != null ? section.getKeys(false).stream().toList() : List.of();
    }

    /** Gets GUI slot number for a given difficulty */
    public static int getSlot(String difficulty) {
        return config.getInt("difficulties." + difficulty + ".slot", 0);
    }

    /** Gets item despawn time for a difficulty */
    public static int getDespawnTime(String difficulty) {
        return config.getInt("difficulties." + difficulty + ".despawn-seconds", 300);
    }

    /** Gets grace period time for a difficulty */
    public static int getGraceTime(String difficulty) {
        return config.getInt("difficulties." + difficulty + ".grace-playtime-seconds", 0);
    }

    /** Returns material of the icon for a difficulty */
    public static String getMaterial(String difficulty) {
        return config.getString("difficulties." + difficulty + ".icon.material", "STONE");
    }

    /** Returns name text of the icon */
    public static String getName(String difficulty) {
        return config.getString("difficulties." + difficulty + ".icon.name");
    }

    /** Returns lore lines for the difficulty's icon */
    public static List<String> getLore(String difficulty) {
        return config.getStringList("difficulties." + difficulty + ".icon.lore");
    }

    // ╔═══⚙️ Per-Difficulty Commands═══════════════════════════════════════╗

    /** Returns a list of console/player commands to run when selecting a difficulty */
    public static List<String> getCommands(String difficulty) {
        return config.getStringList("difficultyCommands." + difficulty);
    }

    // ╔═══🛡️ Grace Period Mechanics═══════════════════════════════════════╗

    /** Whether grace mode is enabled overall */
    public static boolean enableGraceMode() {
        return config.getBoolean("graceMode.enabled", true);
    }

    /** Worlds where grace mode is disabled */
    public static List<String> getDisabledOverrideWorlds() {
        return config.getStringList("graceMode.override-worlds.disabled");
    }

    /** Whether grace reminders are disabled */
    public static boolean disableReminder() {
        return !config.getBoolean("graceMode.showReminder", true);
    }

    /** Mode for showing grace reminders ("onLogin", etc.) */
    public static String getGraceReminderMode() {
        return config.getString("graceMode.graceReminderMode", "onLogin");
    }

    /** Interval between grace reminders (seconds) */
    public static int getGraceReminderIntervalSeconds() {
        return config.getInt("graceMode.intervalSeconds", 1800);
    }

    /** List of damage types that bypass grace protection */
    public static List<String> getGraceBypassDamageTypes() {
        return config.getStringList("graceMode.bypassDamageTypes");
    }

    // ╔═══🖼️ GUI Settings═══════════════════════════════════════════════════╗

    /** Title of the difficulty selection GUI (MiniMessage supported) */
    public static String getGuiTitle() {
        return config.getString("gui.title", "<gold><bold>Select Your Difficulty</bold></gold>");
    }

    /** Number of rows in the GUI */
    public static int getGuiRows() {
        return config.getInt("gui.rows", 3);
    }

    /** Whether to fill all empty GUI slots */
    public static boolean fillGuiEmpty() {
        return config.getBoolean("gui.fill-empty", true);
    }

    /** Whether GUI should close after a selection is made */
    public static boolean guiCloseOnSelect() {
        return config.getBoolean("gui.closeOnSelect", true);
    }

    /** Material of the GUI filler item */
    public static String getGuiFillerItemMaterial() {
        return config.getString("gui.filler-item.material", "GRAY_STAINED_GLASS_PANE");
    }

    /** Display name of the GUI filler item */
    public static String getGuiFillerItemName() {
        return config.getString("gui.filler-item.name");
    }

    /** Lore lines for the GUI filler item */
    public static List<String> getGuiFillerItemLore() {
        return config.getStringList("gui.filler-item.lore");
    }

    // ╔═══🔐 Difficulty Visibility Rules══════════════════════════════════════╗

    /** Whether to hide difficulties the player can’t select */
    public static boolean hideUnselectableDifficulties() {
        return config.getBoolean("hide-unselectable-difficulties", false);
    }

    // ╔═══✅ Difficulty Confirmation GUI═════════════════════════════════════╗

    /** Title of the confirmation GUI */
    public static String getConfirmationGuiTitle() {
        return config.getString("confirmationGUI.title", "<gold><bold>Confirm Your Difficulty</bold></gold>");
    }

    /** Number of rows in the confirmation GUI */
    public static int getConfirmationGuiRows() {
        return config.getInt("confirmationGUI.rows", 3);
    }

    /** Whether to fill empty slots in confirmation GUI */
    public static boolean fillConfirmationGuiEmpty() {
        return config.getBoolean("confirmationGUI.fill-empty", true);
    }

    // ─── Filler Item ──────────────────────────────────────────────────────
    /** Filler item material for confirmation GUI */
    public static String getConfirmationGuiFillerMaterial() {
        return config.getString("confirmationGUI.filler-item.material", "BLACK_STAINED_GLASS_PANE");
    }

    /** Filler item name for confirmation GUI */
    public static String getConfirmationGuiFillerName() {
        return config.getString("confirmationGUI.filler-item.name", "<gray>Confirm or Cancel</gray>");
    }

    /** Filler item lore for confirmation GUI */
    public static List<String> getConfirmationGuiFillerLore() {
        return config.getStringList("confirmationGUI.filler-item.lore");
    }

    // ─── Buttons (Default Mode) ───────────────────────────────────────────
    /** Whether to display the info banner in confirmation GUI */
    public static boolean isInfoBannerEnabled() {
        return config.getBoolean("confirmationGUI.buttons.info-banner.enabled", true);
    }

    /** Slot index for the info banner */
    public static int getInfoBannerSlot() {
        return config.getInt("confirmationGUI.buttons.info-banner.slot", 4);
    }

    /** Material for the info banner item */
    public static String getInfoBannerMaterial() {
        return config.getString("confirmationGUI.buttons.info-banner.material", "PAPER");
    }

    /** Name for the info banner */
    public static String getInfoBannerName() {
        return config.getString("confirmationGUI.buttons.info-banner.name", "<yellow>Confirm Your Choice</yellow>");
    }

    /** Lore for the info banner */
    public static List<String> getInfoBannerLore() {
        return config.getStringList("confirmationGUI.buttons.info-banner.lore");
    }

    /** Slot for the confirm button */
    public static int getConfirmButtonSlot() {
        return config.getInt("confirmationGUI.buttons.confirm-button.slot", 11);
    }

    /** Material for the confirm button */
    public static String getConfirmButtonMaterial() {
        return config.getString("confirmationGUI.buttons.confirm-button.material", "LIME_WOOL");
    }

    /** Display name of the confirm button */
    public static String getConfirmButtonName() {
        return config.getString("confirmationGUI.buttons.confirm-button.name", "<green><bold>Confirm</bold></green>");
    }

    /** Lore of the confirm button */
    public static List<String> getConfirmButtonLore() {
        return config.getStringList("confirmationGUI.buttons.confirm-button.lore");
    }

    /** Slot for the cancel button */
    public static int getCancelButtonSlot() {
        return config.getInt("confirmationGUI.buttons.cancel-button.slot", 15);
    }

    /** Material for the cancel button */
    public static String getCancelButtonMaterial() {
        return config.getString("confirmationGUI.buttons.cancel-button.material", "RED_WOOL");
    }

    /** Display name of the cancel button */
    public static String getCancelButtonName() {
        return config.getString("confirmationGUI.buttons.cancel-button.name", "<red><bold>Cancel</bold></red>");
    }

    /** Lore of the cancel button */
    public static List<String> getCancelButtonLore() {
        return config.getStringList("confirmationGUI.buttons.cancel-button.lore");
    }

    // ─── Overrides (Locked Mode) ──────────────────────────────────────────
    /** Name override for info banner when difficulty is locked */
    public static String getLockedInfoBannerName() {
        return config.getString("confirmationGUI.overrides.locked.buttons.info-banner.name", "<red><bold>Permanent Choice</bold></red>");
    }

    /** Lore override for locked difficulty info banner */
    public static List<String> getLockedInfoBannerLore() {
        return config.getStringList("confirmationGUI.overrides.locked.buttons.info-banner.lore");
    }

    /** Material override for locked confirm button */
    public static String getLockedConfirmButtonMaterial() {
        return config.getString("confirmationGUI.overrides.locked.buttons.confirm-button.material", "BARRIER");
    }

    /** Name override for locked confirm button */
    public static String getLockedConfirmButtonName() {
        return config.getString("confirmationGUI.overrides.locked.buttons.confirm-button.name", "<dark_red><bold>Confirm (One-Time)</bold></dark_red>");
    }

    /** Lore override for locked confirm button */
    public static List<String> getLockedConfirmButtonLore() {
        return config.getStringList("confirmationGUI.overrides.locked.buttons.confirm-button.lore");
    }

    // ╔═══🔊 Sound Effects═════════════════════════════════════════════════╗

    /** 🔊 Sound key for opening GUI */
    public static String getGuiOpenSoundKey() {
        return config.getString("sounds.gui-open.sound", "UI_BUTTON_CLICK");
    }
    public static float getGuiOpenVolume() {
        return (float) config.getDouble("sounds.gui-open.volume", 1.0);
    }
    public static float getGuiOpenPitch() {
        return (float) config.getDouble("sounds.gui-open.pitch", 1.0);
    }

    /** ✅ Sound key for confirming selection */
    public static String getConfirmSoundKey() {
        return config.getString("sounds.confirm-selection.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }
    public static float getConfirmVolume() {
        return (float) config.getDouble("sounds.confirm-selection.volume", 1.0);
    }
    public static float getConfirmPitch() {
        return (float) config.getDouble("sounds.confirm-selection.pitch", 1.0);
    }

    /** ❌ Sound key for cancelling selection */
    public static String getCancelSoundKey() {
        return config.getString("sounds.cancel-selection.sound", "BLOCK_NOTE_BLOCK_BASS");
    }
    public static float getCancelVolume() {
        return (float) config.getDouble("sounds.cancel-selection.volume", 1.0);
    }
    public static float getCancelPitch() {
        return (float) config.getDouble("sounds.cancel-selection.pitch", 1.0);
    }

    /** 🔒 Sound for denied access (locked difficulty) */
    public static String getLockedDeniedSoundKey() {
        return config.getString("sounds.gui-denied-locked.sound", "BLOCK_ANVIL_LAND");
    }
    public static float getLockedDeniedVolume() {
        return (float) config.getDouble("sounds.gui-denied-locked.volume", 1.0);
    }
    public static float getLockedDeniedPitch() {
        return (float) config.getDouble("sounds.gui-denied-locked.pitch", 1.0);
    }

    /** ⏳ Sound for denied access (cooldown active) */
    public static String getCooldownDeniedSoundKey() {
        return config.getString("sounds.gui-denied-cooldown.sound", "ENTITY_VILLAGER_NO");
    }
    public static float getCooldownDeniedVolume() {
        return (float) config.getDouble("sounds.gui-denied-cooldown.volume", 1.0);
    }
    public static float getCooldownDeniedPitch() {
        return (float) config.getDouble("sounds.gui-denied-cooldown.pitch", 1.0);
    }

    // ╔═══🎵 Geyser Sound Compatibility══════════════════════════════════════╗

    /** 🌉 Returns the section for Geyser-specific sound overrides */
    public static ConfigurationSection getGeyserOverrideSection() {
        return config.getConfigurationSection("sounds.geyseroverrides");
    }

    // ╔═══🔌 Plugin Integration Settings═══════════════════════════════════╗

    /** Whether AcceptTheRules integration is enabled */
    public static boolean enableAcceptTheRules() {
        return config.getBoolean("integrations.acceptTheRules", true);
    }

    /** Whether Geyser support is enabled */
    public static boolean enableGeyserSupport() {
        return config.getBoolean("integrations.geyserSupport", true);
    }

    // ╔═══👁️ Hologram Display (DecentHolograms)═══════════════════════════╗

    /** Whether holograms are enabled */
    public static boolean hologramsEnabled() {
        return config.getBoolean("holograms.enabled", true);
    }

    /** Whether hologram visibility requires permission */
    public static boolean hologramsRequirePermission() {
        return config.getBoolean("holograms.requirePermission", true);
    }

    /** Whether holograms are shown by default */
    public static boolean hologramsDefaultEnabled() {
        return config.getBoolean("holograms.defaultEnabled", true);
    }

    /** MiniMessage format string for holograms */
    public static String getHologramFormat() {
        return config.getString("holograms.format", "<gray>Despawn: <despawnTime>s</gray>");
    }

    /** Hologram update interval in ticks */
    public static int getHologramUpdateInterval() {
        return config.getInt("holograms.updateIntervalTicks", 20);
    }

    // ╔═══🪧 PlaceholderAPI════════════════════════════════════════════╗

    /** Whether PlaceholderAPI hook is enabled */
    public static boolean enablePlaceholderAPI() {
        return config.getBoolean("placeholderAPI.enabled", true);
    }

    /** Whether plugin should register its placeholders */
    public static boolean registerPlaceholders() {
        return config.getBoolean("placeholderAPI.registerPlaceholders", true);
    }

    // ╔═══🛡️ Command Permissions═══════════════════════════════════════════╗

    /** Whether permission checks are required for commands */
    public static boolean requireCommandPermissions() {
        return config.getBoolean("commands.requirePermissions", true);
    }

    // ╔═══👋 Welcome Messages═══════════════════════════════════════════════╗

    /** Whether to show welcome message on join */
    public static boolean showWelcomeOnJoin() {
        return config.getBoolean("welcome.enableOnJoin", true);
    }

    /** Whether to show welcome message on difficulty selection */
    public static boolean showWelcomeOnSelection() {
        return config.getBoolean("welcome.enableOnSelection", true);
    }

    // ╔═══💬 Messages & MiniMessage Formatting════════════════════════════╗
    // Messages are stored in messages.yml and loaded separately by MessageManager.

    // ╔═══🐞 Debug Settings═════════════════════════════════════════════════╗

    /** Whether debug info should be logged */
    public static boolean isDebugMode() {
        return config.getBoolean("debugMode", false);
    }

    /** Max online players per page in /pyddebug output */
    public static int getDebugOnlinePlayersPerPage() {
        return plugin.getConfig().getInt("debugEntriesPerPageOnline", 10);
    }

    /** Max stored difficulty entries per page in /pyddebug output */
    public static int getDebugStoredDifficultyPerPage() {
        return plugin.getConfig().getInt("debugEntriesPerPageStored", 10);
    }

    // ╔═══🧪 Developer Mode═════════════════════════════════════════════════╗

    /** Whether to always show GUI in devMode */
    public static boolean devModeAlwaysShow() {
        return config.getBoolean("devMode.alwaysShowGUI", false);
    }

}
