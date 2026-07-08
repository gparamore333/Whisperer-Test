package com.coxoverlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * Visual-only overlays for Chambers of Xeric. This reads NPC state (via RuneLite's own
 * gameval NPC IDs, which change as a monster's fight phase changes) and equipment - never a
 * click, key press, or movement. Prayers are always the player's own action; there is no
 * auto-pray here. Covers the 11 combat/puzzle rooms with live state labels and, where a
 * monster's attack style is unambiguous, a prayer-tab reminder. The Great Olm fight and
 * precise puzzle-solving (Crabs' target colours, exact bomb/lightning timing) are out of
 * scope - see the README for details.
 */
@PluginDescriptor(
	name = "CoX Overlay",
	description = "Visual-only Chambers of Xeric room overlays - no automation, no auto-pray",
	tags = {"raids", "cox", "chambers of xeric", "overlay", "pvm"},
	enabledByDefault = false
)
public class CoxOverlayPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private CoxOverlay overlay;

	@Inject
	private CoxOlmOverlay olmOverlay;

	@Inject
	private CoxOverlayConfig config;

	// Great Olm's lightning-trail spot animation. Its current gameval name (SpotanimID 1356)
	// doesn't mention lightning at all - Jagex's internal cache names frequently don't match
	// what the effect actually looks like in game - but this exact numeric ID is unchanged
	// from when it was verified and named OLM_LIGHTNING in RuneLite's own (now-deprecated)
	// GraphicID table, and gameval IDs are never recycled for a different effect.
	private static final int OLM_LIGHTNING_SPOTANIM_ID = 1356;

	@Getter(AccessLevel.PACKAGE)
	private final List<CoxTrackedNpc> trackedNpcs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private Prayer closestPrayer;

	@Getter(AccessLevel.PACKAGE)
	private boolean showSalveReminder;

	@Getter(AccessLevel.PACKAGE)
	private final Set<CoxOlmBomb> olmBombs = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> olmAcidPools = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> olmCrystalMarkers = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> olmLightningTrail = new ArrayList<>();

	@Provides
	CoxOverlayConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (isInChambers())
		{
			overlayManager.add(overlay);
			overlayManager.add(olmOverlay);
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(olmOverlay);
		resetState();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isInChambers())
		{
			overlayManager.remove(overlay);
			overlayManager.remove(olmOverlay);
			resetState();
			return;
		}

		if (!overlayManager.anyMatch(o -> o == overlay))
		{
			overlayManager.add(overlay);
			overlayManager.add(olmOverlay);
		}

		trackedNpcs.removeIf(tracked -> tracked.getNpc().isDead());

		closestPrayer = calculateClosestPrayer();
		showSalveReminder = calculateShowSalveReminder();

		olmLightningTrail.clear();
		if (config.olmEnable() && config.olmLightningWarning())
		{
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				if (graphicsObject.getId() == OLM_LIGHTNING_SPOTANIM_ID)
				{
					olmLightningTrail.add(WorldPoint.fromLocal(client, graphicsObject.getLocation()));
				}
			}
		}
	}

	private void resetState()
	{
		trackedNpcs.clear();
		closestPrayer = null;
		showSalveReminder = false;
		olmBombs.clear();
		olmAcidPools.clear();
		olmCrystalMarkers.clear();
		olmLightningTrail.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isInChambers())
		{
			return;
		}

		NPC npc = event.getNpc();
		CoxNpcInfo info = CoxNpcInfo.fromId(npc.getId());
		if (info != null)
		{
			trackedNpcs.add(new CoxTrackedNpc(npc, info));
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		trackedNpcs.removeIf(tracked -> tracked.getNpc() == event.getNpc());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!isInChambers() || !config.olmEnable())
		{
			return;
		}

		GameObject gameObject = event.getGameObject();
		switch (gameObject.getId())
		{
			case ObjectID.OLM_CRYSTAL_BOMB:
				olmBombs.add(new CoxOlmBomb(gameObject, client.getTickCount()));
				break;
			case ObjectID.OLM_ACID_POOL:
				olmAcidPools.add(gameObject);
				break;
			case ObjectID.OLM_CRYSTAL_ATTACK_SMALL:
			case ObjectID.OLM_CRYSTAL_ATTACK_LARGE:
				olmCrystalMarkers.add(gameObject);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		olmBombs.removeIf(bomb -> bomb.getGameObject() == gameObject);
		olmAcidPools.remove(gameObject);
		olmCrystalMarkers.remove(gameObject);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.olmEnable() || !config.olmRemoveHeadAttack() || !"Attack".equals(event.getOption()))
		{
			return;
		}

		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null || npc.getId() != NpcID.OLM_HEAD)
		{
			return;
		}

		boolean handAlive = trackedNpcs.stream().anyMatch(tracked ->
			tracked.getInfo() == CoxNpcInfo.OLM_HAND_LEFT || tracked.getInfo() == CoxNpcInfo.OLM_HAND_RIGHT);

		if (handAlive)
		{
			client.getMenu().removeMenuEntry(event.getMenuEntry());
		}
	}

	private boolean isInChambers()
	{
		return client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
	}

	boolean isRoomEnabled(CoxRoom room)
	{
		switch (room)
		{
			case TEKTON:
				return config.roomTekton();
			case VANGUARDS:
				return config.roomVanguards();
			case VESPULA:
				return config.roomVespula();
			case MUTTADILES:
				return config.roomMuttadiles();
			case VASA:
				return config.roomVasa();
			case GUARDIANS:
				return config.roomGuardians();
			case SHAMANS:
				return config.roomShamans();
			case CRABS:
				return config.roomCrabs();
			case ICE_DEMON:
				return config.roomIceDemon();
			case MYSTICS:
				return config.roomMystics();
			case TIGHTROPE:
				return config.roomTightrope();
			case OLM:
				return config.olmEnable();
			default:
				return false;
		}
	}

	private Prayer calculateClosestPrayer()
	{
		if (!config.enablePrayerOverlay() || client.getLocalPlayer() == null)
		{
			return null;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		CoxTrackedNpc closest = null;
		int closestDistance = Integer.MAX_VALUE;

		for (CoxTrackedNpc tracked : trackedNpcs)
		{
			if (!isRoomEnabled(tracked.getInfo().getRoom()))
			{
				continue;
			}

			Prayer prayer = tracked.resolvePrayer(playerLocation);
			if (prayer == null)
			{
				continue;
			}

			int distance = tracked.getNpc().getWorldLocation().distanceTo(playerLocation);
			if (distance < closestDistance)
			{
				closestDistance = distance;
				closest = tracked;
			}
		}

		return closest == null ? null : closest.resolvePrayer(playerLocation);
	}

	private boolean calculateShowSalveReminder()
	{
		if (!config.salveReminder() || !isRoomEnabled(CoxRoom.MYSTICS))
		{
			return false;
		}

		boolean mysticsPresent = trackedNpcs.stream().anyMatch(tracked -> tracked.getInfo().getRoom() == CoxRoom.MYSTICS);
		if (!mysticsPresent)
		{
			return false;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return false;
		}

		Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		if (amulet == null)
		{
			return true;
		}

		String name = itemManager.getItemComposition(amulet.getId()).getName();
		return name == null || !name.toLowerCase().startsWith("salve amulet");
	}
}
