# Changelog

All notable changes to Thermal Systems are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to the version in [`gradle.properties`](gradle.properties).

> This file is maintained as part of the project's normal workflow: every change made through a
> Claude Code session is logged here under `[Unreleased]` as it happens, and moved under a version
> heading at release time. See `CLAUDE.md` for the house rule.

## [Unreleased]

### Added
- Two Zones config options, `playerRoomReach` and `machineRoomReach` (also in the in-game config
  screen's Zones page): how many blocks a player, or a fixed machine/conduit/wall thermostat, may
  sit outside a zone's bounds and still count as belonging to it.

### Changed
- Ender IO Stirling Generator and conduit tooltips show one output - heating/cooling/standby with
  the room and target temperature. The separate "Power ... C/s" line is gone.

### Fixed
- Standing in a heated zone no longer freezes you when it's cold outside. The outdoor seasonal
  temperature was being added on top of the room's temperature (and counted twice outside zones).
- Standing inside a heated/cooled room no longer gets treated as being outside it. Zone membership
  for the player, Cold Sweat, Tough As Nails, and direct source radiation used an exact-bounds
  check against the player's feet position, so a zone marked out at floor level (the common case)
  missed the player entirely - the thermostat showed the room at target while the player still felt
  outdoor/seasonal temperature. Now resolves to the nearest zone within `playerRoomReach` blocks
  first, the same tolerance a wall thermostat already used for its own room.
- Ender IO generators and conduit networks resolve their controlling zone with the same
  `machineRoomReach` tolerance, instead of requiring the generator/conduit block itself to sit
  exactly inside the zone's bounds - reduces output flicker for a machine placed just outside a
  room's marked box.
- Cold Sweat: inside a zone the world temperature you feel is now the room's own temperature,
  instead of a small nudge on top of Cold Sweat's outdoor biome/depth/season temperature, so a
  heated room is actually warm in winter. Rooms between about 50°F and 100°F (10-38°C) are
  comfortable with Cold Sweat's default settings.
- Tough As Nails: inside a zone your temperature now comes from the room itself, instead of only
  nudging the outdoor temperature by a level or two, so a heated room is actually warm in winter.
  A room set very hot or very cold can now make you Hot or Icy.

## [0.0.1-beta.1] - 2026-10-07

### Added
- Ecliptic Seasons and Serene Seasons: the air temperature zones drift toward, and the temperature
  you feel outside a zone, follows the current season. When both are installed, Ecliptic Seasons'
  solar terms are used. Each gets a config page only while that mod is installed.
- New in-game config editor (the Mods list's Config button, or the new unbound "Open Thermal Systems
  Config" keybind): a Marie's lib window with a sidebar of module boxes - Simulation, Integration,
  one per integration, and Tools - each with tabbed sliders/toggles and Reset This Module/Tab. It
  covers every config value, including Default Zone Target and each integration's Enabled switch,
  which the old screen never showed.
- Temperature settings show their ideal value (the neutral point where Thermal Systems adds no heat
  or cold of its own, e.g. Cold Sweat/LSO offsets matching the ambient temperature) with a one-click
  button to use it.
- On a server, the config editor shows and edits the server's own config; only ops (and the owner of
  a singleplayer/LAN world) can change values, everyone else sees them read-only.
- The classic Cloth Config screen, Export and Import are still available from the editor's Tools page.
- The config editor opens on a new Home page: your character rendered in 3D (it follows the mouse)
  next to your live zone and nearby-source temperatures, the core settings, the Ender IO generator
  mode, and whether each integration is active.
- Ender IO "Cooling Mode" switch on the Ender IO config page: when on, Stirling Generators cool
  instead of heat.
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
- Zone Gadget: right-click two corners to mark out a room as a climate zone, with a live wireframe
  preview of nearby zones and your selection (red when it's too big or overlaps another zone) and a
  size readout above the hotbar. Sneak + use opens its menu to name the zone, set its target and
  switch between Create, Edit (resize or delete your zone), Inspect (print a zone's details) and
  Copy (copy one zone's thermostat settings and paste them onto others) modes.
- Thermostat Tablet: use it to see every nearby zone and open a thermostat for any of them - live
  temperature, target (+/- in 0.5 steps, 5 with Shift), Off/Heat/Cool/Auto mode, and the heaters and
  coolers it's using.
- Wall Thermostat: sneak + use the tablet on a wall to mount it as a room thermostat. Its screen
  shows the room's live temperature, target and status; tap its -, Mode and + buttons to adjust it
  in place, or tap the screen for the full thermostat. It outputs a redstone signal while the room is
  calling for heating or cooling.
- Zones now record an owner. Only the owner (or an op) can resize, delete or lock a zone; the owner
  can turn on "Anyone can adjust" so other players can change its target and mode.
- Zone editor: once you've clicked both corners with the Zone Gadget (or picked up a zone in Edit
  mode), a tablet-style editor opens with a 3D model of the area you can drag to rotate and scroll
  to zoom. Grow or shrink the box along X, Y and Z with From/To steppers (Shift for steps of 5),
  name the zone (or rename an existing one), set its target, and Save. It flags a box that's too
  big, too far away, overlapping another zone or using a taken name before you save. The gadget
  menu's Create Zone / Apply New Bounds buttons are replaced by Open Editor.
- New Zones config page: Max Zone Volume and Max Zones Per Player for non-op players.
- `/thermal zone delete <name>` (op only).
- Crafting recipes for the Zone Gadget and Thermostat Tablet, and a Thermal Systems creative tab.
- Fahrenheit display option: switch between °C and °F with the button in the top-right of the
  tablet and Zone Gadget screens, or on the config editor's new Display page. It's a personal
  setting (saved in `thermalsystems-client.toml`) and applies everywhere you see a temperature -
  tablet, wall thermostat, tooltips, chat messages and the config editor's temperature sliders. In
  Fahrenheit the thermostat +/- buttons step by 1°F (10°F with Shift).

### Changed
- The config editor's sidebar, Home integrations list, and classic config screen only list an
  integration when that mod is installed. Legendary Survival Overhaul and Tough As Nails no longer
  show up as "not installed".
- The Thermostat Tablet, while held, is a small item. The "21" painted on its icon is gone; the
  live temperature is on the wall thermostat's screen.
- Requires MariesLib 0.1.2-beta.1 or newer.
- README rewritten for the current mod: zones and thermostats, the Zone Gadget, Thermostat Tablet
  and Wall Thermostat, the config editor, every integration (including Tough As Nails and the
  seasons mods), commands, config files, the public API and build setup.
- The Mods list's Config button now opens the new config editor instead of the Cloth Config screen.
- A zone's mode now decides what runs: Heat only uses heaters, Cool only uses coolers, Auto uses
  whichever it needs, and Off uses neither. When a zone isn't calling, it drifts back toward the
  ambient temperature.
- Zones behave like a real thermostat: they start heating or cooling once they're more than 0.5°C
  off target and stop once they reach it.
- New zones start in Auto mode.
- Ender IO Stirling Generators and their conduit networks inside a zone now follow that zone's
  thermostat: they heat while it calls for heat, cool while it calls for cooling, and stand by once
  it's at target or switched off - including the warmth nearby players feel. The Ender IO Cooling
  Mode switch now only decides what generators outside any zone do.
- Hovering a generator or conduit shows whether it's heating, cooling or on standby, and which zone
  controls it. The tablet shows what a zone's heaters and coolers can deliver.

### Removed
- The HEAT/COOL toggle drawn over Ender IO's Stirling Generator screen, its "Toggle Reposition
  Mode" keybind, and `/thermal enderio setMode`. Generators no longer have their own mode; use the
  Ender IO config page's Cooling Mode switch instead.

### Fixed
- Zone status and Cold Sweat / Legendary Survival Overhaul / Tough As Nails temperature traces no
  longer print every simulation tick. They are debug logs now, so a normal server log stays quiet.
- Ender IO conduits just outside a zone (e.g. under the floor) no longer keep heating while the
  room is at target; a conduit network or Stirling Generator outside every zone now follows the
  zone its generator or other conduits are in, and its tooltip names that zone.
- A Wall Thermostat no longer shows "No zone" when it's mounted just outside the room's zone box
  (e.g. a zone marked by clicking two floor corners); it now picks up the closest zone within 6
  blocks.
- Zones are now saved with the world; previously every zone was lost when the server restarted.
- A zone counted an Ender IO conduit network once for every conduit block inside it (plus the
  generator), multiplying its heat - seven blocks of one 75 C/s network showed as 525 C/s. Each
  network now counts once, for zones and for nearby-player warmth.
- That network fix didn't apply to sources a zone binds automatically, so a Stirling Generator and
  its conduits still showed as 7 heaters on the tablet (with +0.0 output while standing by) and
  heated the room seven times over. Each network now really counts once, and the tablet shows what
  it can deliver.
- Raising a zone's target no longer makes players inside it far hotter than the target: they used
  to feel the zone's temperature plus extra radiated heat from every nearby heater on top. Inside a
  zone you now feel exactly the zone's temperature.
- A zone whose target is within 0.5°C of the ambient temperature (e.g. 20.5°C with the default
  20°C ambient) no longer sits on standby forever without reaching it; the thermostat now kicks in
  sooner for targets that close to ambient.
- Generator and conduit tooltips now show the room's temperature and target, and label the output
  (e.g. 75 C/s) as a rate rather than a temperature.
- Resizing a zone now picks up heaters and coolers inside its new area.
- Leaving a singleplayer world no longer carries its zones and source bindings into the next world
  you open.
- The config editor Home page's 3D player no longer has the back of its head and hat layer cut away
  when it tilts to follow the mouse, and its hat/hood layer no longer separates from the head when an
  Entity Model Features animation pack (e.g. Detailed Animations) is active.
- Config export/import and presets now include Default Zone Target and every integration's Enabled
  switch, which they previously skipped, and imported values are clamped to their valid ranges.
- The classic config screen's Reset for Ender IO's Energy To Heat Coefficient restored 0.001 instead
  of the real default, 60.
- The singleplayer world owner can now flip the control panel's System Enabled toggle without cheats
  enabled.
- Thermal Systems' `MarieContext` registration left `tooltipValueResolver` on its builder default,
  which reads MariesLib's shared, cross-mod `SourceClassificationRegistry` (the generic item
  editor's own per-item override data) for every value key *any* attached MarieLib mod has
  registered - not just Thermal Systems' own (it registers none). A player saving an override for
  any item through that generic editor, for any mod, on any item, silently resurfaced as a "thermal
  systems" contribution to that item's tooltip, merged in by `MarieTooltipHelper` right alongside
  whichever mod actually owns that value key - including making an item a consuming mod's own
  nutrient-exclusion list didn't actually suppress, since Thermal Systems' leaked contribution knew
  nothing about that mod's exclusion rules. `tooltipValueResolver` is now explicitly set to an
  always-empty no-op, since Thermal Systems has no nutrient/value system of its own to show there.
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
