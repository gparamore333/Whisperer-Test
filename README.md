# Whisperer Overlay

A [RuneLite](https://runelite.net/) plugin that adds **visual-only** overlays for the
Whisperer boss fight (Desert Treasure II). It does not automate anything: it never
attacks, walks, activates items, or toggles prayers on your behalf. It only reads game
state that's already visible on screen and draws indicators so you can react yourself.

## Features

- **Tentacle danger tiles** - highlights the tiles about to be hit by the spinning
  tentacle attack, with a tick countdown.
- **Incoming attack prayer** - labels which protection prayer matches the next
  incoming projectile. You still click it yourself.
- **Leech & Vita tracker** - marks leech spawn tiles and highlights "Vita" add NPCs.
- **Pillar health ranking** - labels the three energy pillars 1/2/3 and color-codes
  them by remaining health.
- **Bind timer** - shows a countdown over your player while bound in place.

Every feature can be toggled independently in the plugin's config panel.

## Building

This is a standard RuneLite external plugin, structured the same way as
[runelite/example-plugin](https://github.com/runelite/example-plugin):

```
./gradlew build
```

To run it against your RuneLite client, follow RuneLite's
[external plugin development guide](https://github.com/runelite/runelite/wiki/Developing-plugins-outside-of-the-plugin-hub).
