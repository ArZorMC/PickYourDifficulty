// ╔════════════════════════════════════════════════════════════════════╗
// ║                   🎒 ItemPickupListener.java                       ║
// ║   Removes holograms when tracked items are picked up by players    ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.managers.HologramManager;

import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class ItemPickupListener implements Listener {

    // ─────────────────────────────────────────────────────────────
    // 🎒 Item Pickup Handler — Clean Up Holograms
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {

        // 📦 Mini Block: Get the item being picked up
        Item item = event.getItem();

        // 🧹 Remove the associated hologram, if one exists
        HologramManager.removeHologram(item);
    }
}
