package com.infernooverlay.displaymodes;

public enum SafespotDisplayMode
{
	OFF("Off"),
	INDIVIDUAL_TILES("Individual tiles"),
	AREA("Area (lower fps)");

	private final String name;

	SafespotDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
