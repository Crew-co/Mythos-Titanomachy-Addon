# Titanomachy — Extending

## What it reaches into

**Gaia** (EraOfCreation) gains `/power prophesy` — she has been through this exact thing before,
and in *this* age she can tell you how it ends. `roles.extend` returns false if that jar isn't installed,
which is not an error; it's a story nobody is telling here.

**The Helm of Darkness** is granted as a key to Tartarus via `realms.grant` — a combat item the Cyclopes
forged, which turns out to be the reason its bearer can walk into a prison nobody else can enter.

## The hole it opens in itself

**`titanomachy:allies`** → contribute a `WarAlly`.

It becomes a claimable role **and an optional beat of the war**. The addon does not know who fought in its own war, because some of them haven't been written.

```kotlin
compileOnly("net.crewco:titanomachy:0.1.0")   // for the type
// addon.yml:  depends: [ Titanomachy ]

mythos.extensions.contribute(Allies.POINT, ...)
```

**Load order does not matter.** `consume` replays every contribution already posted and receives
every one posted afterwards — so your jar may load before or after this one, and neither cares.
