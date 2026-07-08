package com.kotoriinfernooverlay.displaymodes;

public enum KotoriSafespotDisplayMode
{
	OFF("Off"),
	INDIVIDUAL_TILES("Individual tiles"),
	AREA("Area (lower fps)");

	private final String name;

	KotoriSafespotDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
