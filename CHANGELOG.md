# 📜 Changelog – PickYourDifficulty

All notable changes to this project will be documented in this file.

---

## [1.0.0] - 2025-06-24

### Added
- Core plugin architecture and class structure
- Difficulty selector GUI with configurable presets
- Per-difficulty item despawn timers and grace periods
- `/pyd` command system:
    - `/pyd gui`, `/pyd info` (player)
    - `/pyd set`, `/pyd reset`, `/pyd reload` (admin)
    - `/pyd toggleholograms` (player)
- Configurable GUI sound effects and MiniMessage formatting
- Cooldown system with bypass permission support
- PlaceholderAPI support (`%pickyourdifficulty_difficulty%`)
- Optional hologram integration (DecentHolograms)
- AcceptTheRules integration (auto-GUI after rule acceptance)
- Bedrock support via Geyser
- Fully documented `plugin.yml` and `config.yml`
- Clean Maven POM with proper external repositories