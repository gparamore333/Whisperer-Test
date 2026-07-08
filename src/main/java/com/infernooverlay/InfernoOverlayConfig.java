package com.infernooverlay;

import java.awt.Color;

import com.infernooverlay.displaymodes.NpcNamingMode;
import com.infernooverlay.displaymodes.PrayerDisplayMode;
import com.infernooverlay.displaymodes.SafespotDisplayMode;
import com.infernooverlay.displaymodes.WaveDisplayMode;
import com.infernooverlay.displaymodes.ZukShieldDisplayMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("infernooverlay")
public interface InfernoOverlayConfig extends Config
{
	@ConfigSection(name = "Prayer", description = "Prayer indicator options", position = 0)
	String PRAYER_SECTION = "prayer";

	@ConfigSection(name = "Safespots", description = "Safespot tile and NPC outline options", position = 1)
	String SAFESPOTS_SECTION = "safespots";

	@ConfigSection(name = "Waves", description = "Wave composition display options", position = 2)
	String WAVES_SECTION = "waves";

	@ConfigSection(name = "Extra", description = "Miscellaneous extras", position = 3)
	String EXTRA_SECTION = "extra";

	@ConfigSection(name = "Nibblers", description = "Nibbler-specific options", position = 4)
	String NIBBLERS_SECTION = "nibblers";

	@ConfigSection(name = "Bats", description = "Bat-specific options", position = 5)
	String BATS_SECTION = "bats";

	@ConfigSection(name = "Blobs", description = "Blob-specific options", position = 6)
	String BLOBS_SECTION = "blobs";

	@ConfigSection(name = "Meleers", description = "Meleer-specific options", position = 7)
	String MELEERS_SECTION = "meleers";

	@ConfigSection(name = "Rangers", description = "Ranger-specific options", position = 8)
	String RANGERS_SECTION = "rangers";

	@ConfigSection(name = "Magers", description = "Mager-specific options", position = 9)
	String MAGERS_SECTION = "magers";

	@ConfigSection(name = "Jad", description = "Jad-specific options", position = 10)
	String JAD_SECTION = "jad";

	@ConfigSection(name = "Jad Healers", description = "Jad healer-specific options", position = 11)
	String JAD_HEALERS_SECTION = "jadHealers";

	@ConfigSection(name = "Zuk", description = "Zuk-specific options", position = 12)
	String ZUK_SECTION = "zuk";

	@ConfigSection(name = "Zuk Healers", description = "Zuk healer-specific options", position = 13)
	String ZUK_HEALERS_SECTION = "zukHealers";

	// --- Prayer ---

	@ConfigItem(position = 0, keyName = "prayerDisplayMode", name = "Prayer display mode",
		description = "Show the prayer indicator in the prayer tab, the bottom-right corner, or both.", section = PRAYER_SECTION)
	default PrayerDisplayMode prayerDisplayMode()
	{
		return PrayerDisplayMode.BOTH;
	}

	@ConfigItem(position = 1, keyName = "indicateWhenPrayingCorrectly", name = "Indicate when praying correctly",
		description = "Keep showing the indicator even when you're already praying the right prayer.", section = PRAYER_SECTION)
	default boolean indicateWhenPrayingCorrectly()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "descendingBoxes", name = "Descending boxes",
		description = "Draws Piano Tiles-style boxes falling toward the prayer icons to show incoming attacks a few ticks out.", section = PRAYER_SECTION)
	default boolean descendingBoxes()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "indicateNonPriorityBoxes", name = "Indicate non-priority boxes",
		description = "Also render descending boxes for prayers that aren't the priority prayer for that tick.", section = PRAYER_SECTION)
	default boolean indicateNonPriorityBoxes()
	{
		return true;
	}

	@ConfigItem(position = 4, keyName = "alwaysShowPrayerHelper", name = "Always show prayer helper",
		description = "Render the prayer helper even when the prayer tab isn't open.", section = PRAYER_SECTION)
	default boolean alwaysShowPrayerHelper()
	{
		return false;
	}

	// --- Safespots ---

	@ConfigItem(position = 0, keyName = "safespotDisplayMode", name = "Tile safespots",
		description = "Color ground tiles by what they're safe from: melee (red), range (green), magic (blue), or combinations.", section = SAFESPOTS_SECTION)
	default SafespotDisplayMode safespotDisplayMode()
	{
		return SafespotDisplayMode.AREA;
	}

	@ConfigItem(position = 1, keyName = "safespotsCheckSize", name = "Check size",
		description = "The size of the area around the player to scan for safespots (size x size).", section = SAFESPOTS_SECTION)
	@Range(min = 2, max = 15)
	default int safespotsCheckSize()
	{
		return 6;
	}

	@ConfigItem(position = 2, keyName = "indicateNonSafespotted", name = "Non-safespotted outline",
		description = "Red outline on monsters that can currently attack you.", section = SAFESPOTS_SECTION)
	default boolean indicateNonSafespotted()
	{
		return false;
	}

	@ConfigItem(position = 3, keyName = "indicateTemporarySafespotted", name = "Temporarily-safespotted outline",
		description = "Yellow outline on monsters that would have to move to attack you.", section = SAFESPOTS_SECTION)
	default boolean indicateTemporarySafespotted()
	{
		return false;
	}

	@ConfigItem(position = 4, keyName = "indicateSafespotted", name = "Safespotted outline",
		description = "Green outline on monsters that can't reach you at all right now.", section = SAFESPOTS_SECTION)
	default boolean indicateSafespotted()
	{
		return false;
	}

	// --- Waves ---

	@ConfigItem(position = 0, keyName = "waveDisplay", name = "Wave display",
		description = "Show the monster composition for the current and/or next wave.", section = WAVES_SECTION)
	default WaveDisplayMode waveDisplay()
	{
		return WaveDisplayMode.BOTH;
	}

	@ConfigItem(position = 1, keyName = "npcNaming", name = "NPC naming",
		description = "Simple (Bat) or complex (Jal-MejRah) monster names.", section = WAVES_SECTION)
	default NpcNamingMode npcNaming()
	{
		return NpcNamingMode.SIMPLE;
	}

	@ConfigItem(position = 2, keyName = "waveHeaderColor", name = "Wave header color",
		description = "Color for the wave panel header.", section = WAVES_SECTION)
	default Color waveHeaderColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(position = 3, keyName = "waveTextColor", name = "Wave text color",
		description = "Color for the wave panel text.", section = WAVES_SECTION)
	default Color waveTextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(position = 4, keyName = "indicateKnownSpawnPoints", name = "Known spawn points",
		description = "Marks every tile a monster has actually spawned from this attempt. The Inferno reuses a small fixed set " +
			"of spawn tiles all fight, so this fills in fast and can help you anticipate where the next wave's monsters will " +
			"appear - it only ever shows tiles a spawn has genuinely happened at, never a guess.", section = WAVES_SECTION)
	default boolean indicateKnownSpawnPoints()
	{
		return true;
	}

	// --- Extra ---

	@ConfigItem(position = 0, keyName = "indicateObstacles", name = "Obstacles",
		description = "Outline tiles that monsters can't path through.", section = EXTRA_SECTION)
	default boolean indicateObstacles()
	{
		return false;
	}

	@ConfigItem(position = 1, keyName = "spawnTimerInfobox", name = "Zuk spawn-timer infobox",
		description = "Countdown to the next mage/ranger spawn set during the Zuk fight.", section = EXTRA_SECTION)
	default boolean spawnTimerInfobox()
	{
		return false;
	}

	// --- Nibblers ---

	@ConfigItem(position = 0, keyName = "indicateNibblers", name = "Indicate nibblers",
		description = "Highlight nibblers that are still alive.", section = NIBBLERS_SECTION)
	default boolean indicateNibblers()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "indicateCentralNibbler", name = "Indicate central nibbler",
		description = "Highlight the nibbler whose death would free the most other nibblers.", section = NIBBLERS_SECTION)
	default boolean indicateCentralNibbler()
	{
		return true;
	}

	// --- Bats ---

	@ConfigItem(position = 0, keyName = "prayerBat", name = "Prayer helper", description = "Include bats in the prayer indicator.", section = BATS_SECTION)
	default boolean prayerBat()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcBat", name = "Attack timer", description = "Draw ticks-until-attack above bats.", section = BATS_SECTION)
	default boolean ticksOnNpcBat()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "safespotsBat", name = "Safespots", description = "Include bats in safespot calculation.", section = BATS_SECTION)
	default boolean safespotsBat()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "indicateNpcPositionBat", name = "Indicate main tile",
		description = "Highlight the canonical tile used for this multi-tile monster's pathfinding.", section = BATS_SECTION)
	default boolean indicateNpcPositionBat()
	{
		return false;
	}

	// --- Blobs ---

	@ConfigItem(position = 0, keyName = "prayerBlob", name = "Prayer helper", description = "Include blobs in the prayer indicator.", section = BLOBS_SECTION)
	default boolean prayerBlob()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "indicateBlobDetectionTick", name = "Detection tick",
		description = "Show a prayer indicator for the tick a blob checks your prayer to decide its attack.", section = BLOBS_SECTION)
	default boolean indicateBlobDetectionTick()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "indicateBlobDeathLocation", name = "Death location",
		description = "Highlight where a blob died with a countdown until mini-blobs spawn.", section = BLOBS_SECTION)
	default boolean indicateBlobDeathLocation()
	{
		return false;
	}

	@ConfigItem(position = 3, keyName = "blobDeathLocationColor", name = "Death location color", description = "Color for the blob death tile outline.", section = BLOBS_SECTION)
	default Color blobDeathLocationColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(position = 4, keyName = "ticksOnNpcBlob", name = "Attack timer", description = "Draw ticks-until-attack above blobs.", section = BLOBS_SECTION)
	default boolean ticksOnNpcBlob()
	{
		return true;
	}

	@ConfigItem(position = 5, keyName = "safespotsBlob", name = "Safespots", description = "Include blobs in safespot calculation.", section = BLOBS_SECTION)
	default boolean safespotsBlob()
	{
		return true;
	}

	@ConfigItem(position = 6, keyName = "indicateNpcPositionBlob", name = "Indicate main tile",
		description = "Highlight the canonical tile used for this multi-tile monster's pathfinding.", section = BLOBS_SECTION)
	default boolean indicateNpcPositionBlob()
	{
		return false;
	}

	// --- Meleers ---

	@ConfigItem(position = 0, keyName = "prayerMeleer", name = "Prayer helper", description = "Include meleers in the prayer indicator.", section = MELEERS_SECTION)
	default boolean prayerMeleer()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcMeleer", name = "Attack timer", description = "Draw ticks-until-attack above meleers.", section = MELEERS_SECTION)
	default boolean ticksOnNpcMeleer()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "digTimer", name = "Dig timer",
		description = "Draws a countdown to when a meleer becomes able to dig underground and reposition next to you. Grounded in " +
			"confirmed Inferno mechanics: a meleer can't dig until 50 ticks after it spawns (then every 40-60 ticks after each dig), " +
			"and never within 15 ticks of its last attack, or while it can already reach you. The exact tick inside that 40-60 " +
			"window isn't public knowledge, so the countdown bottoms out at 'DIG' (could happen any tick) rather than a further " +
			"guess. Once it actually starts digging, the regular 'Attack timer' above takes over with a real countdown to when it " +
			"resurfaces.", section = MELEERS_SECTION)
	default boolean digTimer()
	{
		return false;
	}

	@Range(min = 1, max = 50)
	@ConfigItem(position = 3, keyName = "digTimerThreshold", name = "Dig timer draw threshold", description = "Ticks-until-eligible at which the dig timer starts showing.", section = MELEERS_SECTION)
	default int digTimerThreshold()
	{
		return 20;
	}

	@ConfigItem(position = 5, keyName = "safespotsMeleer", name = "Safespots", description = "Include meleers in safespot calculation.", section = MELEERS_SECTION)
	default boolean safespotsMeleer()
	{
		return true;
	}

	@ConfigItem(position = 6, keyName = "indicateNpcPositionMeleer", name = "Indicate main tile",
		description = "Highlight the canonical tile used for this multi-tile monster's pathfinding.", section = MELEERS_SECTION)
	default boolean indicateNpcPositionMeleer()
	{
		return false;
	}

	// --- Rangers ---

	@ConfigItem(position = 0, keyName = "prayerRanger", name = "Prayer helper", description = "Include rangers in the prayer indicator.", section = RANGERS_SECTION)
	default boolean prayerRanger()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcRanger", name = "Attack timer", description = "Draw ticks-until-attack above rangers.", section = RANGERS_SECTION)
	default boolean ticksOnNpcRanger()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "safespotsRanger", name = "Safespots", description = "Include rangers in safespot calculation.", section = RANGERS_SECTION)
	default boolean safespotsRanger()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "indicateNpcPositionRanger", name = "Indicate main tile",
		description = "Highlight the canonical tile used for this multi-tile monster's pathfinding.", section = RANGERS_SECTION)
	default boolean indicateNpcPositionRanger()
	{
		return false;
	}

	// --- Magers ---

	@ConfigItem(position = 0, keyName = "prayerMage", name = "Prayer helper", description = "Include magers in the prayer indicator.", section = MAGERS_SECTION)
	default boolean prayerMage()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcMage", name = "Attack timer", description = "Draw ticks-until-attack above magers.", section = MAGERS_SECTION)
	default boolean ticksOnNpcMage()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "safespotsMage", name = "Safespots", description = "Include magers in safespot calculation.", section = MAGERS_SECTION)
	default boolean safespotsMage()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "indicateNpcPositionMage", name = "Indicate main tile",
		description = "Highlight the canonical tile used for this multi-tile monster's pathfinding.", section = MAGERS_SECTION)
	default boolean indicateNpcPositionMage()
	{
		return false;
	}

	// --- Jad ---

	@ConfigItem(position = 0, keyName = "prayerJad", name = "Prayer helper", description = "Include Jad in the prayer indicator.", section = JAD_SECTION)
	default boolean prayerJad()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcJad", name = "Attack timer", description = "Draw ticks-until-attack above Jad.", section = JAD_SECTION)
	default boolean ticksOnNpcJad()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "safespotsJad", name = "Safespots (melee range only)", description = "Include Jad in safespot calculation.", section = JAD_SECTION)
	default boolean safespotsJad()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "sixTickJad", name = "6-tick Jads",
		description = "Predicts Jad's attack cycle as 6 ticks instead of 8, for the Leagues Infinite Jad challenge.", section = JAD_SECTION)
	default boolean sixTickJad()
	{
		return false;
	}

	// --- Jad Healers ---

	@ConfigItem(position = 0, keyName = "prayerHealerJad", name = "Prayer helper", description = "Include Jad healers in the prayer indicator.", section = JAD_HEALERS_SECTION)
	default boolean prayerHealerJad()
	{
		return false;
	}

	@ConfigItem(position = 1, keyName = "ticksOnNpcHealerJad", name = "Attack timer", description = "Draw ticks-until-attack above Jad healers.", section = JAD_HEALERS_SECTION)
	default boolean ticksOnNpcHealerJad()
	{
		return false;
	}

	@ConfigItem(position = 2, keyName = "safespotsHealerJad", name = "Safespots", description = "Include Jad healers in safespot calculation.", section = JAD_HEALERS_SECTION)
	default boolean safespotsHealerJad()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "indicateActiveHealerJad", name = "Indicate active healers",
		description = "Highlight healers that are free to kill (not currently healing Jad).", section = JAD_HEALERS_SECTION)
	default boolean indicateActiveHealerJad()
	{
		return true;
	}

	// --- Zuk ---

	@ConfigItem(position = 0, keyName = "ticksOnNpcZuk", name = "Attack timer", description = "Draw ticks-until-attack above Zuk.", section = ZUK_SECTION)
	default boolean ticksOnNpcZuk()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "safespotsZukShieldBeforeHealers", name = "Shield safespot (before healers)",
		description = "Live or predicted safespot behind the moving Zuk shield, before the healer phase.", section = ZUK_SECTION)
	default ZukShieldDisplayMode safespotsZukShieldBeforeHealers()
	{
		return ZukShieldDisplayMode.PREDICT;
	}

	@ConfigItem(position = 2, keyName = "safespotsZukShieldAfterHealers", name = "Shield safespot (after healers)",
		description = "Live or predicted safespot behind the moving Zuk shield, after the healer phase.", section = ZUK_SECTION)
	default ZukShieldDisplayMode safespotsZukShieldAfterHealers()
	{
		return ZukShieldDisplayMode.LIVE;
	}

	@ConfigItem(position = 3, keyName = "ticksOnNpcZukShield", name = "Ticks on shield", description = "Draw ticks-until-attack on the floating shield.", section = ZUK_SECTION)
	default boolean ticksOnNpcZukShield()
	{
		return false;
	}

	// --- Zuk Healers ---

	@ConfigItem(position = 0, keyName = "indicateActiveHealerZuk", name = "Indicate active healers",
		description = "Highlight healers that are free to kill (not currently healing Zuk).", section = ZUK_HEALERS_SECTION)
	default boolean indicateActiveHealerZuk()
	{
		return true;
	}
}
