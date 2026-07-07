package com.infernooverlay.displaymodes;

public enum ZukShieldDisplayMode
{
	OFF("Off"),
	LIVE("Live (follow shield)"),
	PREDICT("Predict"),
	LIVE_PLUS_PREDICT("Live and predict");

	private final String name;

	ZukShieldDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
