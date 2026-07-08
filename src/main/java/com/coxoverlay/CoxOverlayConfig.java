package com.coxoverlay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * No auto-pray or other action-taking options exist here by design: this plugin only reads
 * game state (NPC IDs/positions, equipment) and draws indicators. Prayers are always the
 * player's own click.
 */
@ConfigGroup("coxoverlay")
public interface CoxOverlayConfig extends Config
{
	@ConfigSection(name = "General", description = "General options", position = 0)
	String GENERAL_SECTION = "general";

	@ConfigSection(name = "Rooms", description = "Per-room toggles for the state/prayer overlay", position = 1)
	String ROOMS_SECTION = "rooms";

	@ConfigSection(name = "Mystics", description = "Mystics room options", position = 2)
	String MYSTICS_SECTION = "mystics";

	@ConfigSection(name = "Great Olm", description = "Great Olm fight options", position = 3)
	String OLM_SECTION = "olm";

	@ConfigSection(name = "Solo Olm Kiting Assist", description = "Advanced solo hand-kiting helpers", position = 4, closedByDefault = true)
	String OLM_KITE_SECTION = "olmKite";

	@ConfigItem(position = 0, keyName = "enablePrayerOverlay", name = "Enable prayer overlay",
		description = "Master toggle for the prayer-tab icon indicator.", section = GENERAL_SECTION)
	default boolean enablePrayerOverlay()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "enableStateLabels", name = "Enable NPC state labels",
		description = "Draws a short status label above tracked NPCs (e.g. \"Walking back to anvil\", \"THAWED\").", section = GENERAL_SECTION)
	default boolean enableStateLabels()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "roomTekton", name = "Tekton", description = "Enable the overlay in the Tekton room.", section = ROOMS_SECTION)
	default boolean roomTekton()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "roomVanguards", name = "Vanguards", description = "Enable the overlay in the Vanguards room.", section = ROOMS_SECTION)
	default boolean roomVanguards()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "roomVespula", name = "Vespula", description = "Enable the overlay in the Vespula room.", section = ROOMS_SECTION)
	default boolean roomVespula()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "roomMuttadiles", name = "Muttadiles", description = "Enable the overlay in the Muttadiles room.", section = ROOMS_SECTION)
	default boolean roomMuttadiles()
	{
		return true;
	}

	@ConfigItem(position = 4, keyName = "roomVasa", name = "Vasa", description = "Enable the overlay in the Vasa room.", section = ROOMS_SECTION)
	default boolean roomVasa()
	{
		return true;
	}

	@ConfigItem(position = 5, keyName = "roomGuardians", name = "Guardians", description = "Enable the overlay in the Guardians room.", section = ROOMS_SECTION)
	default boolean roomGuardians()
	{
		return true;
	}

	@ConfigItem(position = 6, keyName = "roomShamans", name = "Shamans", description = "Enable the overlay in the Shamans room.", section = ROOMS_SECTION)
	default boolean roomShamans()
	{
		return true;
	}

	@ConfigItem(position = 7, keyName = "roomCrabs", name = "Crabs", description = "Enable the overlay in the Crabs room.", section = ROOMS_SECTION)
	default boolean roomCrabs()
	{
		return true;
	}

	@ConfigItem(position = 8, keyName = "roomIceDemon", name = "Ice Demon", description = "Enable the overlay in the Ice Demon room.", section = ROOMS_SECTION)
	default boolean roomIceDemon()
	{
		return true;
	}

	@ConfigItem(position = 9, keyName = "roomMystics", name = "Mystics", description = "Enable the overlay in the Mystics room.", section = ROOMS_SECTION)
	default boolean roomMystics()
	{
		return true;
	}

	@ConfigItem(position = 10, keyName = "roomTightrope", name = "Tightrope", description = "Enable the overlay in the Tightrope room.", section = ROOMS_SECTION)
	default boolean roomTightrope()
	{
		return true;
	}

	@ConfigItem(position = 11, keyName = "shamanAcidWarning", name = "Shaman acid spit warning",
		description = "Highlights the exact tile a lizardman shaman's acid spit will land on, with a live tick countdown read directly off the attack itself. No confirmed splash radius exists in public data, so only the impact tile is shown - move off it before the countdown hits zero.",
		section = ROOMS_SECTION)
	default boolean shamanAcidWarning()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "salveReminder", name = "Salve amulet reminder",
		description = "Warns if you enter the Mystics room without a Salve amulet variant equipped (Skeletal Mystics are undead).", section = MYSTICS_SECTION)
	default boolean salveReminder()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "olmEnable", name = "Enable Great Olm overlay", description = "Master toggle for the Olm room overlay.", section = OLM_SECTION)
	default boolean olmEnable()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "olmBombHeatmap", name = "Crystal bomb heatmap",
		description = "Colour-coded danger zone and countdown around crystal bombs (8 ticks from spawn to detonation).", section = OLM_SECTION)
	default boolean olmBombHeatmap()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "olmAcidWarning", name = "Acid pool warning", description = "Highlights acid pools on the ground.", section = OLM_SECTION)
	default boolean olmAcidWarning()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "olmCrystalWarning", name = "Falling crystal warning", description = "Highlights falling-crystal ground markers.", section = OLM_SECTION)
	default boolean olmCrystalWarning()
	{
		return true;
	}

	@ConfigItem(position = 4, keyName = "olmLightningWarning", name = "Lightning trail warning",
		description = "Highlights the Great Olm's lightning trail tiles (binds and disables prayer).", section = OLM_SECTION)
	default boolean olmLightningWarning()
	{
		return true;
	}

	@ConfigItem(position = 5, keyName = "olmRemoveHeadAttack", name = "Remove head Attack option",
		description = "Removes the Attack menu option on Olm's head while a hand is still active, to prevent misclicks (the head just heals itself outside the final phase). You still choose what to click - this only removes a wrong option.", section = OLM_SECTION)
	default boolean olmRemoveHeadAttack()
	{
		return true;
	}

	@ConfigItem(position = 6, keyName = "olmHeadPrayer", name = "Head attack prayer (reactive)",
		description = "Outlines the correct prayer the instant Olm's head fires a magic or ranged projectile. It alternates styles unpredictably, so this reacts to the attack in flight rather than warning in advance.", section = OLM_SECTION)
	default boolean olmHeadPrayer()
	{
		return true;
	}

	@ConfigItem(position = 7, keyName = "olmSpherePrayer", name = "Sphere attack prayer",
		description = "Outlines the correct prayer the moment Olm announces a sphere attack in the chatbox (aggression=melee, magical power=magic, accuracy and dexterity=ranged).", section = OLM_SECTION)
	default boolean olmSpherePrayer()
	{
		return true;
	}

	@ConfigItem(position = 8, keyName = "olmPhaseBanner", name = "Phase banner",
		description = "Briefly shows which elemental phase (acid/crystal/flame) or the final stand has just started, read from Olm's own chat announcement.", section = OLM_SECTION)
	default boolean olmPhaseBanner()
	{
		return true;
	}

	@ConfigItem(position = 9, keyName = "olmHandClenchWarning", name = "Hand clench warning",
		description = "Flags the melee hand as temporarily damage-resistant after it clenches (confirmed melee-hand-only; the mage hand has no equivalent). The exact resistance duration isn't wiki-confirmed, so the warning clears after an approximate window rather than an exact countdown.", section = OLM_SECTION)
	default boolean olmHandClenchWarning()
	{
		return true;
	}

	@ConfigItem(position = 10, keyName = "olmAcidTargetWarning", name = "Acid Drip target warning",
		description = "Highlights whoever Olm's Acid Drip attack is currently targeting (they'll start dropping acid pools under themselves).", section = OLM_SECTION)
	default boolean olmAcidTargetWarning()
	{
		return true;
	}

	@ConfigItem(position = 11, keyName = "olmBurnVictimWarning", name = "Burn victim warning",
		description = "Highlights players currently burning from the Deep Burn attack.", section = OLM_SECTION)
	default boolean olmBurnVictimWarning()
	{
		return true;
	}

	@ConfigItem(position = 12, keyName = "olmTeleportWarning", name = "Teleport target warning",
		description = "Highlights players Olm has just paired for its teleport attack, plus their landing tiles, read from the chat pairing message and the game's own teleport-marker graphics.", section = OLM_SECTION)
	default boolean olmTeleportWarning()
	{
		return true;
	}

	@ConfigItem(position = 13, keyName = "olmHealBeamWarning", name = "Life Siphon beam tiles",
		description = "Highlights the healing-beam tiles during Olm's final-phase Life Siphon attack.", section = OLM_SECTION)
	default boolean olmHealBeamWarning()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "olmKiteEnable", name = "Enable kiting assist",
		description = "Master toggle for the solo hand-kiting helpers below. Off by default - these are advanced, solo-specific techniques, not something everyone in a team raid wants cluttering the screen.",
		section = OLM_KITE_SECTION)
	default boolean olmKiteEnable()
	{
		return false;
	}

	@ConfigItem(position = 1, keyName = "olmKiteHeadFacing", name = "Head-facing indicator",
		description = "Shows which side of the room the head is currently oriented towards, read live off the head NPC's own facing angle every tick (not off the wiki's fixed safespot-tile rules, which two wiki pages actually contradict each other on), plus whether you're currently standing in its line of fire. Highlights your own tile green when the head isn't facing your side, orange when it's facing the middle, and red when it's facing you.",
		section = OLM_KITE_SECTION)
	default boolean olmKiteHeadFacing()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "olmKiteSafespotTiles", name = "Safespot tile grid",
		description = "Draws the 8 numbered safespot tiles from the OSRS Wiki's own safespot diagram (the same tiles every guide calls by number) and highlights the nearest one currently outside the head's facing cone as your move target - so instead of just knowing a turn is needed, you know exactly which named spot to run to before it happens. Safety per-tile is computed live from the head's real facing angle, not from the wiki's fixed left/right rules (its two pages contradict each other on those).",
		section = OLM_KITE_SECTION)
	default boolean olmKiteSafespotTiles()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "olmKiteAttackCounter", name = "Attack ratio counter",
		description = "Counts your own attacks on whichever hand you're fighting and shows progress through your configured X:Y cycle (e.g. \"3 / 4\"), flashing on the last attack of the cycle as a cue to expect the hand's head-attack window. This is a pacing aid based on your real attacks, not a guaranteed no-hit predictor - the wiki itself calls the exact entry/reset technique advanced and situational.",
		section = OLM_KITE_SECTION)
	default boolean olmKiteAttackCounter()
	{
		return true;
	}

	@ConfigItem(position = 4, keyName = "olmKiteMeleeRatio", name = "Melee hand ratio",
		description = "Which melee kiting ratio to track, based on your weapon's attack speed.", section = OLM_KITE_SECTION)
	default CoxOlmMeleeRatio olmKiteMeleeRatio()
	{
		return CoxOlmMeleeRatio.FOUR_ONE;
	}

	@ConfigItem(position = 5, keyName = "olmKiteMageRatio", name = "Mage hand ratio",
		description = "Which mage kiting ratio to track, based on your weapon's attack speed.", section = OLM_KITE_SECTION)
	default CoxOlmMageRatio olmKiteMageRatio()
	{
		return CoxOlmMageRatio.THREE_ZERO;
	}

	@ConfigItem(position = 6, keyName = "olmKiteSpecialWarning", name = "Special attack due warning",
		description = "The actual point of the hand-kiting technique: Olm's special attacks (Crystal Burst/Lightning/Teleport) run on a fixed rotation, always exactly two standard attacks apart. Forcing a head turn - moving somewhere it can't currently see you (check the head-facing indicator) - makes it skip whatever's next, denying a queued special outright rather than delaying it. This counts observed standard attacks to flag when a special is next in line, so you know to force a turn before it fires. It resyncs off Lightning and Teleport (both directly observable); Crystal Burst has no verified tracked ID, so a slot predicted as Crystal Burst is a best-effort guess, not a confirmation.",
		section = OLM_KITE_SECTION)
	default boolean olmKiteSpecialWarning()
	{
		return true;
	}

	@ConfigItem(position = 7, keyName = "olmKiteStaminaReminder", name = "Drink stamina reminder",
		description = "Flags when your run energy drops below the threshold below, since stamina potions are highly recommended for solo Olm. Works anywhere in the raid, not just the Olm room.",
		section = OLM_KITE_SECTION)
	default boolean olmKiteStaminaReminder()
	{
		return true;
	}

	@ConfigItem(position = 8, keyName = "olmKiteStaminaThreshold", name = "Stamina threshold (%)",
		description = "Show the drink-stamina reminder once run energy drops to or below this percentage.", section = OLM_KITE_SECTION)
	default int olmKiteStaminaThreshold()
	{
		return 50;
	}
}
