package com.kotoriinfernooverlay.displaymodes;

public enum KotoriZukShieldDisplayMode
{
	OFF("Off"),
	LIVE("Live (follow shield)"),
	PREDICT("Predict"),
	LIVE_PLUS_PREDICT("Live and Predict");

	private final String name;

	KotoriZukShieldDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
