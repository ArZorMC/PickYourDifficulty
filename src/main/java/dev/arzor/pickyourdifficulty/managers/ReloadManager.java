// ╔════════════════════════════════════════════════════════════════════╗
// ║                         🔁 ReloadManager.java                      ║
// ║  Central registry for components that support live reloading.      ║
// ║  Calls reload() on all registered Reloadable classes.              ║
// ║                                                                    ║
// ║  To register a reloadable class:                                   ║
// ║    static { ReloadManager.register(new YourManager()); }           ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.managers;

import dev.arzor.pickyourdifficulty.PickYourDifficulty;
import dev.arzor.pickyourdifficulty.interfaces.Reloadable;
import dev.arzor.pickyourdifficulty.storage.CooldownTracker;
import dev.arzor.pickyourdifficulty.storage.PlayerDifficultyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ─────────────────────────────────────────────────────────────
// 🔁 ReloadManager — Live reload support + registry
// ─────────────────────────────────────────────────────────────
public class ReloadManager {

    // ╔═══🗂️ Internal Reloadable Registry══════════════════════════════╗

    // Live reference to all registered reloadable classes
    private static final List<Reloadable> reloadables = new ArrayList<>();

    // ╔═══➕ Register a Reloadable Component════════════════════════════╗
    public static void register(Reloadable reloadable) {
        // 🛡️ Avoid nulls or duplicate entries
        if (reloadable != null && !reloadables.contains(reloadable)) {
            reloadables.add(reloadable);

            // 🧪 Debug: Log registration
            PickYourDifficulty.debug("🔁 Registered reloadable: " + reloadable.getClass().getSimpleName());
        }
    }

    // ╔═══♻️ Reload All Registered Components═══════════════════════════╗
    public static void reloadAll() {
        // 📦 Reload core persistent storage before anything else
        PlayerDifficultyStorage.getInstance().loadFromDisk();
        CooldownTracker.loadFromDisk();

        PickYourDifficulty.debug("♻️ Reloading all registered components (" + reloadables.size() + " total)...");

        // 🔁 Reload every registered component
        for (Reloadable reloadable : reloadables) {
            PickYourDifficulty.debug("↩️ Reloading: " + reloadable.getClass().getSimpleName());
            reloadable.reload();
        }

        PickYourDifficulty.debug("✅ Reload complete.");
    }

    // ╔═══📚 Get Read-Only View of Registry═════════════════════════════╗
    public static List<Reloadable> getReloadables() {
        // 🔒 Return immutable snapshot of registered reloadables
        return Collections.unmodifiableList(reloadables);
    }

    // ╔═══🧼 Clear Registry (Dev Use Only)═══════════════════════════════╗
    public static void clearAll() {
        reloadables.clear();

        // 🧪 Debug: Confirm clear
        PickYourDifficulty.debug("🧼 Cleared all registered reloadables");
    }
}
