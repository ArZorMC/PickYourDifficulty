// ╔════════════════════════════════════════════════════════════════════╗
// ║                      GUIClickListener.java                         ║
// ║   Handles clicks inside the difficulty selection GUI               ║
// ║   Detects filler vs. difficulty icons and routes accordingly       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.ConfigManager;
import dev.arzor.pickyourdifficulty.managers.DifficultyManager;
import dev.arzor.pickyourdifficulty.managers.GUIManager;
import dev.arzor.pickyourdifficulty.managers.MessagesManager;
import dev.arzor.pickyourdifficulty.managers.SoundManager;
import dev.arzor.pickyourdifficulty.managers.PlayerDataManager;
import dev.arzor.pickyourdifficulty.utils.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// ─────────────────────────────────────────────────────────────
// 🖱️ GUIClickListener — Handles difficulty GUI clicks
// ─────────────────────────────────────────────────────────────
// This listener handles:
//  • Cancelling invalid clicks (shift, hotbar, drop)
//  • Detecting filler item clicks and blocking them
//  • Matching valid difficulty icons
//  • Routing to confirmation or instant-apply
public class GUIClickListener implements Listener {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Fields to inject
    private final GUIManager guiManager;
    private final PlayerDataManager playerDataManager;

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║               🛠️ Constructor — Dependency Injection                ║
    // ╚════════════════════════════════════════════════════════════════════╝
    public GUIClickListener(GUIManager guiManager, PlayerDataManager playerDataManager) {
        this.guiManager = guiManager;
        this.playerDataManager = playerDataManager;
    }

    // ─────────────────────────────────────────────────────────────
    // 🖱️ Main Click Handler — GUI Interaction Logic
    // ─────────────────────────────────────────────────────────────
    @EventHandler
    public void onDifficultyGUIClick(InventoryClickEvent event) {

        // 📦 Mini Block: Only respond to real player interactions
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // 🧪 Match against expected GUI title
        String rawTitle = ConfigManager.getGuiTitle();
        Component expectedTitle = mm.deserialize(TextUtil.replacePlaceholders(rawTitle, player));
        Component actualTitle = event.getView().title();

        PickYourDifficulty.debug("GUIClick → Expected title: " + expectedTitle + ", Actual: " + actualTitle);
        if (!expectedTitle.equals(actualTitle)) {
            PickYourDifficulty.debug("GUIClick → Title mismatch. Ignoring.");
            return; // ⛔ Not the custom GUI — ignore click
        }

        // ⛔ Cancel all GUI interactions to avoid dragging/moving items
        event.setCancelled(true);

        // 🚫 Block invalid interactions like shift-click, hotbar keys, drops
        if (event.getClick() == ClickType.SHIFT_LEFT
                || event.getClick() == ClickType.SHIFT_RIGHT
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP) {

            PickYourDifficulty.debug("GUIClick → Blocked invalid click type: " + event.getClick());
            player.sendMessage(mm.deserialize(MessagesManager.get("error.gui-interact-blocked")));
            SoundManager.playCancelSound(player);
            return;
        }

        // 🪙 Get the clicked item
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            PickYourDifficulty.debug("GUIClick → Clicked empty slot.");
            return;
        }

        // ─────────────────────────────────────────────────────────────
        // 📦 Filler Check — Ignore clicks on filler slots
        // ─────────────────────────────────────────────────────────────
        Material fillerMat = Material.getMaterial(ConfigManager.getGuiFillerItemMaterial().toUpperCase());
        if (fillerMat == null) {
            PickYourDifficulty.getInstance().getLogger().warning("Invalid GUI filler material in config!");
            return;
        }
        String fillerName = ConfigManager.getGuiFillerItemName();
        if (clicked.getType() == fillerMat && hasDisplayName(clicked, fillerName)) {
            PickYourDifficulty.debug("GUIClick → Clicked filler item. Ignoring.");
            SoundManager.playCancelSound(player);
            return;
        }

        // ─────────────────────────────────────────────────────────────
        // 🔍 Try to Match Click to a Defined Difficulty Option
        // ─────────────────────────────────────────────────────────────
        for (String difficultyId : DifficultyManager.getAllDifficulties()) {
            Material expectedMat = Material.getMaterial(ConfigManager.getMaterial(difficultyId).toUpperCase());
            String expectedName = ConfigManager.getName(difficultyId);

            // ✅ If icon + name match, this is a valid difficulty option
            if (clicked.getType() == expectedMat && hasDisplayName(clicked, expectedName)) {

                PickYourDifficulty.debug("GUIClick → Matched difficulty icon: " + difficultyId);

                // ⛔ If the player lacks permission for this difficulty, deny it
                if (DifficultyManager.cannotSelect(player, difficultyId)) {
                    PickYourDifficulty.debug("GUIClick → " + player.getName() + " lacks permission for " + difficultyId);
                    player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
                    SoundManager.playDeniedSound(player, true);
                    return;
                }

                // ⏳ Check if player is under cooldown before allowing selection
                if (playerDataManager.isGuiCooldownActive(player)) {

                    // 🧮 Get the number of seconds remaining before player can reselect
                    int secondsLeft = playerDataManager.getCooldownSecondsLeft(player);
                    PickYourDifficulty.debug("GUIClick → " + player.getName() + " is under cooldown: " + secondsLeft + "s remaining");

                    // 💬 Send a user-friendly cooldown wait message
                    Component msg = MessagesManager.format("error.cooldown-wait", player, secondsLeft);
                    player.sendMessage(msg);

                    // 🔇 Play denied sound (soft version for cooldowns)
                    SoundManager.playDeniedSound(player, false);
                    return;
                }

                // ✅ Proceed to confirmation or instant apply (based on config)
                PickYourDifficulty.debug("GUIClick → " + player.getName() + " selected difficulty: " + difficultyId.toLowerCase());
                guiManager.handleDifficultySelected(player, difficultyId.toLowerCase());

                // 🔊 Play confirm sound and close the GUI
                SoundManager.playConfirmSound(player);
                player.closeInventory();
                return;
            }
        }

        // 🚫 Click didn't match any known difficulty — no action taken
        PickYourDifficulty.debug("GUIClick → No matching difficulty found for click.");
    }

    // ─────────────────────────────────────────────────────────────
    // 🧪 Utility: Check Item Display Name Matches MiniMessage String
    // ─────────────────────────────────────────────────────────────
    private boolean hasDisplayName(ItemStack item, String expectedMiniMessage) {
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;

        Component actualName = meta.displayName();
        Component expectedName = mm.deserialize(expectedMiniMessage);

        return expectedName.equals(actualName);
    }
}
