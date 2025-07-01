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

public class CommandPydDebug implements CommandExecutor {

    // ─────────────────────────────────────────────────────────────
    // 📦 Storage
    // ─────────────────────────────────────────────────────────────

    private final PlayerDifficultyStorage difficultyStorage = PickYourDifficulty.getInstance()
            .getPlayerDataManager()
            .getDifficultyStorage();

    // ─────────────────────────────────────────────────────────────
    // ⚙️ Command Execution
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        // 💬 Admin-only: Make sure the sender has permission to view debug info
        if (!PermissionUtil.isAdmin(sender)) {
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
                } else if (args[0].equalsIgnoreCase("stored")) {
                    onlyStored = true;
                    storedPage = page;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessagesManager.format("debug.invalid-page"));
                return true;
            }
        }

        // ╔═══🌐 General plugin info═══════════════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored) {
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
            int perPage = ConfigManager.getDebugOnlinePlayersPerPage();
            paginatePlayerList(sender, allPlayers, perPage, playersPage);
        }

        // ╔═══📦 Stored difficulty data (paginated)═══════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat) {
            Map<UUID, String> stored = difficultyStorage.getAllDifficultyData();
            List<Map.Entry<UUID, String>> storedList = new ArrayList<>(stored.entrySet());
            storedList.sort(Comparator.comparing(entry -> {
                OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
                return p.getName() != null ? p.getName() : "~";
            }));
            int perPage = ConfigManager.getDebugStoredDifficultyPerPage();
            paginateStoredList(sender, storedList, perPage, storedPage);
        }

        // ╔═══👁️ Hologram visibility summary══════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored && ConfigManager.hologramsEnabled()) {
            sender.sendMessage(MessagesManager.format("debug.holograms-hidden", Map.of("count", String.valueOf(HologramManager.getHiddenPlayers().size()))));
            sender.sendMessage(MessagesManager.format("debug.holograms-active", Map.of("count", String.valueOf(HologramManager.getHologramMap().size()))));
        }

        // ╔═══♻️ Reloadable class summary═════════════════════════════════════════════════════════════════════╗
        if (!onlyPlayersFlat && !onlyStored) {
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

        // 💬 Clamp current page to be within bounds
        page = Math.min(page, totalPages);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, players.size());

        // 💬 Show each player on current page
        for (Player player : players.subList(start, end)) {
            String difficulty = difficultyStorage.getDifficulty(player);

            // 🧮 Convert playtime from ticks to seconds (20 ticks = 1 second)
            int playSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;

            int graceTotal = ConfigManager.getGraceTime(difficulty);
            int graceRemaining = Math.max(0, graceTotal - playSeconds);
            int despawn = ConfigManager.getDespawnTime(difficulty);

            String formattedGrace = TimeFormatUtil.formatSimple(graceRemaining);

            // 🧮 Calculate percent grace remaining
            double percentLeft = graceTotal > 0 ? (graceRemaining / (double) graceTotal) : 1.0;

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
            sender.sendMessage(buildPageNavigation("🧠 Online Player Data", "players", page, totalPages));
        }

        // 💬 Difficulty jump links
        Set<String> difficulties = new TreeSet<>();
        for (Player p : players) difficulties.add(difficultyStorage.getDifficulty(p));
        sender.sendMessage(buildDifficultyJumpLine(difficulties, "players"));
    }

    // ─────────────────────────────────────────────────────────────
    // 📦 Stored Data Pagination
    // ─────────────────────────────────────────────────────────────

    private void paginateStoredList(CommandSender sender, List<Map.Entry<UUID, String>> stored, int perPage, int page) {
        sender.sendMessage(MessagesManager.format("debug.stored-header", Map.of("count", String.valueOf(stored.size()))));

        int totalPages = Math.max(1, (int) Math.ceil(stored.size() / (double) perPage));
        page = Math.min(page, totalPages);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, stored.size());

        for (Map.Entry<UUID, String> entry : stored.subList(start, end)) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
            String name = p.getName() != null ? p.getName() : "Unknown";

            int grace = ConfigManager.getGraceTime(entry.getValue());
            int despawn = ConfigManager.getDespawnTime(entry.getValue());

            Component line = MessagesManager.format("debug.stored-line", Map.of(
                    "player", name,
                    "difficulty", entry.getValue(),
                    "grace", String.valueOf(grace),
                    "despawn", String.valueOf(despawn)
            )).hoverEvent(HoverEvent.showText(Component.text("UUID: " + entry.getKey())));

            sender.sendMessage(line);
        }

        if (totalPages > 1) {
            sender.sendMessage(buildPageNavigation("📦 Stored Difficulty Data", "stored", page, totalPages));
        }

        Set<String> difficulties = new TreeSet<>();
        for (Map.Entry<UUID, String> entry : stored) difficulties.add(entry.getValue());
        sender.sendMessage(buildDifficultyJumpLine(difficulties, "stored"));
    }

    // ─────────────────────────────────────────────────────────────
    // 📑 Navigation + Hover Builders
    // ─────────────────────────────────────────────────────────────

    private Component buildPageNavigation(String label, String subcommand, int current, int total) {
        TextComponent.Builder nav = Component.text()
                .append(Component.text("↪ ", NamedTextColor.GRAY))
                .append(Component.text(label + " ", NamedTextColor.GOLD));
        for (int i = 1; i <= total; i++) {
            Component num = Component.text("[" + i + "]", i == current ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Page " + i)))
                    .clickEvent(ClickEvent.runCommand("/pyddebug " + subcommand + " " + i));
            nav.append(Component.space()).append(num);
        }
        return nav.build();
    }

    private Component buildDifficultyJumpLine(Collection<String> difficulties, String target) {
        TextComponent.Builder jump = Component.text().append(Component.text("↪ Jump to Players with Difficulty ", NamedTextColor.GRAY));
        for (String diff : difficulties) {
            jump.append(
                    Component.text("[" + diff + "]", NamedTextColor.GOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Scroll to " + diff)))
                            .clickEvent(ClickEvent.suggestCommand("/pyddebug " + target + " 1"))
            ).append(Component.space());
        }
        return jump.build();
    }

    // ─────────────────────────────────────────────────────────────
    // 🔌 Hook Checker
    // ─────────────────────────────────────────────────────────────

    private void checkPlugin(CommandSender sender, String plugin, boolean expected) {
        boolean loaded = Bukkit.getPluginManager().isPluginEnabled(plugin);
        String status = loaded
                ? (expected ? "<green>Loaded ✅" : "<yellow>Loaded ⚠ (not required)")
                : (expected ? "<red>Missing ❌" : "<gray>Not required");

        sender.sendMessage(MessagesManager.format("debug.hook-line", Map.of(
                "plugin", plugin,
                "status", status
        )));
    }
}