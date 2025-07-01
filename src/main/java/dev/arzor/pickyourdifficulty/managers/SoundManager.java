// ╔════════════════════════════════════════════════════════════════════╗
// ║                        🔊 SoundManager.java                        ║
// ║  Handles sound playback, Geyser overrides, and config volume/pitch ║
// ║  support. Uses SoundUtil for dispatching Bukkit-compatible sounds. ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.utils.SoundUtil;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

// ─────────────────────────────────────────────────────────────
// 🎵 Sound Playback Manager — Handles config + Geyser support
// ─────────────────────────────────────────────────────────────

public class SoundManager {

    // ─────────────────────────────────────────────────────────────
    // 🔊 GUI + Action Sound Triggers
    // ─────────────────────────────────────────────────────────────

    /**
     * 🔊 Plays sound when GUI is opened
     */
    public static void playGuiOpenSound(Player player) {
        playFromConfig(
                player,
                ConfigManager.getGuiOpenSoundKey(),
                ConfigManager.getGuiOpenVolume(),
                ConfigManager.getGuiOpenPitch(),
                Sound.UI_BUTTON_CLICK
        );
    }

    /**
     * 🔊 Plays sound when difficulty is confirmed
     */
    public static void playConfirmSound(Player player) {
        playFromConfig(
                player,
                ConfigManager.getConfirmSoundKey(),
                ConfigManager.getConfirmVolume(),
                ConfigManager.getConfirmPitch(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP
        );
    }

    /**
     * 🔊 Plays sound when action is cancelled
     */
    public static void playCancelSound(Player player) {
        playFromConfig(
                player,
                ConfigManager.getCancelSoundKey(),
                ConfigManager.getCancelVolume(),
                ConfigManager.getCancelPitch(),
                Sound.BLOCK_NOTE_BLOCK_BASS
        );
    }

    /**
     * 🔒 Plays denial sound for cooldown or locked difficulty
     */
    public static void playDeniedSound(Player player, boolean isLocked) {

        // 📦 Choose config values depending on denial reason
        if (isLocked) {
            playFromConfig(
                    player,
                    ConfigManager.getLockedDeniedSoundKey(),
                    ConfigManager.getLockedDeniedVolume(),
                    ConfigManager.getLockedDeniedPitch(),
                    Sound.BLOCK_ANVIL_LAND
            );
        } else {
            playFromConfig(
                    player,
                    ConfigManager.getCooldownDeniedSoundKey(),
                    ConfigManager.getCooldownDeniedVolume(),
                    ConfigManager.getCooldownDeniedPitch(),
                    Sound.ENTITY_VILLAGER_NO
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🚀 Central Sound Dispatch Logic
    // ─────────────────────────────────────────────────────────────

    /**
     * 📦 Plays a sound with optional Geyser-safe fallback
     *
     * @param player   Player to hear the sound
     * @param key      Sound name from config (e.g. "BLOCK_NOTE_BLOCK_BASS")
     * @param volume   Configured volume (0.0 to 1.0+)
     * @param pitch    Configured pitch (0.5 to 2.0 typical)
     * @param fallback Fallback Bukkit sound enum if resolution fails
     */
    public static void playFromConfig(Player player, String key, float volume, float pitch, Sound fallback) {
        Sound resolved = resolveSound(key, fallback);

        // 🌉 Apply override if Geyser support is enabled
        Sound finalSound = getCompatibleSound(resolved);

        // 🎧 Dispatch actual sound to player
        SoundUtil.play(player, finalSound, volume, pitch);
    }

    // ─────────────────────────────────────────────────────────────
    // 🧩 Sound Resolver (String to Sound)
    // ─────────────────────────────────────────────────────────────

    /**
     * Attempts to convert a string into a Bukkit Sound.
     *
     * @param key      Raw string from config
     * @param fallback Default if invalid
     * @return Sound enum (or fallback)
     */
    public static Sound resolveSound(String key, Sound fallback) {
        try {
            // Convert config string to namespaced Minecraft key
            NamespacedKey namespaced = NamespacedKey.minecraft(key.toLowerCase());
            Sound sound = Registry.SOUNDS.get(namespaced);

            // ✅ Return resolved sound if valid
            if (sound != null) return sound;

        } catch (Exception ignored) {
            // 🧼 Fail silently if badly formatted
        }

        // ⚠ Warn and fall back
        PickYourDifficulty.getInstance().getLogger().warning("⚠ Invalid sound name in config: '" + key + "'");
        return fallback;
    }

    // ─────────────────────────────────────────────────────────────
    // 🌉 Geyser Compatibility
    // ─────────────────────────────────────────────────────────────

    /**
     * Applies Bedrock-compatible override if enabled
     *
     * @param original Java Edition sound
     * @return Possibly overridden Bedrock-safe sound
     */
    public static Sound getCompatibleSound(Sound original) {
        if (!ConfigManager.enableGeyserSupport()) return original;

        // Extract namespaced key of the original sound
        NamespacedKey key = Registry.SOUNDS.getKey(original);

        // Look up override from config map
        String overrideName = (key != null) ? getGeyserSoundOverrides().get(key.value().toUpperCase()) : null;

        // ❌ No override defined — fallback to original
        if (overrideName == null) return original;

        try {
            NamespacedKey overrideKey = NamespacedKey.minecraft(overrideName.toLowerCase());
            Sound override = Registry.SOUNDS.get(overrideKey);

            if (override != null) return override;

        } catch (Exception e) {
            PickYourDifficulty.getInstance().getLogger().warning("⚠ Invalid Geyser override sound: " + overrideName);
        }

        return original;
    }

    // ─────────────────────────────────────────────────────────────
    // 🗺️ Geyser Override Map Reader
    // ─────────────────────────────────────────────────────────────

    /**
     * Reads geyserOverrides section from config and builds lookup map
     *
     * @return Map of UPPERCASE Java sound → Bedrock-safe override
     */
    public static Map<String, String> getGeyserSoundOverrides() {
        Map<String, String> overrides = new HashMap<>();

        var section = ConfigManager.getGeyserOverrideSection();
        if (section != null) {
            for (String javaSound : section.getKeys(false)) {
                String override = section.getString(javaSound);

                // ✅ Only add valid entries
                if (override != null && !override.isEmpty()) {
                    overrides.put(javaSound.toUpperCase(), override.toUpperCase());
                }
            }
        }

        return overrides;
    }
}