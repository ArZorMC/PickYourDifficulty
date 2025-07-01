// ╔════════════════════════════════════════════════════════════════════╗
// ║                      GUIClickListener.java                         ║
// ║   Handles clicks inside the difficulty selection GUI               ║
// ║   Detects filler vs. difficulty icons and routes accordingly       ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.listeners;

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

import java.util.logging.Logger;

public class GUIClickListener implements Listener {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Fields to inject
    private final GUIManager guiManager;
    private final PlayerDataManager playerDataManager;
    private final Logger logger;

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║               🛠️ Constructor — Dependency Injection                ║
    // ╚════════════════════════════════════════════════════════════════════╝
    public GUIClickListener(GUIManager guiManager, PlayerDataManager playerDataManager, Logger logger) {
        this.guiManager = guiManager;
        this.playerDataManager = playerDataManager;
        this.logger = logger;
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
        String expectedTitle = mm.deserialize(TextUtil.replacePlaceholders(rawTitle, player)).toString();
        String actualTitle = event.getView().title().toString();
        if (!actualTitle.equals(expectedTitle)) return;

        // ⛔ Cancel all GUI interactions to avoid dragging/moving items
        event.setCancelled(true);

        // 🚫 Block invalid interactions like shift-click, hotbar keys, drops
        if (event.getClick() == ClickType.SHIFT_LEFT
                || event.getClick() == ClickType.SHIFT_RIGHT
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP) {

            player.sendMessage(mm.deserialize(MessagesManager.get("error.gui-interact-blocked")));
            SoundManager.playCancelSound(player);
            return;
        }

        // 🪙 Get the clicked item
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // ─────────────────────────────────────────────────────────────
        // 📦 Filler Check — Ignore clicks on filler slots
        // ─────────────────────────────────────────────────────────────
        Material fillerMat = Material.getMaterial(ConfigManager.getGuiFillerItemMaterial().toUpperCase());
        String fillerName = ConfigManager.getGuiFillerItemName();
        if (clicked.getType() == fillerMat && hasDisplayName(clicked, fillerName)) {
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

                // ⛔ If the player lacks permission for this difficulty, deny it
                if (DifficultyManager.cannotSelect(player, difficultyId)) {
                    player.sendMessage(mm.deserialize(MessagesManager.get("error.no-permission")));
                    SoundManager.playDeniedSound(player, true);
                    return;
                }

                // 🧪 Log selected difficulty to console for server-side visibility
                logger.info(player.getName() + " selected difficulty: " + difficultyId);


                // ⏳ Check if player is under cooldown before allowing selection
                if (playerDataManager.isGuiCooldownActive(player)) {

                    // 🧮 Get the number of seconds remaining before player can reselect
                    int secondsLeft = playerDataManager.getCooldownSecondsLeft(player);

                    // 💬 Send a user-friendly cooldown wait message
                    Component msg = MessagesManager.format("error.cooldown-wait", player, secondsLeft);
                    player.sendMessage(msg);

                    // 🔇 Play denied sound (soft version for cooldowns)
                    SoundManager.playDeniedSound(player, false);
                    return;
                }

                // ✅ Proceed to confirmation or instant apply (based on config)
                guiManager.handleDifficultySelected(player, difficultyId);

                // 🔊 Play confirm sound and close the GUI
                SoundManager.playConfirmSound(player);
                player.closeInventory();
                return;
            }
        }

        // 🚫 Click didn't match any known difficulty — no action taken
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
