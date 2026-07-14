# Titanomachy

**Kronos eats his children. One survives. Ten years of war.**

A story addon for the [Mythos](https://github.com/Crew-co/Mythos) engine — the era **`titanomachy`**.

📖 **[Read the wiki →](https://github.com/Crew-co/Titanomachy/wiki)** · roles, powers, extension points

## Install

Drop the jar in `plugins/Mythos/addons/`. That's it — no `depends:`, no configuration, no load order
to worry about. The engine wires the era chain at bootstrap.

## Build

```bash
# once, in the Mythos repo:  ./gradlew publishApiLocally
./gradlew build          # → build/libs/Titanomachy-0.1.0.jar
./gradlew deployAddon    # set testServerPath in ~/.gradle/gradle.properties
```

```kotlin
compileOnly("net.crewco:mythos-addon-api:0.1.0")   // the only dependency
```

`compileOnly`, never `implementation` — a shaded copy of the API is a different class with the same
name, and the addon will silently refuse to load.

## Testing it alone

`/mythos dev` — every crowd-sized number in this chapter becomes 1. You are one person; the story was
written for a hundred.

---

*Part of [Mythos](https://github.com/Crew-co/Mythos): a Greek mythology engine for Folia, where every
story is a separate jar.*
