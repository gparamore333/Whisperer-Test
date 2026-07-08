package com.coxoverlay;

/**
 * Solo mage-hand kiting ratios, per the OSRS Wiki's "Chambers of Xeric/Strategies" Great Olm
 * section. The advanced Tumeken's Shadow options (12:0/8:1, position-dependent on which side
 * of the room Olm is on) are deliberately left out - the wiki itself flags them as advanced
 * and situational, not something to hardcode a fixed cadence for.
 */
enum CoxOlmMageRatio
{
	THREE_ZERO("3:0 (4-tick powered staff)", 3),
	TWO_ZERO("2:0 (Tumeken's Shadow, simple method)", 2);

	private final String label;
	private final int attacks;

	CoxOlmMageRatio(String label, int attacks)
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
