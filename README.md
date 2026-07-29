<div align="center">

<img src="Thermal_banner.png" alt="Thermal Systems" width="600"/>

# 🌡️ Thermal Systems

**A working climate simulation core for NeoForge.**

Zone-based heat and cold, real thermal sources, and deep hooks into the mods you already run.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.235-F16436?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-0.0.1--beta-9370DB?style=for-the-badge)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey?style=for-the-badge)](#license)

</div>

---

## Overview

Thermal Systems simulates temperature as a first-class mechanic. Chunk-bound **climate zones** drift toward a target temperature over time, pulled by real **heat and cooling sources** in the world, and pushed out to players through a live HUD control panel. It's built to be a simulation *core* — other mods plug their machines into it as sources rather than Thermal Systems reimplementing heat generation itself.

## ✨ Features

- **🧊 Zone-based climate simulation** — chunk-scoped zones converge toward a target temperature at a configurable rate, driven by an `IClimateZone` / `ClimateEngine` model.
- **🔥 Pluggable heat & cooling sources** — `IHeatSource` / `ICoolingSource` capabilities let any block or system radiate into a zone; radiation is tracked per-tick with lifecycle-safe active-source bookkeeping.
- **🖥️ Persistent HUD control panel** — an in-game overlay (toggleable via keybind) showing live system status, with a client config screen supporting export/import.
- **🌐 Deep mod integrations**
  - **PneumaticCraft** — heat sources from PneumaticCraft tubes/blocks with in-world hover info.
  - **Mekanism** — heat conversion, network discovery, and hover providers for Mekanism heat networks.
  - **Ender IO** — bidirectional heat/cool toggle blocks with networking, mode request/response payloads, and persisted UI state.
  - **Legendary Survival Overhaul (LSO)** — thermal bridge syncing zone temperature to LSO's survival systems.
  - **Cold Sweat** — thermal bridge syncing zone temperature to Cold Sweat's body temperature system.
- **👤 Player temperature bridge** — reports each player's ambient zone temperature out to whichever survival mod is listening.
- **⚙️ Fully configurable** — tick intervals, heat transfer coefficients, convergence rates, temperature clamps, and granular per-system logging toggles, all editable in-game via the config screen.

## 📦 Requirements

| Dependency | Version | Required |
|---|---|---|
| Minecraft | 1.21.1 | ✅ |
| NeoForge | 21.1.235+ | ✅ |
| [MariesLib](https://github.com/kgbcupcake/MariesLib) | 0.1.1-beta.4+ | ✅ |
| PneumaticCraft | 8.2.20+ | Optional |
| Mekanism | 10.7.16.82+ | Optional |
| Ender IO | 8.2.11-beta+ | Optional |
| Legendary Survival Overhaul | 2.4.5+ | Optional |
| Cold Sweat | 2.4.2+ | Optional |

Optional dependencies unlock their respective integration; Thermal Systems runs standalone without any of them.

## 🚀 Getting Started

```bash
git clone <this-repo-url>
cd Thermal_Systems
./gradlew build
```

To launch a dev client with the mod loaded:

```bash
./gradlew runClient
```

The build output lands in `build/libs/`.

## 🗂️ Project Layout

```
src/main/java/com/marie/thermalsystems/
├── api/            Public capability interfaces (IHeatSource, ICoolingSource, IClimateZone, ...)
├── climate/         Climate simulation engine, zones, and tick handling
├── controller/       Bridges pushing zone temperature to player-facing systems
├── cooling/          Cooling source capability implementations
├── heating/           Heat source capability implementations
├── radiation/         Per-tick source radiation and active-source tracking
├── zone/             Zone definition and management
├── integration/       Per-mod integration packages (pneumaticcraft, mekanism, enderio, lso, coldsweat)
├── client/hud/        HUD control panel and client config screens
├── hud/              Shared HUD state/components
├── hover/            In-world tooltip/hover providers for source blocks
├── data/config/       ThermalConfig and I/O
├── registry/          NeoForge registration
└── util/             Shared helpers
```

## 🤝 Contributing

This is a personal project maintained by Marie. Issues and PRs are welcome, but there's no formal contribution process yet — open an issue to discuss significant changes first.

See [CHANGELOG.md](CHANGELOG.md) for a history of changes.

## 📄 License

All Rights Reserved. See the [`mod_license`](gradle.properties) declaration for details.

---

<div align="center">
<sub>Built with 🔥 and ❄️ by Marie</sub>
</div>
