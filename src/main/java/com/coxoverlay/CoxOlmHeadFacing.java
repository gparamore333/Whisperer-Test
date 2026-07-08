package com.coxoverlay;

/**
 * Which side of the room the Great Olm head is currently oriented towards, read live off
 * {@code NPC#getOrientation()} rather than the fixed room-quadrant rules described on the
 * wiki (those rules disagree between the "Chambers of Xeric/Strategies" and
 * "Perfect Olm (Solo)" pages on which numbered safespots turn the head left vs. right, so
 * this plugin sidesteps that dispute entirely by reading the head's real facing angle each
 * tick instead of hardcoding either page's claim).
 */
enum CoxOlmHeadFacing
{
	UNKNOWN,
	LEFT,
	MIDDLE,
	RIGHT
}
