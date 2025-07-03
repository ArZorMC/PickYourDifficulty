// ╔════════════════════════════════════════════════════════════════════╗
// ║                         🧩 GUIManager.java                         ║
// ║   Handles opening the difficulty selection GUI and routing to      ║
// ║   the confirmation GUI if required. Also manages per-player cache  ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;
import dev.arzor.pickyourdifficulty.utils.TextUtil;
import dev.arzor.pickyourdifficulty.utils.TimeFormatUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────
// 🎨 GUIManager — Opens main difficulty GUI for players
// ─────────────────────────────────────────────────────────────
public class GUIManager {

    // ╔═══════════════════════════════════════════════════════╗
    // ║                 🔂 Singleton Structure                ║
    // ╚═══════════════════════════════════════════════════════╝

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final GUIManager instance = new GUIManager(); // 🧩 Singleton

    public static GUIManager getInstance() {
        return instance;
    }

    private GUIManager() {} // ❌ Prevent external instantiation


    // ╔═══════════════════════════════════════════════════════╗
    // ║                🧠 Player Selection Cache              ║
    // ╚═══════════════════════════════════════════════════════╝

    private final Map<Player, String> selectionCache = new HashMap<>();


    // ─────────────────────────────────────────────────────────────
    // 🪟 openDifficultyGUI — Main GUI builder for difficulty pick
    // ─────────────────────────────────────────────────────────────
    public void openDifficultyGUI(Player player) {

        // 📦 Step 0: Cooldown Check
        if (CooldownTracker.isCooldownActive(player.getUniqueId())) {
            long secondsLeft = CooldownTracker.getRemainingSeconds(player.getUniqueId());

            // 🧮 Format seconds into a readable string
            String formattedTime = TimeFormatUtil.formatCooldown(secondsLeft);

            // 💬 Send denial message with placeholder replaced manually
            String raw = MessagesManager.get("gui.cooldown-wait").replace("<time>", formattedTime);
            player.sendMessage(mm.deserialize(raw));

            // 🔊 Play cooldown denial sound
            SoundManager.playDeniedSound(player, false);

            // 🧪 Debug: cooldown message triggered
            PickYourDifficulty.debug("GUI not opened — cooldown active for " + player.getName() + " (" + formattedTime + ")");
            return;
        }

        // 🧪 Debug: Begin building GUI
        PickYourDifficulty.debug("Building difficulty GUI for " + player.getName());

        // 📦 Step 1: Build GUI inventory object
        String rawTitle = ConfigManager.getGuiTitle();
        Component title = mm.deserialize(TextUtil.replacePlaceholders(rawTitle, player));

        int rows = ConfigManager.getGuiRows();
        int size = Math.max(rows * 9, 9); // 🧮 Ensure inventory is always minimum 1 row (9 slots)

        Inventory gui = Bukkit.createInventory(null, size, title);

        // 📦 Step 2: Optional GUI filler item
        if (ConfigManager.fillGuiEmpty()) {
            Material fillerMaterial = Material.getMaterial(ConfigManager.getGuiFillerItemMaterial().toUpperCase());

            if (fillerMaterial != null) {
                ItemStack filler = new ItemStack(fillerMaterial);
                ItemMeta meta = filler.getItemMeta();

                if (meta != null) {
                    meta.displayName(mm.deserialize(ConfigManager.getGuiFillerItemName()));
                    List<String> rawLore = ConfigManager.getGuiFillerItemLore();
                    meta.lore(TextUtil.deserializeMiniMessageList(rawLore));
                    filler.setItemMeta(meta);
                }

                // 🔁 Fill all slots with filler
                for (int i = 0; i < size; i++) {
                    gui.setItem(i, filler);
                }

                // 🧪 Debug: filler applied
                PickYourDifficulty.debug("Filler items added to GUI (" + size + " slots)");
            }
        }

        // 📦 Step 3: Add difficulty options
        for (String difficultyId : ConfigManager.getDifficultyNames()) {

            // 🔐 Permission check
            String perm = "pickyourdifficulty.difficulty." + difficultyId.toLowerCase();
            boolean hasPermission = PermissionUtil.has(player, perm);

            Material iconMaterial = Material.getMaterial(ConfigManager.getMaterial(difficultyId).toUpperCase());
            if (iconMaterial == null) {
                if (ConfigManager.isDebugMode()) {
                    PickYourDifficulty.debug("⚠️ Skipping difficulty '" + difficultyId + "' — invalid material.");
                }
                continue;
            }

            int slot = ConfigManager.getSlot(difficultyId);
            String name = ConfigManager.getName(difficultyId);
            List<String> rawLore = ConfigManager.getLore(difficultyId);

            ItemStack icon = new ItemStack(iconMaterial);
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                // 🎨 Name and lore with placeholders replaced
                Component displayName = mm.deserialize(TextUtil.replacePlaceholders(name, player));

                // 🧩 Replace placeholders in each lore line before converting to MiniMessage Components
                List<Component> loreLines = TextUtil.deserializeMiniMessageList(
                        rawLore.stream()
                                .map(line -> TextUtil.replacePlaceholders(line, player))
                                .toList()
                );

                if (!hasPermission) {
                    if (ConfigManager.hideUnselectableDifficulties()) { // 🔒 Completely skip if hidden
                        if (ConfigManager.isDebugMode()) {
                            PickYourDifficulty.debug("🔒 Hiding unselectable difficulty: " + difficultyId);
                        }
                        continue;
                    }

                    // 🛑 Show grayed-out unselectable version
                    displayName = displayName.color(net.kyori.adventure.text.format.NamedTextColor.GRAY);
                    meta.displayName(displayName);

                    Component hoverLine = mm.deserialize(MessagesManager.get("gui.no-permission-hover"));
                    meta.lore(List.of(hoverLine));
                } else {
                    meta.displayName(displayName);
                    meta.lore(loreLines);
                }

                icon.setItemMeta(meta);
            }

            // ⬇️ Place in slot and store in cache if allowed
            if (slot >= 0 && slot < size) {
                gui.setItem(slot, icon);
                if (hasPermission) {
                    // 💾 Cache the player's last valid difficulty selection for confirmation tracking
                    selectionCache.put(player, difficultyId.toLowerCase());
                }

                // 🧪 Debug: icon placed
                PickYourDifficulty.debug("🪪 Placed icon for '" + difficultyId + "' at slot " + slot + " (permitted=" + hasPermission + ")");
            }
        }

        // 📦 Step 4: Open the GUI
        player.openInventory(gui);
        SoundManager.playGuiOpenSound(player);

        // 🧪 Debug: GUI opened
        PickYourDifficulty.debug("GUI opened for " + player.getName() + " (" + selectionCache.size() + " selections cached)");
    }

    // ─────────────────────────────────────────────────────────────
    // 🎯 handleDifficultySelected — Routes click to confirmation GUI
    // ─────────────────────────────────────────────────────────────
    public void handleDifficultySelected(Player player, String difficultyId) {

        // 🧼 Convert to lowercase to avoid case-sensitivity issues during lookup
        String normalized = difficultyId.toLowerCase();

        // 🔍 Attempt to resolve the canonical config key (e.g., easy → Easy)
        String canonical = DifficultyManager.getCanonicalKey(normalized);

        // 🧪 Debug: click processing
        PickYourDifficulty.debug("Player clicked difficulty: " + difficultyId);
        PickYourDifficulty.debug("Resolved canonical difficulty: " + canonical);

        // ❌ If canonical couldn't be resolved, warn and exit
        if (canonical == null) {
            PickYourDifficulty.getInstance().getLogger().warning(
                    "[PickYourDifficulty] Failed to find valid difficulty key: '" + difficultyId + "'"
            );
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    MessagesManager.get("error.invalid-difficulty")
            ));
            SoundManager.playCancelSound(player);
            return;
        }

        // 📋 If confirmation is required, open confirmation GUI
        if (ConfigManager.requireConfirmation()) {
            PickYourDifficulty.debug("Opening confirmation GUI for " + player.getName() + " → " + canonical);
            ConfirmationGUIManager.openConfirmGUI(player, canonical);
        } else {
            PickYourDifficulty.debug("Confirmation bypassed — accepting selection directly for " + player.getName() + " → " + canonical);

            // ✅ No confirmation required — finalize difficulty selection directly
            ConfirmationGUIManager.acceptSelection(player, canonical);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🧠 getLastSelectedDifficulty — Retrieve last clicked difficulty
    // ─────────────────────────────────────────────────────────────
    public String getLastSelectedDifficulty(Player player) {
        String cached = selectionCache.get(player);

        // 🧪 Debug: return from cache
        PickYourDifficulty.debug("getLastSelectedDifficulty(): " + player.getName() + " → " + cached);

        return cached;
    }
}
