<div align="center">


# 🌡️ Thermal Systems

**A working climate simulation core for NeoForge.**

Mark out rooms as climate zones, heat and cool them with the machines you already run, and feel it through your favourite survival mod.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1-F16436?style=for-the-badge)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-0.0.1--beta.1-9370DB?style=for-the-badge)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey?style=for-the-badge)](#-license)

</div>

---

## Overview

Thermal Systems simulates temperature as a first-class mechanic. You mark out a room as a **climate zone**, give it a target temperature, and its thermostat runs the **heaters and coolers** inside it - Mekanism, PneumaticCraft and Ender IO machines - to get there. Idle zones drift back toward the outside air, which follows the season when a seasons mod is installed. The temperature a player stands in is then handed to whichever survival mod is listening (Cold Sweat, Legendary Survival Overhaul, Tough As Nails).

It's built to be a simulation *core*: other mods plug their machines in as sources through a small public API rather than Thermal Systems reimplementing heat generation itself.

## ✨ Features

### Zones and thermostats

- **🏠 Climate zones**: box-shaped zones that drift toward a target temperature, saved with the world. Each zone records its owner; only the owner (or an op) can resize, delete or lock it, and the owner can let anyone adjust its target and mode.
- **🎛️ Real thermostat behaviour**: each zone runs in **Off**, **Heat**, **Cool** or **Auto** mode. It starts heating or cooling once it's more than 0.5°C off target and stops when it gets there. Heat mode only uses heaters, Cool only coolers, and Auto uses whichever it needs. New zones start in Auto.
- **🔌 Automatic source pickup**: heaters and coolers inside a zone, or within `sourceBindingRadius` blocks of it, are bound to it automatically. Each machine network counts once, however many of its blocks are inside the zone.
- **🌤️ Ambient temperature and seasons**: idle zones and players outside every zone feel the ambient temperature. That's `defaultAmbientTemperature` plus the current season's shift from **Ecliptic Seasons** (solar terms) or **Serene Seasons**. If both are installed, Ecliptic Seasons is used.
- **☀️ Direct radiation**: players standing near an active heater or cooler outside a zone feel it within `sourceRadiationRadius` blocks. Inside a zone you feel exactly the zone's temperature.

### Items and blocks

| | How to use it |
| --- | --- |
| **Zone Gadget** | Right-click two corners to mark out a room. A live wireframe shows nearby zones and your selection (red if it's too big or overlaps another zone). Sneak + use opens its menu to switch between **Create**, **Edit** (resize, rename or delete your zone), **Inspect** (print a zone's details) and **Copy** (copy one zone's thermostat settings and paste them onto others). Creating or editing opens the **zone editor**: a 3D model of the area you can rotate and zoom, From/To steppers on each axis, a name field and the target temperature. It warns you before saving a box that's too big, too far away, overlapping another zone, or using a name that's already taken. |
| **Thermostat Tablet** | Use it to list every nearby zone and open a thermostat for any of them. The thermostat shows the live temperature, the target (+/- in 0.5° steps, 5° with Shift), the Off/Heat/Cool/Auto mode, and the heaters and coolers the zone is using, with what they can deliver. |
| **Wall Thermostat** | Sneak + use the Thermostat Tablet on a wall to mount it as a room thermostat. Its screen shows the room's live temperature, target and status. Tap its -, Mode and + buttons to adjust it in place, or tap the screen for the full thermostat. It emits a redstone signal while the room is calling for heating or cooling, and links to the nearest zone within 6 blocks if it's mounted just outside the zone's box. |

Both items are craftable and have their own creative tab.

<details>
<summary>Recipes</summary>

**Zone Gadget**

```
I G I     I = Iron Ingot
I R I     G = Glass Pane
  I       R = Redstone
```

**Thermostat Tablet**

```
I G I     I = Iron Ingot
I R I     G = Glass Pane
I C I     R = Redstone
          C = Copper Ingot
```

</details>

### Interface

- **⚙️ In-game config editor**: open it from the Mods list's Config button or the **Open Thermal Systems Config** keybind (unbound by default). The sidebar has one module per area (Simulation, Zones, Integration, one page per installed integration, Display, Tools), each with sliders and toggles and Reset This Module/Tab buttons. Temperature settings show their ideal "neutral" value with a one-click button to use it.
- **🧍 Home page**: your character rendered in 3D (it follows the mouse), next to your live zone and nearby-source temperatures, the core settings, the Ender IO generator mode, and which integrations are active.
- **🌐 Server-aware**: on a server the editor shows and edits the server's own config. Only ops (and the owner of a singleplayer or LAN world) can change values; everyone else sees them read-only.
- **🧰 Tools page**: the classic Cloth Config screen, presets, and config **Export** / **Import**.
- **🌡️ °C or °F**: switch units with the button in the top-right of the tablet and Zone Gadget screens, or on the Display page. It's a personal setting and applies everywhere a temperature is shown - screens, the wall thermostat, tooltips, chat and config sliders. In Fahrenheit the thermostat steps by 1°F (10°F with Shift).
- **🖥️ HUD control panel**: a draggable overlay with live system status, toggled by the **Toggle Thermal Systems Control Panel** keybind (unbound by default).
- **🔍 Hover tooltips**: hovering an integrated machine, cable or conduit shows whether it's heating, cooling or on standby, which zone controls it, the room's temperature and target, and its output rate.

## 🔗 Integrations

Every integration is optional. Each turns on only when its mod is installed **and** its `enabled` switch in the config is on. Its config page only appears while that mod is installed.

### Heat and cooling sources

| Mod | What it does |
| --- | --- |
| **Mekanism** | Machines on a thermodynamic conductor network act as one heat/cooling source. Their output comes from how far the network's temperature is from `referenceTemperatureKelvin`. |
| **PneumaticCraft: Repressurized** | Heat Sinks, Refineries and Thermopneumatic Processing Plants act as heat/cooling sources, converted from their PneumaticCraft heat. |
| **Ender IO** | Stirling Generators and their conduit networks work like heat pumps. Inside a zone they follow its thermostat: they heat while it calls for heat, cool while it calls for cooling, and stand by once it reaches the target. Outside every zone, the **Cooling Mode** switch decides whether they heat or cool nearby players. Output scales with how full the generator's energy buffer is (`energyToHeatCoefficient` at a full buffer). |

### Player temperature

| Mod | What it does |
| --- | --- |
| **Cold Sweat** | Shifts the player's world temperature by the difference between what they feel and `temperatureOffset`, scaled by `outputScale`. It works in both directions, so it can push a player into Hot or Cold. |
| **Legendary Survival Overhaul** | Feeds the felt temperature into LSO, relative to its neutral `temperatureOffset`. |
| **Tough As Nails** | Shifts the player's Tough As Nails temperature by one or two levels once heat or cold passes configurable thresholds. It never pushes a player into Hot or Icy from outside them. |

### Seasons

| Mod | What it does |
| --- | --- |
| **Ecliptic Seasons** | Adds the current solar term's temperature shift to the ambient temperature. |
| **Serene Seasons** | Adds the current sub-season's temperature shift to the ambient temperature (used only when Ecliptic Seasons isn't installed). |

## 📦 Requirements

| Dependency | Supported | Built against | Required |
| --- | --- | --- | --- |
| Minecraft | 1.21.1 | 1.21.1 | ✅ |
| NeoForge | 21.1.0+ | 21.1.235 | ✅ |
| [MariesLib](https://github.com/kgbcupcake/MariesLib) | 0.1.2-beta.1+ | 0.1.2-beta.1 | ✅ |
| [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | 15.0+ | 15.0.140 | ✅ |
| Mekanism | 10.7.0+ | 10.7.16.82 | Optional |
| PneumaticCraft: Repressurized | 8.2.0+ | 8.2.20 | Optional |
| Ender IO | 8.2.0+ | 8.2.11-beta | Optional |
| Cold Sweat | 2.4.0+ | 2.4.2 | Optional |
| Legendary Survival Overhaul | 2.4.0+ | 2.4.5 | Optional |
| Tough As Nails | 10.1.0.0+ | 10.1.0.11 | Optional |
| Ecliptic Seasons | 0.12+ | 0.15.3.1 | Optional |
| Serene Seasons | 10.1+ | 10.1.0.9 | Optional |

Thermal Systems runs standalone without any of the optional mods.

## ⌨️ Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/thermal zone create <name> <targetTemp>` | Everyone | Create a zone in the current dimension, owned by you. |
| `/thermal zone setBounds <name> <x1> <y1> <z1> <x2> <y2> <z2>` | Everyone | Set a zone's box. |
| `/thermal zone clearBounds <name>` | Everyone | Remove a zone's box. |
| `/thermal zone delete <name>` | Op | Delete a zone and unbind its sources. |
| `/thermal source add <zoneName> <heatOutput>` | Everyone | Add a virtual heat source (°C/s) to a zone, for testing. |
| `/thermal <mekanism\|pneumaticcraft\|enderio> bind <zoneName>` | Everyone | Manually bind the source you're looking at to a zone (only registered when that mod is installed). |
| `/thermal <mekanism\|pneumaticcraft\|enderio> unbind` | Everyone | Unbind the source you're looking at. |
| `/thermal debug radiation` | Op | Report each player's applied direct-radiation temperature (and Tough As Nails levels, when installed). |

In normal play you shouldn't need these. The Zone Gadget, the Thermostat Tablet and automatic source binding cover the same ground.

## 🔧 Configuration

| File | Scope | Contents |
| --- | --- | --- |
| `config/thermalsystems-common.toml` | Server / world | Simulation rates and clamps, ambient temperature, source binding and radiation ranges, zone limits for non-ops (`maxZoneVolume`, `maxZonesPerPlayer`), logging, and one section per integration. |
| `config/thermalsystems-client.toml` | Per player | Display settings, such as °C / °F. |

Everything in both files can be edited from the in-game config editor, so you shouldn't need to edit the TOML by hand.

## 🧩 API

Other mods integrate through `com.marie.thermalsystems.api`:

- **`IHeatSource` / `ICoolingSource`**: block capabilities (`HeatSourceCapabilities.HEAT_SOURCE`, `CoolingSourceCapabilities.COOLING_SOURCE`) that report output in °C per simulation second. Expose them on your block entity and zones pick the block up automatically.
- **`IHeatPump`**: implement it alongside both capabilities for a source that can run either way. Zones then drive it with their thermostat.
- **`ITemperatureBridge`**: receive each player's felt temperature. Register it with `ThermalSystemsAPI.registerTemperatureBridge`.
- **`ThermalSystemsAPI`**: read-only zone lookups (`getZone`, `getZoneByName`, `getZoneAt`, returning immutable `ZoneSnapshot`s) and manual `bind`/`unbind` of heat and cooling sources.
- **`ZoneTemperatureUpdatedEvent`**: fired when a zone's temperature changes.

## 🚀 Getting Started

MariesLib is pulled from GitHub Packages, which needs a GitHub token with `read:packages`. Either set `GITHUB_ACTOR` / `GITHUB_TOKEN` in your environment, or add `gpr.user` / `gpr.key` to your `~/.gradle/gradle.properties`.

```bash
git clone https://github.com/kgbcupcake/Thermal_Systems.git
cd Thermal_Systems
./gradlew build
```

| Task | What it does |
| --- | --- |
| `./gradlew runClient` | Launch a dev client with the mod loaded. |
| `./gradlew runServer` | Launch a dev dedicated server. |
| `./gradlew test` | Run the JUnit test suite. |

The built jar lands in `build/libs/`.

## 🗂️ Project Layout

```
src/main/java/com/marie/thermalsystems/
├── api/              Public API: source capabilities, IHeatPump, ITemperatureBridge, zone snapshots, events
├── block/            Wall Thermostat block, block entity and touch handling
├── item/             Zone Gadget and Thermostat Tablet
├── climate/          Simulation engine, tick handling, ambient temperature and season offsets
├── controller/       Thermostat logic, climate modes, heat pump control, player temperature bridging
├── zone/             Zones, spatial index, automatic source scanning, world persistence
├── heating/          Heat source implementations
├── cooling/          Cooling source implementations
├── radiation/        Direct source-to-player radiation and active-source tracking
├── network/          Zone, thermostat and temperature-unit packets
├── integration/      Optional mod integrations (mekanism, pneumaticcraft, enderio, coldsweat, lso,
│                     toughasnails, eclipticseasons, sereneseasons)
├── client/
│   ├── config/       Config editor (hub pages, Cloth Config categories, export/import)
│   ├── hud/          HUD control panel and keybinds
│   ├── render/       Wall Thermostat renderer
│   ├── screen/       Tablet, thermostat, Zone Gadget and zone editor screens
│   └── zone/         Client zone cache, selection and 3D preview rendering
├── data/config/      Common and client config specs, plus server config sync
├── hud/              Shared HUD state and packets
├── hover/            Hover tooltip providers
└── registry/         Blocks, items, creative tab and commands
```

## 🤝 Contributing

This is a personal project maintained by Marie. Issues and PRs are welcome, but there's no formal contribution process yet, so open an issue to discuss significant changes first.

See [CHANGELOG.md](CHANGELOG.md) for a history of changes.

## 📄 License

All Rights Reserved. See the [`mod_license`](gradle.properties) declaration for details.

---

<div align="center">
<sub>Built with 🔥 and ❄️ by Marie</sub>
</div>
