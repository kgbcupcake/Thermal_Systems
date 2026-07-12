# Marie's Thermal Systems — Phase 4/5 Correction: No Integration Blocks

Treat this specification as authoritative. If implementation details are ambiguous, choose the simplest implementation that satisfies the specification without adding additional features or abstractions.

## Root Cause

Phase 3 built the correct mechanism for external-mod integration: `HeatSourceCapabilities.HEAT_SOURCE` / `CoolingSourceCapabilities.COOLING_SOURCE`, plus `CapabilityHeatSource` / `CapabilityCoolingSource` as thin `BlockPos`-holding adapters that resolve a foreign capability fresh on every evaluation. This is a general mechanism for wrapping *any* block that exposes the right capability — it does not require that block to be one this mod registered.

Phase 4 and Phase 5 did not use that mechanism as designed. Instead, each introduced a brand-new block owned by this mod (`ThermalExchangerBlock`, `MekanismHeatExchangerBlock`, `EnderIOThermalAdapterBlock`) that trivially implements `IHeatSource`/`ICoolingSource` on its own, and gets placed *next to* the target mod's real machinery rather than reading from it directly. This produced exactly the outcome Phase 3 was designed to avoid: a new block, model, item, and lang entry per integration, none of which represent anything the target mod actually has.

This correction removes those three blocks and rewires each integration to register this mod's capability directly onto the target mod's own existing `BlockEntityType`s.

## Hard Constraints

These are non-negotiable for this pass, not preferences:

1. **No new blocks.** No `Block`, `BlockItem`, `BlockEntityType`, blockstate JSON, model JSON, or lang entry may be added by this correction, for any integration, under any name, for any reason.
2. **No hardcoded values.** Every coefficient, threshold, or reference value used in a conversion function must come from config, exactly as already established (`ThermalExchangerConversion`, `MekanismHeatConversion`, and any Ender IO conversion logic — if Ender IO ends up needing no conversion logic of its own per the Verification section below, this constraint is moot for that integration).
3. **No new binding mechanism.** All binding continues through `ThermalSystemsAPI.bindHeatSource` / `bindCoolingSource` exactly as Phase 3 defined them. Do not add a parallel registry, map, or lookup structure for integration-specific bindings.
4. **Target mods are read, never written to.** This mod may register a capability *onto* a foreign `BlockEntityType` (that's what `RegisterCapabilitiesEvent` is for) but must never modify, extend, or mixin the foreign mod's own classes, GUIs, recipes, or block behavior.
5. **Test fixtures are the only exception to constraint 1**, and only under `src/test`, never registered with NeoForge, never present in a running game. See Testing below.

## Verification Required Before Implementation

Do not guess API surface. Confirm each of the following against the actual current API jar for the target mod (1.21.1/NeoForge) before writing the corresponding registration code. If any finding differs from what's assumed here, implement against the real finding and note the discrepancy — do not silently substitute.

### Mekanism
- Which of Mekanism's own `BlockEntityType`s expose `mekanism.api.heat.IHeatHandler` / `IMekanismHeatHandler` — i.e. which Mekanism machines actually have heat capacitors (candidates: Thermal Evaporation Controller/Valve, Resistive Heater, Boiler-related blocks — confirm the real list, do not assume this list is complete or correct).
- Confirm the capability registration pattern Mekanism itself expects other mods to use to *read* this capability (as opposed to how Mekanism's own internal machines share it with each other).

### PneumaticCraft: Repressurized
- Which of PNC:R's own `BlockEntityType`s expose its heat exchange capability (candidates from PNC:R's own documentation/changelog: Refinery, Thermopneumatic Processing Plant, Heat Sink — confirm against the actual 1.21.1 API jar, `me.desht.pneumaticcraft.api`, per the unresolved verification items already flagged in the Phase 4 spec).
- Confirm the exact capability class/constant name and whether it's a `BlockCapability` in the NeoForge-idiomatic sense or something PNC:R-specific.

### Ender IO
- Re-confirm the Phase 5 finding: Ender IO's Heat Conduit only exists in `enderio-modded-conduits`, requires Mekanism, and relays Mekanism's own `IHeatHandler` capability rather than owning any heat concept itself.
- If this holds, Ender IO requires **zero new registration code** in this pass — once Mekanism's own `BlockEntityType`s are registered against `HEAT_SOURCE`/`COOLING_SOURCE` per the Mekanism section above, Ender IO's conduits will already connect to them, because Ender IO is querying the same Mekanism capability Mekanism's own blocks expose. This is a documentation/verification outcome, not an implementation task.
- If `integration/enderio/` has no registration code left after this — because there is nothing Ender IO-specific to register — delete the package entirely. Do not leave an empty placeholder class, an empty `EnderIOIntegration.java` with a comment, or a no-op `init()` method. A deleted package is the correct outcome, not a gap to fill.

## Package Changes

### Delete entirely
- `integration/pneumaticcraft/block/ThermalExchangerBlock.java` and its `blockentity/` counterpart
- `integration/mekanism/block/MekanismHeatExchangerBlock.java` and its `blockentity/` counterpart
- `integration/enderio/block/EnderIOThermalAdapterBlock.java` and its `blockentity/` counterpart
- All associated blockstate/model/lang resources for the three blocks above
- Any registry entries (`DeferredRegister` calls) for the three blocks above
- `integration/enderio/` in its entirety, if the Ender IO verification finding above confirms no registration code is needed

### Keep unchanged
- `ThermalSystemsAPI`, `IHeatSource`, `ICoolingSource`, `HeatSourceCapabilities`, `CoolingSourceCapabilities`, `CapabilityHeatSource`, `CapabilityCoolingSource` — these are correct as designed and are what this correction is finally putting to proper use
- `MekanismHeatConversion`, `ThermalExchangerConversion` — the pure conversion math doesn't care which block hosts the state it converts; keep it, just change what feeds it

### Add
- `RegisterCapabilitiesEvent` handlers in `integration/mekanism/MekanismIntegration.java` and `integration/pneumaticcraft/PneumaticCraftIntegration.java` that register `HEAT_SOURCE`/`COOLING_SOURCE` against the confirmed real `BlockEntityType`s from the Verification section, with a provider function that reads that block entity's existing native heat state and passes it through the existing conversion function.

## Commands

- Rename `/thermal exchanger bind` → `/thermal mekanism bind <zoneName>` and add a separate `/thermal pneumaticcraft bind <zoneName>` (the two target mods are no longer the same block, so they can no longer share one command).
- Each command raycasts from the executing player exactly as `/thermal radiator bind` already does, but resolves the *target mod's* capability at the hit position rather than checking for one of this mod's own block entity classes.
- `/thermal exchanger unbind` splits the same way into `/thermal mekanism unbind` and `/thermal pneumaticcraft unbind`.
- Failure messages must be specific: "You are not looking at a Mekanism heat-capable block." / "You are not looking at a PneumaticCraft heat-capable block." — not a generic "no capability found."

## Escape Hatch

If a target mod genuinely has no suitable existing `BlockEntityType` to hook — for example, its heat-capable state only exists on a block so generic or so central to unrelated mechanics that hooking it would be unsafe or semantically wrong — stop and report that specific case with the exact block/mechanic in question before inventing a new block to work around it. This is expected to be rare; do not treat it as a default fallback.

## Testing

- Delete all tests tied to the three removed blocks.
- All existing Phase 1–3 tests continue passing unmodified.
- Add capability-registration tests for the new Mekanism and PneumaticCraft binding paths. These may use a minimal in-test stub `BlockEntity` implementing the target mod's real capability interface (e.g. a fake `IHeatHandler` for Mekanism), used only to verify `CapabilityHeatSource`/`CapabilityCoolingSource` correctly resolve and bind to it. This stub lives under `src/test`, is never registered with NeoForge, and never appears in a running game — it is not a reintroduction of the block pattern being removed here.
- Build succeeds with Mekanism absent, PneumaticCraft absent, both absent, and both present.
- In-game: `/thermal mekanism bind <zoneName>` while looking at a real, unmodified Mekanism machine (not anything this mod added) reaches a Climate Zone. Same for `/thermal pneumaticcraft bind` against a real PNC:R machine.
- In-game, if Ender IO turns out to need no code: build a small Mekanism heat network with a real Mekanism machine bound to a zone, connect it via a real Ender IO Heat Conduit run to a second real Mekanism machine several blocks away, and confirm heat still reaches the bound zone — proving Ender IO's conduit transport is genuinely free, not just untested.

## Success Criteria

- ✔ Zero blocks, items, block entity types, models, or lang entries exist for any integration, anywhere in the codebase.
- ✔ Every integration reads capability state from a real, unmodified block belonging to the target mod.
- ✔ No target mod's own classes, GUIs, recipes, or behavior were touched — only capability registration was added from this mod's side.
- ✔ Ender IO integration is proven to work with zero Ender IO-specific code, riding entirely on the Mekanism capability registration.
- ✔ All conversion coefficients remain config-driven; none were hardcoded during this correction.
- ✔ `git diff` for this change shows deletions of block/blockentity/registry/resource files as the dominant shape of the change, not additions.
