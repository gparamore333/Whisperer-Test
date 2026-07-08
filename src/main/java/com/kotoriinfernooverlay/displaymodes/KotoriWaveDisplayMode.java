package com.kotoriinfernooverlay.displaymodes;

public enum KotoriWaveDisplayMode
{
	CURRENT("Current wave"),
	NEXT("Next wave"),
	BOTH("Both"),
	NONE("None");

	private final String name;

	KotoriWaveDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
