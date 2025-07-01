// ╔════════════════════════════════════════════════════════════════════╗
// ║                  💀 DeathDropListener.java                         ║
// ║  Tags death-related item drops so they can be tracked or handled   ║
// ║  differently (e.g., despawn timing, holograms)                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.HologramManager;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class DeathDropListener implements Listener {

    // ─────────────────────────────────────────────────────────────
    // 🔑 Persistent Data Key for Marking Death Drops
    // ─────────────────────────────────────────────────────────────
    private static final NamespacedKey DEATH_DROP_KEY =
            new NamespacedKey(PickYourDifficulty.getInstance(), "deathdrop");

    // ─────────────────────────────────────────────────────────────
    // ⚰️ Handle Player Death Event
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        // 📛 Skip if despawnOnlyAffectsDeathDrops is false in config
        if (!ConfigManager.despawnOnlyAffectsDeathDrops()) return;

        // 👤 Get player who died
        Player player = event.getEntity();

        // 📦 Lookup difficulty for this player
        PlayerDifficultyStorage storage = PickYourDifficulty.getInstance().getPlayerDifficultyStorage();
        String difficulty = storage.getDifficulty(player);

        // ⏱️ Fetch despawn time for this difficulty
        int despawnSeconds = ConfigManager.getDespawnTime(difficulty);

        // ╔═══⏳ Delay tagging by 1 tick to ensure dropped items exist══════════════════════════════════╗
        Bukkit.getScheduler().runTaskLater(PickYourDifficulty.getInstance(), () -> {

            // 🔍 Scan for item entities in the world (only class Item)
            for (Item itemEntity : player.getWorld().getEntitiesByClass(Item.class)) {

                ItemStack stack = itemEntity.getItemStack();

                // 📍 Check if the item is near the death location (distance² ≤ 4)
                if (itemEntity.getLocation().distanceSquared(player.getLocation()) > 4) continue;

                // 🧮 TicksLived > 5 means item existed before death — skip it
                if (itemEntity.getTicksLived() > 5) continue;

                // 🏷️ Mark the item as a death drop using persistent data
                itemEntity.getPersistentDataContainer().set(DEATH_DROP_KEY, PersistentDataType.INTEGER, 1);

                // 📣 Optional debug logging
                if (ConfigManager.isDebugMode()) {
                    PickYourDifficulty.getInstance().getLogger().info("[PickYourDifficulty] Tagged deathdrop: "
                            + stack.getAmount() + "x " + stack.getType() + " from " + player.getName()
                            + " (Despawn in " + despawnSeconds + "s)");
                }

                // 🪧 Spawn a hologram above the item if enabled
                if (ConfigManager.hologramsEnabled()) {
                    HologramManager.createHologram(itemEntity, despawnSeconds);
                }
            }
        }, 1L); // 🧮 Delay 1 tick = 1/20th of a second to let items finish dropping
    }

    // ─────────────────────────────────────────────────────────────
    // 🏷️ Check If an Item is Tagged as a Death Drop
    // ─────────────────────────────────────────────────────────────
    public static boolean isDeathDrop(Item item) {
        return item.getPersistentDataContainer().has(DEATH_DROP_KEY, PersistentDataType.INTEGER);
    }
}
