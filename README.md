# Deconstructed Core

The library that Deconstructed Backpacks and Deconstructed Storage run on: storage inventories, upgrades and their logic, settings, sorting and the storage screens. On its own it adds nothing to the game. For Minecraft 1.12.2.

## Origin and license

This is a port to Minecraft 1.12.2 of [Sophisticated Core](https://github.com/P3pp3rF1y/SophisticatedCore) by P3pp3rF1y, made from
[`3690ed29`](https://github.com/P3pp3rF1y/SophisticatedCore/tree/3690ed29a179640c31b465bd6d663d736bed3d30), the last commit of that repository published under the
GNU General Public License v3, on 2025-02-10. Its later commits are under All Rights Reserved; nothing in this
port comes from them.

The GPL v3 under which that commit was published lets anyone use, change and share the code, and the rights it
gives can't be taken back for what was released under it: a later change of license applies only to what comes
after. So this port:

- is under the same GNU General Public License v3 (see [LICENSE](LICENSE)), as a work based on a GPL one must be;
- keeps P3pp3rF1y's copyright of the original code, and states that the code is changed: it is a port to an
  older game version, with much rewritten for it;
- is published with its full source, as the GPL asks;
- doesn't use the original's name for the mod, and isn't made, endorsed or supported by P3pp3rF1y. Don't report
  problems with it to the original mod.

## Building

```bash
./gradlew build
```

The jar to install is `build/libs/deconstructedcore-<version>.jar` (not `-dev` or `-sources`).

Another mod can be built against this one through JitPack:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation rfg.deobf('com.github.Deconstructed-Team:Deconstructed-Core:v1.0.0')
}
```
