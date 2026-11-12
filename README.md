# Ganich's Lanterns (Fabric 1.21.1)

Wear a vanilla lantern or soul lantern in its own Trinkets slot `legs/lantern`:
your hands stay free, the lantern swings as you move and gives off dynamic light (LambDynamicLights).

- **Slot**: `src/main/resources/data/trinkets` (the slot, its assignment to players, the item tag of allowed items).
- **Equipping**: drag a lantern into the slot on the Trinkets screen.
- **Rendering**: `client/BeltLanternRenderer`, a pendulum on a hook attached to the player model's torso.
- **Light**: `client/compat/BeltLanternDynamicLights`.

## Requirements

- Fabric Loader, Fabric API
- [Trinkets](https://modrinth.com/mod/trinkets)
- Recommended: [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) (without it the lantern is still shown and swings, but gives no dynamic light)

Install on both the client and the server: the slot lives on the server, rendering and light are client side.

## Physics

The lantern reacts to acceleration and braking, turns (the hook is off the turning axis, so a turn whips it around),
jumps and falls (the effective gravity changes), landing, being hit, swinging an arm, sneaking and the walking rhythm.
It is nailed to its hook: it only swings and never moves up or down. It sways gently when you stand still.
Angles are limited so it rests against the body instead of passing through it.

## Resource packs

Everything can be changed from a resource pack, no recompiling needed:

| File in the pack | What it changes |
| --- | --- |
| `assets/beltlantern/pose/default.json` | placement (`hook`), size, physics and limits for all lanterns |
| `assets/beltlantern/pose/lantern.json`, `soul_lantern.json` | the same, for a single lantern |
| `assets/beltlantern/models/item/lantern_worn.json`, `soul_lantern_worn.json` | the 3D model on the hip (can be replaced completely, `model_top_px` sets the attachment height) |
| `assets/minecraft/textures/block/lantern.png` | the texture, picked up like for the normal block |

The files of all packs are merged field by field (the higher pack overrides the lower one), so you only need to list
the fields you want to change. The full list of fields and their defaults:
`src/main/resources/assets/beltlantern/pose/default.json`. `"physics": false` makes the lantern rigid (no swinging).

Animation mods and packs that change the player's torso pose are followed automatically: the lantern is attached to
the `body` model part after it has been posed.

## Building and running

```bash
JAVA_HOME=<jdk 21> ./gradlew build     # jar in build/libs
JAVA_HOME=<jdk 21> ./gradlew runClient
```

`libs/lambdynamiclights-api-*.jar` (LGPL) is kept in the repository: the LambDynamicLights API is not published to any
maven on its own, it is nested inside the jar from Modrinth, and it is needed to compile.

For `runClient` with dynamic light, put the jars nested inside the LambDynamicLights jar into `dev-libs/`
(`lambdynamiclights-runtime`, `spruceui`, `pridelib`, `yumi-*`). That folder is not stored in git. Without them the
client only starts if you remove LambDynamicLights from the dependencies in `build.gradle`.

## License

Code: Apache License 2.0 (file `LICENSE`).
The mod icon (`assets/beltlantern/icon.png`) is drawn from the lantern texture of the Bare Bones resource pack
(RobotPants, All Rights Reserved) and is not covered by this repository's license.
