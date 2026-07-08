package com.coxoverlay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * No auto-pray or other action-taking options exist here by design: this plugin only reads
 * game state (NPC IDs/positions, equipment) and draws indicators. Prayers are always the
 * player's own click.
 */
@ConfigGroup("coxoverlay")
public interface CoxOverlayConfig extends Config
{
	@ConfigSection(name = "General", description = "General options", position = 0)
	String GENERAL_SECTION = "general";

	@ConfigSection(name = "Rooms", description = "Per-room toggles for the state/prayer overlay", position = 1)
	String ROOMS_SECTION = "rooms";

	@ConfigSection(name = "Mystics", description = "Mystics room options", position = 2)
	String MYSTICS_SECTION = "mystics";

	@ConfigItem(position = 0, keyName = "enablePrayerOverlay", name = "Enable prayer overlay",
		description = "Master toggle for the prayer-tab icon indicator.", section = GENERAL_SECTION)
	default boolean enablePrayerOverlay()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "enableStateLabels", name = "Enable NPC state labels",
		description = "Draws a short status label above tracked NPCs (e.g. \"Walking back to anvil\", \"THAWED\").", section = GENERAL_SECTION)
	default boolean enableStateLabels()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "roomTekton", name = "Tekton", description = "Enable the overlay in the Tekton room.", section = ROOMS_SECTION)
	default boolean roomTekton()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "roomVanguards", name = "Vanguards", description = "Enable the overlay in the Vanguards room.", section = ROOMS_SECTION)
	default boolean roomVanguards()
	{
		return true;
	}

	@ConfigItem(position = 2, keyName = "roomVespula", name = "Vespula", description = "Enable the overlay in the Vespula room.", section = ROOMS_SECTION)
	default boolean roomVespula()
	{
		return true;
	}

	@ConfigItem(position = 3, keyName = "roomMuttadiles", name = "Muttadiles", description = "Enable the overlay in the Muttadiles room.", section = ROOMS_SECTION)
	default boolean roomMuttadiles()
	{
		return true;
	}

	@ConfigItem(position = 4, keyName = "roomVasa", name = "Vasa", description = "Enable the overlay in the Vasa room.", section = ROOMS_SECTION)
	default boolean roomVasa()
	{
		return true;
	}

	@ConfigItem(position = 5, keyName = "roomGuardians", name = "Guardians", description = "Enable the overlay in the Guardians room.", section = ROOMS_SECTION)
	default boolean roomGuardians()
	{
		return true;
	}

	@ConfigItem(position = 6, keyName = "roomShamans", name = "Shamans", description = "Enable the overlay in the Shamans room.", section = ROOMS_SECTION)
	default boolean roomShamans()
	{
		return true;
	}

	@ConfigItem(position = 7, keyName = "roomCrabs", name = "Crabs", description = "Enable the overlay in the Crabs room.", section = ROOMS_SECTION)
	default boolean roomCrabs()
	{
		return true;
	}

	@ConfigItem(position = 8, keyName = "roomIceDemon", name = "Ice Demon", description = "Enable the overlay in the Ice Demon room.", section = ROOMS_SECTION)
	default boolean roomIceDemon()
	{
		return true;
	}

	@ConfigItem(position = 9, keyName = "roomMystics", name = "Mystics", description = "Enable the overlay in the Mystics room.", section = ROOMS_SECTION)
	default boolean roomMystics()
	{
		return true;
	}

	@ConfigItem(position = 10, keyName = "roomTightrope", name = "Tightrope", description = "Enable the overlay in the Tightrope room.", section = ROOMS_SECTION)
	default boolean roomTightrope()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "salveReminder", name = "Salve amulet reminder",
		description = "Warns if you enter the Mystics room without a Salve amulet variant equipped (Skeletal Mystics are undead).", section = MYSTICS_SECTION)
	default boolean salveReminder()
	{
		return true;
	}
}
