package com.coxoverlay;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

/**
 * A single Chambers of Xeric NPC currently on screen, paired with its {@link CoxNpcInfo}.
 * Resolves hybrid monsters (lizardman shamans, the small Muttadile) that switch between
 * melee and ranged based on whether they're adjacent to the player - a standard, well
 * documented OSRS NPC behavior pattern, not a guess specific to this fight.
 */
@RequiredArgsConstructor
class CoxTrackedNpc
{
	@Getter(AccessLevel.PACKAGE)
	private final NPC npc;
	@Getter(AccessLevel.PACKAGE)
	private final CoxNpcInfo info;

	Prayer resolvePrayer(WorldPoint playerLocation)
	{
		if (info.getPrayer() != null)
		{
			return info.getPrayer();
		}

		switch (info)
		{
			case SHAMAN_A:
			case SHAMAN_B:
			case MUTTADILE_JUNIOR:
				boolean adjacent = npc.getWorldArea().isInMeleeDistance(playerLocation);
				return adjacent ? Prayer.PROTECT_FROM_MELEE : Prayer.PROTECT_FROM_MISSILES;
			default:
				return null;
		}
	}
}
