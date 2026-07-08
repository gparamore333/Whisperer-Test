package com.coxoverlay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
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
import net.runelite.api.Actor;
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
import net.runelite.api.events.AnimationChanged;
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

	@Getter(AccessLevel.PACKAGE)
	private final Set<CoxShamanAcid> shamanAcidWarnings = new HashSet<>();

	// Live head-facing/exposure read - tells you whether you're currently somewhere Olm has to
	// actively turn to find, which is the precondition for the special-denial technique below.
	// Computed from the head NPC's own orientation each tick, never from the wiki's disputed
	// numbered-safespot rules - see CoxOlmHeadFacing's javadoc for why.
	@Getter(AccessLevel.PACKAGE)
	private CoxOlmHeadFacing olmHeadFacing = CoxOlmHeadFacing.UNKNOWN;

	@Getter(AccessLevel.PACKAGE)
	private boolean olmPlayerExposed;

	@Getter(AccessLevel.PACKAGE)
	private final Map<CoxOlmSafespot, WorldPoint> olmSafespotPositions = new EnumMap<>(CoxOlmSafespot.class);

	@Getter(AccessLevel.PACKAGE)
	private CoxOlmSafespot olmRecommendedSafespot;

	// The actual point of the kiting technique: Olm's special attacks (Crystal Burst/Lightning/
	// Teleport) run on a fixed rotation, always exactly two standard attacks apart. Forcing a
	// head turn (moving somewhere it can't see you) makes it skip whatever's next - including a
	// queued special, denying it outright rather than delaying it. This tracks position in that
	// rotation by counting observed standard-attack events, and resyncs off the two specials
	// that have a directly-observable signal (Lightning's graphic, Teleport's pairing message).
	// Crystal Burst has no verified tracked ID, so it can't be directly confirmed - see
	// registerOlmStandardAttack() for how that gap is handled.
	@Getter(AccessLevel.PACKAGE)
	private CoxOlmSpecial olmNextSpecial = CoxOlmSpecial.CRYSTAL_BURST;
	private int olmStandardAttacksSinceSpecial;
	private boolean olmRotationConfirmed;
	@Getter(AccessLevel.PACKAGE)
	private boolean olmSpecialImminent;
	private boolean olmStandardCountedThisTick;
	private boolean olmLightningActiveLastTick;
	@Getter(AccessLevel.PACKAGE)
	private int olmSpecialDeniedFlashTicks;

	// The head's attack speed is 4 per the wiki's own infobox (Great_Olm page, "attack speed =
	// 4") - a real, verified tick cadence, not a guess. Every 4 ticks it re-checks whether a
	// player is in whichever zone it's currently facing; if not, that whole slot is spent just
	// turning (a "skip"), no attack happens. This tracks live ticks-until-next-check, resynced
	// off the same directly-observed standard/special events used for the rotation tracker
	// above (skipped/empty steps are silent, but still land exactly on the same 4-tick beat).
	@Getter(AccessLevel.PACKAGE)
	private int olmTicksUntilNextStep;
	private boolean olmTickTrackerResyncedThisTick;
	@Getter(AccessLevel.PACKAGE)
	private boolean olmActionTicksConfirmed;

	private int olmMeleeAttackCount;
	private int olmMageAttackCount;

	@Getter(AccessLevel.PACKAGE)
	private boolean showStaminaReminder;
	private boolean staminaReminderArmed = true;

	// Wiki-confirmed: exactly two standard attacks occur between each special in the rotation.
	private static final int STANDARDS_BETWEEN_SPECIALS = 2;
	private static final int SPECIAL_RESULT_DISPLAY_TICKS = 4;
	private static final int STAMINA_HYSTERESIS_PERCENT = 10;
	// Great Olm's head attack speed, per the wiki infobox - verified, not estimated.
	private static final int OLM_HEAD_ATTACK_SPEED_TICKS = 4;

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

		olmStandardCountedThisTick = false;
		olmTickTrackerResyncedThisTick = false;

		olmLightningTrail.clear();
		olmHealBeamTiles.clear();
		olmTeleportDestinations.clear();
		boolean lightningPresentThisTick = false;
		if (config.olmEnable())
		{
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				int id = graphicsObject.getId();
				if (id == OLM_LIGHTNING_SPOTANIM_ID)
				{
					lightningPresentThisTick = true;
					if (config.olmLightningWarning())
					{
						olmLightningTrail.add(WorldPoint.fromLocal(client, graphicsObject.getLocation()));
					}
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
		if (lightningPresentThisTick && !olmLightningActiveLastTick)
		{
			registerOlmSpecialObserved(CoxOlmSpecial.LIGHTNING);
		}
		olmLightningActiveLastTick = lightningPresentThisTick;

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

		shamanAcidWarnings.removeIf(CoxShamanAcid::hasLanded);

		updateOlmKiteAssist();
		updateStaminaReminder();
	}

	private void updateOlmKiteAssist()
	{
		if (!config.olmEnable() || !config.olmKiteEnable())
		{
			olmHeadFacing = CoxOlmHeadFacing.UNKNOWN;
			olmPlayerExposed = false;
			olmSafespotPositions.clear();
			olmRecommendedSafespot = null;
			olmMeleeAttackCount = 0;
			olmMageAttackCount = 0;
			olmActionTicksConfirmed = false;
			olmTicksUntilNextStep = 0;
			resetOlmSpecialRotation();
			return;
		}

		olmHeadFacing = calculateOlmHeadFacing();
		// Populates olmSafespotPositions (and the recommended tile) before exposure is checked,
		// since calculateOlmPlayerExposed() looks up the nearest resolved tile's visibility.
		updateOlmSafespots();
		olmPlayerExposed = calculateOlmPlayerExposed();

		// The rotation is a fixed loop across every non-head phase except the final stand,
		// where the forced teleport/lightning/crystal burst are removed entirely per the wiki.
		if (olmPhase == CoxOlmPhase.FINAL_STAND)
		{
			resetOlmSpecialRotation();
		}

		olmSpecialDeniedFlashTicks = Math.max(0, olmSpecialDeniedFlashTicks - 1);

		if (olmActionTicksConfirmed && !olmTickTrackerResyncedThisTick)
		{
			olmTicksUntilNextStep = olmTicksUntilNextStep <= 0
				? OLM_HEAD_ATTACK_SPEED_TICKS - 1
				: olmTicksUntilNextStep - 1;
		}

		Player player = client.getLocalPlayer();
		Actor interacting = player == null ? null : player.getInteracting();
		boolean onMeleeHand = interacting instanceof NPC && ((NPC) interacting).getId() == NpcID.OLM_HAND_LEFT;
		boolean onMageHand = interacting instanceof NPC && ((NPC) interacting).getId() == NpcID.OLM_HAND_RIGHT;
		if (!onMeleeHand)
		{
			olmMeleeAttackCount = 0;
		}
		if (!onMageHand)
		{
			olmMageAttackCount = 0;
		}
	}

	private void resetOlmSpecialRotation()
	{
		olmNextSpecial = CoxOlmSpecial.CRYSTAL_BURST;
		olmStandardAttacksSinceSpecial = 0;
		olmRotationConfirmed = false;
		olmSpecialImminent = false;
	}

	/**
	 * Registers one observed "standard attack" event (the head's basic magic/range attack, a
	 * sphere, or one of the elemental-phase abilities that substitute for it - Acid Spray/Drip,
	 * Falling Crystals, Crystal Bombs). Multiple game objects can spawn for a single such event
	 * (e.g. up to 3 crystal bombs, or 11 falling-crystal markers at once), so this is rate-
	 * limited to once per tick via {@code olmStandardCountedThisTick}.
	 * <p>
	 * Fire Wall has no verified tracked ID, so a standard attack during the flame phase that
	 * manifests only as a Fire Wall won't be counted here - the rotation can drift for at most
	 * one cycle in that case, and self-corrects the next time Lightning or Teleport is directly
	 * observed via {@link #registerOlmSpecialObserved}.
	 */
	private void registerOlmStandardAttack()
	{
		if (!config.olmEnable() || !config.olmKiteEnable() || olmStandardCountedThisTick)
		{
			return;
		}
		olmStandardCountedThisTick = true;
		resyncOlmActionTicks();

		if (olmSpecialImminent)
		{
			// A standard attack fired where the special was expected - per the wiki, a denied
			// special never queues up again, it's simply skipped, so advance past it.
			olmSpecialDeniedFlashTicks = SPECIAL_RESULT_DISPLAY_TICKS;
			olmNextSpecial = olmNextSpecial.next();
			olmStandardAttacksSinceSpecial = 0;
			olmSpecialImminent = false;
		}
		else if (olmRotationConfirmed)
		{
			olmStandardAttacksSinceSpecial++;
			if (olmStandardAttacksSinceSpecial >= STANDARDS_BETWEEN_SPECIALS)
			{
				olmSpecialImminent = true;
			}
		}
	}

	/** Registers a directly-confirmed special attack (Lightning's graphic, Teleport's chat pairing message). */
	private void registerOlmSpecialObserved(CoxOlmSpecial special)
	{
		if (!config.olmEnable() || !config.olmKiteEnable())
		{
			return;
		}
		resyncOlmActionTicks();
		olmRotationConfirmed = true;
		olmSpecialImminent = false;
		olmStandardAttacksSinceSpecial = 0;
		olmNextSpecial = special.next();
	}

	/**
	 * Marks the current tick as a confirmed head-action step, so the 4-tick countdown
	 * re-anchors to it. Deliberately separate from {@code olmRotationConfirmed} (which gates
	 * the special-rotation-position tracker and only anchors off an actually-observed special)
	 * - this anchors off *any* observed standard or special attack, which is a much more
	 * frequent signal, so the tick countdown can start well before the first special is seen.
	 */
	private void resyncOlmActionTicks()
	{
		olmActionTicksConfirmed = true;
		olmTicksUntilNextStep = OLM_HEAD_ATTACK_SPEED_TICKS;
		olmTickTrackerResyncedThisTick = true;
	}

	/**
	 * Buckets the head's live orientation into one of its 3 real states. Olm's head doesn't
	 * sweep a smooth cone - the wiki confirms it only ever faces LEFT, MIDDLE, or RIGHT, and
	 * per-tile visibility is a fixed table keyed off that state (see {@link CoxOlmSafespot}),
	 * not off the raw angle. MIDDLE is a safe bucket (facing roughly straight into the room,
	 * i.e. toward due south from the head - not in dispute between the two wiki pages).
	 * Which physical side (west/melee vs. east/mage) is LEFT vs. RIGHT *is* disputed between
	 * them, so that part honours {@link CoxOverlayConfig#olmKiteSwapLeftRight()}.
	 */
	private CoxOlmHeadFacing calculateOlmHeadFacing()
	{
		if (!config.olmKiteHeadFacing())
		{
			return CoxOlmHeadFacing.UNKNOWN;
		}

		NPC head = findOlmHead();
		if (head == null)
		{
			return CoxOlmHeadFacing.UNKNOWN;
		}

		int orientation = head.getOrientation();
		int westDiff = angularDiff(orientation, 512);
		int eastDiff = angularDiff(orientation, 1536);
		int southDiff = angularDiff(orientation, 0);

		if (southDiff <= westDiff && southDiff <= eastDiff)
		{
			return CoxOlmHeadFacing.MIDDLE;
		}

		boolean facingWest = westDiff < eastDiff;
		boolean westIsLeft = !config.olmKiteSwapLeftRight();
		if (facingWest)
		{
			return westIsLeft ? CoxOlmHeadFacing.LEFT : CoxOlmHeadFacing.RIGHT;
		}
		return westIsLeft ? CoxOlmHeadFacing.RIGHT : CoxOlmHeadFacing.LEFT;
	}

	private boolean calculateOlmPlayerExposed()
	{
		if (!config.olmKiteHeadFacing() || client.getLocalPlayer() == null || olmHeadFacing == CoxOlmHeadFacing.UNKNOWN)
		{
			return false;
		}

		CoxOlmSafespot nearest = findNearestSafespot(client.getLocalPlayer().getWorldLocation());
		return nearest != null && !nearest.isVisibleWhen(olmHeadFacing);
	}

	/**
	 * Resolves the 8 named safespot tiles (see {@link CoxOlmSafespot}) from wiki template-map
	 * coordinates to their live, instance-mapped positions, and picks the nearest one currently
	 * visible-safe per the wiki's own per-tile table as the recommended move target. Always
	 * computed while kiting assist is on (not gated by the tile-grid display toggle), since
	 * {@link #calculateOlmPlayerExposed()} depends on the resolved positions too.
	 */
	private void updateOlmSafespots()
	{
		NPC head = findOlmHead();
		if (head == null)
		{
			olmSafespotPositions.clear();
			olmRecommendedSafespot = null;
			return;
		}

		olmSafespotPositions.clear();
		for (CoxOlmSafespot spot : CoxOlmSafespot.values())
		{
			Collection<WorldPoint> instancePoints = WorldPoint.toLocalInstance(client, spot.toTemplatePoint());
			if (!instancePoints.isEmpty())
			{
				olmSafespotPositions.put(spot, instancePoints.iterator().next());
			}
		}

		olmRecommendedSafespot = null;
		WorldPoint playerLocation = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
		if (playerLocation == null || olmHeadFacing == CoxOlmHeadFacing.UNKNOWN)
		{
			return;
		}

		int bestDistance = Integer.MAX_VALUE;
		for (Map.Entry<CoxOlmSafespot, WorldPoint> entry : olmSafespotPositions.entrySet())
		{
			if (entry.getKey().isVisibleWhen(olmHeadFacing))
			{
				continue;
			}

			int distance = entry.getValue().distanceTo(playerLocation);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				olmRecommendedSafespot = entry.getKey();
			}
		}
	}

	private CoxOlmSafespot findNearestSafespot(WorldPoint location)
	{
		CoxOlmSafespot nearest = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Map.Entry<CoxOlmSafespot, WorldPoint> entry : olmSafespotPositions.entrySet())
		{
			int distance = entry.getValue().distanceTo(location);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				nearest = entry.getKey();
			}
		}
		return nearest;
	}

	private NPC findOlmHead()
	{
		return trackedNpcs.stream()
			.filter(tracked -> tracked.getInfo() == CoxNpcInfo.OLM_HEAD)
			.map(CoxTrackedNpc::getNpc)
			.findFirst()
			.orElse(null);
	}

	private static int angularDiff(int a, int b)
	{
		int diff = Math.abs(a - b) % 2048;
		return diff > 1024 ? 2048 - diff : diff;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!isInChambers() || !config.olmEnable() || !config.olmKiteEnable())
		{
			return;
		}

		Player player = client.getLocalPlayer();
		if (event.getActor() != player)
		{
			return;
		}

		Actor target = player.getInteracting();
		if (!(target instanceof NPC))
		{
			return;
		}

		int targetId = ((NPC) target).getId();
		if (targetId == NpcID.OLM_HAND_LEFT)
		{
			olmMeleeAttackCount++;
			if (olmMeleeAttackCount > config.olmKiteMeleeRatio().getAttacks())
			{
				olmMeleeAttackCount = 1;
			}
		}
		else if (targetId == NpcID.OLM_HAND_RIGHT)
		{
			olmMageAttackCount++;
			if (olmMageAttackCount > config.olmKiteMageRatio().getAttacks())
			{
				olmMageAttackCount = 1;
			}
		}
	}

	private void updateStaminaReminder()
	{
		if (!config.olmKiteStaminaReminder())
		{
			showStaminaReminder = false;
			staminaReminderArmed = true;
			return;
		}

		int energyPercent = client.getEnergy() / 100;
		int threshold = config.olmKiteStaminaThreshold();
		if (staminaReminderArmed && energyPercent <= threshold)
		{
			showStaminaReminder = true;
			staminaReminderArmed = false;
		}
		else if (energyPercent >= threshold + STAMINA_HYSTERESIS_PERCENT)
		{
			showStaminaReminder = false;
			staminaReminderArmed = true;
		}
	}

	int getOlmMeleeAttackCount()
	{
		return olmMeleeAttackCount;
	}

	int getOlmMageAttackCount()
	{
		return olmMageAttackCount;
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
		shamanAcidWarnings.clear();
		olmHeadFacing = CoxOlmHeadFacing.UNKNOWN;
		olmPlayerExposed = false;
		olmSafespotPositions.clear();
		olmRecommendedSafespot = null;
		resetOlmSpecialRotation();
		olmStandardCountedThisTick = false;
		olmLightningActiveLastTick = false;
		olmSpecialDeniedFlashTicks = 0;
		olmActionTicksConfirmed = false;
		olmTicksUntilNextStep = 0;
		olmTickTrackerResyncedThisTick = false;
		olmMeleeAttackCount = 0;
		olmMageAttackCount = 0;
		showStaminaReminder = false;
		staminaReminderArmed = true;
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

		boolean isSphere = message.contains("fires a sphere of aggression")
			|| message.contains("fires a sphere of magical power")
			|| message.contains("fires a sphere of accuracy and dexterity");
		if (isSphere)
		{
			// Spheres are one of the alternatives that make up a "standard attack" in the
			// wiki's own terminology, so they count towards the special-attack rotation too.
			registerOlmStandardAttack();
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

		Matcher teleportMatcher = OLM_TELEPORT_PAIR_PATTERN.matcher(rawMessage);
		if (teleportMatcher.find())
		{
			registerOlmSpecialObserved(CoxOlmSpecial.TELEPORT);

			if (config.olmTeleportWarning())
			{
				String targetName = Text.sanitize(teleportMatcher.group(1));
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
		if (!isInChambers())
		{
			return;
		}

		Projectile projectile = event.getProjectile();
		switch (projectile.getId())
		{
			case SpotanimID.OLM_FIREBREATH_TRAVEL:
				registerOlmStandardAttack();
				if (config.olmEnable() && config.olmHeadPrayer())
				{
					olmHeadPrayer = Prayer.PROTECT_FROM_MAGIC;
					olmHeadPrayerTicks = HEAD_PRAYER_DISPLAY_TICKS;
				}
				break;
			case SpotanimID.OLM_GENERIC_RANGE_PROJ:
				registerOlmStandardAttack();
				if (config.olmEnable() && config.olmHeadPrayer())
				{
					olmHeadPrayer = Prayer.PROTECT_FROM_MISSILES;
					olmHeadPrayerTicks = HEAD_PRAYER_DISPLAY_TICKS;
				}
				break;
			case SpotanimID.OLM_ACID_SPIT:
				registerOlmStandardAttack();
				if (config.olmEnable() && config.olmAcidTargetWarning() && projectile.getInteracting() instanceof Player)
				{
					olmAcidTarget = (Player) projectile.getInteracting();
					olmAcidTargetTicks = ACID_TARGET_DISPLAY_TICKS;
				}
				break;
			case SpotanimID.LIZARDSHAMAN_SPIT_ACID:
				if (isRoomEnabled(CoxRoom.SHAMANS) && config.shamanAcidWarning())
				{
					WorldPoint target = WorldPoint.fromLocal(client, event.getPosition());
					shamanAcidWarnings.add(new CoxShamanAcid(projectile, target));
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
				// Crystal Bombs (a Crystal-phase standard-attack substitute) - not to be
				// confused with Crystal Burst, the unrelated special-rotation attack.
				registerOlmStandardAttack();
				olmBombs.add(new CoxOlmBomb(gameObject, client.getTickCount()));
				break;
			case ObjectID.OLM_ACID_POOL:
				registerOlmStandardAttack();
				olmAcidPools.add(gameObject);
				break;
			case ObjectID.OLM_CRYSTAL_ATTACK_SMALL:
			case ObjectID.OLM_CRYSTAL_ATTACK_LARGE:
				registerOlmStandardAttack();
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
