// ╔════════════════════════════════════════════════════════════════════╗
// ║                   🎒 ItemPickupListener.java                       ║
// ║   Removes holograms when tracked items are picked up by players    ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.HologramManager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.persistence.PersistentDataType;

// ─────────────────────────────────────────────────────────────
// 🎒 ItemPickupListener — Cleans up holograms on pickup
// ─────────────────────────────────────────────────────────────
// This listener handles:
//  • Removing DecentHolograms from item drops once picked up
//  • Saving a pickup timestamp to PersistentDataContainer
//  • Optional debug logs when debug mode is enabled
public class ItemPickupListener implements Listener {

    // ─────────────────────────────────────────────────────────────
    // 🎒 Item Pickup Handler — Clean Up Holograms
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {

        // 📦 Get the item entity being picked up
        Item item = event.getItem();

        // 🧹 Remove the associated hologram, if one exists
        HologramManager.removeHologram(item);

        // 🕓 Save the current time as the last pickup timestamp
        item.getPersistentDataContainer().set(
                new NamespacedKey(PickYourDifficulty.getInstance(), "pickup_time"),
                PersistentDataType.LONG,
                System.currentTimeMillis()
        );

        // 🧪 Optional debug log for pickup events
        if (ConfigManager.isDebugMode()) {

            // 👤 Name of the entity (usually player) who picked up the item
            String pickerName = event.getEntity().getName();

            // 📦 Get basic item info
            String itemName = item.getItemStack().getType().name();
            int amount = item.getItemStack().getAmount();

            // 💬 Output pickup log
            PickYourDifficulty.debug("ItemPickup → " + pickerName + " picked up " + amount + "x " + itemName);
        }
    }
}