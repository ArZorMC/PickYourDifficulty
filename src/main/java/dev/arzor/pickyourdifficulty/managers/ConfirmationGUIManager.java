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

        // 🧾 Grab the GUI title and inject <difficulty> placeholder
        String rawTitle = ConfigManager.getConfirmationGuiTitle();
        Component title = mm.deserialize(TextUtil.replacePlaceholders(
                rawTitle.replace("<difficulty>", difficultyId), player));

        // 🧮 Compute GUI size: rows * 9 (minimum 9 slots)
        int rows = ConfigManager.getConfirmationGuiRows();
        int size = Math.max(rows * 9, 9);

        // 🎨 Create inventory
        Inventory gui = Bukkit.createInventory(null, size, title);

        // 📦 Optional: fill all empty slots with filler item
        if (ConfigManager.fillConfirmationGuiEmpty()) {
            Material fillerMat = Material.getMaterial(ConfigManager.getConfirmationGuiFillerMaterial().toUpperCase());
            if (fillerMat != null) {
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.displayName(mm.deserialize(ConfigManager.getConfirmationGuiFillerName()));
                    meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getConfirmationGuiFillerLore()));
                    filler.setItemMeta(meta);
                }
                for (int i = 0; i < size; i++) {
                    gui.setItem(i, filler);
                }
            }
        }

        // 📘 Info Banner
        if (ConfigManager.isInfoBannerEnabled()) {
            int infoSlot = ConfigManager.getInfoBannerSlot();
            Material infoMat = Material.getMaterial(ConfigManager.getInfoBannerMaterial().toUpperCase());
            if (infoMat != null) {
                ItemStack info = new ItemStack(infoMat);
                ItemMeta meta = info.getItemMeta();
                if (meta != null) {
                    if (ConfigManager.lockInDifficulty()) {
                        meta.displayName(mm.deserialize(ConfigManager.getLockedInfoBannerName()));
                        meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getLockedInfoBannerLore()));
                    } else {
                        meta.displayName(mm.deserialize(ConfigManager.getInfoBannerName()));
                        meta.lore(TextUtil.deserializeMiniMessageList(ConfigManager.getInfoBannerLore()));
                    }
                    info.setItemMeta(meta);
                }
                if (infoSlot >= 0 && infoSlot < size) {
                    gui.setItem(infoSlot, info);
                }
            }
        }

        // ✅ Confirm Button (uses alternate style if locked difficulty is enabled)
        int confirmSlot = ConfigManager.getConfirmButtonSlot();
        Material confirmMat = Material.getMaterial(
                (ConfigManager.lockInDifficulty()
                        ? ConfigManager.getLockedConfirmButtonMaterial()
                        : ConfigManager.getConfirmButtonMaterial()).toUpperCase());

        if (confirmMat != null) {
            ItemStack confirm = new ItemStack(confirmMat);
            ItemMeta confirmMeta = confirm.getItemMeta();
            if (confirmMeta != null) {
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
            if (confirmSlot >= 0 && confirmSlot < size) {
                gui.setItem(confirmSlot, confirm);
            }
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
            }
        }

        // 🚪 Open the GUI and play sound
        player.openInventory(gui);
        SoundManager.playGuiOpenSound(player);
    }

    // ╔═══✅ Confirm difficulty═════════════════════════════════════════╗
    public static void acceptSelection(Player player, String difficultyId) {
        PlayerDifficultyStorage storage = PickYourDifficulty.getInstance().getPlayerDifficultyStorage();

        // 💾 Save difficulty and apply GUI cooldown
        storage.setDifficulty(player, difficultyId);
        PickYourDifficulty.getInstance().getPlayerDataManager().startGuiCooldown(player);

        // 📜 Run difficulty-specific commands
        List<String> commands = ConfigManager.getCommands(difficultyId);
        for (String rawCommand : commands) {
            String replaced = rawCommand.replace("<player>", player.getName());

            // 📦 Detect prefix for command source
            if (replaced.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.substring("console:".length()).trim());

            } else if (replaced.startsWith("player:")) {
                player.performCommand(replaced.substring("player:".length()).trim());

            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced); // default: console
            }
        }

        // 👋 Show welcome message (if enabled)
        if (ConfigManager.showWelcomeOnSelection()) {
            player.sendMessage(MessagesManager.get(difficultyId, player));
        }

        SoundManager.playConfirmSound(player);

        // ❎ Auto-close GUI if configured
        if (ConfigManager.guiCloseOnSelect()) {
            player.closeInventory();
        }
    }

    // ╔═══❌ Cancel difficulty selection═════════════════════════════════╗
    public static void cancelSelection(Player player) {
        SoundManager.playCancelSound(player);

        // 🔁 Return to difficulty GUI
        PickYourDifficulty.getInstance()
                .getGuiManager()
                .openDifficultyGUI(player);
    }
}