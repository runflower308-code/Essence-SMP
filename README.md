# EssenceSMP Solar Plugin

Paper plugin project for Minecraft 1.21.11 (Java 21).

## Current features
- Solar Essence abilities:
  - Solar Flare: quick damage blast with slight knockback.
  - Solar Judgment: straight-line beam.
  - Solar Eclipse: short-duration area damage ultimate.
- Three Solar tiers; players begin at Tier 1.
- Tier 2 unlocks Solar Judgment; Tier 3 unlocks Solar Eclipse.
- Solar fire resistance improves by tier (lava still damages players).
- Craftable, one-use Essence Upgraders:
  - Tier 2 Upgrader: 4 Diamonds, 4 Gold Ingots, 1 Diamond Block.
  - Tier 3 Upgrader: 4 Netherite Ingots, 4 Diamond Blocks, 1 Nether Star.
- Per-player tier progress is saved separately for each Essence, so switching does not erase progress.
- The plugin currently implements Solar abilities; the upgrader framework stores tiers by Essence key so other Essences can be added later.

## Build
Requires Java 21 and Maven:
```sh
mvn package
```
The plugin JAR will be in `target/EssenceSMP-Solar-1.0.0.jar`.

## Install
1. Build the JAR.
2. Upload the JAR into your server's `plugins` folder / MineKeep plugin manager.
3. Restart the server.
4. Use `/solar` for ability instructions and `/essence` to view your Solar tier.
5. Admins can use `/essence giveupgrader <player> <tier2|tier3>`.

## Controls
- `/solar flare` — Solar Flare (Tier 1).
- `/solar judgment` — Solar Judgment (Tier 2).
- `/solar eclipse` — Solar Eclipse (Tier 3).
- `/essence` — shows your Solar tier.
- `/essence giveupgrader <player> <tier2|tier3>` — admin testing command.
- `/essence giveupgrader self <tier2|tier3>` — operator gives self one.
- Right-click with a Solar Upgrader to open its upgrade prompt. The item is consumed only if the upgrade succeeds.
- Right-click with a Blaze Rod while Solar is equipped to cast Solar Flare; use a stick for Solar Judgment and a nether star for Solar Eclipse. The named ability items are convenience controls; commands also work.

## Notes
This is a first implementation and should be tested on a private server before use on a public SMP. Paper APIs and plugin support can change; compile against the matching Paper API.

## Easiest build method (no Java/Maven installation on your PC)

1. Create a GitHub repository and upload the contents of this project folder (including the `.github` folder and `pom.xml`).
2. Open the repository's **Actions** tab and select **Build EssenceSMP Solar Plugin**.
3. If it has not run automatically, choose **Run workflow**.
4. Open the completed workflow run. Under **Artifacts**, download **EssenceSMP-Solar-JAR** and extract the JAR.
5. Upload the `.jar` to your server's `plugins` folder and restart the server.

The workflow compiles the project; it does not fix source-code or API errors automatically. If the build fails, open the workflow log to see the error.
