// ╔════════════════════════════════════════════════════════════════════╗
// ║                         🎧 SoundUtil.java                          ║
// ║  Centralized helper for playing sounds with volume and pitch       ║
// ║  Used to standardize sound behavior across plugin actions          ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    // ─────────────────────────────────────────────────────────────
    // 🔊 Sound Playback Utility
    // ─────────────────────────────────────────────────────────────

    /**
     * Plays a sound to the player with customizable volume and pitch.
     *
     * @param player The player to play the sound to
     * @param sound  The Bukkit Sound enum (e.g., BLOCK_NOTE_BLOCK_PLING)
     * @param volume Volume multiplier (1.0f = normal volume)
     * @param pitch  Pitch multiplier (1.0f = normal pitch)
     */
    public static void play(Player player, Sound sound, float volume, float pitch) {
        // 💬 Play the specified sound at the player's current location
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
