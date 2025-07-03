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

// ─────────────────────────────────────────────────────────────
// 🧩 DeathDropListener — Tags drops near player death
// ─────────────────────────────────────────────────────────────
// This listener tracks item drops caused by player death and tags them
// using persistent data. This tag allows special behavior like:
// - Configurable despawn time
// - Holograms showing item timers
// - Filtering vs. normal dropped items
//
// ⚙️ Tagging is only applied if enabled in config.
// 🧼 Tagging is delayed by 1 tick to ensure the items have spawned.
// 🧪 Only items nearby the death location and recently spawned are tagged.
public class DeathDropListener implements Listener {

    // ─────────────────────────────────────────────────────────────
    // 🔑 Persistent Data Key for Marking Death Drops
    // ─────────────────────────────────────────────────────────────

    // This key will mark an item as having come from a player death
    private static final NamespacedKey DEATH_DROP_KEY =
            new NamespacedKey(PickYourDifficulty.getInstance(), "deathdrop");

    // ─────────────────────────────────────────────────────────────
    // ⚰️ Handle Player Death Event
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        // ╔═══⚙️ Config Check══════════════════════════════════════════════════════════════════════╗
        // Skip tagging logic entirely if config disables tracking death-only drops
        if (!ConfigManager.despawnOnlyAffectsDeathDrops()) {
            PickYourDifficulty.debug("DeathDropListener: Skipping tagging — config disables it.");
            return;
        }

        // ╔═══👤 Player and Difficulty══════════════════════════════════════════════════════════════╗

        // 👤 Get player who just died
        Player player = event.getEntity();

        // 📦 Lookup difficulty for this player
        PlayerDifficultyStorage storage = PickYourDifficulty.getInstance().getPlayerDifficultyStorage();
        String difficulty = storage.getDifficulty(player);

        // ⏱️ Fetch despawn time for this difficulty
        int despawnSeconds = ConfigManager.getDespawnTime(difficulty);
        PickYourDifficulty.debug("DeathDropListener: " + player.getName() + " died with difficulty '" + difficulty +
                "' (despawn = " + despawnSeconds + "s)");

        // ╔═══⏳ Delay to Let Drops Spawn═══════════════════════════════════════════════════════════╗

        // We delay by 1 tick to ensure item drops have actually appeared in the world
        Bukkit.getScheduler().runTaskLater(PickYourDifficulty.getInstance(), () -> {

            int taggedCount = 0;     // ✅ Items tagged successfully
            int skippedFar = 0;      // 🚫 Items too far from player
            int skippedOld = 0;      // 🚫 Items that existed before the death

            // ╔═══🔍 Scan for Nearby Items═══════════════════════════════════════════════════════════╗

            for (Item itemEntity : player.getWorld().getEntitiesByClass(Item.class)) {
                ItemStack stack = itemEntity.getItemStack();

                // 📏 Only tag items within 8 blocks of death location (distance² ≤ 64)
                double distanceSq = itemEntity.getLocation().distanceSquared(player.getLocation());

                // 🧮 Use square distance to avoid sqrt computation (performance win)
                if (distanceSq > 64) {
                    skippedFar++;
                    continue;
                }

                // ⏱️ Ignore items that have existed more than 5 ticks (likely not part of this death)
                if (itemEntity.getTicksLived() > 5) {
                    skippedOld++;
                    continue;
                }

                // ╔═══🏷️ Mark the Item as Death Drop═════════════════════════════════════════════════╗

                itemEntity.getPersistentDataContainer().set(DEATH_DROP_KEY, PersistentDataType.INTEGER, 1);
                taggedCount++;

                PickYourDifficulty.debug("Tagged deathdrop: " + stack.getAmount() + "x " + stack.getType());

                // ╔═══🪧 Create Hologram (Optional)═══════════════════════════════════════════════════╗

                if (ConfigManager.hologramsEnabled()) {
                    PickYourDifficulty.debug("Spawning hologram for item: " + stack.getType());
                    HologramManager.createHologram(itemEntity, despawnSeconds);
                }
            }

            // ╔═══📊 Debug Summary═══════════════════════════════════════════════════════════════════╗

            if (ConfigManager.isDebugMode()) {
                PickYourDifficulty.getInstance().getLogger().info("[DEBUG] DeathDrop tagging summary for " + player.getName()
                        + " — Tagged: " + taggedCount + ", Skipped (far): " + skippedFar + ", Skipped (old): " + skippedOld);
            }

        }, 1L); // 🧮 Delay 1 tick = 1/20th of a second to let items finish dropping
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 Public Accessor: Check If Item Was Death Drop
    // ─────────────────────────────────────────────────────────────

    public static boolean isDeathDrop(Item item) {
        return item.getPersistentDataContainer().has(DEATH_DROP_KEY, PersistentDataType.INTEGER);
    }
}
