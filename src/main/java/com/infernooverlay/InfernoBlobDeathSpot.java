package com.infernooverlay;

import net.runelite.api.coords.LocalPoint;

/**
 * Tracks the fading tile marker shown where a blob died, until its mini-blobs spawn.
 */
class InfernoBlobDeathSpot
{
	static final int BLOB_DEATH_ANIMATION = 7584;
	private static final int DEATH_TICKS = 3;

	private final LocalPoint location;
	private int ticksUntilDone;

	InfernoBlobDeathSpot(LocalPoint location)
	{
		this.location = location;
		this.ticksUntilDone = DEATH_TICKS;
	}

	LocalPoint getLocation()
	{
		return location;
	}

	int getTicksUntilDone()
	{
		return ticksUntilDone;
	}

	void decrementTick()
	{
		if (ticksUntilDone > 0)
		{
			ticksUntilDone--;
		}
	}

	boolean isDone()
	{
		return ticksUntilDone <= 0;
	}

	int fillAlpha()
	{
		double progress = 1.0 - (ticksUntilDone / (double) DEATH_TICKS);
		return (int) Math.max(0, 255 - (255 * progress));
	}
}
