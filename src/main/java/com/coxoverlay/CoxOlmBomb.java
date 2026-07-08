package com.coxoverlay;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

/**
 * A Great Olm crystal bomb. Detonates a fixed 8 ticks after it spawns (confirmed, stable
 * mechanic - not a guess).
 */
@Getter(AccessLevel.PACKAGE)
class CoxOlmBomb
{
	static final int TICKS_TO_DETONATE = 8;

	private final GameObject gameObject;
	private final WorldPoint location;
	private final int spawnTick;

	CoxOlmBomb(GameObject gameObject, int spawnTick)
	{
		this.gameObject = gameObject;
		this.location = gameObject.getWorldLocation();
		this.spawnTick = spawnTick;
	}

	int ticksUntilDetonation(int currentTick)
	{
		return Math.max(0, TICKS_TO_DETONATE - (currentTick - spawnTick));
	}
}
