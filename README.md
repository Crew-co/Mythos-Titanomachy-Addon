# Titanomachy

Kronos eats his children. One survives. Ten years of war. **Story #2 — the era `titanomachy`.**

A story addon for the [Mythos](https://github.com/Crew-co/Mythos) engine, built on
[FoliaAddonTemplate](https://github.com/Crew-co/FoliaAddonTemplate). Its own repo, its
own jar, its own release cycle — and exactly one dependency:

```kotlin
compileOnly("net.crewco:mythos-addon-api:0.1.0")
```

No `depends:` in addon.yml. The engine is the plugin, not an addon: it's already running
before this jar is loaded.

## Build

```bash
# once, in the Mythos repo:  ./gradlew publishApiLocally
./gradlew build          # → build/libs/Titanomachy-0.1.0.jar
./gradlew deployAddon    # set testServerPath in ~/.gradle/gradle.properties first
```

Drop the jar in `plugins/Mythos/addons/`. `/addons` should list it.

## What it registers

- **Era** `titanomachy` → declares `next = "olympian-order"`.
- **6 Olympians, borne by Rhea** (`/power bear <spirit> <olympian>`) — same gate as
  Gaia's Titans, one age later.
- **3 Cyclopes + 3 Hekatoncheires, claimable** — but claiming one drops you at the
  bottom of Tartarus, leashed there, until a god comes down for you.
- **Titan-sworn / Olympian-sworn** — 60 seats each, no gates. On a 100-player server
  this is where 90 of them live: not spirits, not gods. An army.
- **Powers** — `devour` (an Olympian goes into a *stomach*: leashed at bedrock, alive,
  not in the queue) · `stone` (Rhea hides one child and hands Kronos a rock in a
  blanket) · `draught` (Zeus right-clicks his father with a cup) · `free` · `forge`
  (the Cyclopes make the thunderbolt, trident and helm — and hand them out) ·
  `thunderbolt` / `quake` / `unseen` / `hundredfold`.

## The shape

**Soft dependency.** Kronos and Rhea belong to EraOfCreation — but this addon never
imports it and doesn't check whether it's installed. One tick after enable (when every
other addon has registered its cast), it asks core whether `kronos` exists; if nobody
registered it, it registers its own. Delete the Creation jar and this still runs
standalone from `/mythos advance titanomachy`.

That's the pattern for *extending a myth you don't own*. Use it.

**The war.** Cross-faction kills are tallied. Kronos is literally unkillable until the
tally hits `war.kills-to-end` — and then only by a god holding one of the three gifts.

`compileOnly`, never `implementation` — a shaded copy of the API is a different class
with the same name, and the addon silently refuses to load.
