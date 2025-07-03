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

// ─────────────────────────────────────────────────────────────
// 🧩 DespawnTimerListener — Customizes item despawn time
// ─────────────────────────────────────────────────────────────
// This listener sets a custom despawn timer on items that spawn
// into the world. It supports two pathways:
//
// - 🪦 Death drops → Tagged via DeathDropListener
// - 🎯 Manual drops → Tracked via PlayerDropItemListener
//
// ✅ Custom timers are based on player difficulty
// ⏳ Uses ticksLived offset trick to set despawn logic
// 🔒 Optionally prevents downgrading below vanilla 6000 ticks
public class DespawnTimerListener implements Listener {

    // 🧠 Storage system that maps player UUIDs to difficulty levels
    private final PlayerDifficultyStorage difficultyStorage;

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

        // 🧪 Debug: Show item spawn info
        PickYourDifficulty.debug("ItemSpawnEvent: " + itemStack.getAmount() + "x " + itemStack.getType());

        // 📌 Check for deathdrop tag or metadata-based despawn time
        boolean isDeathDrop = DeathDropListener.isDeathDrop(itemEntity);
        int taggedDespawn = PlayerDropItemListener.getSavedDespawnSeconds(itemEntity);
        long previousPickup = PlayerDropItemListener.getSavedPickupTime(itemEntity);

        // 🧪 Debug: Deathdrop & tagged info
        PickYourDifficulty.debug(" - isDeathDrop: " + isDeathDrop + ", taggedDespawn: " + taggedDespawn + ", pickupTime: " + previousPickup);

        // 📛 Skip if config restricts to deathdrops only, and this item is neither
        if (ConfigManager.despawnOnlyAffectsDeathDrops() && !isDeathDrop && taggedDespawn <= 0) {
            PickYourDifficulty.debug(" - Skipped: Not a deathdrop and no manual tag, config restricts.");
            return;
        }

        // ╔═══⏲️ Determine Despawn Time═════════════════════════════════════════════════════════════╗

        int customSeconds;

        // 📦 Use manually tagged time if present
        if (taggedDespawn > 0) {
            customSeconds = taggedDespawn;
            PickYourDifficulty.debug(" - Using manually tagged despawn time: " + customSeconds + "s");

        } else {
            // 📦 Otherwise, fall back to dropper's difficulty
            UUID dropperUuid = getDropperUuid(itemEntity);

            // ❌ If no UUID, skip
            if (dropperUuid == null) {
                PickYourDifficulty.debug(" - Skipped: No dropper UUID found in metadata.");
                return;
            }

            // 🧠 Lookup dropper's difficulty and get their custom despawn time
            String difficulty = difficultyStorage.getDifficulty(Bukkit.getOfflinePlayer(dropperUuid));
            customSeconds = ConfigManager.getDespawnTime(difficulty);

            PickYourDifficulty.debug(" - Using dropper difficulty '" + difficulty + "' → " + customSeconds + "s");
        }

        // ╔═══🧮 Convert Seconds to Ticks════════════════════════════════════════════════════════════╗

        // 🧮 Convert seconds to ticks (1 second = 20 ticks)
        int customTicks = customSeconds * 20;

        // ╔═══🔐 Downgrade Protection Logic═════════════════════════════════════════════════════════╗

        if (ConfigManager.preventDespawnTimerDowngrade()) {

            // 🕓 Time since previous pickup (in milliseconds)
            long now = System.currentTimeMillis();
            // ⛔ Skip downgrade: Item was held recently AND had a longer timer applied
            long heldMillis = (previousPickup > 0) ? now - previousPickup : Long.MAX_VALUE;

            // 🧮 Convert threshold to milliseconds
            long thresholdMillis = ConfigManager.ownershipTransferThresholdSeconds() * 1000L;

            // ✅ Ownership has not yet transferred — treat previous timer as protected
            if (heldMillis < thresholdMillis && itemEntity.getTicksLived() < 0) {
                PickYourDifficulty.debug(" - Skipped: Preventing downgrade, held for only " + heldMillis + "ms < " + thresholdMillis + "ms");
                return;
            }
        }

        // 🧠 Trick: Setting a negative age offsets internal despawn countdown
        // This effectively resets the despawn time to customTicks
        itemEntity.setTicksLived(-customTicks);

        // 📣 Log result
        PickYourDifficulty.debug(" - Custom despawn timer applied: " + customTicks + " ticks (" + customSeconds + "s)");

        // 🧪 Optional verbose log to console
        PickYourDifficulty.debug("Custom despawn: " + itemStack.getAmount() + "x " + itemStack.getType()
                + " → " + customSeconds + "s");
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

        // ❌ No valid UUID found
        return null;
    }
}