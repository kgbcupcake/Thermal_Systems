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
- Cold Sweat integration now actually pushes players into Hot/Cold near a source, in both
  directions. It previously reused Cold Sweat's `WarmthTempModifier`/`FrigidnessTempModifier` -
  Hearth/Icebox comfort modifiers whose `calculate()` only ever nudges temperature back toward Cold
  Sweat's neutral midpoint, and can never push it past that midpoint - so cooling only ever worked
  while already warmer than neutral (never the common case) and heating only while already colder.
  Now uses Cold Sweat's `SimpleTempModifier` with an unconditional add, which works regardless of
  the player's current temperature, the way an actual heat/cooling source needs to.
- Cold Sweat integration now actually updates the player's warmth/cooling every time it changes,
  instead of applying it once and freezing forever. `Temperature.addModifier(..., Placement.LAST.noDuplicates(...))`
  only inserts a modifier when none of the same class exists yet, so every call after the first
  silently did nothing - the bridge's own logs showed the correct, changing values the whole time,
  but Cold Sweat never received any update past the first one. Now uses Cold Sweat's
  `replaceOrAddModifier`, which updates the existing modifier in place.
- Cold Sweat integration no longer silently discards a genuine heat/cooling contribution: any
  ambient delta under ~5C (with the default `outputScale`) used to round down to `WarmthTempModifier`/
  `FrigidnessTempModifier` strength 0, so an active but modest heat source produced no effect at all.
  A nonzero delta is now floored at strength 1 instead of rounding away to nothing.
- Opening the Ender IO Stirling Generator control panel (and the draggable HUD control panel) no
  longer crashes the client with `NoClassDefFoundError`; both followed MariesLib's `DraggableResizable`
  after it moved from `dev.marie.framework.ui.edit` to `dev.marie.framework.ui.drag`.
- Chunk loads/unloads no longer flood the log: source add/remove tracking and the per-chunk Ender IO
  unload line now log only at DEBUG, and only with `radiationDebugEnabled` on.
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
- Direct radiation no longer drops a player standing right next to an Ender IO Stirling Generator
  whose conduit run happens to be farther than `sourceRadiationRadius` away (previously logged as
  "no longer has a tracked source within radiation radius" and delivered no warmth even while next
  to the visible machine). The generator's own position is tracked again alongside its conduits, and
  now reports the same network id its conduits do (via a direct, capability-free flood-fill), so
  being in range of either the generator or its conduit dedupes to one contribution instead of
  double-counting.
- Fixed a server-crashing `StackOverflowError` in the Ender IO integration: the generator-tracking
  fix above briefly had the generator's heat/cooling output delegate into its adjacent conduit
  network's summed value, but that network sum itself queries the generator's own output as part of
  its boundary scan, so the two called each other forever. The generator now only shares its
  conduit network's *id* (computed independently, without touching the network's cached sum) and
  keeps reporting its own single-machine output directly, breaking the cycle.
- `SourceRadiationTickHandler`'s direct-radiation debug/log output no longer lists the same network
  under both `heatNetworks` and `coolingNetworks` when only one side is actually producing anything;
  a source's inactive side (e.g. a heat/cool-mode-gated Ender IO generator sitting in Heat mode) no
  longer gets recorded into the opposite side's network set just because its capability resolved.
- Ender IO Stirling Generator heat/cooling output is no longer wildly too high and no longer swings
  up and down as the generator's buffer fills and drains. It previously scaled output by the
  generator's raw stored FE, an unbounded amount that grows with buffer capacity; it now scales by
  the buffer's fill fraction (0.0-1.0), so `energyToHeatCoefficient` (default raised from `0.001` to
  `60.0`) is now the output at a full buffer, bounded and stable regardless of buffer size. A stored
  amount reported above capacity is clamped to a full buffer instead of throwing, since a foreign
  mod's `IEnergyStorage` isn't guaranteed to hold that invariant at every instant - the throw was
  silently blanking the Ender IO conduit's entire Jade tooltip whenever it happened.

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
