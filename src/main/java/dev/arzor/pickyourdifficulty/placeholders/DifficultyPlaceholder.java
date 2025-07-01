// ╔════════════════════════════════════════════════════════════════════╗
// ║                 🧩 DifficultyPlaceholder.java                      ║
// ║  PlaceholderAPI expansion for %pickyourdifficulty_*% variables     ║
// ║  Supports: difficulty, despawn_seconds, grace_seconds              ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.placeholders;

import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 📦 Handles dynamic placeholder values like:
 *   - %pickyourdifficulty_difficulty%
 *   - %pickyourdifficulty_despawn_seconds%
 *   - %pickyourdifficulty_grace_seconds%
 */
public class DifficultyPlaceholder extends PlaceholderExpansion {

    // 🔗 Link to player data manager (for difficulty queries)
    private final PlayerDataManager playerDataManager;

    // ─────────────────────────────────────────────────────────────
    // 🏗️ Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Injects the PlayerDataManager used for difficulty lookups.
     *
     * @param playerDataManager Reference to active data manager
     */
    public DifficultyPlaceholder(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 📛 Expansion Metadata
    // ─────────────────────────────────────────────────────────────

    @Override
    public @NotNull String getIdentifier() {
        return "pickyourdifficulty"; // Root name for all placeholders
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
    // 🧠 Placeholder Resolver
    // ─────────────────────────────────────────────────────────────

    /**
     * Resolves placeholder values per player.
     *
     * @param player     OfflinePlayer to query
     * @param identifier The placeholder name (e.g., "difficulty")
     * @return The resolved string or null if unrecognized
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null || !player.isOnline()) return "";

        // ✅ Ensure player is online before casting
        Player online = player.getPlayer();
        if (online == null) return ""; // Defensive null check

        // 📥 Fetch difficulty (or fallback if missing)
        String difficulty = playerDataManager.getDifficultyStorage().getDifficulty(online);
        if (difficulty == null) {
            difficulty = ConfigManager.getFallbackDifficulty();
        }

        // 📦 Map placeholders to data
        return switch (identifier.toLowerCase()) {
            case "difficulty" -> difficulty;

            case "despawn_seconds" ->
                    String.valueOf(ConfigManager.getDespawnTime(difficulty));

            case "grace_seconds" ->
                    String.valueOf(ConfigManager.getGraceTime(difficulty));

            default -> null; // ❓ Unknown placeholder
        };
    }
}