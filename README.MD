# ATT2 Mod

A client-side Fabric mod by Poubone for the **Across The Time II — Time For Regrets** Minecraft adventure map.

The project makes the map's systems easier to read and use through a custom HUD, a quest journal, contextual menus and visual effects. It follows the map's existing mechanics rather than changing its progression. Most interactions use the same commands available through the map's own menus; no server-side mod is required.

## Features

- Custom HUD for resources, progression, attributes and spells.
- Quest journal with search, filters and objective tracking.
- Menus for attribute upgrades, repairs, shops and crafting recipes.
- Cinematic teleportation, loot beams and contextual controls.
- Optional party pings, item sharing and Discord Rich Presence.
- Equipment comparison in shops and the inventory, configurable shop layouts and dropped-item performance options.
- In-game translations and changelogs in eleven languages.

This mod is designed specifically for Across The Time II. Some interfaces only become available once the map has initialized its systems.

## Install

1. Install **Minecraft 1.21.11**, **Fabric Loader 0.18.4 or newer**, and the matching **Fabric API**.
2. Download the mod from [guide-att2.com/mod](https://guide-att2.com/mod/).
3. Place the mod JAR in your Minecraft `mods/` folder and launch the Fabric profile.

Open the HUD settings with **K**. Other controls can be configured in Minecraft's key binding settings.

## Run from source

You need a **JDK 21** with `JAVA_HOME` or `java` configured. The included Gradle wrapper downloads Gradle and the project dependencies on the first run.

On Windows:

```powershell
.\gradlew.bat runClient
```

On Linux or macOS:

```sh
./gradlew runClient
```

This starts a development Minecraft client. The adventure map and its resource pack are separate downloads and must be installed in that client's `run/` directory to test map-specific features.

## Build and test

On Windows:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```sh
./gradlew build
```

The build runs the automated tests and produces the mod JAR in `build/libs/`. Use the regular JAR for installation, rather than the sources JAR.

To run the optional integration test in a disposable Minecraft world, use `./gradlew -PprGameTest runClientGameTest` (or `.\gradlew.bat` on Windows). It checks dropped-item rendering, inventory and settings screens, translations and name-tag caching.

## Contribute

Bug reports and pull requests are welcome. For a bug report, include the mod version, Minecraft and Fabric versions, steps to reproduce, and relevant logs or screenshots. Avoid including personal information in logs.

For a code change, fork this repository, create a branch and explain the problem and the resulting behavior in your pull request. Run `build` and test affected interfaces in the adventure map when applicable. Keep changes focused and update all eleven files in `src/main/resources/assets/att2/lang_mod/` when adding visible text.

The main mod sources are in `src/main/`, and automated tests are in `src/test/`. Translated mod descriptions and release notes are available in `desc_mod/` and `changelog_mod/`.

## Contributors

- Poubone
- Simuciokas

## License

The mod is distributed under the [MIT license](LICENSE).
