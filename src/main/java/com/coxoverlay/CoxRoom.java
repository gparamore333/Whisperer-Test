package com.coxoverlay;

enum CoxRoom
{
	TEKTON("Tekton"),
	VANGUARDS("Vanguards"),
	VESPULA("Vespula"),
	MUTTADILES("Muttadiles"),
	VASA("Vasa"),
	GUARDIANS("Guardians"),
	SHAMANS("Shamans"),
	CRABS("Crabs"),
	ICE_DEMON("Ice Demon"),
	MYSTICS("Mystics"),
	TIGHTROPE("Tightrope");

	private final String displayName;

	CoxRoom(String displayName)
	{
		this.displayName = displayName;
	}

	String getDisplayName()
	{
		return displayName;
	}
}
