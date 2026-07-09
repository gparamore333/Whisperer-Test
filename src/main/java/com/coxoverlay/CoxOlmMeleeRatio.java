package com.coxoverlay;

/**
 * Solo melee-hand kiting ratios, per the OSRS Wiki's "Chambers of Xeric/Strategies" Great Olm
 * section: "X:Y means the player attacks X times and is hit by Olm Y times in those X attacks
 * before the cycle repeats." Which ratio applies depends on the melee weapon's attack speed.
 */
public enum CoxOlmMeleeRatio
{
	FOUR_ONE("4:1 (4-tick weapon, e.g. dragon hunter lance)", 4),
	THREE_ONE("3:1 (5-tick weapon, e.g. scythe of vitur)", 3);

	private final String label;
	private final int attacks;

	CoxOlmMeleeRatio(String label, int attacks)
	{
		this.label = label;
		this.attacks = attacks;
	}

	int getAttacks()
	{
		return attacks;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
