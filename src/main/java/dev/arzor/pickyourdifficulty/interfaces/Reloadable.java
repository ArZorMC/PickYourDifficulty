// ╔════════════════════════════════════════════════════════════════════╗
// ║                     🔁 Reloadable.java                             ║
// ║   Interface for components that support dynamic reload behavior    ║
// ║   Used by ConfigManager, MessagesManager, and others               ║
// ╚════════════════════════════════════════════════════════════════════╝

package dev.arzor.pickyourdifficulty.interfaces;

// ─────────────────────────────────────────────────────────────
// 🧩 Reloadable Interface
// ─────────────────────────────────────────────────────────────
// This interface is used for components that can be safely reloaded
// while the plugin is running (e.g., config, messages, etc.)
//
// Implementing classes are registered with ReloadManager and
// must define a safe and repeatable reload() method.

public interface Reloadable {

    // ╔═══🔁 reload() — Live reload entry point════════════════════════════════════╗
    // This method is called by ReloadManager when a /pyd reload occurs.
    // Components should:
    //  - Re-read any external file or state (e.g., config.yml)
    //  - Clear and rebuild any internal data structures
    //  - Avoid assuming the prior state is valid
    //  - Be safe to call multiple times (idempotent)

    void reload();
}
