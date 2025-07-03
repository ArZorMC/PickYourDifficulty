// ╔════════════════════════════════════════════════════════════════════╗
// ║                 🧩 DifficultyPlaceholder.java                      ║
// ║  PlaceholderAPI expansion for %pickyourdifficulty_*% variables     ║
// ║  Supports: difficulty, despawn_seconds, grace_seconds              ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.placeholders;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// ─────────────────────────────────────────────────────────────
// 🧭 Placeholder Expansion: Registers dynamic PAPI variables
// ─────────────────────────────────────────────────────────────
public class DifficultyPlaceholder extends PlaceholderExpansion {

    // ╔═══📦 Dependency: Player Data Access═════════════════════════════╗
    private final PlayerDataManager playerDataManager;

    // ─────────────────────────────────────────────────────────────
    // 🏗️ Constructor
    // ─────────────────────────────────────────────────────────────
    public DifficultyPlaceholder(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 🏷️ Placeholder Expansion Metadata
    // ─────────────────────────────────────────────────────────────

    @Override
    public @NotNull String getIdentifier() {
        return "pickyourdifficulty"; // Prefix for all %pickyourdifficulty_*% variables
    }

    @Override
    public @NotNull String getAuthor() {
        return "ArZor";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // 🔁 Don’t unregister on PlaceholderAPI reload
    }

    // ─────────────────────────────────────────────────────────────
    // 🔄 Placeholder Request Handler
    // ─────────────────────────────────────────────────────────────
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {

        // ❌ Reject null or offline players
        if (player == null || !player.isOnline()) return "";

        // 💬 Get Player instance (safe cast)
        Player online = player.getPlayer();
        if (online == null) return ""; // Defensive null check

        // 📥 Load stored difficulty for this player
        String difficulty = playerDataManager.getDifficultyStorage().getDifficulty(online);
        boolean usedFallback = false;

        // ⛑ Use fallback if difficulty not found
        if (difficulty == null) {
            difficulty = ConfigManager.getFallbackDifficulty();
            usedFallback = true;
        }

        // 🧪 Log resolved difficulty and fallback usage
        PickYourDifficulty.debug("📦 PlaceholderAPI → " + online.getName()
                + " resolved difficulty = " + difficulty + (usedFallback ? " (fallback used)" : ""));

        // 🧩 Process each supported placeholder
        return switch (identifier.toLowerCase()) {

            // ╔═══📛 %pickyourdifficulty_difficulty%════════════════════════════════════╗
            case "difficulty" -> {
                // 🧪 Log request
                PickYourDifficulty.debug("📛 Resolving placeholder: %difficulty% → " + difficulty);
                yield difficulty;
            }

            // ╔═══⏱️ %pickyourdifficulty_despawn_seconds%══════════════════════════════╗
            case "despawn_seconds" -> {
                // ⏱ Get despawn time in seconds for this difficulty
                int seconds = ConfigManager.getDespawnTime(difficulty);
                PickYourDifficulty.debug("⏱️ Resolving placeholder: %despawn_seconds% → " + seconds);
                yield String.valueOf(seconds);
            }

            // ╔═══🛡️ %pickyourdifficulty_grace_seconds%════════════════════════════════╗
            case "grace_seconds" -> {
                // 🛡 Get grace time in seconds for this difficulty
                int seconds = ConfigManager.getGraceTime(difficulty);
                PickYourDifficulty.debug("🛡️ Resolving placeholder: %grace_seconds% → " + seconds);
                yield String.valueOf(seconds);
            }

            // ╔═══❓ Unknown placeholder═══════════════════════════════════════════════╗
            default -> {
                PickYourDifficulty.debug("❓ Unknown placeholder requested: " + identifier);
                yield null;
            }
        };
    }
}