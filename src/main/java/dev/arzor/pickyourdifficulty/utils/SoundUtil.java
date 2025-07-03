// ╔════════════════════════════════════════════════════════════════════╗
// ║                         🎧 SoundUtil.java                          ║
// ║  Centralized helper for playing sounds with volume and pitch       ║
// ║  Used to standardize sound behavior across plugin actions          ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.utils;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

// ─────────────────────────────────────────────────────────────
// 🔊 SoundUtil — Simplified sound playback
// ─────────────────────────────────────────────────────────────
public class SoundUtil {

    /// ╔═══📢 play() — Play a sound to a player═══════════════════════════╗

    // 💬 Plays a sound at the player's current location with given volume and pitch
    public static void play(Player player, Sound sound, float volume, float pitch) {

        // 🔉 Actually play the sound to the player
        player.playSound(player.getLocation(), sound, volume, pitch);

        // 🧪 Debug: log the sound name, volume, and pitch
        // 🧠 Note: sound.toString() gives safe enum name like "BLOCK_ANVIL_LAND"
        PickYourDifficulty.debug("🔊 Played sound to " + player.getName()
                + " → " + sound + " | vol=" + volume + " | pitch=" + pitch);
    }
}
