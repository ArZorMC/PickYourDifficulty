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

// ─────────────────────────────────────────────────────────────
// 📤 PlayerDropItemListener — Tags manual drops for despawn
// ─────────────────────────────────────────────────────────────
// This listener handles:
//  • Player Q-key or drag-and-drop item drops
//  • Applies custom despawn timers based on difficulty
//  • Creates holograms if enabled in config
//  • Skips processing if set to death-drops only
public class PlayerDropItemListener implements Listener {

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║        🏷️ Persistent Data Key — Tracks Despawn Timer on Items     ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private static final NamespacedKey DESPAWN_SECONDS_KEY =
            new NamespacedKey(PickYourDifficulty.getInstance(), "manualdrop_despawn");

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║        🕓 Persistent Data Key — Tracks Last Pickup Timestamp       ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private static final NamespacedKey PICKUP_TIME_KEY =
            new NamespacedKey(PickYourDifficulty.getInstance(), "pickup_time");

    // ─────────────────────────────────────────────────────────────
    // 📥 Player Drop Listener — Fired when a player manually drops
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();

        // 🧪 Debug: Event triggered
        PickYourDifficulty.debug("PlayerDropItemEvent triggered by " + player.getName());

        // 📦 Mini Block: Respect config if drops are limited to deaths only
        if (ConfigManager.despawnOnlyAffectsDeathDrops()) {
            PickYourDifficulty.debug("Manual drop ignored (config restricts to death drops only).");
            return;
        }

        // 🛑 Skip if no difficulty is set for this player
        String difficulty = PickYourDifficulty.getInstance().getPlayerDifficultyStorage().getDifficulty(player);
        if (difficulty == null) {
            PickYourDifficulty.debug("Drop ignored — no difficulty set for " + player.getName());
            return;
        }

        // 🧮 Lookup despawn time for selected difficulty
        int despawnSeconds = ConfigManager.getDespawnTime(difficulty);

        // 🧪 Debug: Show time being applied
        PickYourDifficulty.debug("Applying despawn time of " + despawnSeconds + "s for difficulty '" + difficulty + "'");

        // 🏷️ Save despawn time to item metadata (for tracking)
        droppedItem.getPersistentDataContainer().set(
                DESPAWN_SECONDS_KEY,
                PersistentDataType.INTEGER,
                despawnSeconds
        );

        // 🧪 Debug: Show final applied drop info
        PickYourDifficulty.debug("Manual drop: "
                + droppedItem.getItemStack().getAmount() + "x " + droppedItem.getItemStack().getType()
                + " from " + player.getName() + " (Despawn in " + despawnSeconds + "s)");

        // 🪧 Show hologram if enabled globally
        if (ConfigManager.hologramsEnabled()) {
            HologramManager.createHologram(droppedItem, despawnSeconds);
            PickYourDifficulty.debug("Hologram created above dropped item for " + player.getName());
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

    // ─────────────────────────────────────────────────────────────
    // 🕓 Retrieve the previous pickup time from item metadata
    // ─────────────────────────────────────────────────────────────
    // Returns the timestamp (in ms) when the item was last dropped
    // Returns 0 if no value is found or invalid
    public static long getSavedPickupTime(Item item) {
        if (item.getPersistentDataContainer().has(PICKUP_TIME_KEY, PersistentDataType.LONG)) {
            Long timestamp = item.getPersistentDataContainer().get(PICKUP_TIME_KEY, PersistentDataType.LONG);
            return (timestamp != null) ? timestamp : 0L;
        }
        return 0L;
    }
}
