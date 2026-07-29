# Changelog

All notable changes to Thermal Systems are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to the version in [`gradle.properties`](gradle.properties).

> This file is maintained as part of the project's normal workflow: every change made through a
> Claude Code session is logged here under `[Unreleased]` as it happens, and moved under a version
> heading at release time. See `CLAUDE.md` for the house rule.

## [Unreleased]

### Added
- README, CHANGELOG, and CLAUDE.md project documentation, including Cloth Config API in the
  requirements list as a required runtime dependency for the client config screen.

## [0.0.1-beta] - 2026-07-29

### Added
- Cold Sweat integration with a thermal bridge syncing zone temperature to player body temperature.
- Client config screen with export/import support.
- Ender IO output multiplier setting.
- Ender IO integration: heat/cool toggle blocks, networking, and hover providers.

### Changed
- Reworked source tracking (`ActiveSourcePositions`, `SourceRadiationTickHandler`) to close
  chunk-lifecycle gaps where active heat/cooling sources could be lost on unload/reload.

### Removed
- Thermal Exchanger and related components, in favor of the capability-based
  `IHeatSource` / `ICoolingSource` model.

---

[Unreleased]: https://github.com/kgbcupcake/Thermal_Systems/compare/v0.0.1-beta...HEAD
[0.0.1-beta]: https://github.com/kgbcupcake/Thermal_Systems/releases/tag/v0.0.1-beta
