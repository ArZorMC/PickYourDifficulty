// ╔════════════════════════════════════════════════════════════════════╗
// ║                         🔁 ReloadManager.java                      ║
// ║  Central registry for components that support live reloading.      ║
// ║  Calls reload() on all registered Reloadable classes.              ║
// ║                                                                    ║
// ║  To register a reloadable class:                                   ║
// ║    static { ReloadManager.register(new YourManager()); }           ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.interfaces.Reloadable;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ─────────────────────────────────────────────────────────────
// 📦 ReloadManager: Live reload support system
// ─────────────────────────────────────────────────────────────

public class ReloadManager {

    // ╔════════════════════════════════════════════════════════════╗
    // 🧱 Internal Registry
    // ╚════════════════════════════════════════════════════════════╝

    /** Holds all registered reloadable components */
    private static final List<Reloadable> reloadables = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    // ➕ Register a Component
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a Reloadable component to be reloaded later.
     *
     * @param reloadable The reloadable instance
     */
    public static void register(Reloadable reloadable) {
        // ✅ Avoid nulls or duplicates
        if (reloadable != null && !reloadables.contains(reloadable)) {
            reloadables.add(reloadable);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ♻️ Reload All
    // ─────────────────────────────────────────────────────────────

    /**
     * Reloads all registered components in order.
     * This includes core YAML state and any custom managers.
     */
    public static void reloadAll() {

        // 📦 Load core persistent storage first
        PlayerDifficultyStorage.getInstance().loadFromDisk();
        CooldownTracker.loadFromDisk();

        // 🔁 Reload all other registered components
        for (Reloadable reloadable : reloadables) {
            if (ConfigManager.isDebugMode()) {
                System.out.println("[PickYourDifficulty] Reloading: " + reloadable.getClass().getSimpleName());
            }
            reloadable.reload(); // Call instance method
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 📚 Read-Only View of Registry
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable list of registered reloadables.
     *
     * @return List of Reloadable instances
     */
    public static List<Reloadable> getReloadables() {
        return Collections.unmodifiableList(reloadables);
    }

    // ─────────────────────────────────────────────────────────────
    // 🧼 Dev Reset
    // ─────────────────────────────────────────────────────────────

    /**
     * Clears all registered reloadables — not normally needed except for testing.
     */
    public static void clearAll() {
        reloadables.clear();
    }
}
