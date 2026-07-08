package com.coxoverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

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

	private static final Pattern OLM_TELEPORT_PAIR_PATTERN =
		Pattern.compile("You have been paired with <col=ff0000>(.*?)</col>!");

	// These are all display-duration windows for momentary, message/projectile-triggered
	// events (how long to keep showing a warning after it fires), not precise mechanic
	// timers - none of them claim to predict anything in advance.
	private static final int SPHERE_PRAYER_DISPLAY_TICKS = 5;
	private static final int HEAD_PRAYER_DISPLAY_TICKS = 3;
	private static final int PHASE_BANNER_DISPLAY_TICKS = 8;
	// The exact clench-resistance duration isn't wiki-confirmed; this is an approximate
	// ~30-second window sourced from community strategy guides, not a precise number.
	private static final int HAND_CLENCH_DISPLAY_TICKS = 50;
	private static final int ACID_TARGET_DISPLAY_TICKS = 10;
	private static final int TELEPORT_TARGET_DISPLAY_TICKS = 10;

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

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> olmHealBeamTiles = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<Player> olmBurnVictims = new HashSet<>();

	private final Map<Player, Integer> olmTeleportTargets = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> olmTeleportDestinations = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private CoxOlmPhase olmPhase;
	private int olmPhaseBannerTicks;

	private Prayer olmSpherePrayer;
	private int olmSpherePrayerTicks;

	private Prayer olmHeadPrayer;
	private int olmHeadPrayerTicks;

	@Getter(AccessLevel.PACKAGE)
	private boolean olmHandClenched;
	private int olmHandClenchTicks;

	@Getter(AccessLevel.PACKAGE)
	private Player olmAcidTarget;
	private int olmAcidTargetTicks;

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
		olmHealBeamTiles.clear();
		olmTeleportDestinations.clear();
		if (config.olmEnable())
		{
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				int id = graphicsObject.getId();
				if (config.olmLightningWarning() && id == OLM_LIGHTNING_SPOTANIM_ID)
				{
					olmLightningTrail.add(WorldPoint.fromLocal(client, graphicsObject.getLocation()));
				}
				else if (config.olmHealBeamWarning() && id == SpotanimID.OLM_HEALME_SPOTANIM)
				{
					olmHealBeamTiles.add(WorldPoint.fromLocal(client, graphicsObject.getLocation()));
				}
				else if (config.olmTeleportWarning() && isOlmPlayerSwapSpotanim(id))
				{
					olmTeleportDestinations.add(WorldPoint.fromLocal(client, graphicsObject.getLocation()));
				}
			}
		}

		olmBurnVictims.clear();
		if (config.olmEnable() && config.olmBurnVictimWarning())
		{
			for (Player player : client.getTopLevelWorldView().players())
			{
				if (player != null && player.hasSpotAnim(SpotanimID.OLM_BURNWITHME_PL_SPOT))
				{
					olmBurnVictims.add(player);
				}
			}
		}

		olmPhaseBannerTicks = Math.max(0, olmPhaseBannerTicks - 1);
		olmSpherePrayerTicks = Math.max(0, olmSpherePrayerTicks - 1);
		olmHeadPrayerTicks = Math.max(0, olmHeadPrayerTicks - 1);
		olmAcidTargetTicks = Math.max(0, olmAcidTargetTicks - 1);
		olmHandClenchTicks = Math.max(0, olmHandClenchTicks - 1);
		olmHandClenched = olmHandClenchTicks > 0;
		if (olmAcidTargetTicks == 0)
		{
			olmAcidTarget = null;
		}

		olmTeleportTargets.replaceAll((player, ticks) -> ticks - 1);
		olmTeleportTargets.values().removeIf(ticks -> ticks <= 0);
	}

	private static boolean isOlmPlayerSwapSpotanim(int id)
	{
		return id == SpotanimID.OLM_PLAYERSWAP_0 || id == SpotanimID.OLM_PLAYERSWAP_1
			|| id == SpotanimID.OLM_PLAYERSWAP_2 || id == SpotanimID.OLM_PLAYERSWAP_3;
	}

	Set<Player> getOlmTeleportTargets()
	{
		return olmTeleportTargets.keySet();
	}

	Prayer getOlmSpherePrayer()
	{
		return olmSpherePrayerTicks > 0 ? olmSpherePrayer : null;
	}

	Prayer getOlmHeadPrayer()
	{
		return olmHeadPrayerTicks > 0 ? olmHeadPrayer : null;
	}

	boolean isOlmPhaseBannerActive()
	{
		return olmPhaseBannerTicks > 0;
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
		olmHealBeamTiles.clear();
		olmTeleportDestinations.clear();
		olmTeleportTargets.clear();
		olmBurnVictims.clear();
		olmPhase = null;
		olmPhaseBannerTicks = 0;
		olmSpherePrayer = null;
		olmSpherePrayerTicks = 0;
		olmHeadPrayer = null;
		olmHeadPrayerTicks = 0;
		olmHandClenched = false;
		olmHandClenchTicks = 0;
		olmAcidTarget = null;
		olmAcidTargetTicks = 0;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isInChambers() || !config.olmEnable() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String rawMessage = event.getMessage();
		String message = Text.removeTags(rawMessage).toLowerCase();

		if (config.olmPhaseBanner())
		{
			if (message.contains("rises with the power of acid"))
			{
				olmPhase = CoxOlmPhase.ACID;
				olmPhaseBannerTicks = PHASE_BANNER_DISPLAY_TICKS;
			}
			else if (message.contains("rises with the power of crystal"))
			{
				olmPhase = CoxOlmPhase.CRYSTAL;
				olmPhaseBannerTicks = PHASE_BANNER_DISPLAY_TICKS;
			}
			else if (message.contains("rises with the power of flame"))
			{
				olmPhase = CoxOlmPhase.FLAME;
				olmPhaseBannerTicks = PHASE_BANNER_DISPLAY_TICKS;
			}
			else if (message.contains("giving its all"))
			{
				olmPhase = CoxOlmPhase.FINAL_STAND;
				olmPhaseBannerTicks = PHASE_BANNER_DISPLAY_TICKS;
			}
		}

		if (config.olmSpherePrayer())
		{
			if (message.contains("fires a sphere of aggression"))
			{
				olmSpherePrayer = Prayer.PROTECT_FROM_MELEE;
				olmSpherePrayerTicks = SPHERE_PRAYER_DISPLAY_TICKS;
			}
			else if (message.contains("fires a sphere of magical power"))
			{
				olmSpherePrayer = Prayer.PROTECT_FROM_MAGIC;
				olmSpherePrayerTicks = SPHERE_PRAYER_DISPLAY_TICKS;
			}
			else if (message.contains("fires a sphere of accuracy and dexterity"))
			{
				olmSpherePrayer = Prayer.PROTECT_FROM_MISSILES;
				olmSpherePrayerTicks = SPHERE_PRAYER_DISPLAY_TICKS;
			}
		}

		// Confirmed melee-hand-only: the mage hand has a permanent, non-clenching damage
		// mitigation instead, so there's no equivalent message to match for it.
		if (config.olmHandClenchWarning() && message.contains("left claw clenches"))
		{
			olmHandClenchTicks = HAND_CLENCH_DISPLAY_TICKS;
		}

		if (config.olmTeleportWarning())
		{
			Matcher matcher = OLM_TELEPORT_PAIR_PATTERN.matcher(rawMessage);
			if (matcher.find())
			{
				String targetName = Text.sanitize(matcher.group(1));
				for (Player player : client.getTopLevelWorldView().players())
				{
					if (player != null && targetName.equals(Text.sanitize(String.valueOf(player.getName()))))
					{
						olmTeleportTargets.put(player, TELEPORT_TARGET_DISPLAY_TICKS);
					}
				}
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (!isInChambers() || !config.olmEnable())
		{
			return;
		}

		Projectile projectile = event.getProjectile();
		switch (projectile.getId())
		{
			case SpotanimID.OLM_FIREBREATH_TRAVEL:
				if (config.olmHeadPrayer())
				{
					olmHeadPrayer = Prayer.PROTECT_FROM_MAGIC;
					olmHeadPrayerTicks = HEAD_PRAYER_DISPLAY_TICKS;
				}
				break;
			case SpotanimID.OLM_GENERIC_RANGE_PROJ:
				if (config.olmHeadPrayer())
				{
					olmHeadPrayer = Prayer.PROTECT_FROM_MISSILES;
					olmHeadPrayerTicks = HEAD_PRAYER_DISPLAY_TICKS;
				}
				break;
			case SpotanimID.OLM_ACID_SPIT:
				if (config.olmAcidTargetWarning() && projectile.getInteracting() instanceof Player)
				{
					olmAcidTarget = (Player) projectile.getInteracting();
					olmAcidTargetTicks = ACID_TARGET_DISPLAY_TICKS;
				}
				break;
			default:
				break;
		}
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

		// Olm's sphere/head attacks take priority over the room's usual per-monster prayer,
		// since they're the imminent threat when active.
		Prayer spherePrayer = getOlmSpherePrayer();
		if (spherePrayer != null)
		{
			return spherePrayer;
		}

		Prayer headPrayer = getOlmHeadPrayer();
		if (headPrayer != null)
		{
			return headPrayer;
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
