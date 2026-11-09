# Super Vault

Fabric mod, strongly inspired by Terraria's Magic Storage: an upgradable vault that keeps practically infinite items, with search, filter tabs and a Vault Link that opens a max tier vault from anywhere.

# Installation
Requirements:
- Minecraft 1.21.11
- Fabric Loader 0.18.0+
- Fabric API

1. Download the `.jar`.
2. Put it in the `mods` folder.
3. Repeat on the server and on every client.

# Usage
Each cell holds one item type. Cells are shown in registry order.

| Action | Result |
| --- | --- |
| Shift-click an item in your inventory | Store it |
| Click a cell while holding an item | Store the whole stack (right click: one item) |
| Click a stored item | Take a stack |
| Right-click a stored item | Take half a stack |
| Shift-click a stored item | Move a stack to your inventory |
| Shift-right-click a stored item | Move one item to your inventory |

Search bar: matches item names. `@name` matches the mod namespace, e.g. `@minecraft`. Choosing another tab clears the search.

# Crafting
## Super Vault
```
Coal    Iron     Gold
Copper  Diamond  Copper
Gold    Iron     Coal
```

## Vault Link
```
        Diamond
Diamond Eye of Ender Diamond
        Diamond
```

# Upgrades
Buttons appear on the right side of the vault. Upgrades are paid with items stored inside the vault.

| Upgrade | Cost | Effect |
| --- | --- | --- |
| Cells 10-27 | 4 Copper Ingots each | Unlock one cell (start: 9, max: 54) |
| Cells 28-54 | 4 Coal each | Unlock one cell |
| Iron | 64 Iron Ingots | Unlimited items per cell, contents stay in the item when broken. Needs all 54 cells |
| Gold, level 1 | 8 Gold Ingots | Search bar and All tab. Needs all 54 cells |
| Gold, levels 2-11 | 8 Gold Ingots each | One tab each: Blocks, Furniture, Tools, Combat, Food, Resources, Potions, Mobs, Redstone, Misc |
| Diamond | 64 Diamonds | Unlimited cells with scrolling. Needs Iron and all 11 Gold levels |

Max tier means every upgrade above is bought.

Breaking a vault returns the vault item with its upgrades. With Iron or any Gold level the contents stay inside the item, like a shulker box. Otherwise they drop as normal items. Explosions cannot destroy a vault.

# Vault Link
Opens a max tier vault from any distance and from any dimension.

1. Right-click a max tier vault with the Vault Link to bind it. Other vaults cannot be linked.
2. Right-click with the Vault Link in the air, or on a block without its own right-click action, to open the bound vault.

Notes:
- The vault's chunk is kept loaded while the menu is open.
- A Vault Link points to a position. If the vault is broken or moved, bind the link again.
- Binding again replaces the old target.
- Anyone holding the link can open the vault.

# Development
- `gradlew runClient`: start a development client
- `gradlew runServer`: start a development server

# Building
Requirements:
- Java 21
- Internet (the first build downloads dependencies)

Run `build.bat [-c] [-i] [-r] [-n] [-h]`. Flags combine in any order, e.g. `build.bat -cir`.

| Flag | Action |
| --- | --- |
| `-c` | Clean: delete previous build output first |
| `-i` | Install: copy the build to `%APPDATA%\.minecraft\mods`, replacing older versions |
| `-r` | Release: package the build into `release\` (the jar, replacing older versions) |
| `-n` | Nobuild: skip the build and reuse the existing output (needs `-i` and/or `-r`, not with `-c`) |
| `-h` | Show help, no build |

The project path and `%USERPROFILE%\.gradle` (override with `GRADLE_USER_HOME`) may contain only letters, digits, `_`, `-` and `.`, since `!` and non-ASCII characters break the Fabric build; `build.bat` checks the project path.

Build output: `build\libs\supervault-<version>.jar`

# License
- [MIT](LICENSE.md)
