# 🧩 PickYourDifficulty

**Let your players choose how they want to play.**  
PickYourDifficulty is a flexible, Bedrock-compatible Paper plugin that lets players select their own difficulty level via an in-game GUI.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21-blue?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
![Build](https://img.shields.io/badge/build-Maven-blue?style=flat-square)
## 🎮 Features

- 🕹️ Easy-to-use selector GUI with fully customizable options
- ⏳ Per-difficulty item despawn timers
- 🛡️ Grace period protection for new or respawning players
- 🌀 Prevents grief by blocking short-timer trolls
- 📋 Supports PlaceholderAPI and MiniMessage formatting
- 🧩 Full Geyser support (Bedrock-compatible)
- 🪧 Holograms above dropped items (DecentHolograms)
- 🧠 Changeable or permanent difficulty selection (configurable)
- 🛠️ Easy admin overrides and permission control

---

## 📦 Installation

1. 📥 Download the plugin `.jar` or build it yourself:
    ```bash
    mvn clean package
    ```
2. 🔧 Drop the compiled JAR into your server’s `/plugins/` folder
3. 🚀 Restart or reload the server
4. ✍️ Edit `config.yml` to tweak behaviors:
    ```bash
    /pyd reload
    ```

---
5. (Optional) Install:
    - **PlaceholderAPI** for %pickyourdifficulty_*% support
    - **AcceptTheRules** to show GUI only after players agree to server rules
    - **DecentHolograms** for despawn timers above dropped items

---

## 🧪 Commands

| Command                   | Description                              | Permission                          |
|--------------------------|------------------------------------------|--------------------------------------|
| `/pyd gui`               | Open the difficulty selector              | `pickyourdifficulty.gui`             |
| `/pyd info`              | View your current difficulty              | `pickyourdifficulty.info`            |
| `/pyd reload`            | Reload the plugin config                  | `pickyourdifficulty.reload`          |
| `/pyd set <player> <diff>` | Force-set a player’s difficulty         | `pickyourdifficulty.set`             |
| `/pyd reset <player>`    | Reset a player’s difficulty               | `pickyourdifficulty.reset`           |
| `/pyd toggleholograms`   | Toggle hologram display above items       | `pickyourdifficulty.toggleholograms` |

---
## 🔧 Configuration Highlights

```yaml
despawnBehavior:
  onlyAffectsDeathDrops: true      # Only apply custom timers to death drops
  preventTimerDowngrade: true      # Protect long-timer items from short-timer grief

grace:
  enabled: true
  defaultDurationSeconds: 1800     # 30 minutes of invincibility
  reminderIntervalSeconds: 300     # Remind every 5 minutes

difficultyPresets:
  hardcore:
    label: "<dark_red><bold>Hardcore</bold></dark_red>"
    despawnTimeSeconds: 30
    graceTimeSeconds: 0
    commands:
      - "console:msg <player> Good luck..."
```
## 🛡️ Permissions

See [`plugin.yml`](plugin.yml) for full permission node list.  
Includes wildcards, admin bypasses, and toggle holograms support.

---

## 📁 Configuration

See [`config.yml`](config.yml) for the full, structured, and documented config format.

---

## 📜 License

This plugin is provided under the terms of the [MIT License](LICENSE.txt).