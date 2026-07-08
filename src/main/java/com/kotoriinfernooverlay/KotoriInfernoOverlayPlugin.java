package com.kotoriinfernooverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Provides;
import com.kotoriinfernooverlay.displaymodes.KotoriSafespotDisplayMode;
import com.kotoriinfernooverlay.displaymodes.KotoriWaveDisplayMode;
import com.kotoriinfernooverlay.displaymodes.KotoriZukShieldDisplayMode;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;

/**
 * A faithful, self-contained port of OreoCupcakes' kotori-plugins Inferno plugin
 * (https://github.com/OreoCupcakes/kotori-plugins/tree/master/inferno), so its visuals and
 * mechanics can be compared side-by-side against {@code com.infernooverlay}. Everything here
 * is observation and rendering only - it reads NPC animations/positions/prayers (already
 * visible on screen) and predicts attack timing and safespots so the overlay can draw them.
 * It never sends a click, key press, or movement to the game. The original plugin had no
 * automation to strip; the only change from upstream is removing its dependency on the
 * external KotoriUtils library (replaced with direct RuneLite API calls) so it builds
 * standalone here.
 */
@PluginDescriptor(
	name = "Kotori Inferno Overlay",
	description = "Visual-only port of OreoCupcakes' kotori-plugins Inferno overlay - no automation",
	tags = {"inferno", "overlay", "pvm", "boss", "kotori"},
	enabledByDefault = false
)
public class KotoriInfernoOverlayPlugin extends Plugin
{
	private static final int INFERNO_REGION = 9043;

	public static final int JAL_NIB = 7574;
	public static final int JAL_MEJRAH = 7578;
	public static final int JAL_MEJRAH_STAND = 7577;
	public static final int JAL_AK_RANGE_ATTACK = 7581;
	public static final int JAL_AK_MELEE_ATTACK = 7582;
	public static final int JAL_AK_MAGIC_ATTACK = 7583;
	public static final int JAL_IMKOT = 7597;
	public static final int JAL_XIL_MELEE_ATTACK = 7604;
	public static final int JAL_XIL_RANGE_ATTACK = 7605;
	public static final int JAL_ZEK_MAGE_ATTACK = 7610;
	public static final int JAL_ZEK_MELEE_ATTACK = 7612;
	public static final int JALTOK_JAD_MAGE_ATTACK = 7592;
	public static final int JALTOK_JAD_RANGE_ATTACK = 7593;
	public static final int TZKAL_ZUK = 7566;
	static final int MELEE_BURROW_ANIMATION = 7600;
	static final int MAGE_RESPAWN_ANIMATION = 7611;
	static final int NIBBLER_DESPAWN_ANIMATION = 7576;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private NPCManager npcManager;

	@Inject
	private KotoriInfernoOverlay infernoOverlay;

	@Inject
	private KotoriInfernoWaveOverlay waveOverlay;

	@Inject
	private KotoriInfernoInfoBoxOverlay infoBoxOverlay;

	@Inject
	private KotoriInfernoOverlayConfig config;

	private WorldPoint lastLocation = new WorldPoint(0, 0, 0);

	@Getter(AccessLevel.PACKAGE)
	private int currentWaveNumber = -1;

	@Getter(AccessLevel.PACKAGE)
	private final List<KotoriInfernoNPC> infernoNpcs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, Map<KotoriInfernoNPC.Attack, Integer>> upcomingAttacks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private KotoriInfernoNPC.Attack closestAttack = null;

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> obstacles = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private boolean finalPhase = false;
	private boolean finalPhaseTick = false;
	private int ticksSinceFinalPhase = 0;
	@Getter(AccessLevel.PACKAGE)
	private NPC zukShield = null;
	private NPC zuk = null;
	private WorldPoint zukShieldLastPosition = null;
	private int zukShieldCornerTicks = -2;

	private int zukShieldNegativeXCoord = -1;
	private int zukShieldPositiveXCoord = -1;
	private int zukShieldLastNonZeroDelta = 0;
	private int zukShieldLastDelta = 0;
	private int zukShieldTicksLeftInCorner = -1;

	@Getter(AccessLevel.PACKAGE)
	private KotoriInfernoNPC centralNibbler = null;

	// 0 = total safespot, 1 = pray melee, 2 = pray range, 3 = pray magic,
	// 4 = pray melee+range, 5 = pray melee+magic, 6 = pray range+magic, 7 = pray all
	@Getter(AccessLevel.PACKAGE)
	private final Map<WorldPoint, Integer> safeSpotMap = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, List<WorldPoint>> safeSpotAreas = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<KotoriInfernoBlobDeathSpot> blobDeathSpots = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private long lastTick;

	private KotoriInfernoSpawnTimerInfobox spawnTimerInfoBox;

	@Provides
	KotoriInfernoOverlayConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(KotoriInfernoOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		waveOverlay.setDisplayMode(config.waveDisplay());
		waveOverlay.setWaveHeaderColor(config.waveOverlayHeaderColor());
		waveOverlay.setWaveTextColor(config.waveTextColor());

		if (isInInferno())
		{
			addOverlays();
		}
	}

	@Override
	protected void shutDown()
	{
		removeOverlays();

		infernoNpcs.clear();
		upcomingAttacks.clear();
		obstacles.clear();
		safeSpotMap.clear();
		safeSpotAreas.clear();
		blobDeathSpots.clear();

		currentWaveNumber = -1;
		zuk = null;
		zukShield = null;
		centralNibbler = null;
		zukShieldLastPosition = null;
		closestAttack = null;
	}

	private void addOverlays()
	{
		overlayManager.add(infernoOverlay);
		overlayManager.add(infoBoxOverlay);

		if (config.waveDisplay() != KotoriWaveDisplayMode.NONE)
		{
			overlayManager.add(waveOverlay);
		}
	}

	private void removeOverlays()
	{
		overlayManager.remove(infernoOverlay);
		overlayManager.remove(waveOverlay);
		overlayManager.remove(infoBoxOverlay);

		if (spawnTimerInfoBox != null)
		{
			infoBoxManager.removeInfoBox(spawnTimerInfoBox);
		}
		spawnTimerInfoBox = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (!isInInferno())
		{
			if (currentWaveNumber != -1)
			{
				shutDown();
			}
		}
		else if (currentWaveNumber == -1)
		{
			addOverlays();
			infernoNpcs.clear();
			currentWaveNumber = 1;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isInInferno())
		{
			return;
		}

		lastTick = System.currentTimeMillis();

		upcomingAttacks.clear();
		calculateUpcomingAttacks();

		closestAttack = null;
		calculateClosestAttack();

		safeSpotMap.clear();
		calculateSafespots();

		safeSpotAreas.clear();
		calculateSafespotAreas();

		obstacles.clear();
		calculateObstacles();

		centralNibbler = null;
		calculateCentralNibbler();

		calculateSpawnTimerInfobox();

		manageBlobDeathLocations();

		if (finalPhaseTick)
		{
			finalPhaseTick = false;
		}
		else if (finalPhase)
		{
			ticksSinceFinalPhase++;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isInInferno())
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		if (npcId == NpcID.ANCESTRAL_GLYPH)
		{
			zukShield = npc;
			return;
		}

		KotoriInfernoNPC.Type type = KotoriInfernoNPC.Type.typeFromId(npcId);
		if (type == null)
		{
			return;
		}

		switch (type)
		{
			case BLOB:
				// Blobs go at the end of the list because their detection-tick prayer depends
				// on the upcoming attacks of other NPCs already calculated this tick.
				infernoNpcs.add(new KotoriInfernoNPC(npc));
				return;
			case MAGE:
				if (zuk != null && spawnTimerInfoBox != null)
				{
					spawnTimerInfoBox.reset();
					spawnTimerInfoBox.run();
				}
				break;
			case ZUK:
				finalPhase = false;
				zukShieldCornerTicks = -2;
				zukShieldLastPosition = null;

				if (config.spawnTimerInfobox())
				{
					zuk = npc;

					if (spawnTimerInfoBox != null)
					{
						infoBoxManager.removeInfoBox(spawnTimerInfoBox);
					}

					spawnTimerInfoBox = new KotoriInfernoSpawnTimerInfobox(itemManager.getImage(ItemID.TZREKZUK), this);
					infoBoxManager.addInfoBox(spawnTimerInfoBox);
				}
				break;
			case HEALER_ZUK:
				finalPhase = true;
				ticksSinceFinalPhase = 1;
				finalPhaseTick = true;
				for (KotoriInfernoNPC infernoNPC : infernoNpcs)
				{
					if (infernoNPC.getType() == KotoriInfernoNPC.Type.ZUK)
					{
						infernoNPC.setTicksTillNextAttack(-1);
					}
				}
				break;
			default:
				break;
		}

		infernoNpcs.add(0, new KotoriInfernoNPC(npc));
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!isInInferno())
		{
			return;
		}

		NPC npc = event.getNpc();
		int npcId = npc.getId();

		if (npcId == NpcID.ANCESTRAL_GLYPH)
		{
			zukShield = null;
		}
		else if (npcId == NpcID.TZKALZUK)
		{
			zuk = null;

			if (spawnTimerInfoBox != null)
			{
				infoBoxManager.removeInfoBox(spawnTimerInfoBox);
			}

			spawnTimerInfoBox = null;
		}

		infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == npc);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!isInInferno() || !(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		int animationId = npc.getAnimation();

		if (KotoriInfernoNPC.Type.typeFromId(npc.getId()) == KotoriInfernoNPC.Type.NIBBLER && animationId == NIBBLER_DESPAWN_ANIMATION)
		{
			infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == npc);
		}

		if (config.indicateBlobDeathLocation() && KotoriInfernoNPC.Type.typeFromId(npc.getId()) == KotoriInfernoNPC.Type.BLOB
			&& animationId == KotoriInfernoBlobDeathSpot.BLOB_DEATH_ANIMATION)
		{
			// Remove from the list so the ticks overlay doesn't compete with the tile overlay.
			infernoNpcs.removeIf(infernoNPC -> infernoNPC.getNpc() == npc);
			blobDeathSpots.add(new KotoriInfernoBlobDeathSpot(npc.getLocalLocation()));
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isInInferno() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		if (message.contains("Wave:"))
		{
			int waveIndex = message.indexOf(": ") + 2;
			int tagIndex = message.indexOf('<', waveIndex);
			if (tagIndex != -1)
			{
				try
				{
					currentWaveNumber = Integer.parseInt(message.substring(waveIndex, tagIndex));
				}
				catch (NumberFormatException ignored)
				{
					// message format didn't match what we expected; leave the wave number as-is
				}
			}
		}
	}

	private boolean isInInferno()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		WorldPoint location = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		return location.getRegionID() == INFERNO_REGION;
	}

	int getNextWaveNumber()
	{
		return currentWaveNumber == -1 || currentWaveNumber == 69 ? -1 : currentWaveNumber + 1;
	}

	private void calculateUpcomingAttacks()
	{
		for (KotoriInfernoNPC infernoNPC : infernoNpcs)
		{
			infernoNPC.gameTick(client, config, lastLocation, finalPhase, ticksSinceFinalPhase);

			if (infernoNPC.getType() == KotoriInfernoNPC.Type.ZUK && zukShieldCornerTicks == -1)
			{
				infernoNPC.updateNextAttack(KotoriInfernoNPC.Attack.UNKNOWN, 12);
				zukShieldCornerTicks = 0;
			}

			if (infernoNPC.getTicksTillNextAttack() > 0 && isPrayerHelper(infernoNPC)
				&& (infernoNPC.getNextAttack() != KotoriInfernoNPC.Attack.UNKNOWN
				|| (config.indicateBlobDetectionTick() && infernoNPC.getType() == KotoriInfernoNPC.Type.BLOB
				&& infernoNPC.getTicksTillNextAttack() >= 4)))
			{
				upcomingAttacks.computeIfAbsent(infernoNPC.getTicksTillNextAttack(), k -> new HashMap<>());

				if (config.indicateBlobDetectionTick() && infernoNPC.getType() == KotoriInfernoNPC.Type.BLOB
					&& infernoNPC.getTicksTillNextAttack() >= 4)
				{
					addBlobDetectionAttack(infernoNPC);
				}
				else
				{
					KotoriInfernoNPC.Attack attack = infernoNPC.getNextAttack();
					int priority = infernoNPC.getType().getPriority();

					if (!upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).containsKey(attack)
						|| upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).get(attack) > priority)
					{
						upcomingAttacks.get(infernoNPC.getTicksTillNextAttack()).put(attack, priority);
					}
				}
			}
		}
	}

	private void addBlobDetectionAttack(KotoriInfernoNPC infernoNPC)
	{
		int attackTick = infernoNPC.getTicksTillNextAttack();
		int detectionTick = attackTick - 3;

		upcomingAttacks.computeIfAbsent(detectionTick, k -> new HashMap<>());
		upcomingAttacks.computeIfAbsent(attackTick - 4, k -> new HashMap<>());

		int blobPriority = KotoriInfernoNPC.Type.BLOB.getPriority();
		Map<KotoriInfernoNPC.Attack, Integer> detectionTickAttacks = upcomingAttacks.get(detectionTick);

		if (detectionTickAttacks.containsKey(KotoriInfernoNPC.Attack.MAGIC))
		{
			if (detectionTickAttacks.get(KotoriInfernoNPC.Attack.MAGIC) > blobPriority)
			{
				detectionTickAttacks.put(KotoriInfernoNPC.Attack.MAGIC, blobPriority);
			}
		}
		else if (detectionTickAttacks.containsKey(KotoriInfernoNPC.Attack.RANGED))
		{
			if (detectionTickAttacks.get(KotoriInfernoNPC.Attack.RANGED) > blobPriority)
			{
				detectionTickAttacks.put(KotoriInfernoNPC.Attack.RANGED, blobPriority);
			}
		}
		else if (upcomingAttacks.get(attackTick).containsKey(KotoriInfernoNPC.Attack.MAGIC)
			|| upcomingAttacks.get(attackTick - 4).containsKey(KotoriInfernoNPC.Attack.MAGIC))
		{
			if (!detectionTickAttacks.containsKey(KotoriInfernoNPC.Attack.RANGED)
				|| detectionTickAttacks.get(KotoriInfernoNPC.Attack.RANGED) > blobPriority)
			{
				detectionTickAttacks.put(KotoriInfernoNPC.Attack.RANGED, blobPriority);
			}
		}
		else if (upcomingAttacks.get(attackTick).containsKey(KotoriInfernoNPC.Attack.RANGED)
			|| upcomingAttacks.get(attackTick - 4).containsKey(KotoriInfernoNPC.Attack.RANGED))
		{
			if (!detectionTickAttacks.containsKey(KotoriInfernoNPC.Attack.MAGIC)
				|| detectionTickAttacks.get(KotoriInfernoNPC.Attack.MAGIC) > blobPriority)
			{
				detectionTickAttacks.put(KotoriInfernoNPC.Attack.MAGIC, blobPriority);
			}
		}
		else
		{
			detectionTickAttacks.put(KotoriInfernoNPC.Attack.MAGIC, blobPriority);
		}
	}

	private void calculateClosestAttack()
	{
		int closestTick = 999;
		int closestPriority = 999;

		for (Integer tick : upcomingAttacks.keySet())
		{
			Map<KotoriInfernoNPC.Attack, Integer> attackPriority = upcomingAttacks.get(tick);

			for (KotoriInfernoNPC.Attack currentAttack : attackPriority.keySet())
			{
				int currentPriority = attackPriority.get(currentAttack);
				if (tick < closestTick || (tick == closestTick && currentPriority < closestPriority))
				{
					closestAttack = currentAttack;
					closestPriority = currentPriority;
					closestTick = tick;
				}
			}
		}
	}

	private void calculateSafespots()
	{
		if (currentWaveNumber < 69)
		{
			if (config.safespotDisplayMode() != KotoriSafespotDisplayMode.OFF)
			{
				calculateNormalSafespots();
			}
		}
		else if (currentWaveNumber == 69 && zukShield != null)
		{
			calculateZukShieldSafespots();
		}
	}

	private void calculateNormalSafespots()
	{
		int checkSize = config.safespotsCheckSize() / 2;

		for (int x = -checkSize; x <= checkSize; x++)
		{
			for (int y = -checkSize; y <= checkSize; y++)
			{
				WorldPoint checkLoc = client.getLocalPlayer().getWorldLocation().dx(x).dy(y);

				if (obstacles.contains(checkLoc))
				{
					continue;
				}

				for (KotoriInfernoNPC infernoNPC : infernoNpcs)
				{
					if (!isNormalSafespots(infernoNPC))
					{
						continue;
					}

					safeSpotMap.putIfAbsent(checkLoc, 0);

					if (infernoNPC.canAttack(client, checkLoc) || infernoNPC.canMoveToAttack(client, checkLoc, obstacles))
					{
						markUnsafe(checkLoc, infernoNPC);
					}
				}
			}
		}
	}

	private void markUnsafe(WorldPoint checkLoc, KotoriInfernoNPC infernoNPC)
	{
		KotoriInfernoNPC.Type type = infernoNPC.getType();
		int current = safeSpotMap.get(checkLoc);

		if (type.getDefaultAttack() == KotoriInfernoNPC.Attack.MELEE)
		{
			current = addUnsafeBit(current, 1, 2, 4, 3, 5, 6, 7);
		}

		if (type.getDefaultAttack() == KotoriInfernoNPC.Attack.MAGIC
			|| (type == KotoriInfernoNPC.Type.BLOB && current != 2 && current != 4))
		{
			current = addUnsafeBit(current, 3, 1, 5, 2, 6, 5, 7);
		}

		if (type.getDefaultAttack() == KotoriInfernoNPC.Attack.RANGED
			|| (type == KotoriInfernoNPC.Type.BLOB && current != 3 && current != 5))
		{
			current = addUnsafeBit(current, 2, 1, 4, 3, 6, 4, 7);
		}

		if (type == KotoriInfernoNPC.Type.JAD && infernoNPC.getNpc().getWorldArea().isInMeleeDistance(checkLoc))
		{
			current = addUnsafeBit(current, 1, 2, 4, 3, 5, 6, 7);
		}

		safeSpotMap.put(checkLoc, current);
	}

	/**
	 * Safespot codes combine like a bitmask over {melee=1, range=2, magic=3}: adding a danger
	 * to a tile moves it along a fixed lattice (0-&gt;single, single-&gt;pair, pair-&gt;7). The
	 * from/to pairs below mirror that lattice for one danger type at a time.
	 */
	private int addUnsafeBit(int current, int fromZero, int fromOtherA, int toOtherA, int fromOtherB, int toOtherB, int fromPair, int toAll)
	{
		if (current == 0)
		{
			return fromZero;
		}
		if (current == fromOtherA)
		{
			return toOtherA;
		}
		if (current == fromOtherB)
		{
			return toOtherB;
		}
		if (current == fromPair)
		{
			return toAll;
		}
		return current;
	}

	private void calculateZukShieldSafespots()
	{
		WorldPoint zukShieldCurrentPosition = zukShield.getWorldLocation();

		if (zukShieldLastPosition != null && zukShieldLastPosition.getX() != zukShieldCurrentPosition.getX() && zukShieldCornerTicks == -2)
		{
			zukShieldCornerTicks = -1;
		}

		if (zukShieldLastPosition != null)
		{
			int zukShieldDelta = zukShieldCurrentPosition.getX() - zukShieldLastPosition.getX();

			if (zukShieldDelta != 0)
			{
				zukShieldLastNonZeroDelta = zukShieldDelta;
			}

			if (zukShieldLastDelta == 0 && zukShieldDelta != 0)
			{
				zukShieldTicksLeftInCorner = 4;
			}

			if (zukShieldDelta == 0)
			{
				if (zukShieldLastNonZeroDelta > 0)
				{
					zukShieldPositiveXCoord = zukShieldCurrentPosition.getX();
				}
				else if (zukShieldLastNonZeroDelta < 0)
				{
					zukShieldNegativeXCoord = zukShieldCurrentPosition.getX();
				}

				if (zukShieldTicksLeftInCorner > 0)
				{
					zukShieldTicksLeftInCorner--;
				}
			}

			zukShieldLastDelta = zukShieldDelta;
		}

		zukShieldLastPosition = zukShieldCurrentPosition;

		if (config.safespotDisplayMode() == KotoriSafespotDisplayMode.OFF)
		{
			return;
		}

		KotoriZukShieldDisplayMode mode = finalPhase ? config.safespotsZukShieldAfterHealers() : config.safespotsZukShieldBeforeHealers();

		if (mode == KotoriZukShieldDisplayMode.LIVE || mode == KotoriZukShieldDisplayMode.LIVE_PLUS_PREDICT)
		{
			drawZukSafespot(zukShield.getWorldLocation().getX(), zukShield.getWorldLocation().getY(), 0);
		}

		if (mode == KotoriZukShieldDisplayMode.PREDICT || mode == KotoriZukShieldDisplayMode.LIVE_PLUS_PREDICT)
		{
			drawZukPredictedSafespot();
		}
	}

	private void drawZukPredictedSafespot()
	{
		WorldPoint zukShieldCurrentPosition = zukShield.getWorldLocation();

		if (zukShieldPositiveXCoord == -1 || zukShieldNegativeXCoord == -1)
		{
			return;
		}

		int nextShieldXCoord = zukShieldCurrentPosition.getX();

		for (KotoriInfernoNPC infernoNPC : infernoNpcs)
		{
			if (infernoNPC.getType() != KotoriInfernoNPC.Type.ZUK)
			{
				continue;
			}

			int ticksTilZukAttack = finalPhase ? infernoNPC.getTicksTillNextAttack() : infernoNPC.getTicksTillNextAttack() - 1;

			if (ticksTilZukAttack < 1)
			{
				if (finalPhase)
				{
					return;
				}
				ticksTilZukAttack = 10;
			}

			if (zukShieldLastNonZeroDelta > 0)
			{
				nextShieldXCoord += ticksTilZukAttack;

				if (nextShieldXCoord > zukShieldPositiveXCoord)
				{
					nextShieldXCoord -= zukShieldTicksLeftInCorner;
					nextShieldXCoord = nextShieldXCoord <= zukShieldPositiveXCoord
						? zukShieldPositiveXCoord
						: zukShieldPositiveXCoord - nextShieldXCoord + zukShieldPositiveXCoord;
				}
			}
			else
			{
				nextShieldXCoord -= ticksTilZukAttack;

				if (nextShieldXCoord < zukShieldNegativeXCoord)
				{
					nextShieldXCoord += zukShieldTicksLeftInCorner;
					nextShieldXCoord = nextShieldXCoord >= zukShieldNegativeXCoord
						? zukShieldNegativeXCoord
						: zukShieldNegativeXCoord - nextShieldXCoord + zukShieldNegativeXCoord;
				}
			}
		}

		drawZukSafespot(nextShieldXCoord, zukShieldCurrentPosition.getY(), 2);
	}

	private void drawZukSafespot(int xCoord, int yCoord, int colorSafeSpotId)
	{
		for (int x = xCoord - 1; x <= xCoord + 3; x++)
		{
			for (int y = yCoord - 4; y <= yCoord - 2; y++)
			{
				safeSpotMap.put(new WorldPoint(x, y, client.getTopLevelWorldView().getPlane()), colorSafeSpotId);
			}
		}
	}

	private void calculateSafespotAreas()
	{
		if (config.safespotDisplayMode() == KotoriSafespotDisplayMode.AREA)
		{
			for (Map.Entry<WorldPoint, Integer> entry : safeSpotMap.entrySet())
			{
				safeSpotAreas.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
			}
		}

		lastLocation = client.getLocalPlayer().getWorldLocation();
	}

	private void calculateObstacles()
	{
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null)
			{
				obstacles.addAll(npc.getWorldArea().toWorldPointList());
			}
		}
	}

	private void manageBlobDeathLocations()
	{
		if (config.indicateBlobDeathLocation())
		{
			blobDeathSpots.forEach(KotoriInfernoBlobDeathSpot::decrementTick);
			blobDeathSpots.removeIf(KotoriInfernoBlobDeathSpot::isDone);
		}
	}

	private void calculateCentralNibbler()
	{
		KotoriInfernoNPC bestNibbler = null;
		int bestAmountInArea = 0;
		int bestDistanceToPlayer = 999;

		for (KotoriInfernoNPC infernoNPC : infernoNpcs)
		{
			if (infernoNPC.getType() != KotoriInfernoNPC.Type.NIBBLER)
			{
				continue;
			}

			int amountInArea = 0;
			int distanceToPlayer = infernoNPC.getNpc().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());

			for (KotoriInfernoNPC checkNpc : infernoNpcs)
			{
				if (checkNpc.getType() != KotoriInfernoNPC.Type.NIBBLER
					|| checkNpc.getNpc().getWorldArea().distanceTo(infernoNPC.getNpc().getWorldArea()) > 1)
				{
					continue;
				}

				amountInArea++;
			}

			if (amountInArea > bestAmountInArea || (amountInArea == bestAmountInArea && distanceToPlayer < bestDistanceToPlayer))
			{
				bestNibbler = infernoNPC;
				bestAmountInArea = amountInArea;
				bestDistanceToPlayer = distanceToPlayer;
			}
		}

		if (bestNibbler != null)
		{
			centralNibbler = bestNibbler;
		}
	}

	private void calculateSpawnTimerInfobox()
	{
		if (zuk == null || finalPhase || spawnTimerInfoBox == null)
		{
			return;
		}

		int pauseHp = 600;
		int resumeHp = 480;

		Integer zukMaxHealth = npcManager.getHealth(zuk.getId());
		if (zukMaxHealth == null)
		{
			return;
		}

		int hp = calculateNpcHp(zuk.getHealthRatio(), zuk.getHealthScale(), zukMaxHealth);

		if (hp <= 0)
		{
			return;
		}

		if (spawnTimerInfoBox.isRunning())
		{
			if (hp >= resumeHp && hp < pauseHp)
			{
				spawnTimerInfoBox.pause();
			}
		}
		else if (hp < resumeHp)
		{
			spawnTimerInfoBox.run();
		}
	}

	// Ported from RuneLite's OpponentInfo plugin (BSD 2-Clause), which estimates an NPC's
	// exact hitpoints from its health ratio/scale - the client is only ever given a ratio.
	private static int calculateNpcHp(int ratio, int health, int maxHp)
	{
		if (ratio < 0 || health <= 0 || maxHp == -1)
		{
			return -1;
		}

		int minHealth = 1;
		int maxHealth = maxHp;

		if (health > 1)
		{
			if (ratio > 1)
			{
				minHealth = (maxHp * (ratio - 1) + health - 2) / (health - 1);
			}
			maxHealth = Math.min(maxHp, (maxHp * ratio - 1) / (health - 1));
		}

		return (minHealth + maxHealth + 1) / 2;
	}

	private boolean isPrayerHelper(KotoriInfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.prayerBat();
			case BLOB:
				return config.prayerBlob();
			case MELEE:
				return config.prayerMeleer();
			case RANGER:
				return config.prayerRanger();
			case MAGE:
				return config.prayerMage();
			case HEALER_JAD:
				return config.prayerHealerJad();
			case JAD:
				return config.prayerJad();
			default:
				return false;
		}
	}

	boolean isTicksOnNpc(KotoriInfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.ticksOnNpcBat();
			case BLOB:
				return config.ticksOnNpcBlob();
			case MELEE:
				return config.ticksOnNpcMeleer();
			case RANGER:
				return config.ticksOnNpcRanger();
			case MAGE:
				return config.ticksOnNpcMage();
			case HEALER_JAD:
				return config.ticksOnNpcHealerJad();
			case JAD:
				return config.ticksOnNpcJad();
			case ZUK:
				return config.ticksOnNpcZuk();
			default:
				return false;
		}
	}

	boolean isNormalSafespots(KotoriInfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.safespotsBat();
			case BLOB:
				return config.safespotsBlob();
			case MELEE:
				return config.safespotsMeleer();
			case RANGER:
				return config.safespotsRanger();
			case MAGE:
				return config.safespotsMage();
			case HEALER_JAD:
				return config.safespotsHealerJad();
			case JAD:
				return config.safespotsJad();
			default:
				return false;
		}
	}

	boolean isIndicateNpcPosition(KotoriInfernoNPC infernoNPC)
	{
		switch (infernoNPC.getType())
		{
			case BAT:
				return config.indicateNpcPositionBat();
			case BLOB:
				return config.indicateNpcPositionBlob();
			case MELEE:
				return config.indicateNpcPositionMeleer();
			case RANGER:
				return config.indicateNpcPositionRanger();
			case MAGE:
				return config.indicateNpcPositionMage();
			default:
				return false;
		}
	}
}
