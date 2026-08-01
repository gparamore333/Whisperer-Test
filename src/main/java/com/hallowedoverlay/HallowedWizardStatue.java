package com.hallowedoverlay;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.gameval.AnimationID;

/**
 * Tracks one Hallowed Sepulchre fire-trap (wizard statue) by reading its own live renderable
 * animation each tick - the same "watch the real animation instead of guessing a timer" pattern
 * used elsewhere in this project (e.g. Inferno's meleer dig timer). The fire-cycle length (2 or
 * 3 ticks) is a fixed, known property of which statue variant this is - T3 statues cycle 1 tick
 * faster than the rest, per the OSRS Wiki's own line about floor 5 ("Flame statues on this floor
 * change phases 1 tick faster than on floors 1-4", where the T3 variants live) - rather than
 * something this class has to empirically derive at runtime.
 */
@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class HallowedWizardStatue
{
	@NonNull
	private final GameObject gameObject;

	private final int fireCycleTicks;

	// Ticks elapsed since this statue was last observed firing, wrapping at fireCycleTicks - not
	// exposed directly; see getTicksUntilFire() for the countdown this actually represents.
	private int ticksSinceFire = -1;

	void updateTick()
	{
		if (getAnimation() == AnimationID.HALLOWED_STATUE_FIRE_ATTACK)
		{
			// Fire and damage are synchronised (confirmed by a 2020 patch note), so this tick
			// IS the dangerous one - ticksSinceFire = 0 makes getTicksUntilFire() read 0 (now).
			ticksSinceFire = 0;
			return;
		}

		if (ticksSinceFire >= 0)
		{
			ticksSinceFire = (ticksSinceFire + 1) % fireCycleTicks;
		}
	}

	boolean isKnown()
	{
		return ticksSinceFire >= 0;
	}

	/** 0 = firing right now, counting up to fireCycleTicks-1 as the next fire approaches from a "full" state. */
	int getTicksUntilFire()
	{
		return (fireCycleTicks - ticksSinceFire) % fireCycleTicks;
	}

	private int getAnimation()
	{
		if (!(gameObject.getRenderable() instanceof DynamicObject))
		{
			return -1;
		}

		DynamicObject dynamicObject = (DynamicObject) gameObject.getRenderable();
		return dynamicObject.getAnimation() == null ? -1 : dynamicObject.getAnimation().getId();
	}
}
