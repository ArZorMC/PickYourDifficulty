// ╔════════════════════════════════════════════════════════════════════╗
// ║                🖱️ ConfirmGUIClickListener.java                     ║
// ║   Handles clicks inside the confirmation GUI                       ║
// ║   Confirms or cancels the difficulty selection                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.managers.*;
import dev.arzor.pickyourdifficulty.utils.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConfirmGUIClickListener implements Listener {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final GUIManager guiManager;

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║               🛠️ Constructor — Dependency Injection                ║
    // ╚════════════════════════════════════════════════════════════════════╝
    public ConfirmGUIClickListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 🖱️ Handle Clicks Inside Confirmation GUI
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onConfirmGUIClick(InventoryClickEvent event) {

        // 📦 Only players can click
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 📦 Ignore null inventories
        if (event.getClickedInventory() == null) return;

        // ╔═══🪪 Match GUI title to confirmation screen═════════════════════════════════════════════╗

        // 🧠 Pull the expected GUI title from config and replace placeholders
        String rawTitle = ConfigManager.getConfirmationGuiTitle();
        String expectedTitle = mm.deserialize(TextUtil.replacePlaceholders(rawTitle, player)).toString();

        // 📋 Get actual GUI title
        String actualTitle = event.getView().title().toString();

        // 🚪 Exit early if not the confirmation GUI
        if (!actualTitle.equals(expectedTitle)) return;

        // ╔═══⛔ Block inventory movement types══════════════════════════════════════════════════════╗

        // 🛑 Cancel all interactions in this GUI
        event.setCancelled(true);

        // 🚫 Block sneaky inventory actions like shift-clicking or hotbar swap
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT
                || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.CONTROL_DROP || event.getClick() == ClickType.DROP) {

            // 💬 Tell player this kind of click is blocked
            player.sendMessage(mm.deserialize(MessagesManager.get("error.gui-interact-blocked")));
            SoundManager.playCancelSound(player);
            return;
        }

        // ╔═══📦 Check clicked item═════════════════════════════════════════════════════════════════╗

        // 🎯 Get the item they clicked
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // 🧾 Ensure item has a display name
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Component actualName = meta.displayName();
        if (actualName == null) return;

        // ╔═══✅ Confirm button selected═════════════════════════════════════════════════════════════╗

        // 📋 Grab proper button name depending on config (locked or not)
        Component confirmName = mm.deserialize(ConfigManager.lockInDifficulty()
                ? ConfigManager.getLockedConfirmButtonName()
                : ConfigManager.getConfirmButtonName());

        // ✅ If this is the confirm button
        if (confirmName.equals(actualName)) {

            // 🧠 Retrieve the difficulty the player had selected from GUI
            String selectedDifficulty = guiManager.getLastSelectedDifficulty(player);

            // ❌ If somehow nothing was selected, send error
            if (selectedDifficulty == null) {
                player.sendMessage(mm.deserialize(MessagesManager.get("error.no-selection-found")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // 🧼 Normalize to canonical key (e.g. easy → Easy)
            String canonical = DifficultyManager.getCanonicalKey(selectedDifficulty);

            // ❌ Catch if normalization failed
            if (canonical == null) {
                player.sendMessage(mm.deserialize(MessagesManager.get("error.invalid-difficulty")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // ⛔ Block selection if permission check fails
            if (DifficultyManager.cannotSelect(player, canonical)) {
                player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // 🎉 Finalize the difficulty selection
            ConfirmationGUIManager.acceptSelection(player, canonical);
            return;
        }

        // ╔═══❌ Cancel button selected══════════════════════════════════════════════════════════════╗

        // 🛑 If the clicked button is the cancel button
        Component cancelName = mm.deserialize(ConfigManager.getCancelButtonName());

        if (cancelName.equals(actualName)) {
            // 🧯 Cancel the pending difficulty selection
            ConfirmationGUIManager.cancelSelection(player);
        }
    }
}
