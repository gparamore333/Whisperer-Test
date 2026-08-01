package com.hallowedoverlay;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GraphicsObject;

/**
 * A lit blue/yellow "strange tile" teleport pad. The wiki confirms these light up randomly
 * (unlike the wizard-statue fire pattern), so there's nothing to predict here - this just
 * tracks how long a currently-lit pad has left before it despawns, read live from when it
 * first appeared.
 */
@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class HallowedTeleportPortal
{
	@NonNull
	private final GraphicsObject graphicsObject;

	// The exact despawn window isn't wiki-published ("a few seconds" is all the wiki gives) -
	// this starting value is carried over from the legacy plugin's own observed timing.
	private int ticksUntilDespawn = 5;

	void decrementTicksUntilDespawn()
	{
		ticksUntilDespawn--;
	}
}
