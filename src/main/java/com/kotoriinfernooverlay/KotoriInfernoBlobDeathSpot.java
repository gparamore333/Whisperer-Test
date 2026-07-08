package com.kotoriinfernooverlay;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.coords.LocalPoint;

class KotoriInfernoBlobDeathSpot
{
	static final int BLOB_DEATH_TICKS = 3;
	static final int BLOB_DEATH_ANIMATION = 7584;
	private static final int FILL_START_ALPHA = 255;

	@Getter(AccessLevel.PACKAGE)
	private final LocalPoint location;

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilDone;

	private final long deathTime;

	KotoriInfernoBlobDeathSpot(LocalPoint location)
	{
		this.location = location;
		this.ticksUntilDone = BLOB_DEATH_TICKS;
		this.deathTime = System.currentTimeMillis();
	}

	void decrementTick()
	{
		if (ticksUntilDone > 0)
		{
			ticksUntilDone -= 1;
		}
	}

	boolean isDone()
	{
		return ticksUntilDone == 0;
	}

	private double fillProgress()
	{
		return (System.currentTimeMillis() - deathTime) / ((BLOB_DEATH_TICKS - 1) * 600.0);
	}

	int fillAlpha()
	{
		return Math.min(Math.max((int) ((1 - fillProgress()) * FILL_START_ALPHA), 0), 255);
	}
}
