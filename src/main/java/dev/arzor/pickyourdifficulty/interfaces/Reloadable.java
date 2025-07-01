// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🔁 Reloadable.java                             ║
// ║   Interface for components that support dynamic reload behavior    ║
// ║   Used by ConfigManager, MessagesManager, and others               ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.interfaces;

// ─────────────────────────────────────────────────────────────
// 🧩 Reloadable Interface
// ─────────────────────────────────────────────────────────────

/**
 * 🔁 Reloadable
 * This interface defines the contract for any plugin component that
 * supports being reloaded while the server is running.

 * Typical use cases include:
 * - ConfigManager (reload config.yml)
 * - MessagesManager (reload messages.yml)

 * Classes that implement this will be picked up by ReloadManager.
 * They must implement a safe and idempotent `reload()` method.
 */
public interface Reloadable {

    /**
     * ⏳ Reload logic for this component.

     * This method should:
     * - Safely reload internal state
     * - Re-read any config or file data as needed
     * - Not assume prior state exists (clean slate)

     * 🔁 It may be called multiple times safely (idempotent).
     */
    void reload();
}
