// ╔════════════════════════════════════════════════════════════════════╗
// ║                      🐞 CommandPydDebug.java                       ║
// ║  /pyddebug — view internal state, hooks, config status             ║
// ║  Supports pagination via:                                          ║
// ║    /pyddebug players <page>                                        ║
// ║    /pyddebug stored <page>                                         ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.commands;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.managers.*;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;
import dev.arzor.pickyourdifficulty.utils.PermissionUtil;
import dev.arzor.pickyourdifficulty.utils.TimeFormatUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

// ─────────────────────────────────────────────────────────────
// 🧪 CommandPydDebug — Diagnostic command for developers/admins
// ─────────────────────────────────────────────────────────────
public class CommandPydDebug implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 📦 Storage
    // ─────────────────────────────────────────────────────────────

    // 💾 Cache the player difficulty storage instance
    private final PlayerDifficultyStorage difficultyStorage = PickYourDifficulty.getInstance()
            .getPlayerDataManager()
            .getDifficultyStorage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        PickYourDifficulty.debug("Executing /pyddebug by: " + sender.getName() + " with args: " + Arrays.toString(args));

        // 💬 Admin-only: Make sure the sender has permission to view debug info
        if (!PermissionUtil.isAdmin(sender)) {
            PickYourDifficulty.debug("Denied access to /pyddebug for: " + sender.getName());
            sender.sendMessage(MessagesManager.format("debug.no-permission"));
            return true;
        }

        // 📦 Default pagination state
        int playersPage = 1;
        int storedPage = 1;
        boolean onlyPlayersFlat = false;
        boolean onlyStored = false;

        // ╔═══🔁 /pyddebug <section> <page> — parse section/page══════════════════════════════════════════════╗
        if (args.length >= 2) {
            try {
                int page = Integer.parseInt(args[1]);
                if (args[0].equalsIgnoreCase("players")) {
                    onlyPlayersFlat = true;
                    playersPage = page;
                    PickYourDifficulty.debug("Parsed player debug page: " + page);
                } else if (args[0].equalsIgnoreCase("stored")) {
                    onlyStored = true;
                    storedPage = page;
                    PickYourDifficulty.debug("Parsed stored debug page: " + page);
                }
            } catch (NumberFormatException e) {
                PickYourDifficulty.debug("Invalid page number: " + args[1]);
                sender.sendMessage(MessagesManager.format("debug.invalid-page"));
                return true;
            }
        }

        // ╔═══🌐 General plugin info═══════════════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored) {
            PickYourDifficulty.debug("Displaying general plugin info");
            sender.sendMessage(MessagesManager.format("debug.prefix"));
            sender.sendMessage(MessagesManager.format("debug.online-players", Map.of("count", String.valueOf(Bukkit.getOnlinePlayers().size()))));
            sender.sendMessage(MessagesManager.format("debug.fallback", Map.of("difficulty", ConfigManager.getFallbackDifficulty())));
            sender.sendMessage(MessagesManager.format("debug.dev-mode", Map.of("state", ConfigManager.devModeAlwaysShow() ? "<green>Enabled" : "<red>Disabled")));
            sender.sendMessage(MessagesManager.format("debug.switching", Map.of("state", ConfigManager.allowDifficultyChange() ? "<green>Yes" : "<red>No")));
            sender.sendMessage(MessagesManager.format("debug.holograms", Map.of("state", ConfigManager.hologramsEnabled() ? "<green>Yes" : "<red>No")));

            // 🔌 Show plugin hook status
            sender.sendMessage(MessagesManager.format("debug.hooks-header"));
            checkPlugin(sender, "PlaceholderAPI", ConfigManager.enablePlaceholderAPI());
            checkPlugin(sender, "AcceptTheRules", ConfigManager.enableAcceptTheRules());
            checkPlugin(sender, "Geyser", ConfigManager.enableGeyserSupport());
            checkPlugin(sender, "DecentHolograms", ConfigManager.hologramsEnabled());
            checkPlugin(sender, "NBTAPI", true);
        }

        // ╔═══🧠 Online player data (paginated)═══════════════════════════════════════════════════════════════╗
        if (!onlyStored) {
            List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            allPlayers.sort(Comparator.comparing(Player::getName));
            PickYourDifficulty.debug("Displaying online players: " + allPlayers.size() + " total, page " + playersPage);
            paginatePlayerList(sender, allPlayers, ConfigManager.getDebugOnlinePlayersPerPage(), playersPage);
        }

        // ╔═══📦 Stored difficulty data (paginated)═══════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat) {
            Map<UUID, String> stored = difficultyStorage.getAllDifficultyData();
            List<Map.Entry<UUID, String>> storedList = new ArrayList<>(stored.entrySet());
            storedList.sort(Comparator.comparing(entry -> {
                OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
                return p.getName() != null ? p.getName() : "~";
            }));
            PickYourDifficulty.debug("Displaying stored players: " + storedList.size() + " total, page " + storedPage);
            paginateStoredList(sender, storedList, ConfigManager.getDebugStoredDifficultyPerPage(), storedPage);
        }

        // ╔═══👁️ Hologram visibility summary══════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored && ConfigManager.hologramsEnabled()) {
            PickYourDifficulty.debug("Displaying hologram visibility summary");
            sender.sendMessage(MessagesManager.format("debug.holograms-hidden", Map.of("count", String.valueOf(HologramManager.getHiddenPlayers().size()))));
            sender.sendMessage(MessagesManager.format("debug.holograms-active", Map.of("count", String.valueOf(HologramManager.getHologramMap().size()))));
        }

        // ╔═══♻️ Reloadable class summary═════════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored) {
            PickYourDifficulty.debug("Listing reloadable class implementations");
            sender.sendMessage(MessagesManager.format("debug.reloadables-header"));
            for (var reloadable : ReloadManager.getReloadables()) {
                sender.sendMessage(MessagesManager.format("debug.reloadables-entry", Map.of(
                        "class", reloadable.getClass().getSimpleName()
                )));
            }
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────
    // 🧠 Online Players Pagination
    // ─────────────────────────────────────────────────────────────
    private void paginatePlayerList(CommandSender sender, List<Player> players, int perPage, int page) {
        sender.sendMessage(Component.text("🧠 Online Player Data:", NamedTextColor.GOLD));

        // 🧮 Calculate total number of pages
        int totalPages = Math.max(1, (int) Math.ceil(players.size() / (double) perPage));
        PickYourDifficulty.debug("Paginating online players — page " + page + "/" + totalPages + ", perPage=" + perPage);

        // 🧮 Clamp current page between 1 and totalPages
        page = Math.min(page, totalPages);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, players.size());
        PickYourDifficulty.debug("Displaying players[" + start + " → " + end + "]");

        // 💬 Show each player on current page
        for (Player player : players.subList(start, end)) {
            String difficulty = difficultyStorage.getDifficulty(player);

            // 🧮 Convert player playtime from ticks to seconds
            // Minecraft tracks PLAY_ONE_MINUTE in ticks (20 ticks = 1 second).
            // So we divide the raw value by 20 to get total seconds played.
            // This value is used to calculate how long the player has been online
            // during their current session.
            int playSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;

            // 🧮 Determine total grace period for this player's difficulty
            // Each difficulty level has a configured grace period (in seconds).
            // This is how long the player should be protected after joining.
            // For example, Easy might have 60s grace, while Hardcore has 0s.
            int graceTotal = ConfigManager.getGraceTime(difficulty);

            // 🧮 Calculate remaining grace time
            // graceRemaining = graceTotal - playSeconds
            // - graceTotal: how long grace lasts total (in seconds)
            // - playSeconds: how long the player has already been online (in seconds)
            // We clamp to 0 to avoid showing negative values if the player has exceeded the grace duration.
            int graceRemaining = Math.max(0, graceTotal - playSeconds);

            // 🧮 Get despawn time for this difficulty (used for display purposes)
            // This is how long dropped items last before disappearing, in seconds.
            int despawn = ConfigManager.getDespawnTime(difficulty);

            // 🧮 Format remaining grace time as human-readable string
            // For example: "45s", "1m 15s", etc. Used for display in the debug UI.
            String formattedGrace = TimeFormatUtil.formatSimple(graceRemaining);

            // 🧮 Calculate percent of grace time remaining
            // percentLeft = graceRemaining / graceTotal
            // This value is used for visual cues (e.g. color the name red if grace is almost over).
            // We cast one side to double to ensure floating-point division.
            // Example: if 6 seconds remain out of 30, then:
            //     graceRemaining / (double) graceTotal = 6 / 30 = 0.2 → 20% left
            // If graceTotal is 0 (e.g. Hardcore), default to 100% (1.0) to avoid divide-by-zero.
            double percentLeft = graceTotal > 0 ? (graceRemaining / (double) graceTotal) : 1.0;

            PickYourDifficulty.debug("→ " + player.getName() + " [" + difficulty + "] — graceRemaining=" + graceRemaining + "s (" + (int)(percentLeft * 100) + "%), despawn=" + despawn + "s");

            Component line = MessagesManager.format("debug.player-line", Map.of(
                    "player", player.getName(),
                    "difficulty", difficulty,
                    "grace", formattedGrace,
                    "despawn", String.valueOf(despawn)
            ));

            if (percentLeft < 0.05) line = line.color(NamedTextColor.RED);

            sender.sendMessage(line);
        }

        // 💬 Navigation bar
        if (totalPages > 1) {
            PickYourDifficulty.debug("Building navigation bar for players (" + totalPages + " pages total)");
            sender.sendMessage(buildPageNavigation("🧠 Online Player Data", "players", page, totalPages));
        }

        // 💬 Difficulty jump links
        Set<String> difficulties = new TreeSet<>();
        for (Player p : players) difficulties.add(difficultyStorage.getDifficulty(p));
        PickYourDifficulty.debug("Built difficulty jump line: " + difficulties);
        sender.sendMessage(buildDifficultyJumpLine(difficulties, "players"));
    }

    // ─────────────────────────────────────────────────────────────
    // 📦 Stored Data Pagination
    // ─────────────────────────────────────────────────────────────
    private void paginateStoredList(CommandSender sender, List<Map.Entry<UUID, String>> stored, int perPage, int page) {

        // 💬 Header: Show total number of stored players
        sender.sendMessage(MessagesManager.format("debug.stored-header", Map.of("count", String.valueOf(stored.size()))));

        // 🧮 Calculate total number of pages needed
        // totalPages = ceil(stored.size / perPage)
        // Always at least 1 page, even if there are 0 entries.
        int totalPages = Math.max(1, (int) Math.ceil(stored.size() / (double) perPage));
        PickYourDifficulty.debug("Paginating stored players — page " + page + "/" + totalPages + ", perPage=" + perPage);

        // 🧮 Clamp page number to be within range (1 to totalPages)
        page = Math.min(page, totalPages);

        // 🧮 Determine which slice of entries to show
        // start = (page - 1) * perPage
        // end = min(start + perPage, total)
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, stored.size());
        PickYourDifficulty.debug("Displaying stored[" + start + " → " + end + "]");

        // 💬 Show each stored entry in this page
        for (Map.Entry<UUID, String> entry : stored.subList(start, end)) {

            // 💬 Convert UUID to offline player name (fallback to "Unknown" if missing)
            OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
            String name = p.getName() != null ? p.getName() : "Unknown";

            // 💬 Get config values for this difficulty
            int grace = ConfigManager.getGraceTime(entry.getValue());
            int despawn = ConfigManager.getDespawnTime(entry.getValue());
            PickYourDifficulty.debug("→ " + name + " [" + entry.getValue() + "] — grace=" + grace + "s, despawn=" + despawn + "s");

            // 💬 Format stored line with hover showing UUID
            Component line = MessagesManager.format("debug.stored-line", Map.of(
                    "player", name,
                    "difficulty", entry.getValue(),
                    "grace", String.valueOf(grace),
                    "despawn", String.valueOf(despawn)
            )).hoverEvent(HoverEvent.showText(Component.text("UUID: " + entry.getKey())));
            sender.sendMessage(line);
        }

        // 💬 Add page navigation if there's more than 1 page
        if (totalPages > 1) {
            PickYourDifficulty.debug("Building navigation bar for stored entries (" + totalPages + " pages total)");
            sender.sendMessage(buildPageNavigation("📦 Stored Difficulty Data", "stored", page, totalPages));
        }

        // 💬 Add jump-to-difficulty links
        Set<String> difficulties = new TreeSet<>();
        for (Map.Entry<UUID, String> entry : stored) {
            difficulties.add(entry.getValue());
        }

        PickYourDifficulty.debug("Built difficulty jump line: " + difficulties);
        sender.sendMessage(buildDifficultyJumpLine(difficulties, "stored"));
    }

    // ─────────────────────────────────────────────────────────────
    // 📑 Navigation + Hover Builders
    // ─────────────────────────────────────────────────────────────
    private Component buildPageNavigation(String label, String subcommand, int current, int total) {

        // 🧱 Create base navigation line — example: "↪ Online Player Data"
        TextComponent.Builder nav = Component.text()
                .append(Component.text("↪ ", NamedTextColor.GRAY))
                .append(Component.text(label + " ", NamedTextColor.GOLD));

        // 🔢 Loop through all page numbers and create numbered buttons
        for (int i = 1; i <= total; i++) {

            // 💬 Highlight current page in yellow; others in gray
            Component num = Component.text("[" + i + "]",
                            i == current ? NamedTextColor.YELLOW : NamedTextColor.GRAY)

                    // 🖱️ Hover shows "Page X" as a tooltip
                    .hoverEvent(HoverEvent.showText(Component.text("Page " + i)))

                    // 🔘 Click runs /pyddebug subcommand X (e.g. /pyddebug players 2)
                    .clickEvent(ClickEvent.runCommand("/pyddebug " + subcommand + " " + i));

            // ➕ Append to the navigation line with spacing
            nav.append(Component.space()).append(num);
        }

        // 📤 Return the final built navigation bar
        return nav.build();
    }

    private Component buildDifficultyJumpLine(Collection<String> difficulties, String target) {

        // 🧱 Start line with label — example: "↪ Jump to Players with Difficulty"
        TextComponent.Builder jump = Component.text()
                .append(Component.text("↪ Jump to Players with Difficulty ", NamedTextColor.GRAY));

        for (String diff : difficulties) {

            // 📎 Create clickable difficulty tag like [Easy], [Hard], etc.
            Component tag = Component.text("[" + diff + "]", NamedTextColor.GOLD)

                    // 🖱️ Hover shows tooltip "Scroll to Easy", etc.
                    .hoverEvent(HoverEvent.showText(Component.text("Scroll to " + diff)))

                    // 🧪 Use suggestCommand to show user the filter without running it
                    // This lets users click it, see the command fill in chat, and modify if needed
                    .clickEvent(ClickEvent.suggestCommand("/pyddebug " + target + " 1"));

            // ➕ Append to jump line with spacing
            jump.append(tag).append(Component.space());
        }

        // 📤 Return the built jump line component
        return jump.build();
    }

    // ─────────────────────────────────────────────────────────────
    // 🔌 Hook Checker
    // ─────────────────────────────────────────────────────────────
    private void checkPlugin(CommandSender sender, String plugin, boolean expected) {

        // 🔍 Check if the plugin is loaded on the server
        boolean loaded = Bukkit.getPluginManager().isPluginEnabled(plugin);

        // 🧮 Build status string based on plugin presence + expectation
        //
        // ✅ Loaded and required → "<green>Loaded ✅"
        // ⚠ Loaded but optional → "<yellow>Loaded ⚠ (not required)"
        // ❌ Missing but required → "<red>Missing ❌"
        // 💤 Missing and optional → "<gray>Not required"
        String status = loaded
                ? (expected ? "<green>Loaded ✅" : "<yellow>Loaded ⚠ (not required)")
                : (expected ? "<red>Missing ❌" : "<gray>Not required");

        // 💬 Send the plugin status line using the configured format
        sender.sendMessage(MessagesManager.format("debug.hook-line", Map.of(
                "plugin", plugin,
                "status", status
        )));
    }
}