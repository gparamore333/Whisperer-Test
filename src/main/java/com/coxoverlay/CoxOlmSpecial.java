package com.coxoverlay;

/**
 * Great Olm's special-attack rotation, confirmed on the OSRS Wiki as a fixed, repeating cycle
 * throughout every non-head phase: standard, Crystal Burst, standard, (skip), standard,
 * Lightning, standard, (skip), standard, Teleport, standard, (skip), loop. Always exactly two
 * standard attacks between specials. This is the rotation the "force a head turn to deny the
 * special" kiting technique exploits - see {@link CoxOverlayPlugin#registerOlmStandardAttack()}.
 */
enum CoxOlmSpecial
{
	CRYSTAL_BURST("Crystal Burst"),
	LIGHTNING("Lightning"),
	TELEPORT("Teleport");

	private final String label;

	CoxOlmSpecial(String label)
	{
		this.label = label;
	}

	CoxOlmSpecial next()
	{
		CoxOlmSpecial[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	@Override
	public String toString()
	{
		return label;
	}
}
