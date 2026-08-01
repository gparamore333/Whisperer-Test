# OSRS Overlay Helpers

[RuneLite](https://runelite.net/) plugins that add **visual-only** overlays for tough boss
fights and trap-heavy minigames. None of them automate anything: they never attack, walk,
activate items, or toggle prayers on your behalf. They only read game state that's already
visible on screen and draw indicators so you can react yourself.

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

## CoX Overlay

Visual overlays for Chambers of Xeric (Raids 1) - the 11 combat/puzzle rooms plus the Great
Olm fight. Unlike the other plugins here, most CoX monsters don't have public, verified
tick-by-tick attack timing, so instead of predictive countdowns this plugin reads each
monster's **live state** directly from RuneLite's own NPC ID data - Tekton, Vespula, Vasa,
the Ice Demon, Olm's head/hands, and the crabs all change NPC ID as their fight phase
changes (e.g. Ice Demon frozen vs. thawed, Tekton fighting vs. walking back to the anvil),
so the state labels shown are exact, not guessed.

- **Prayer-tab reminder** - outlines the correct protection prayer for whichever tracked
  monster is closest, for every monster whose attack style is unambiguous (Tekton, the three
  Vanguards, the Guardians, Skeletal Mystics, the Tightrope ranger/mage) or determinable by
  distance (lizardman shamans and the small Muttadile switch between melee and ranged
  depending on whether they're adjacent to you, same as their normal wilderness behavior).
  You still click the prayer yourself - there is no auto-pray. Olm's hands are deliberately
  excluded here: despite being called the "melee hand" and "mage hand," neither one actually
  attacks - that naming describes which damage type *you* need to deal to bypass their 66%
  resistance, not what they hit you with. Olm's head does get a reminder (see below), just a
  reactive one rather than advance warning.
- **Live state labels** - a short status drawn above each tracked monster: Tekton's
  fighting/walking-back/hammering-at-anvil state, Vespula's flying/portal phases, Vasa's
  walking/healing/crystal states, the Ice Demon's frozen/thawed state, each crab's current
  colour, Olm's head/hand spawning/active/disabled states (labelled with which damage type
  each hand is weak to), and so on.
- **Salve amulet reminder** - flags if you enter the Mystics room without a Salve amulet
  variant equipped (Skeletal Mystics are undead).
- **Shaman acid spit warning** - highlights the exact tile a lizardman shaman's acid spit
  will land on, with a live tick countdown to impact read directly off the attack's own
  projectile (the game's real timing, not a guess). No confirmed splash radius exists in
  public data, so only the impact tile is shown - move off it before the countdown hits zero.

### Great Olm

Cross-verified against a second, independent open-source plugin (`coxhelper`, 2019) and
re-checked ID-by-ID against RuneLite's *current* gameval tables and the OSRS Wiki before any
of it was ported - Olm's kit has had real balance history, so nothing here is taken on faith
from a years-old source. Two things that plugin got wrong for other CoX rooms were caught
and *not* carried over: Tekton actually attacks on a 3-tick cycle (not 4) and the Guardian on
a 4-tick cycle (not 5), per the current wiki - but since this plugin never claimed a tick
number for either of them to begin with (state labels only), there was nothing to fix there.

- **Crystal bomb heatmap + countdown** - a colour-coded danger zone around each crystal
  bomb (green/yellow/orange/red/lethal by tile distance) with an exact tick-to-detonation
  countdown - bombs always detonate exactly 8 ticks after spawning, so this is a hard
  number, not an estimate.
- **Acid pool, falling-crystal, lightning-trail, and Life Siphon beam warnings** - highlights
  the ground objects/graphics for each of these hazards as soon as they appear.
- **Head attack prayer (reactive)** - the head alternates magic/ranged attacks
  unpredictably (roughly a 1-in-5 chance to switch each time), so this can't warn you in
  advance - but it does outline the correct prayer the instant the attack projectile
  actually spawns, which is still ahead of it landing.
- **Sphere attack prayer** - Olm's sphere attacks (aggression/magical power/accuracy and
  dexterity) are announced in the chatbox before they land; this reads that announcement to
  outline the exact prayer needed.
- **Phase banner** - briefly announces each elemental phase (acid/crystal/flame) and the
  final stand, read from Olm's own phase-transition chat message.
- **Hand clench warning** - flags the melee hand as temporarily resistant right after it
  clenches (confirmed melee-hand-only - the mage hand has a permanent, non-clenching
  resistance instead, not a bug in what this mirrors). The exact resistance duration isn't
  wiki-confirmed, so this clears after an approximate window rather than an exact countdown.
- **Acid Drip / burn victim highlighting** - highlights whoever the Acid Drip attack is
  currently targeting, and any player currently burning from Deep Burn.
- **Teleport target warning** - highlights players Olm has just paired for its teleport
  attack (read from the pairing chat message) and their landing tiles (read from the game's
  own teleport-marker graphics).
- **Head Attack-option removal** - removes the "Attack" option on Olm's head from your
  right-click menu while a hand is still alive (the head just heals itself outside the
  final phase, so attacking it then is a wasted click) - you still choose and click
  whatever option remains, this only removes a wrong one.

#### Solo Olm Kiting Assist

Off by default (advanced/solo-specific, closed section in the config panel). Purely
informational - it highlights tiles and shows text, it never moves or clicks anything for you.
The actual point of the 3:0/4:1/3:1 hand-kiting techniques isn't dodging a hand attack (the
hands never attack - only the head does); it's forcing the head to turn away right before one
of its special attacks is due, which makes it skip that attack entirely rather than delay it.
The head's attack speed is 4 (the wiki's own infobox value, not an estimate) - it re-checks
whether it can see a player every 4 ticks, forever, whether that check lands a real attack, a
no-op, or a wasted turn. This assist is built around that real cadence:

- **Head-facing indicator** - the head only ever faces one of 3 real states (LEFT/MIDDLE/
  RIGHT, not a smooth cone), read live off the head NPC's own orientation every tick, and
  highlights your own tile green (safe), orange (head centred), or red (facing you) using the
  wiki's own per-tile visibility table (spots 1/8 visible only when LEFT, 4/5 only when RIGHT,
  2/7 and 3/6 visible from two of the three states - see `CoxOlmSafespot`'s javadoc for the
  full table). One thing this can't independently verify: which physical side (west/melee-hand
  vs. east/mage-hand) the wiki's "LEFT" actually refers to - the "Chambers of Xeric/Strategies"
  and "Perfect Olm (Solo)" pages directly contradict each other on that, so if it looks
  backwards once you've tested it live, flip the "Swap head-facing left/right" option.
- **Safespot tile grid** - draws the actual 8 numbered tiles from the wiki's own safespot
  diagram (the tiles every guide calls by number, resolved to your live raid instance) and
  highlights the nearest one currently safe per that same per-tile table as your move target.
- **Next action tick countdown** - a live countdown to the head's next 4-tick check, resynced
  off every directly-observed attack. If you're on a hidden tile when it hits zero, that check
  is denied for free; combined with the special-due warning below, this tells you exactly how
  many ticks you have left to get to a safe tile before a queued special would otherwise fire.
- **Special attack due warning** - Olm's specials (Crystal Burst/Lightning/Teleport) run on a
  wiki-confirmed fixed rotation, always exactly two standard attacks apart. This counts
  observed standard attacks (the head's basic magic/range attack, spheres, and the elemental-
  phase abilities that substitute for them) to flag when a special is next in line, resyncing
  off Lightning and Teleport whenever they're directly observed (both have a confirmed
  graphic/chat signal). Crystal Burst has no verified tracked ID, so a slot predicted as
  Crystal Burst is a best-effort inference, not a confirmation - and Fire Wall (also
  untracked) can let the count drift by at most one cycle during the flame phase before the
  next Lightning/Teleport resyncs it.
- **Attack ratio counter** - counts your own attacks on whichever hand you're fighting and
  shows progress through your configured cycle (3:0 mage / 4:1 or 3:1 melee, picked in
  config based on your weapon's attack speed) as a secondary pacing aid.
- **Drink stamina reminder** - flags when your run energy drops below a configurable
  threshold, since stamina potions are highly recommended for solo Olm. Works anywhere in the
  raid, not just the Olm room.

Not covered: precise puzzle-solving for the Crabs room (which crystal needs which colour
isn't derivable from public data), the resource room prep counter, and the thieving room.
Vespula's actual attack pattern is deliberately left unpredicted (not a fixed style), matching
how the upstream RoeLite plugin handles it - configure your in-game quick-prayers if you want
a reminder there.

## Hallowed Sepulchre Overlay

Visual overlays for the Hallowed Sepulchre minigame, ported from
[OreoCupcakes' kotori-plugins `hallowedhelper`](https://github.com/OreoCupcakes/kotori-plugins/tree/master/hallowedhelper)
and re-verified: every hardcoded object/NPC/animation/graphic ID was cross-checked against
RuneLite's current gameval tables (all confirmed still valid and correctly named) and the
mechanics were cross-checked against the current OSRS Wiki.

- **Fire-trap tiles** - each wizard statue is tracked live off its own renderable animation
  (the same "watch the real animation instead of guessing a timer" approach as Inferno's dig
  timer), coloured safe/risky/unsafe by ticks until it fires next, with the danger line
  projected from the statue's own orientation. T3 statues use a 2-tick cycle instead of 3,
  per the wiki's own note that floor 5's statues "change phases 1 tick faster than on floors
  1-4" - the exact absolute tick numbers aren't stated verbatim on the wiki, so treat them as
  strongly-supported rather than word-for-word confirmed.
- **Lightning tiles** - highlights tiles a priest statue's lightning has just struck, with a
  countdown. The exact cycle length isn't wiki-published (only that there's "one tick between
  cycles with no flames present"), so the countdown carries over the legacy plugin's own
  observed timing rather than a confirmed number.
- **Sword and arrow danger tiles** - highlights a thrown sword's or arrow's current tile, plus
  (for arrows) the tiles ahead of its travel direction, all read live from the NPC's own
  position/orientation. The throwing/firing statues themselves are also outlined while
  mid-animation.
- **Coffins** - highlights each coffin's closed/lockpicking/opening/open state, and flags a
  failed lockpick (poison risk). The closed/open states are read from the object's own
  resolved appearance (its "impostor" ID); the opening-stage animations are read off your own
  character, matching how the legacy plugin identified them. The poison-fail graphic ID isn't
  wiki-confirmed for this specific effect - its current gameval name refers to an unrelated
  swamp effect, which may just mean Jagex reuses a generic poison-gas graphic across content,
  but verify in-game before fully trusting it.
- **End-of-floor portal, bridge, stairs, floor gates** - highlighted by their live built/
  unbuilt or closed/open state, read from confirmed current object IDs.
- **Strange tiles (teleporters)** - highlights currently-lit blue (forward) and yellow
  (backward) pads with a despawn countdown. The wiki confirms these light up randomly, so
  this is reactive tracking, not prediction.
- **Server tile** - outlines your own network-authoritative tile, off by default.

**Deliberately not ported**, with why:

- The original's floor 4/5 "predict the whole room's tile grid" system - large, hand-tuned,
  per-floor state machines (with literal `//HOTFIX` comments) that synchronized live statue
  observations against memorized pattern tables. It's superseded here by the same live
  per-statue tracking used everywhere else in this plugin, which needs no memorized patterns
  and can't drift out of sync with a room layout.
- The floor-gate "pick the single correct gate among several" logic, which relied on hardcoded
  object hashes and coordinate thresholds per floor/subfloor that couldn't be verified without
  live testing. All tracked floor gates are highlighted uniformly instead.
- A single hardcoded "safespot" tile coordinate for floor 1 - it's local-instance-relative
  with no live signal confirming which floor's instance it actually applies to, so keeping it
  risked highlighting the wrong tile on an unrelated floor.
- Two cosmetic, non-gameplay toggles ("Explode on hit", "Glitchy Grapple") that overrode which
  animation played on your own character for a visual gag. They didn't click, move, or send
  input on your behalf, so they're not automation by this project's definition, but they're
  also not "read game state and draw an overlay" - just an unrelated novelty, dropped for scope.

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
- `com.coxoverlay.CoxOverlayPluginTest`
- `com.hallowedoverlay.HallowedOverlayPluginTest`
- `com.osrsoverlaytest.AllOverlaysTest` (all five at once - handy for comparing the two
  Inferno plugins side-by-side)

This launches the actual client - log in with your own account as normal, then enable
the plugin by name in the plugin list and open its config panel to toggle features.

For background on this pattern, see RuneLite's
[external plugin development guide](https://github.com/runelite/runelite/wiki/Developing-plugins-outside-of-the-plugin-hub).
