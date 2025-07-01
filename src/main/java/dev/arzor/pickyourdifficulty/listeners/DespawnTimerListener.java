// ╔════════════════════════════════════════════════════════════════════╗
// ║                  ⏱️ DespawnTimerListener.java                      ║
// ║    Sets custom despawn timers for item entities based on           ║
// ║    player difficulty — supports both deathdrops and manual drops   ║
// ║    using PersistentDataContainer and tagging systems               ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.UUID;

public class DespawnTimerListener implements Listener {

    private final PlayerDifficultyStorage difficultyStorage;

    // ⏳ Default vanilla despawn timer is 6000 ticks = 5 minutes
    private static final int DEFAULT_DESPAWN_TICKS = 6000;

    public DespawnTimerListener(PlayerDifficultyStorage difficultyStorage) {
        this.difficultyStorage = difficultyStorage;
    }

    // ─────────────────────────────────────────────────────────────
    // 🎯 Apply Custom Despawn Timers When Items Spawn
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {

        // 📦 Grab the item entity and its stack
        Item itemEntity = event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();

        // 📌 Check for deathdrop tag or metadata-based despawn time
        boolean isDeathDrop = DeathDropListener.isDeathDrop(itemEntity);
        int taggedDespawn = PlayerDropItemListener.getSavedDespawnSeconds(itemEntity);

        // 📦 Mini Block: Skip if config restricts to deathdrops only
        if (ConfigManager.despawnOnlyAffectsDeathDrops() && !isDeathDrop && taggedDespawn <= 0) {
            return;
        }

        int customSeconds;

        // 📦 Mini Block: Use manually tagged time if present
        if (taggedDespawn > 0) {
            customSeconds = taggedDespawn;
        } else {
            // 📦 Mini Block: Fallback — use dropper's difficulty
            UUID dropperUuid = getDropperUuid(itemEntity);
            if (dropperUuid == null) return;

            String difficulty = difficultyStorage.getDifficulty(Bukkit.getOfflinePlayer(dropperUuid));
            customSeconds = ConfigManager.getDespawnTime(difficulty);
        }

        // 🧮 Convert seconds to ticks (1 second = 20 ticks)
        int customTicks = customSeconds * 20;

        // 🚫 Do not allow reducing timer below vanilla unless config allows
        if (ConfigManager.preventDespawnTimerDowngrade() && customTicks < DEFAULT_DESPAWN_TICKS) {
            return;
        }

        // 🧠 Trick: Setting negative age offsets despawn logic to start from customTicks
        itemEntity.setTicksLived(-customTicks);

        // 🧪 Log the change if debug is enabled
        if (ConfigManager.isDebugMode()) {
            PickYourDifficulty.getInstance().getLogger().info("[PickYourDifficulty] Custom despawn: "
                    + itemStack.getAmount() + "x " + itemStack.getType()
                    + " → " + customSeconds + "s");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🧩 Extract Dropper UUID from Metadata (if available)
    // ─────────────────────────────────────────────────────────────
    private UUID getDropperUuid(Item item) {

        // 📦 Look for metadata tag created by PlayerDropItemListener or others
        if (item.hasMetadata("pyd_dropper")) {
            for (MetadataValue value : item.getMetadata("pyd_dropper")) {
                if (value.value() instanceof UUID uuid) {
                    return uuid;
                }
            }
        }

        return null;
    }
}