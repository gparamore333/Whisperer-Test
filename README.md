# OSRS Overlay Helpers

[RuneLite](https://runelite.net/) plugins that add **visual-only** overlays for tough boss
fights. None of them automate anything: they never attack, walk, activate items, or toggle
prayers on your behalf. They only read game state that's already visible on screen and draw
indicators so you can react yourself.

## Whisperer Overlay

Visual overlays for the Whisperer boss fight (Desert Treasure II).

- **Tentacle danger tiles** - highlights the tiles about to be hit by the spinning
  tentacle attack, with a tick countdown.
- **Incoming attack prayer** - labels which protection prayer matches the next
  incoming projectile. You still click it yourself.
- **Leech & Vita tracker** - marks leech spawn tiles and highlights "Vita" add NPCs.
- **Pillar health ranking** - labels the three energy pillars 1/2/3 and color-codes
  them by remaining health.
- **Bind timer** - shows a countdown over your player while bound in place.

## Inferno Overlay

Visual overlays for the Inferno.

- **Prayer indicator** - highlights the correct prayer icon (prayer tab and/or
  bottom-right corner) and draws "descending boxes" showing incoming attacks a few
  ticks out. You still click the prayer yourself.
- **Safespot tiles** - color-codes ground tiles by what they're safe from
  (melee/range/magic/combinations), plus red/yellow/green outlines on monsters
  showing whether you're currently safespotted from them.
- **Attack timers** - a tick countdown drawn above each monster.
- **Wave composition display** - reference panel listing which monsters spawn on the
  current/next wave.
- **Obstacle tiles** - outlines tiles monsters can't path through.
- **Nibbler helper** - highlights alive nibblers and the "central" one worth killing first.
- **Blob helpers** - detection-tick indicator and a fading death-location marker.
- **Meleer dig timer** - countdown to when a meleer becomes able to dig underground and
  reposition, grounded in confirmed Inferno mechanics (50 ticks after spawn, then every
  40-60 ticks after each dig, never within 15 ticks of its last attack); bottoms out at
  "DIG" once eligible since the exact tick in that window isn't public knowledge.
- **Jad/Zuk healer helper** - highlights healers that are free to kill.
- **Zuk shield** - live and/or predicted safespot behind the moving shield.
- **Zuk spawn-timer infobox** - countdown to the next mage/ranger spawn set.
- **6-tick Jads** - optional toggle to predict Jad's attack cycle as 6 ticks instead of 8,
  for the Leagues Infinite Jad challenge.

## Kotori Inferno Overlay

A separate, independent Inferno plugin - a faithful port of
[OreoCupcakes' kotori-plugins Inferno overlay](https://github.com/OreoCupcakes/kotori-plugins/tree/master/inferno),
kept side-by-side with `Inferno Overlay` above so you can enable both at once and compare
them directly. The original had no automation to strip; the only change from upstream is
swapping its external `KotoriUtils` dependency for direct RuneLite API calls so it builds
standalone here. It covers the same ground as `Inferno Overlay` (prayer indicator, safespot
tiles, attack timers, wave display, obstacles, nibbler/blob helpers, Jad/Zuk healer
highlighting, Zuk shield safespots, 6-tick Jads) but differs visually and behaviorally in a
few notable ways:

- **Configurable prayer colors** - the four prayer-indicator colors (correct/must-pray/
  upcoming/non-priority) are each their own config option, instead of fixed colors.
- **Bottom-right prayer icon** - an always-available prayer sprite indicator with a
  red/standard background, separate from the prayer-tab outline.
- **Fading blob death tiles** - the death-location outline fades out over real time instead
  of staying a flat color.
- **Meleer dig timer** - upstream's original heuristic (counts idle ticks up to a
  configurable danger threshold) rather than the mechanics-grounded countdown in
  `Inferno Overlay`; useful for seeing how the two compare.
- **6-tick Jad** forces the magic attack specifically (matching upstream's exact behavior),
  rather than just shortening the cycle length.

Every feature in all three plugins can be toggled independently in its config panel.

## Building

These are standard RuneLite external plugins, structured the same way as
[runelite/example-plugin](https://github.com/runelite/example-plugin):

```
./gradlew build
```

## Trying them out locally

Each plugin has a small launcher under `src/test/java` that boots the real RuneLite
client with that plugin registered, using `ExternalPluginManager.loadBuiltin(...)` - no
need to check out RuneLite's own source tree. Run the `main` method of one of these
(e.g. from your IDE):

- `com.whispereroverlay.WhispererOverlayPluginTest`
- `com.infernooverlay.InfernoOverlayPluginTest`
- `com.kotoriinfernooverlay.KotoriInfernoOverlayPluginTest`
- `com.osrsoverlaytest.AllOverlaysTest` (all three at once - handy for comparing the two
  Inferno plugins side-by-side)

This launches the actual client - log in with your own account as normal, then enable
the plugin by name in the plugin list and open its config panel to toggle features.

For background on this pattern, see RuneLite's
[external plugin development guide](https://github.com/runelite/runelite/wiki/Developing-plugins-outside-of-the-plugin-hub).
