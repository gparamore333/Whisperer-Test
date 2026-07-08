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
 * Per-tile visibility is the "Chambers of Xeric/Strategies" page's own discrete table (the
 * head only ever faces one of 3 states - LEFT/MIDDLE/RIGHT, not a smooth cone):
 * <ul>
 *     <li>Spots 1 and 8: visible only when the head is LEFT.</li>
 *     <li>Spots 4 and 5: visible only when the head is RIGHT.</li>
 *     <li>Spots 2 and 7: visible when LEFT or MIDDLE (hidden only when RIGHT).</li>
 *     <li>Spots 3 and 6: visible when RIGHT or MIDDLE (hidden only when LEFT).</li>
 * </ul>
 * Note spots 1 and 8 sit in different room columns yet share a state, and likewise 4/5 - this
 * is a real, wiki-documented mechanic (the head's line of sight is bounded by two lines from
 * near its own position, not by a simple "which column" split), not a typo carried over here.
 * <p>
 * What this class does NOT resolve: whether a live orientation reading near the room's west
 * side actually corresponds to this table's "LEFT" or its "RIGHT" - the two wiki pages
 * contradict each other on that exact point, and this plugin can't independently verify it
 * without live testing. See {@link CoxOverlayConfig#olmKiteSwapLeftRight()}.
 */
enum CoxOlmSafespot
{
	SPOT_1(28, 38, CoxOlmHeadFacing.LEFT),
	SPOT_2(28, 43, CoxOlmHeadFacing.LEFT, CoxOlmHeadFacing.MIDDLE),
	SPOT_3(28, 46, CoxOlmHeadFacing.RIGHT, CoxOlmHeadFacing.MIDDLE),
	SPOT_4(28, 50, CoxOlmHeadFacing.RIGHT),
	SPOT_5(37, 38, CoxOlmHeadFacing.RIGHT),
	SPOT_6(37, 42, CoxOlmHeadFacing.RIGHT, CoxOlmHeadFacing.MIDDLE),
	SPOT_7(37, 45, CoxOlmHeadFacing.LEFT, CoxOlmHeadFacing.MIDDLE),
	SPOT_8(37, 50, CoxOlmHeadFacing.LEFT);

	private static final int OLM_TEMPLATE_REGION_ID = 12889;

	private final int regionX;
	private final int regionY;
	private final CoxOlmHeadFacing[] visibleWhen;

	CoxOlmSafespot(int regionX, int regionY, CoxOlmHeadFacing... visibleWhen)
	{
		this.regionX = regionX;
		this.regionY = regionY;
		this.visibleWhen = visibleWhen;
	}

	WorldPoint toTemplatePoint()
	{
		return WorldPoint.fromRegion(OLM_TEMPLATE_REGION_ID, regionX, regionY, 0);
	}

	int getNumber()
	{
		return ordinal() + 1;
	}

	/** Whether this tile is inside the head's facing cone for the given (possibly swapped) state. */
	boolean isVisibleWhen(CoxOlmHeadFacing state)
	{
		for (CoxOlmHeadFacing candidate : visibleWhen)
		{
			if (candidate == state)
			{
				return true;
			}
		}
		return false;
	}
}
