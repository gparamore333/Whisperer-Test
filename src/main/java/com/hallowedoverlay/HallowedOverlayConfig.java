package com.hallowedoverlay;

import java.awt.Color;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * Visual-only Hallowed Sepulchre overlays - no automation. Ported from OreoCupcakes'
 * kotori-plugins {@code hallowedhelper}, re-verified against current game data and stripped of
 * everything that wasn't a pure read-and-draw overlay (see the README for what was dropped and
 * why - mainly a fragile, hand-tuned "predict the whole room's fire pattern" system, replaced
 * here with live per-statue animation tracking).
 */
@ConfigGroup("hallowedoverlay")
public interface HallowedOverlayConfig extends Config
{
	@ConfigSection(name = "Fire traps", description = "Wizard statue fire-trap options", position = 0)
	String FIRE_SECTION = "fire";

	@ConfigSection(name = "Lightning", description = "Priest statue lightning options", position = 1)
	String LIGHTNING_SECTION = "lightning";

	@ConfigSection(name = "Swords", description = "Sword-trap options", position = 2)
	String SWORDS_SECTION = "swords";

	@ConfigSection(name = "Arrows", description = "Crossbow-trap options", position = 3)
	String ARROWS_SECTION = "arrows";

	@ConfigSection(name = "Chests", description = "Coffin options", position = 4)
	String CHESTS_SECTION = "chests";

	@ConfigSection(name = "Portals", description = "End-of-floor portal and bridge options", position = 5)
	String PORTALS_SECTION = "portals";

	@ConfigSection(name = "Teleporters", description = "Blue/yellow strange tile options", position = 6)
	String TELEPORTERS_SECTION = "teleporters";

	@ConfigSection(name = "Navigation", description = "Stairs, floor gates and server tile", position = 7)
	String NAVIGATION_SECTION = "navigation";

	@ConfigItem(position = 0, keyName = "showFireSafe", name = "Show safe tiles", description = "Highlights tiles a wizard statue's fire has just cleared.", section = FIRE_SECTION)
	default boolean showFireSafe()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "fireSafeColor", name = "Safe colour", description = "Colour for tiles safe from fire.", section = FIRE_SECTION)
	default Color fireSafeColor()
	{
		return new Color(0, 200, 0, 120);
	}

	@ConfigItem(position = 2, keyName = "showFireRisky", name = "Show risky tiles", description = "Highlights tiles 2 ticks from a wizard statue's fire.", section = FIRE_SECTION)
	default boolean showFireRisky()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 3, keyName = "fireRiskyColor", name = "Risky colour", description = "Colour for tiles about to become unsafe.", section = FIRE_SECTION)
	default Color fireRiskyColor()
	{
		return new Color(255, 200, 0, 120);
	}

	@ConfigItem(position = 4, keyName = "showFireUnsafe", name = "Show unsafe tiles", description = "Highlights tiles a wizard statue is about to fire on.", section = FIRE_SECTION)
	default boolean showFireUnsafe()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 5, keyName = "fireUnsafeColor", name = "Unsafe colour", description = "Colour for tiles about to be hit by fire.", section = FIRE_SECTION)
	default Color fireUnsafeColor()
	{
		return new Color(220, 30, 30, 130);
	}

	@ConfigItem(position = 6, keyName = "showFireTickCounter", name = "Show tick counter", description = "Shows a live countdown to each statue's next fire, read directly off its own animation.", section = FIRE_SECTION)
	default boolean showFireTickCounter()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "showLightningTiles", name = "Show lightning tiles", description = "Highlights tiles a priest statue's lightning has struck.", section = LIGHTNING_SECTION)
	default boolean showLightningTiles()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "showLightningCountdown", name = "Show lightning countdown", description = "Shows a countdown on lightning-struck tiles. The exact cycle length isn't wiki-published, so this is carried over from the legacy plugin's own observed timing rather than a confirmed number.", section = LIGHTNING_SECTION)
	default boolean showLightningCountdown()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "showSwordDanger", name = "Show sword danger tile", description = "Highlights the tile a thrown sword is currently on.", section = SWORDS_SECTION)
	default boolean showSwordDanger()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "swordDangerColor", name = "Sword danger colour", description = "Colour for the sword's current tile.", section = SWORDS_SECTION)
	default Color swordDangerColor()
	{
		return new Color(220, 30, 30, 130);
	}

	@ConfigItem(position = 2, keyName = "showSwordStatue", name = "Show throwing statue", description = "Outlines a sword-trap statue while it's mid-throw animation.", section = SWORDS_SECTION)
	default boolean showSwordStatue()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 3, keyName = "swordStatueColor", name = "Throwing statue colour", description = "Outline colour for a statue currently throwing.", section = SWORDS_SECTION)
	default Color swordStatueColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigItem(position = 0, keyName = "showArrows", name = "Show arrow danger tiles", description = "Highlights an arrow/bolt's current tile and its projected path.", section = ARROWS_SECTION)
	default boolean showArrows()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "arrowDangerColor", name = "Arrow tile colour", description = "Colour for the arrow's current tile.", section = ARROWS_SECTION)
	default Color arrowDangerColor()
	{
		return new Color(220, 30, 30, 130);
	}

	@Alpha
	@ConfigItem(position = 2, keyName = "arrowPathColor", name = "Arrow path colour", description = "Colour for tiles ahead of the arrow's travel direction.", section = ARROWS_SECTION)
	default Color arrowPathColor()
	{
		return new Color(255, 200, 0, 110);
	}

	@ConfigItem(position = 3, keyName = "showCrossbowStatue", name = "Show firing statue", description = "Outlines a crossbow-trap statue while it's mid-fire animation.", section = ARROWS_SECTION)
	default boolean showCrossbowStatue()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 4, keyName = "crossbowStatueColor", name = "Firing statue colour", description = "Outline colour for a statue currently firing.", section = ARROWS_SECTION)
	default Color crossbowStatueColor()
	{
		return new Color(220, 30, 30, 160);
	}

	@ConfigItem(position = 0, keyName = "showChests", name = "Show coffins", description = "Highlights coffins and their current open/opening/failed state.", section = CHESTS_SECTION)
	default boolean showChests()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "chestClosedColor", name = "Closed colour", description = "Colour for an unopened coffin.", section = CHESTS_SECTION)
	default Color chestClosedColor()
	{
		return new Color(200, 0, 200, 110);
	}

	@Alpha
	@ConfigItem(position = 2, keyName = "chestOpeningColor", name = "Opening colour", description = "Colour while you're picking the coffin's lock.", section = CHESTS_SECTION)
	default Color chestOpeningColor()
	{
		return new Color(0, 200, 200, 110);
	}

	@Alpha
	@ConfigItem(position = 3, keyName = "chestOpenColor", name = "Open colour", description = "Colour once the coffin is open.", section = CHESTS_SECTION)
	default Color chestOpenColor()
	{
		return new Color(0, 200, 0, 110);
	}

	@Alpha
	@ConfigItem(position = 4, keyName = "chestFailColor", name = "Poisoned colour", description = "Colour immediately after a failed lockpick (you'll be poisoned). The graphic this reads from isn't wiki-confirmed for this specific effect - carried over from the legacy plugin, verify in-game.", section = CHESTS_SECTION)
	default Color chestFailColor()
	{
		return new Color(255, 0, 0, 160);
	}

	@ConfigItem(position = 0, keyName = "showEndPortal", name = "Show floor-end portal", description = "Highlights the portal that opens the way to the next floor.", section = PORTALS_SECTION)
	default boolean showEndPortal()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "portalClosedColor", name = "Closed colour", description = "Colour before the portal has been conjured.", section = PORTALS_SECTION)
	default Color portalClosedColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(position = 2, keyName = "portalOpenColor", name = "Open colour", description = "Colour once the portal is ready to use.", section = PORTALS_SECTION)
	default Color portalOpenColor()
	{
		return new Color(0, 200, 0, 140);
	}

	@ConfigItem(position = 3, keyName = "showBridge", name = "Show bridge", description = "Highlights floor 1's bridge and whether it's been repaired.", section = PORTALS_SECTION)
	default boolean showBridge()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 4, keyName = "bridgeUnbuiltColor", name = "Unrepaired colour", description = "Colour before the bridge has been repaired.", section = PORTALS_SECTION)
	default Color bridgeUnbuiltColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(position = 5, keyName = "bridgeBuiltColor", name = "Repaired colour", description = "Colour once the bridge is safe to cross.", section = PORTALS_SECTION)
	default Color bridgeBuiltColor()
	{
		return new Color(0, 200, 0, 140);
	}

	@ConfigItem(position = 0, keyName = "showTeleporterTiles", name = "Highlight lit tiles", description = "Highlights currently-lit blue (forward) and yellow (backward) strange tiles. These light up randomly per the wiki, so this is reactive, not predictive.", section = TELEPORTERS_SECTION)
	default boolean showTeleporterTiles()
	{
		return true;
	}

	@ConfigItem(position = 1, keyName = "showTeleporterTimer", name = "Show despawn timer", description = "Shows a countdown to when a lit tile despawns.", section = TELEPORTERS_SECTION)
	default boolean showTeleporterTimer()
	{
		return true;
	}

	@ConfigItem(position = 0, keyName = "showStairs", name = "Show stairs", description = "Highlights the stairs/drop between floor sections.", section = NAVIGATION_SECTION)
	default boolean showStairs()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 1, keyName = "stairsColor", name = "Stairs colour", description = "Colour for stairs.", section = NAVIGATION_SECTION)
	default Color stairsColor()
	{
		return new Color(0, 200, 200, 110);
	}

	@ConfigItem(position = 2, keyName = "showFloorGates", name = "Show floor gates", description = "Highlights the barrier that closes once you commit to the next floor.", section = NAVIGATION_SECTION)
	default boolean showFloorGates()
	{
		return true;
	}

	@Alpha
	@ConfigItem(position = 3, keyName = "floorGateColor", name = "Floor gate colour", description = "Colour for floor gates.", section = NAVIGATION_SECTION)
	default Color floorGateColor()
	{
		return new Color(0, 200, 200, 110);
	}

	@ConfigItem(position = 4, keyName = "showServerTile", name = "Show server tile", description = "Outlines your own server-authoritative tile - useful for judging exact positioning against fire/sword tiles.", section = NAVIGATION_SECTION)
	default boolean showServerTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(position = 5, keyName = "serverTileColor", name = "Server tile colour", description = "Outline colour for your own tile.", section = NAVIGATION_SECTION)
	default Color serverTileColor()
	{
		return Color.CYAN;
	}
}
