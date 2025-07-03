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
// 🎵 SoundManager — Handles config sound playback + Geyser support
// ─────────────────────────────────────────────────────────────
public class SoundManager {

    // ╔═══🔊 GUI + Action Sound Triggers════════════════════════════════╗

    public static void playGuiOpenSound(Player player) {
        // 📦 Plays GUI Open sound (e.g. Button Click)
        playFromConfig(
                player,
                ConfigManager.getGuiOpenSoundKey(),
                ConfigManager.getGuiOpenVolume(),
                ConfigManager.getGuiOpenPitch(),
                Sound.UI_BUTTON_CLICK
        );
    }

    public static void playConfirmSound(Player player) {
        // 📦 Plays confirmation sound (e.g. EXP Pickup)
        playFromConfig(
                player,
                ConfigManager.getConfirmSoundKey(),
                ConfigManager.getConfirmVolume(),
                ConfigManager.getConfirmPitch(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP
        );
    }

    public static void playCancelSound(Player player) {
        // 📦 Plays cancel/back sound (e.g. low-pitch bass)
        playFromConfig(
                player,
                ConfigManager.getCancelSoundKey(),
                ConfigManager.getCancelVolume(),
                ConfigManager.getCancelPitch(),
                Sound.BLOCK_NOTE_BLOCK_BASS
        );
    }

    public static void playDeniedSound(Player player, boolean isLocked) {
        // 📦 Plays either locked-denied or cooldown-denied sound
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

    // ╔═══🚀 Central Sound Dispatch Logic═══════════════════════════════╗

    public static void playFromConfig(Player player, String key, float volume, float pitch, Sound fallback) {
        // 🎯 Try to resolve the sound from config
        Sound resolved = resolveSound(key, fallback);

        // 🌉 Apply override if Geyser support is enabled
        Sound finalSound = getCompatibleSound(resolved);

        // 🧪 Debug: Log playback details
        PickYourDifficulty.debug("🎵 Playing sound for " + player.getName()
                + " → '" + finalSound + "' (volume: " + volume + ", pitch: " + pitch + ")");

        // 🎧 Actually dispatch the sound to the player
        SoundUtil.play(player, finalSound, volume, pitch);
    }

    // ╔═══🧩 Sound Resolver (Config Key → Sound Enum)════════════════════╗

    public static Sound resolveSound(String key, Sound fallback) {

        // 🧼 Exit early if blank or null, use the fallback immediately
        if (key == null || key.isBlank()) return fallback;

        try {
            // 🧪 Log resolution attempt
            PickYourDifficulty.debug("🔍 Resolving sound from config: '" + key + "'");

            // 🧼 Normalize to lowercase and trim extra spaces
            // ⚠ Sound keys in Registry are always lowercase (e.g. "block.note_block.pling")
            NamespacedKey namespacedKey = NamespacedKey.minecraft(key.trim().toLowerCase());

            // 🗂️ Look up the Sound from Bukkit's registry
            Sound resolved = Registry.SOUNDS.get(namespacedKey);

            // ✅ Return resolved sound if found
            if (resolved != null) return resolved;

        } catch (Exception ex) {
            // 🧼 Ignore any malformed keys or unknown sound formats
        }

        // ⚠ If resolution failed, warn and use fallback
        PickYourDifficulty.getInstance().getLogger().warning("⚠ Invalid sound name in config: '" + key + "'");
        return fallback;
    }


    // ╔═══🌉 Geyser Compatibility Override═══════════════════════════════╗

    public static Sound getCompatibleSound(Sound original) {
        // 💬 If Geyser support is disabled, return sound unchanged
        if (!ConfigManager.enableGeyserSupport()) return original;

        // 📦 Get the registry key of the original sound
        NamespacedKey key = Registry.SOUNDS.getKey(original);

        // 🔎 Look for override mapping based on config map (uppercased)
        String overrideName = (key != null) ? getGeyserSoundOverrides().get(key.value().toUpperCase()) : null;

        // ❌ No override defined — fallback to original
        if (overrideName == null) return original;

        try {
            // 🧼 Normalize override and resolve it
            NamespacedKey overrideKey = NamespacedKey.minecraft(overrideName.toLowerCase());
            Sound override = Registry.SOUNDS.get(overrideKey);

            if (override != null) {
            // 🧪 Log override applied
                PickYourDifficulty.debug("🌉 Geyser override applied: '" + key.value() + "' → '" + overrideName + "'");
                return override;
            }

        } catch (Exception e) {
            // ⚠ Log issue with override name
            PickYourDifficulty.getInstance().getLogger().warning("⚠ Invalid Geyser override sound: " + overrideName);
        }

        // 🔁 Fall back to original if resolution failed
        return original;
    }

    // ╔═══🗺️ Geyser Sound Override Map═══════════════════════════════════╗

    public static Map<String, String> getGeyserSoundOverrides() {
        Map<String, String> overrides = new HashMap<>();

        var section = ConfigManager.getGeyserOverrideSection();
        if (section != null) {
            for (String javaSound : section.getKeys(false)) {
                String override = section.getString(javaSound);

                // ✅ Only include valid, non-empty values
                if (override != null && !override.isEmpty()) {
                    // 💬 Store keys in UPPERCASE to avoid case sensitivity issues
                    overrides.put(javaSound.toUpperCase(), override.toUpperCase());
                }
            }

            // 🧪 Log count of loaded overrides
            PickYourDifficulty.debug("🎧 Loaded " + overrides.size() + " Geyser sound overrides");
        }

        return overrides;
    }
}