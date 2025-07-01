// ╔════════════════════════════════════════════════════════════════════╗
// ║             📤 PlayerDropItemListener.java                         ║
// ║    Listens for player manual item drops (Q key or drag/drop)       ║
// ║    Attaches holograms if enabled and not restricted by config      ║
// ║    Skips drops if config is limited to death drops only            ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.HologramManager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlayerDropItemListener implements Listener {

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║        🏷️ Persistent Data Key — Tracks Despawn Timer on Items     ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private static final NamespacedKey DESPAWN_SECONDS_KEY =
            new NamespacedKey(PickYourDifficulty.getInstance(), "manualdrop_despawn");

    // ─────────────────────────────────────────────────────────────
    // 📥 Player Drop Listener — Fired when a player manually drops
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();

        // 📦 Mini Block: Respect config if drops are limited to deaths only
        if (ConfigManager.despawnOnlyAffectsDeathDrops()) return;

        // 🛑 Skip if no difficulty is set for this player
        String difficulty = PickYourDifficulty.getInstance().getPlayerDifficultyStorage().getDifficulty(player);
        if (difficulty == null) return;

        // 🧮 Lookup despawn time for selected difficulty
        int despawnSeconds = ConfigManager.getDespawnTime(difficulty);

        // 🏷️ Save despawn time to item metadata (for tracking)
        droppedItem.getPersistentDataContainer().set(
                DESPAWN_SECONDS_KEY,
                PersistentDataType.INTEGER,
                despawnSeconds
        );

        // 🧪 Optional debug log
        if (ConfigManager.isDebugMode()) {
            PickYourDifficulty.getInstance().getLogger().info("[PickYourDifficulty] Manual drop: "
                    + droppedItem.getItemStack().getAmount() + "x " + droppedItem.getItemStack().getType()
                    + " from " + player.getName() + " (Despawn in " + despawnSeconds + "s)");
        }

        // 🪧 Show hologram if enabled globally
        if (ConfigManager.hologramsEnabled()) {
            HologramManager.createHologram(droppedItem, despawnSeconds);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🧾 Public Accessor — Read Persisted Despawn Seconds
    // ─────────────────────────────────────────────────────────────
    public static int getSavedDespawnSeconds(Item item) {
        // 🧼 Default value is -1 if not tagged
        return item.getPersistentDataContainer().getOrDefault(
                DESPAWN_SECONDS_KEY,
                PersistentDataType.INTEGER,
                -1
        );
    }
}
