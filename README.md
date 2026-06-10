# Accurate Block Placement (1.12.2)

Backport of [Accurate Block Placement Reborn](https://github.com/hunterhaunter/Accurate-Block-Placement-Forge-Port) to Minecraft Forge 1.12.2.

Build at super speed with your bare hands! Hold right-click with a block in hand and sweep your crosshair — blocks place instantly on each new position without ever double-placing on the same spot.

## Features

- **Drag placement** — hold right-click and move the crosshair; every new position places immediately, ignoring the vanilla 4-tick right-click delay
- **No double-placing** — each block position places exactly once until you move ≥1 block along the placement axis
- **Backfill** — positions swept over during the initial cooldown are queued and placed the moment the spree engages
- **Jump-pillaring works** — player-movement tracking re-arms placement on the same target after you move a full block
- **Plays nice** — chests, furnaces and other activatable blocks still open normally (sneak-place against them works); food and items with custom use actions are untouched
- **Toggle keybind** — switch between accurate and vanilla placement (unbound by default, set in Controls)

## Requirements

- Minecraft 1.12.2 + Forge 14.23.5.x
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) 9.0+

Client-side only — safe to join any server.

## Installation

Drop the release jar and MixinBooter into your `mods` folder.

## Compatibility

Patches are pure Mixin `@Inject`s (no `@Overwrite`, no vanilla class or registry replacement), so other coremods and mixin mods targeting `Minecraft`/`EntityRenderer` coexist cleanly. Placement goes through the vanilla right-click path, so Forge `RightClickBlock`/`RightClickItem` events (protection mods, map utilities, etc.) fire for every placed block. Modded blocks with custom right-click behavior are automatically deferred to.

## Building

```
./gradlew build
```

Jar lands in `build/libs/`. Built with RetroFuturaGradle, MCP stable_39, Java 8.

## Credits

- Original Fabric mod by [Clayborn](https://github.com/Clayborn), maintained by Flourick
- Forge 1.20.1 port by KadTheHunter
- 1.12.2 backport by schwar
