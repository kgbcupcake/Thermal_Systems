# Changelog

All notable changes to Thermal Systems are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to the version in [`gradle.properties`](gradle.properties).

> This file is maintained as part of the project's normal workflow: every change made through a
> Claude Code session is logged here under `[Unreleased]` as it happens, and moved under a version
> heading at release time. See `CLAUDE.md` for the house rule.

## [Unreleased]

### Added
- `/thermal debug radiation` (op only) now ships with the core mod and reports each player's applied
  radiation temperature; new `radiationDebugEnabled` (default off) and `radiationChangeEpsilon`
  options on the Simulation tab.
- Optional Tough As Nails integration: heat and cooling from zones and nearby sources now shift a
  player's Tough As Nails temperature by one or two levels once they cross configurable
  thresholds, never pushing a player into Hot or Icy from outside it. Adds a Tough As Nails tab to
  the config screen, and `/thermal debug radiation` (op only, available with Tough As Nails
  installed) to show each player's stored value, step count, and Tough As Nails level before/after.
- README, CHANGELOG, and CLAUDE.md project documentation, including Cloth Config API in the
  requirements list as a required runtime dependency for the client config screen.

### Fixed
- Direct-radiation handler no longer logs "no longer has a tracked source" and "temperature changed"
  every interval for an idle player with nothing nearby. Both now log only on a real transition (a
  source leaving range, or the value moving more than the new `radiationChangeEpsilon`); per-player
  state is reset on logout and dimension change. Bridge calls are unchanged.
- Dedicated servers with Ender IO integration no longer reject clients with "channel missing on the
  server side" for `thermalsystems:enderio_mode_response`; the response packet is now registered on
  both sides while its handling stays client-only.
- Ender IO Stirling Generator heat/cool mode now persists across server restarts, stored as a data
  attachment on the generator's own block entity instead of an in-memory registry that silently
  reset every generator to heating on restart.
- Cold Sweat thermal bridge no longer crashes the server tick loop when Cold Sweat's own
  modifier-update internals throw; it now always re-adds modifiers through Cold Sweat's
  deduplicating API instead of mutating them manually, and catches (and logs) any exception from
  Cold Sweat rather than letting it propagate.
- Cold Sweat and LSO thermal bridges no longer spam the log every interval; they now log only when
  the delivered value actually changes for a given caller, matching the direct-radiation handler's
  existing behavior. The temperature effect itself is still applied on every call.
- Ender IO integration no longer logs a line for every chunk unload in the world regardless of
  logging settings; that diagnostic log is now gated behind `LOGGING_ENABLED`/
  `RADIATION_LOGGING_ENABLED` like the rest of the module's logging.
- Direct radiation now warms/cools players standing near an Ender IO conduit fed by a generator
  outside `sourceRadiationRadius`, instead of only near the generator itself. Direct-radiation
  tracking follows conduit positions rather than the generator, and sums each distinct conduit
  network's heat/cooling exactly once even when several of its segments are simultaneously in
  range, instead of double-counting the network total once per nearby segment.

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
