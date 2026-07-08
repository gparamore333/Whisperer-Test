package com.coxoverlay;

import net.runelite.api.coords.WorldPoint;

/**
 * The 8 numbered Great Olm safespot tiles from the OSRS Wiki's "Great Olm safespots.png" -
 * the same image and the same 8 tiles both the "Chambers of Xeric/Strategies" and
 * "Perfect Olm (Solo)" wiki pages use, so these are the actual tiles the community calls by
 * number. Coordinates are template-map (regionId 12889) coordinates as published in both
 * pages' tile-marker data; {@link CoxOverlayPlugin} resolves them to live, instance-mapped
 * positions each tick via {@code WorldPoint#toLocalInstance}.
 * <p>
 * Deliberately does NOT encode either page's claim about which tiles turn the head left vs.
 * right - those two pages contradict each other on that point. Which tiles are currently safe
 * is instead computed live from the head's real orientation - see
 * {@link CoxOverlayPlugin#updateOlmSafespots()}.
 */
enum CoxOlmSafespot
{
	SPOT_1(28, 38),
	SPOT_2(28, 43),
	SPOT_3(28, 46),
	SPOT_4(28, 50),
	SPOT_5(37, 38),
	SPOT_6(37, 42),
	SPOT_7(37, 45),
	SPOT_8(37, 50);

	private static final int OLM_TEMPLATE_REGION_ID = 12889;

	private final int regionX;
	private final int regionY;

	CoxOlmSafespot(int regionX, int regionY)
	{
		this.regionX = regionX;
		this.regionY = regionY;
	}

	WorldPoint toTemplatePoint()
	{
		return WorldPoint.fromRegion(OLM_TEMPLATE_REGION_ID, regionX, regionY, 0);
	}

	int getNumber()
	{
		return ordinal() + 1;
	}
}
