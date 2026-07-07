package com.whispereroverlay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("whispereroverlay")
public interface WhispererOverlayConfig extends Config
{
	@ConfigItem(
		keyName = "showUnsafeTiles",
		name = "Show tentacle danger tiles",
		description = "Highlights tiles about to be hit by the spinning tentacle attack, with a tick countdown. You still choose where to move.",
		position = 1
	)
	default boolean showUnsafeTiles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerIndicator",
		name = "Show incoming attack prayer",
		description = "Shows which protection prayer matches the next incoming projectile. You still click the prayer yourself.",
		position = 2
	)
	default boolean showPrayerIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLeechAndVitaTracker",
		name = "Show leech & Vita tracker",
		description = "Marks leech spawn tiles and highlights 'Vita' add NPCs.",
		position = 3
	)
	default boolean showLeechAndVitaTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPillarRanking",
		name = "Show pillar health ranking",
		description = "Labels the three energy pillars 1/2/3 and color-codes them by remaining health.",
		position = 4
	)
	default boolean showPillarRanking()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBindTimer",
		name = "Show bind timer",
		description = "Shows a countdown over your player when you're bound in place.",
		position = 5
	)
	default boolean showBindTimer()
	{
		return true;
	}
}
