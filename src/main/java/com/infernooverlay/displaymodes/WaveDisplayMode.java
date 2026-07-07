package com.infernooverlay.displaymodes;

public enum WaveDisplayMode
{
	NONE("None"),
	CURRENT("Current wave"),
	NEXT("Next wave"),
	BOTH("Both");

	private final String name;

	WaveDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
