// ╔════════════════════════════════════════════════════════════════════╗
// ║                🖱️ ConfirmGUIClickListener.java                     ║
// ║   Handles clicks inside the confirmation GUI                       ║
// ║   Confirms or cancels the difficulty selection                     ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
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

// ─────────────────────────────────────────────────────────────
// 🧩 ConfirmGUIClickListener — Confirms or cancels difficulty
// ─────────────────────────────────────────────────────────────
// This listener handles player clicks inside the difficulty
// confirmation GUI. Based on which button was clicked, it will:
//
// - ✅ Confirm and lock in the selected difficulty
// - ❌ Cancel and close the GUI without changing settings
//
// 🎛️ It blocks sneaky shift-clicks and hotbar swaps.
// 🧼 It ensures only the actual confirmation GUI is handled.
// 🧠 It pulls data from GUIManager to know the pending selection.
public class ConfirmGUIClickListener implements Listener {

    // 🧵 MiniMessage parser for GUI title and labels
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // 🗃️ Access to GUI memory (stores selected difficulty per player)
    private final GUIManager guiManager;

    // 📦 Inject dependency
    public ConfirmGUIClickListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 🖱️ Handle Clicks Inside Confirmation GUI
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onConfirmGUIClick(InventoryClickEvent event) {

        // ╔═══🧑 Must Be Player══════════════════════════════════════════════════════════════════════════════╗

        // 📦 Only players can trigger this — not console or entities
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 📦 Ignore cases where the clicked inventory is null (edge cases)
        if (event.getClickedInventory() == null) return;

        // ╔═══🪪 Match GUI title════════════════════════════════════════════════════════════════════════════╗

        // 🧠 Pull the expected GUI title from config and apply placeholder replacements
        String rawTitle = ConfigManager.getConfirmationGuiTitle();
        String expectedTitle = mm.deserialize(TextUtil.replacePlaceholders(rawTitle, player)).toString();

        // 📋 Get the actual GUI title from the player's view
        String actualTitle = event.getView().title().toString();

        // 📦 Debug: Compare actual vs expected GUI titles
        PickYourDifficulty.debug("Confirm GUI title check for " + player.getName() +
                ": expected = " + expectedTitle + " | actual = " + actualTitle);

        // 🚪 Exit early if this GUI is not the confirmation screen
        if (!actualTitle.equals(expectedTitle)) return;

        // ╔═══⛔ Block interaction types════════════════════════════════════════════════════════════════════╗

        // 🛑 Cancel the event to prevent any item movement
        event.setCancelled(true);

        // 🚫 Block sneaky movement clicks: shift, hotbar swaps, offhand swaps, etc.
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT
                || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.CONTROL_DROP || event.getClick() == ClickType.DROP) {

            // 💬 Tell player this kind of click is blocked
            PickYourDifficulty.debug("Blocked sneaky click type from " + player.getName() + ": " + event.getClick());
            player.sendMessage(mm.deserialize(MessagesManager.get("error.gui-interact-blocked")));
            SoundManager.playCancelSound(player);
            return;
        }

        // ╔═══📦 Check clicked item═════════════════════════════════════════════════════════════════╗

        // 🎯 Get the item they clicked
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // 🧾 Ensure the item has metadata and a name
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // 🏷️ Get the actual display name
        Component actualName = meta.displayName();
        if (actualName == null) return;

        // ╔═══✅ Confirm Button Clicked═════════════════════════════════════════════════════════════════════╗

        // 📋 Determine the correct confirm button text depending on lock-in status
        Component confirmName = mm.deserialize(ConfigManager.lockInDifficulty()
                ? ConfigManager.getLockedConfirmButtonName()
                : ConfigManager.getConfirmButtonName());

        // ✅ If they clicked the confirm button
        if (confirmName.equals(actualName)) {
            PickYourDifficulty.debug(player.getName() + " clicked Confirm button");

            // 🧠 Retrieve their last selected difficulty from GUI memory
            String selectedDifficulty = guiManager.getLastSelectedDifficulty(player);

            // ❌ If for some reason it's null, show error
            if (selectedDifficulty == null) {
                PickYourDifficulty.debug("No difficulty selected for " + player.getName() + " — aborting.");
                player.sendMessage(mm.deserialize(MessagesManager.get("error.no-selection-found")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // 🧼 Normalize to canonical key (e.g. easy → Easy)
            String canonical = DifficultyManager.getCanonicalKey(selectedDifficulty.toLowerCase());

            // ❌ Invalid or unrecognized difficulty key
            if (canonical == null) {
                PickYourDifficulty.debug("Invalid difficulty: " + selectedDifficulty);
                player.sendMessage(mm.deserialize(MessagesManager.get("error.invalid-difficulty")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // ⛔ If they don’t have permission for this difficulty, cancel
            if (DifficultyManager.cannotSelect(player, canonical)) {
                PickYourDifficulty.debug(player.getName() + " lacks permission for difficulty: " + canonical);
                player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
                SoundManager.playCancelSound(player);
                player.closeInventory();
                return;
            }

            // 🎉 All checks passed — finalize the difficulty
            PickYourDifficulty.debug("Finalizing difficulty selection for " + player.getName() + ": " + canonical);
            ConfirmationGUIManager.acceptSelection(player, canonical);
            return;
        }

        // ╔═══❌ Cancel button selected══════════════════════════════════════════════════════════════╗

        // 🛑 If the clicked button is the cancel button
        Component cancelName = mm.deserialize(ConfigManager.getCancelButtonName());

        if (cancelName.equals(actualName)) {
            // 🧯 Cancel the pending difficulty selection
            PickYourDifficulty.debug(player.getName() + " clicked Cancel button");
            ConfirmationGUIManager.cancelSelection(player);
        }
    }
}
