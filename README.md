# Super Vault

Fabric mod for Minecraft, strongly inspired by Terraria's Magic Storage.

Adds an upgradable vault that keeps practically infinite items, with search and filter tabs.

## Installation

Requirements:
- Minecraft 1.21.11
- Fabric Loader 0.18.0+
- Fabric API

Download the `.jar` and put it in the `mods` folder.

## Recipe

```
Coal   Iron     Gold
Copper Diamond  Copper
Gold   Iron     Coal
```

## Building

Requirements: Java 21, internet (first build downloads dependencies).

Run `build.bat [task]` from the project directory:

| Task | Action |
| --- | --- |
| `build` (default) | Build the mod |
| `clean` | Clean, then build |
| `client` | Launch a development client |
| `server` | Launch a development server |
| `deploy [dir]` | Build and copy the jar to `%APPDATA%\.minecraft\mods` (or `dir`), replacing older versions |
| `help` | Show usage |

Output: `build/libs/supervault-<version>.jar`. Without the script: `gradlew build`.

## License

[MIT](LICENSE)
