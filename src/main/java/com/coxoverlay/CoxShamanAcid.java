package com.coxoverlay;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;

/**
 * A lizardman shaman's acid spit in flight. Ticks-until-impact is read live off the
 * projectile's own remaining animation cycles - the game's real, server-driven timing, not a
 * guessed number.
 */
@Getter(AccessLevel.PACKAGE)
class CoxShamanAcid
{
	private final Projectile projectile;
	private final WorldPoint targetPoint;

	CoxShamanAcid(Projectile projectile, WorldPoint targetPoint)
	{
		this.projectile = projectile;
		this.targetPoint = targetPoint;
	}

	int ticksUntilImpact()
	{
		return (int) Math.ceil(projectile.getRemainingCycles() / 30.0);
	}

	boolean hasLanded()
	{
		return projectile.getRemainingCycles() <= 0;
	}
}
