package com.coxoverlay;

enum CoxOlmPhase
{
	ACID("Acid phase"),
	CRYSTAL("Crystal phase"),
	FLAME("Flame phase"),
	FINAL_STAND("Final stand!");

	private final String label;

	CoxOlmPhase(String label)
	{
		this.label = label;
	}

	String getLabel()
	{
		return label;
	}
}
