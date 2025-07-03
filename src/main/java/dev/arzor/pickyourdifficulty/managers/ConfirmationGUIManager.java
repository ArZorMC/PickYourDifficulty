// ╔════════════════════════════════════════════════════════════════════╗
// ║              🧩 ConfirmationGUIManager.java                        ║
// ║   Shows confirmation screen for selected difficulty via config     ║
// ║   Supports placeholders, lock-in logic, sound effects, and lore    ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.utils.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

// ─────────────────────────────────────────────────────────────
// 📂 GUI Construction & Interaction
// ─────────────────────────────────────────────────────────────
public class ConfirmationGUIManager {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // ╔═══📤 Show confirmation GUI════════════════════════════════════╗
    public static void openConfirmGUI(Player player, String difficultyId) {

        // 🧪 Debug: Log GUI open attempt
        PickYourDifficulty.debug("Opening confirmation GUI for " + player.getName() + " with difficulty: " + difficultyId);

        // 🧾 Grab the GUI title and inject <difficulty> placeholder
        String rawTitle = ConfigManager.getConfirmationGuiTitle();
        Component title = mm.deserialize(TextUtil.replacePlaceholders(
                rawTitle.replace("<difficulty>", difficultyId), player));

        // 🧮 Compute GUI size: rows * 9 (minimum 9 slots)
        int rows = ConfigManager.getConfirmationGuiRows();
        int size = Math.max(rows * 9, 9); // GUI must have a multiple of 9 slots

        // 🎨 Create inventory using computed size and title
        Inventory gui = Bukkit.createInventory(null, size, title);

        // 📦 Optional: fill all empty slots with filler item
        if (ConfigManager.fillConfirmationGuiEmpty()) {

            // 🧱 Convert configured material string to enum safely
            Material fillerMat = Material.getMaterial(ConfigManager.getConfirmationGuiFillerMaterial().toUpperCase());

            if (fillerMat != null) {
                PickYourDifficulty.debug("Using filler material: " + fillerMat + " to fill empty slots.");

                // 🧱 Create filler item and apply name/lore
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.displayName(mm.deserialize(ConfigManager.getConfirmationGuiFillerName()));
                    meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getConfirmationGuiFillerLore()));
                    filler.setItemMeta(meta);
                }

                // 🧩 Fill all slots with the filler item
                for (int i = 0; i < size; i++) {
                    gui.setItem(i, filler);
                }
            } else {
                PickYourDifficulty.debug("❌ Invalid filler material configured: " + ConfigManager.getConfirmationGuiFillerMaterial());
            }
        }

        // 📘 Info Banner (explanatory text about your selection)
        if (ConfigManager.isInfoBannerEnabled()) {
            int infoSlot = ConfigManager.getInfoBannerSlot();
            Material infoMat = Material.getMaterial(ConfigManager.getInfoBannerMaterial().toUpperCase());

            if (infoMat != null) {
                ItemStack info = new ItemStack(infoMat);
                ItemMeta meta = info.getItemMeta();

                if (meta != null) {
                    // 🔐 Use locked or unlocked lore depending on config
                    if (ConfigManager.lockInDifficulty()) {
                        meta.displayName(mm.deserialize(ConfigManager.getLockedInfoBannerName()));
                        meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getLockedInfoBannerLore()));
                    } else {
                        meta.displayName(mm.deserialize(ConfigManager.getInfoBannerName()));
                        meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getInfoBannerLore()));
                    }
                    info.setItemMeta(meta);
                }

                // 🧩 Place info banner into configured slot
                if (infoSlot >= 0 && infoSlot < size) {
                    gui.setItem(infoSlot, info);
                    PickYourDifficulty.debug("Placed info banner (" + infoMat + ") at slot " + infoSlot);
                }

            } else {
                PickYourDifficulty.debug("❌ Invalid info banner material configured: " + ConfigManager.getInfoBannerMaterial());
            }
        }

        // ✅ Confirm Button (appearance changes if locked difficulty mode is enabled)
        int confirmSlot = ConfigManager.getConfirmButtonSlot();
        Material confirmMat = Material.getMaterial(
                (ConfigManager.lockInDifficulty()
                        ? ConfigManager.getLockedConfirmButtonMaterial()
                        : ConfigManager.getConfirmButtonMaterial()).toUpperCase());

        if (confirmMat != null) {
            ItemStack confirm = new ItemStack(confirmMat);
            ItemMeta confirmMeta = confirm.getItemMeta();

            if (confirmMeta != null) {
                // 🏷️ Set button name and lore depending on lock-in
                confirmMeta.displayName(mm.deserialize(
                        ConfigManager.lockInDifficulty()
                                ? ConfigManager.getLockedConfirmButtonName()
                                : ConfigManager.getConfirmButtonName()));
                confirmMeta.lore(TextUtil.deserializeMiniMessageList(
                        ConfigManager.lockInDifficulty()
                                ? ConfigManager.getLockedConfirmButtonLore()
                                : ConfigManager.getConfirmButtonLore()));
                confirm.setItemMeta(confirmMeta);
            }

            // 🧩 Place button into slot
            if (confirmSlot >= 0 && confirmSlot < size) {
                gui.setItem(confirmSlot, confirm);
                PickYourDifficulty.debug("Placed confirm button (" + confirmMat + ") at slot " + confirmSlot);
            }
        } else {
            PickYourDifficulty.debug("❌ Invalid confirm button material configured.");
        }

        // ❌ Cancel Button
        int cancelSlot = ConfigManager.getCancelButtonSlot();
        Material cancelMat = Material.getMaterial(ConfigManager.getCancelButtonMaterial().toUpperCase());

        if (cancelMat != null) {
            ItemStack cancel = new ItemStack(cancelMat);
            ItemMeta cancelMeta = cancel.getItemMeta();

            if (cancelMeta != null) {
                cancelMeta.displayName(mm.deserialize(ConfigManager.getCancelButtonName()));
                cancelMeta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getCancelButtonLore()));
                cancel.setItemMeta(cancelMeta);
            }

            if (cancelSlot >= 0 && cancelSlot < size) {
                gui.setItem(cancelSlot, cancel);
                PickYourDifficulty.debug("Placed cancel button (" + cancelMat + ") at slot " + cancelSlot);
            }

        } else {
            PickYourDifficulty.debug("❌ Invalid cancel button material configured.");
        }

        // 🚪 Open the GUI and play sound
        player.openInventory(gui);
        SoundManager.playGuiOpenSound(player);
    }

    // ╔═══✅ Confirm difficulty═════════════════════════════════════════╗
    public static void acceptSelection(Player player, String difficultyId) {
        PlayerDifficultyStorage storage = PickYourDifficulty.getInstance().getPlayerDifficultyStorage();

        // 🧪 Debug: Log acceptance
        PickYourDifficulty.debug(player.getName() + " confirmed difficulty selection: " + difficultyId);

        // 💾 Save difficulty to storage and begin GUI cooldown timer
        storage.setDifficulty(player, difficultyId);
        PickYourDifficulty.getInstance().getPlayerDataManager().startGuiCooldown(player);
        PickYourDifficulty.debug("Applied GUI cooldown for " + player.getName());

        // 📜 Run difficulty-specific setup commands
        List<String> commands = ConfigManager.getCommands(difficultyId);
        for (String rawCommand : commands) {

            // 🪄 Replace <player> with actual player name
            String replaced = rawCommand.replace("<player>", player.getName());

            // 🧪 Debug: Log command dispatch
            PickYourDifficulty.debug("Running command: " + replaced);

            // 📦 Choose command sender type
            if (replaced.startsWith("console:")) {
                // ⚙️ console: — run as server console
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.substring("console:".length()).trim());

            } else if (replaced.startsWith("player:")) {
                // 🧑 player: — run as the player themselves
                player.performCommand(replaced.substring("player:".length()).trim());

            } else {
                // 🔁 fallback — assume console sender
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced);
            }
        }

        // 👋 Show welcome message (if enabled)
        if (ConfigManager.showWelcomeOnSelection()) {
            player.sendMessage(MessagesManager.get(difficultyId, player));
            PickYourDifficulty.debug("Sent welcome message to " + player.getName());
        }

        // 🔊 Play confirmation sound
        SoundManager.playConfirmSound(player);

        // ❎ Auto-close GUI if enabled in config
        if (ConfigManager.guiCloseOnSelect()) {
            player.closeInventory();
            PickYourDifficulty.debug("Closed GUI for " + player.getName() + " after selection.");
        }
    }

    // ╔═══❌ Cancel difficulty selection═════════════════════════════════╗
    public static void cancelSelection(Player player) {

        // 🧪 Debug: Log cancellation
        PickYourDifficulty.debug(player.getName() + " cancelled difficulty selection.");

        // 🔊 Play cancel sound
        SoundManager.playCancelSound(player);

        // 🔁 Return to difficulty selection GUI
        PickYourDifficulty.getInstance()
                .getGuiManager()
                .openDifficultyGUI(player);
    }
}