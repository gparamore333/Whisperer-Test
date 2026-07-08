package com.infernooverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Provides;
import com.infernooverlay.displaymodes.WaveDisplayMode;
import com.infernooverlay.displaymodes.ZukShieldDisplayMode;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;

/**
 * Visual-only overlays for the Inferno. This plugin never sends a click, key press, or
 * movement to the game - it only tracks monster animations/positions via events (which are
 * already visible on screen) and predicts attack timing and safespots so the overlay can
 * draw them. Prayers, attacks, and movement are always the player's own actions.
 */
@PluginDescriptor(
	name = "Inferno Overlay",
	description = "Visual-only overlays for the Inferno - no automation",
	tags = {"inferno", "overlay", "pvm", "boss"},
	enabledByDefault = false
)
public class InfernoOverlayPlugin extends Plugin
{
	private static final int INFERNO_REGION = 9043;

	// Sourced from RuneLite's gameval.AnimationID (generated from the game's own cache),
	// not hardcoded, so these track future game updates automatically. A verification pass
	// against that table also caught the ranger/mage blob animations being swapped below
	// relative to an earlier hand-copied version - fixed here.
	static final int TZKAL_ZUK_ANIMATION = AnimationID.ZUK_ATTACK;
	static final int JAL_NIB_ANIMATION = AnimationID.JALNIB_ATTACK;
	static final int NIBBLER_DESPAWN_ANIMATION = AnimationID.JALNIB_DEATH;
	static final int JAL_MEJRAH_STAND_ANIMATION = AnimationID.JALMEJRAH_READY;
	static final int JAL_MEJRAH_ANIMATION = AnimationID.JALMEJRAH_ATTACK;
	static final int JAL_AK_MAGIC_ANIMATION = AnimationID.JALAK_ATTACK_MAGIC;
	static final int JAL_AK_MELEE_ANIMATION = AnimationID.JALAK_ATTACK_MELEE;
	static final int JAL_AK_RANGE_ANIMATION = AnimationID.JALAK_ATTACK_RANGED;
	static final int JAL_IMKOT_ANIMATION = AnimationID.JALIMKOT_ATTACK;
	static final int MELEE_BURROW_ANIMATION = AnimationID.JALIMKOT_DIGDOWN;
	static final int JAL_XIL_MELEE_ANIMATION = AnimationID.JALXIL_ATTACK_MELEE;
	static final int JAL_XIL_RANGE_ANIMATION = AnimationID.JALXIL_ATTACK_RANGED;
	static final int JALTOK_JAD_MAGE_ANIMATION = AnimationID.JALTOKJAD_ATTACK_MAGIC;
	static final int JALTOK_JAD_RANGE_ANIMATION = AnimationID.JALTOKJAD_ATTACK_RANGED;
	static final int JAL_ZEK_MAGE_ANIMATION = AnimationID.JALAKXIL_ATTACK_MAGIC;
	static final int MAGE_RESPAWN_ANIMATION = AnimationID.JALAKXIL_RESURRECT;
	static final int JAL_ZEK_MELEE_ANIMATION = AnimationID.JALAKXIL_ATTACK_MELEE;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfernoOverlay infernoOverlay;

	@Inject
	private InfernoWaveOverlay waveOverlay;

	@Inject
	private InfernoPrayerCornerOverlay prayerCornerOverlay;

	@Inject
	private InfernoOverlayConfig config;

	private WorldPoint lastPlayerLocation = new WorldPoint(0, 0, 0);

	private int currentWaveNumber = -1;

	private final List<InfernoMonster> infernoMonsters = new ArrayList<>();

	private final Map<Integer, Map<InfernoMonster.Attack, Integer>> upcomingAttacks = new HashMap<>();
	private InfernoMonster.Attack closestAttack = null;

	private final List<WorldPoint> obstacles = new ArrayList<>();

	private boolean finalPhase = false;
	private boolean finalPhaseTick = false;
	private int ticksSinceFinalPhase = 0;

	private NPC zukShield = null;
	private NPC zuk = null;
	private WorldPoint zukShieldLastPosition = null;
	private int zukShieldCornerTicks = -2;
	private int zukShieldNegativeXCoord = -1;
	private int zukShieldPositiveXCoord = -1;
	private int zukShieldLastNonZeroDelta = 0;
	private int zukShieldLastDelta = 0;
	private int zukShieldTicksLeftInCorner = -1;

	private InfernoMonster centralNibbler = null;

	// 0 = safespot, 1 = pray melee, 2 = pray range, 3 = pray magic,
	// 4 = melee+range, 5 = melee+magic, 6 = range+magic, 7 = all three
	private final Map<WorldPoint, Integer> safeSpotMap = new HashMap<>();
	private final Map<Integer, List<WorldPoint>> safeSpotAreas = new HashMap<>();

	private final List<InfernoBlobDeathSpot> blobDeathSpots = new ArrayList<>();

	// Every tile a wave monster has actually been observed spawning from this attempt.
	// The Inferno reuses a small fixed set of spawn tiles across the whole fight, so this
	// fills in fast and is never a guess - only tiles a spawn has genuinely happened at.
	private final Set<WorldPoint> knownSpawnPoints = new HashSet<>();

	private long lastTickMillis;

	private InfernoSpawnTimerInfobox spawnTimerInfoBox;

	@Provides
	InfernoOverlayConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfernoOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (isInInferno())
		{
			addOverlays();
		}
	}

	@Override
	protected void shutDown()
	{
		removeOverlays();
		currentWaveNumber = -1;
		knownSpawnPoints.clear();
	}

	private void addOverlays()
	{
		overlayManager.add(infernoOverlay);
		overlayManager.add(waveOverlay);
		overlayManager.add(prayerCornerOverlay);
	}

	private void removeOverlays()
	{
		overlayManager.remove(infernoOverlay);
		overlayManager.remove(waveOverlay);
		overlayManager.remove(prayerCornerOverlay);

		if (spawnTimerInfoBox != null)
		{
			infoBoxManager.removeInfoBox(spawnTimerInfoBox);
			spawnTimerInfoBox = null;
		}
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
			infernoMonsters.clear();
			currentWaveNumber = -1;
			removeOverlays();
			zukShield = null;
			zuk = null;
			knownSpawnPoints.clear();
		}
		else if (currentWaveNumber == -1)
		{
			infernoMonsters.clear();
			currentWaveNumber = 1;
			knownSpawnPoints.clear();
			addOverlays();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isInInferno())
		{
			return;
		}

		lastTickMillis = System.currentTimeMillis();

		upcomingAttacks.clear();
		calculateUpcomingAttacks();

		closestAttack = calculateClosestAttack();

		safeSpotMap.clear();
		calculateSafespots();

		safeSpotAreas.clear();
		calculateSafespotAreas();

		obstacles.clear();
		calculateObstacles();

		centralNibbler = calculateCentralNibbler();

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

		lastPlayerLocation = client.getLocalPlayer().getWorldLocation();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isInInferno())
		{
			return;
		}

		String message = event.getMessage();
		int waveIndex = message.indexOf("Wave:");
		if (waveIndex != -1)
		{
			String afterColon = message.substring(waveIndex + "Wave: ".length());
			int tagIndex = afterColon.indexOf('<');
			if (tagIndex != -1)
			{
				try
				{
					currentWaveNumber = Integer.parseInt(afterColon.substring(0, tagIndex));
				}
				catch (NumberFormatException ignored)
				{
					// message format didn't match what we expected; leave the wave number as-is
				}
			}
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

		InfernoMonster.Type type = InfernoMonster.Type.typeFromId(npcId);
		if (type == null)
		{
			return;
		}

		if (isWaveSpawnType(type))
		{
			knownSpawnPoints.add(WorldPoint.fromLocalInstance(client, npc.getLocalLocation()));
		}

		switch (type)
		{
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
				zuk = npc;

				if (config.spawnTimerInfobox())
				{
					if (spawnTimerInfoBox != null)
					{
						infoBoxManager.removeInfoBox(spawnTimerInfoBox);
					}
					spawnTimerInfoBox = new InfernoSpawnTimerInfobox(itemManager.getImage(ItemID.TZREKZUK), this);
					infoBoxManager.addInfoBox(spawnTimerInfoBox);
				}
				break;
			case HEALER_ZUK:
				finalPhase = true;
				ticksSinceFinalPhase = 1;
				finalPhaseTick = true;
				for (InfernoMonster monster : infernoMonsters)
				{
					if (monster.getType() == InfernoMonster.Type.ZUK)
					{
						monster.setTicksTillNextAttack(-1);
					}
				}
				break;
			default:
				break;
		}

		infernoMonsters.add(0, new InfernoMonster(npc));
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
				spawnTimerInfoBox = null;
			}
		}

		infernoMonsters.removeIf(monster -> monster.getNpc() == npc);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!isInInferno() || !(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		int animation = npc.getAnimation();

		if (InfernoMonster.Type.typeFromId(npc.getId()) == InfernoMonster.Type.NIBBLER && animation == NIBBLER_DESPAWN_ANIMATION)
		{
			infernoMonsters.removeIf(monster -> monster.getNpc() == npc);
			return;
		}

		if (config.indicateBlobDeathLocation()
			&& InfernoMonster.Type.typeFromId(npc.getId()) == InfernoMonster.Type.BLOB
			&& animation == InfernoBlobDeathSpot.BLOB_DEATH_ANIMATION)
		{
			infernoMonsters.removeIf(monster -> monster.getNpc() == npc);
			blobDeathSpots.add(new InfernoBlobDeathSpot(npc.getLocalLocation()));
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

	int getCurrentWaveNumber()
	{
		return currentWaveNumber;
	}

	List<InfernoMonster> getInfernoMonsters()
	{
		return infernoMonsters;
	}

	Map<Integer, Map<InfernoMonster.Attack, Integer>> getUpcomingAttacks()
	{
		return upcomingAttacks;
	}

	InfernoMonster.Attack getClosestAttack()
	{
		return closestAttack;
	}

	List<WorldPoint> getObstacles()
	{
		return obstacles;
	}

	NPC getZukShield()
	{
		return zukShield;
	}

	InfernoMonster getCentralNibbler()
	{
		return centralNibbler;
	}

	Map<WorldPoint, Integer> getSafeSpotMap()
	{
		return safeSpotMap;
	}

	Map<Integer, List<WorldPoint>> getSafeSpotAreas()
	{
		return safeSpotAreas;
	}

	List<InfernoBlobDeathSpot> getBlobDeathSpots()
	{
		return blobDeathSpots;
	}

	Set<WorldPoint> getKnownSpawnPoints()
	{
		return knownSpawnPoints;
	}

	long getLastTickMillis()
	{
		return lastTickMillis;
	}

	private void calculateUpcomingAttacks()
	{
		for (InfernoMonster monster : infernoMonsters)
		{
			monster.gameTick(client, lastPlayerLocation, finalPhase, ticksSinceFinalPhase);

			if (monster.getType() == InfernoMonster.Type.ZUK && zukShieldCornerTicks == -1)
			{
				monster.setTicksTillNextAttack(12);
				zukShieldCornerTicks = 0;
			}

			if ((monster.getType() == InfernoMonster.Type.RANGER || monster.getType() == InfernoMonster.Type.MAGE)
				&& isDead(monster.getNpc()))
			{
				continue;
			}

			if (monster.getTicksTillNextAttack() <= 0 || !isPrayerHelper(monster))
			{
				continue;
			}

			boolean isBlobDetectionTick = config.indicateBlobDetectionTick()
				&& monster.getType() == InfernoMonster.Type.BLOB
				&& monster.getTicksTillNextAttack() >= 4;

			if (monster.getNextAttack() == InfernoMonster.Attack.UNKNOWN && !isBlobDetectionTick)
			{
				continue;
			}

			int tick = monster.getTicksTillNextAttack();
			upcomingAttacks.computeIfAbsent(tick, k -> new HashMap<>());

			if (isBlobDetectionTick)
			{
				addBlobDetectionAttack(tick);
			}
			else
			{
				InfernoMonster.Attack attack = monster.getNextAttack();
				int priority = monster.getType().getPriority();

				if (!upcomingAttacks.get(tick).containsKey(attack) || upcomingAttacks.get(tick).get(attack) > priority)
				{
					upcomingAttacks.get(tick).put(attack, priority);
				}
			}
		}
	}

	private void addBlobDetectionAttack(int attackTick)
	{
		int detectionTick = attackTick - 3;
		upcomingAttacks.computeIfAbsent(detectionTick, k -> new HashMap<>());
		upcomingAttacks.computeIfAbsent(attackTick - 4, k -> new HashMap<>());

		Map<InfernoMonster.Attack, Integer> detectionTickAttacks = upcomingAttacks.get(detectionTick);
		int blobPriority = InfernoMonster.Type.BLOB.getPriority();

		if (detectionTickAttacks.containsKey(InfernoMonster.Attack.MAGIC))
		{
			if (detectionTickAttacks.get(InfernoMonster.Attack.MAGIC) > blobPriority)
			{
				detectionTickAttacks.put(InfernoMonster.Attack.MAGIC, blobPriority);
			}
		}
		else if (detectionTickAttacks.containsKey(InfernoMonster.Attack.RANGED))
		{
			if (detectionTickAttacks.get(InfernoMonster.Attack.RANGED) > blobPriority)
			{
				detectionTickAttacks.put(InfernoMonster.Attack.RANGED, blobPriority);
			}
		}
		else if (upcomingAttacks.get(attackTick).containsKey(InfernoMonster.Attack.MAGIC)
			|| upcomingAttacks.get(attackTick - 4).containsKey(InfernoMonster.Attack.MAGIC))
		{
			if (!detectionTickAttacks.containsKey(InfernoMonster.Attack.RANGED)
				|| detectionTickAttacks.get(InfernoMonster.Attack.RANGED) > blobPriority)
			{
				detectionTickAttacks.put(InfernoMonster.Attack.RANGED, blobPriority);
			}
		}
		else if (upcomingAttacks.get(attackTick).containsKey(InfernoMonster.Attack.RANGED)
			|| upcomingAttacks.get(attackTick - 4).containsKey(InfernoMonster.Attack.RANGED))
		{
			if (!detectionTickAttacks.containsKey(InfernoMonster.Attack.MAGIC)
				|| detectionTickAttacks.get(InfernoMonster.Attack.MAGIC) > blobPriority)
			{
				detectionTickAttacks.put(InfernoMonster.Attack.MAGIC, blobPriority);
			}
		}
		else
		{
			detectionTickAttacks.put(InfernoMonster.Attack.MAGIC, blobPriority);
		}
	}

	private InfernoMonster.Attack calculateClosestAttack()
	{
		InfernoMonster.Attack best = null;
		int bestTick = 999;
		int bestPriority = 999;

		for (Map.Entry<Integer, Map<InfernoMonster.Attack, Integer>> tickEntry : upcomingAttacks.entrySet())
		{
			for (Map.Entry<InfernoMonster.Attack, Integer> attackEntry : tickEntry.getValue().entrySet())
			{
				int tick = tickEntry.getKey();
				int priority = attackEntry.getValue();
				if (tick < bestTick || (tick == bestTick && priority < bestPriority))
				{
					best = attackEntry.getKey();
					bestPriority = priority;
					bestTick = tick;
				}
			}
		}

		return best;
	}

	private void calculateSafespots()
	{
		if (currentWaveNumber < 69)
		{
			if (config.safespotDisplayMode() != com.infernooverlay.displaymodes.SafespotDisplayMode.OFF)
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

				for (InfernoMonster monster : infernoMonsters)
				{
					if (!isNormalSafespots(monster))
					{
						continue;
					}

					safeSpotMap.putIfAbsent(checkLoc, 0);

					if (monster.canAttack(client, checkLoc) || monster.canMoveToAttack(client, checkLoc, obstacles))
					{
						markUnsafe(checkLoc, monster);
					}
				}
			}
		}
	}

	private void markUnsafe(WorldPoint checkLoc, InfernoMonster monster)
	{
		InfernoMonster.Type type = monster.getType();
		int current = safeSpotMap.get(checkLoc);

		if (type.getDefaultAttack() == InfernoMonster.Attack.MELEE)
		{
			current = addUnsafeBit(current, 1, 2, 4, 3, 5, 6, 7);
		}

		if (type.getDefaultAttack() == InfernoMonster.Attack.MAGIC
			|| (type == InfernoMonster.Type.BLOB && current != 2 && current != 4))
		{
			current = addUnsafeBit(current, 3, 1, 5, 2, 6, 5, 7);
		}

		if (type.getDefaultAttack() == InfernoMonster.Attack.RANGED
			|| (type == InfernoMonster.Type.BLOB && current != 3 && current != 5))
		{
			current = addUnsafeBit(current, 2, 1, 4, 3, 6, 4, 7);
		}

		if (type == InfernoMonster.Type.JAD && monster.getNpc().getWorldArea().isInMeleeDistance(checkLoc))
		{
			current = addUnsafeBit(current, 1, 2, 4, 3, 5, 6, 7);
		}

		safeSpotMap.put(checkLoc, current);
	}

	/**
	 * Safespot codes combine like a bitmask over {melee=1, range=2, magic=3}: adding a
	 * danger to a tile moves it along a fixed lattice (0-&gt;single, single-&gt;pair, pair-&gt;7).
	 * The from/to pairs below mirror that lattice for one danger type at a time.
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
		WorldPoint shieldPosition = zukShield.getWorldLocation();

		if (zukShieldLastPosition != null && zukShieldLastPosition.getX() != shieldPosition.getX() && zukShieldCornerTicks == -2)
		{
			zukShieldCornerTicks = -1;
		}

		if (zukShieldLastPosition != null)
		{
			int delta = shieldPosition.getX() - zukShieldLastPosition.getX();

			if (delta != 0)
			{
				zukShieldLastNonZeroDelta = delta;
			}

			if (zukShieldLastDelta == 0 && delta != 0)
			{
				zukShieldTicksLeftInCorner = 4;
			}

			if (delta == 0)
			{
				if (zukShieldLastNonZeroDelta > 0)
				{
					zukShieldPositiveXCoord = shieldPosition.getX();
				}
				else if (zukShieldLastNonZeroDelta < 0)
				{
					zukShieldNegativeXCoord = shieldPosition.getX();
				}

				if (zukShieldTicksLeftInCorner > 0)
				{
					zukShieldTicksLeftInCorner--;
				}
			}

			zukShieldLastDelta = delta;
		}

		zukShieldLastPosition = shieldPosition;

		if (config.safespotDisplayMode() == com.infernooverlay.displaymodes.SafespotDisplayMode.OFF)
		{
			return;
		}

		ZukShieldDisplayMode mode = finalPhase ? config.safespotsZukShieldAfterHealers() : config.safespotsZukShieldBeforeHealers();

		if (mode == ZukShieldDisplayMode.LIVE || mode == ZukShieldDisplayMode.LIVE_PLUS_PREDICT)
		{
			drawZukSafespot(shieldPosition.getX(), shieldPosition.getY(), 0);
		}

		if (mode == ZukShieldDisplayMode.PREDICT || mode == ZukShieldDisplayMode.LIVE_PLUS_PREDICT)
		{
			drawZukPredictedSafespot();
		}
	}

	private void drawZukPredictedSafespot()
	{
		WorldPoint shieldPosition = zukShield.getWorldLocation();

		if (zukShieldPositiveXCoord == -1 || zukShieldNegativeXCoord == -1)
		{
			return;
		}

		int nextShieldXCoord = shieldPosition.getX();

		for (InfernoMonster monster : infernoMonsters)
		{
			if (monster.getType() != InfernoMonster.Type.ZUK)
			{
				continue;
			}

			int ticksTilAttack = finalPhase ? monster.getTicksTillNextAttack() : monster.getTicksTillNextAttack() - 1;

			if (ticksTilAttack < 1)
			{
				if (finalPhase)
				{
					return;
				}
				ticksTilAttack = 10;
			}

			if (zukShieldLastNonZeroDelta > 0)
			{
				nextShieldXCoord += ticksTilAttack;
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
				nextShieldXCoord -= ticksTilAttack;
				if (nextShieldXCoord < zukShieldNegativeXCoord)
				{
					nextShieldXCoord += zukShieldTicksLeftInCorner;
					nextShieldXCoord = nextShieldXCoord >= zukShieldNegativeXCoord
						? zukShieldNegativeXCoord
						: zukShieldNegativeXCoord - nextShieldXCoord + zukShieldNegativeXCoord;
				}
			}
		}

		drawZukSafespot(nextShieldXCoord, shieldPosition.getY(), 2);
	}

	private void drawZukSafespot(int xCoord, int yCoord, int colorId)
	{
		for (int x = xCoord - 1; x <= xCoord + 3; x++)
		{
			for (int y = yCoord - 4; y <= yCoord - 2; y++)
			{
				safeSpotMap.put(new WorldPoint(x, y, client.getTopLevelWorldView().getPlane()), colorId);
			}
		}
	}

	private void calculateSafespotAreas()
	{
		if (config.safespotDisplayMode() != com.infernooverlay.displaymodes.SafespotDisplayMode.AREA)
		{
			return;
		}

		for (Map.Entry<WorldPoint, Integer> entry : safeSpotMap.entrySet())
		{
			safeSpotAreas.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
		}
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
		if (!config.indicateBlobDeathLocation())
		{
			return;
		}
		blobDeathSpots.forEach(InfernoBlobDeathSpot::decrementTick);
		blobDeathSpots.removeIf(InfernoBlobDeathSpot::isDone);
	}

	private InfernoMonster calculateCentralNibbler()
	{
		InfernoMonster best = null;
		int bestAmountInArea = 0;
		int bestDistanceToPlayer = 999;

		for (InfernoMonster monster : infernoMonsters)
		{
			if (monster.getType() != InfernoMonster.Type.NIBBLER)
			{
				continue;
			}

			int amountInArea = 0;
			int distanceToPlayer = monster.getNpc().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());

			for (InfernoMonster other : infernoMonsters)
			{
				if (other.getType() == InfernoMonster.Type.NIBBLER
					&& other.getNpc().getWorldArea().distanceTo(monster.getNpc().getWorldArea()) <= 1)
				{
					amountInArea++;
				}
			}

			if (amountInArea > bestAmountInArea || (amountInArea == bestAmountInArea && distanceToPlayer < bestDistanceToPlayer))
			{
				best = monster;
				bestAmountInArea = amountInArea;
				bestDistanceToPlayer = distanceToPlayer;
			}
		}

		return best;
	}

	private void calculateSpawnTimerInfobox()
	{
		if (zuk == null || finalPhase || spawnTimerInfoBox == null)
		{
			return;
		}

		int pauseHp = 600;
		int resumeHp = 480;
		int hp = calculateNpcHp(zuk.getHealthRatio(), zuk.getHealthScale(), 1200);

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
		if (ratio < 0 || health <= 0)
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

	private static boolean isDead(NPC npc)
	{
		return npc.getHealthRatio() == 0;
	}

	private static boolean isWaveSpawnType(InfernoMonster.Type type)
	{
		switch (type)
		{
			case NIBBLER:
			case BAT:
			case BLOB:
			case MELEE:
			case RANGER:
			case MAGE:
			case JAD:
				return true;
			default:
				return false;
		}
	}

	private boolean isPrayerHelper(InfernoMonster monster)
	{
		switch (monster.getType())
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

	boolean isTicksOnNpc(InfernoMonster monster)
	{
		switch (monster.getType())
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

	boolean isNormalSafespots(InfernoMonster monster)
	{
		switch (monster.getType())
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

	boolean isIndicateNpcPosition(InfernoMonster monster)
	{
		switch (monster.getType())
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
