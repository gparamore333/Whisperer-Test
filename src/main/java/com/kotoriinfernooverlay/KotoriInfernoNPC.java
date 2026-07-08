package com.kotoriinfernooverlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JALTOK_JAD_MAGE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JALTOK_JAD_RANGE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_AK_MAGIC_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_AK_MELEE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_AK_RANGE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_IMKOT;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_MEJRAH;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_MEJRAH_STAND;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_NIB;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_XIL_MELEE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_XIL_RANGE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_ZEK_MAGE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.JAL_ZEK_MELEE_ATTACK;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.MAGE_RESPAWN_ANIMATION;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.MELEE_BURROW_ANIMATION;
import static com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin.TZKAL_ZUK;

/**
 * Tracks a single Inferno monster's predicted attack timing and safespot state. Observation
 * only: reads NPC animations/positions/line-of-sight and exposes predictions for the overlay
 * to draw. Never sends a click, key press, or movement to the game.
 */
class KotoriInfernoNPC
{
	@Getter(AccessLevel.PACKAGE)
	private final NPC npc;
	@Getter(AccessLevel.PACKAGE)
	private final Type type;
	@Getter(AccessLevel.PACKAGE)
	private Attack nextAttack;
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private int ticksTillNextAttack;
	@Getter(AccessLevel.PACKAGE)
	private int idleTicks;
	private int lastAnimation;
	private boolean lastCanAttack;
	// 0 = not in LOS, 1 = in LOS after moving, 2 = in LOS right now
	private final Map<WorldPoint, Integer> safeSpotCache = new HashMap<>();

	KotoriInfernoNPC(NPC npc)
	{
		this.npc = npc;
		this.type = Objects.requireNonNull(Type.typeFromId(npc.getId()));
		this.nextAttack = type.getDefaultAttack();
		this.ticksTillNextAttack = 0;
		this.lastAnimation = -1;
		this.lastCanAttack = false;
		this.idleTicks = 0;
	}

	void updateNextAttack(Attack nextAttack, int ticksTillNextAttack)
	{
		this.idleTicks = 0;
		this.nextAttack = nextAttack;
		this.ticksTillNextAttack = ticksTillNextAttack;
	}

	private void updateNextAttack(Attack nextAttack)
	{
		this.nextAttack = nextAttack;
	}

	boolean canAttack(Client client, WorldPoint target)
	{
		if (safeSpotCache.containsKey(target))
		{
			return safeSpotCache.get(target) == 2;
		}

		boolean hasLos = new WorldArea(target, 1, 1).hasLineOfSightTo(client.getTopLevelWorldView(), npc.getWorldArea());
		boolean hasRange = type.getDefaultAttack() == Attack.MELEE
			? npc.getWorldArea().isInMeleeDistance(target)
			: npc.getWorldArea().distanceTo(target) <= type.getRange();

		if (hasLos && hasRange)
		{
			safeSpotCache.put(target, 2);
		}

		return hasLos && hasRange;
	}

	/**
	 * Predicts the next tile area a monster would occupy if it tried to walk toward the
	 * target, using the normal NPC travel pattern (move diagonally, then straighten out).
	 */
	private WorldArea calculateNextTravellingPoint(Client client, WorldArea travelling, WorldArea target,
		boolean stopAtMeleeDistance, Predicate<? super WorldPoint> extraCondition)
	{
		if (travelling.getPlane() != target.getPlane())
		{
			return null;
		}

		if (travelling.intersectsWith(target))
		{
			return stopAtMeleeDistance ? null : travelling;
		}

		int dx = target.getX() - travelling.getX();
		int dy = target.getY() - travelling.getY();
		Point axisDistances = getAxisDistances(travelling, target);

		if (stopAtMeleeDistance && axisDistances.getX() + axisDistances.getY() == 1)
		{
			return travelling;
		}

		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), travelling.getX(), travelling.getY());
		if (lp == null
			|| lp.getSceneX() + dx < 0 || lp.getSceneX() + dx >= Constants.SCENE_SIZE
			|| lp.getSceneY() + dy < 0 || lp.getSceneY() + dy >= Constants.SCENE_SIZE)
		{
			return null;
		}

		int dxSig = Integer.signum(dx);
		int dySig = Integer.signum(dy);

		if (stopAtMeleeDistance && axisDistances.getX() == 1 && axisDistances.getY() == 1)
		{
			if (travelling.canTravelInDirection(client.getTopLevelWorldView(), dxSig, 0, extraCondition))
			{
				return new WorldArea(travelling.getX() + dxSig, travelling.getY(), travelling.getWidth(), travelling.getHeight(), travelling.getPlane());
			}
		}
		else
		{
			if (travelling.canTravelInDirection(client.getTopLevelWorldView(), dxSig, dySig, extraCondition))
			{
				return new WorldArea(travelling.getX() + dxSig, travelling.getY() + dySig, travelling.getWidth(), travelling.getHeight(), travelling.getPlane());
			}
			else if (dx != 0 && travelling.canTravelInDirection(client.getTopLevelWorldView(), dxSig, 0, extraCondition))
			{
				return new WorldArea(travelling.getX() + dxSig, travelling.getY(), travelling.getWidth(), travelling.getHeight(), travelling.getPlane());
			}
			else if (dy != 0 && Math.max(Math.abs(dx), Math.abs(dy)) > 1
				&& travelling.canTravelInDirection(client.getTopLevelWorldView(), 0, dySig, extraCondition))
			{
				return new WorldArea(travelling.getX(), travelling.getY() + dySig, travelling.getWidth(), travelling.getHeight(), travelling.getPlane());
			}
		}

		return travelling;
	}

	private static Point getAxisDistances(WorldArea a, WorldArea b)
	{
		int aMaxX = a.getX() + a.getWidth() - 1;
		int aMaxY = a.getY() + a.getHeight() - 1;
		int bMaxX = b.getX() + b.getWidth() - 1;
		int bMaxY = b.getY() + b.getHeight() - 1;

		int dx = 0;
		if (aMaxX < b.getX())
		{
			dx = b.getX() - aMaxX;
		}
		else if (bMaxX < a.getX())
		{
			dx = a.getX() - bMaxX;
		}

		int dy = 0;
		if (aMaxY < b.getY())
		{
			dy = b.getY() - aMaxY;
		}
		else if (bMaxY < a.getY())
		{
			dy = a.getY() - bMaxY;
		}

		return new Point(dx, dy);
	}

	boolean canMoveToAttack(Client client, WorldPoint target, List<WorldPoint> obstacles)
	{
		if (safeSpotCache.containsKey(target))
		{
			int cached = safeSpotCache.get(target);
			return cached == 1 || cached == 2;
		}

		List<WorldPoint> realObstacles = new ArrayList<>();
		for (WorldPoint obstacle : obstacles)
		{
			if (!npc.getWorldArea().toWorldPointList().contains(obstacle))
			{
				realObstacles.add(obstacle);
			}
		}

		WorldArea targetArea = new WorldArea(target, 1, 1);
		WorldArea currentWorldArea = npc.getWorldArea();

		for (int steps = 0; steps < 30; steps++)
		{
			WorldArea predictedWorldArea = calculateNextTravellingPoint(client, currentWorldArea, targetArea, true, point ->
			{
				for (WorldPoint obstacle : realObstacles)
				{
					if (new WorldArea(point, 1, 1).intersectsWith(new WorldArea(obstacle, 1, 1)))
					{
						return false;
					}
				}
				return true;
			});

			if (predictedWorldArea == null)
			{
				safeSpotCache.put(target, 1);
				return true;
			}

			if (predictedWorldArea == currentWorldArea)
			{
				safeSpotCache.put(target, 0);
				return false;
			}

			boolean hasLos = targetArea.hasLineOfSightTo(client.getTopLevelWorldView(), predictedWorldArea);
			boolean hasRange = type.getDefaultAttack() == Attack.MELEE
				? predictedWorldArea.isInMeleeDistance(target)
				: predictedWorldArea.distanceTo(target) <= type.getRange();

			if (hasLos && hasRange)
			{
				safeSpotCache.put(target, 1);
				return true;
			}

			currentWorldArea = predictedWorldArea;
		}

		return false;
	}

	private boolean couldAttackPrevTick(Client client, WorldPoint lastPlayerLocation)
	{
		return new WorldArea(lastPlayerLocation, 1, 1).hasLineOfSightTo(client.getTopLevelWorldView(), npc.getWorldArea());
	}

	void gameTick(Client client, KotoriInfernoOverlayConfig config, WorldPoint lastPlayerLocation, boolean finalPhase, int ticksSinceFinalPhase)
	{
		int animation = npc.getAnimation();
		safeSpotCache.clear();
		idleTicks++;

		if (ticksTillNextAttack > 0)
		{
			ticksTillNextAttack--;
		}

		if (type == Type.JAD && animation != -1 && animation != lastAnimation)
		{
			Attack currentAttack = Attack.attackFromId(animation);
			if (currentAttack != null && currentAttack != Attack.UNKNOWN)
			{
				updateNextAttack(currentAttack, type.getTicksAfterAnimation());
			}
		}

		if (ticksTillNextAttack <= 0)
		{
			switch (type)
			{
				case ZUK:
					if (animation == TZKAL_ZUK)
					{
						if (finalPhase)
						{
							if (ticksSinceFinalPhase > 3)
							{
								updateNextAttack(type.getDefaultAttack(), 7);
							}
						}
						else
						{
							updateNextAttack(type.getDefaultAttack(), 10);
						}
					}
					break;
				case JAD:
					if (nextAttack != Attack.UNKNOWN)
					{
						if (config.sixTickJad())
						{
							// Default to the magic attack because its animation is longer than the 6-tick cycle.
							updateNextAttack(Attack.MAGIC, 6);
						}
						else
						{
							updateNextAttack(type.getDefaultAttack(), 8);
						}
					}
					break;
				case BLOB:
					if (!lastCanAttack && couldAttackPrevTick(client, lastPlayerLocation))
					{
						updateNextAttack(Attack.UNKNOWN, 3);
					}
					else if (!lastCanAttack && canAttack(client, client.getLocalPlayer().getWorldLocation()))
					{
						updateNextAttack(Attack.UNKNOWN, 4);
					}
					else if (animation != -1)
					{
						updateNextAttack(type.getDefaultAttack(), type.getTicksAfterAnimation());
					}
					break;
				case BAT:
					if (canAttack(client, client.getLocalPlayer().getWorldLocation())
						&& animation != JAL_MEJRAH_STAND && animation != -1)
					{
						updateNextAttack(type.getDefaultAttack(), type.getTicksAfterAnimation());
					}
					break;
				case MELEE:
				case RANGER:
				case MAGE:
					if (animation == JAL_IMKOT
						|| animation == JAL_XIL_RANGE_ATTACK || animation == JAL_XIL_MELEE_ATTACK
						|| animation == JAL_ZEK_MAGE_ATTACK || animation == JAL_ZEK_MELEE_ATTACK)
					{
						updateNextAttack(type.getDefaultAttack(), type.getTicksAfterAnimation());
					}
					else if (animation == MELEE_BURROW_ANIMATION)
					{
						updateNextAttack(type.getDefaultAttack(), 12);
					}
					else if (animation == MAGE_RESPAWN_ANIMATION)
					{
						updateNextAttack(type.getDefaultAttack(), 8);
					}
					break;
				default:
					if (animation != -1)
					{
						updateNextAttack(type.getDefaultAttack(), type.getTicksAfterAnimation());
					}
					break;
			}
		}

		if (type == Type.BLOB && ticksTillNextAttack == 3
			&& client.getLocalPlayer().getWorldLocation().distanceTo(npc.getWorldArea()) <= Type.BLOB.getRange())
		{
			Attack nextBlobAttack = Attack.UNKNOWN;
			if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES))
			{
				nextBlobAttack = Attack.MAGIC;
			}
			else if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
			{
				nextBlobAttack = Attack.RANGED;
			}
			updateNextAttack(nextBlobAttack);
		}

		lastAnimation = animation;
		lastCanAttack = canAttack(client, client.getLocalPlayer().getWorldLocation());
	}

	@Getter(AccessLevel.PACKAGE)
	enum Attack
	{
		MELEE(Prayer.PROTECT_FROM_MELEE, Color.ORANGE, Color.RED, new int[]{
			JAL_NIB,
			JAL_AK_MELEE_ATTACK,
			JAL_IMKOT,
			JAL_XIL_MELEE_ATTACK,
			JAL_ZEK_MELEE_ATTACK,
		}),
		RANGED(Prayer.PROTECT_FROM_MISSILES, Color.GREEN, new Color(0, 128, 0), new int[]{
			JAL_MEJRAH,
			JAL_AK_RANGE_ATTACK,
			JAL_XIL_RANGE_ATTACK,
			JALTOK_JAD_RANGE_ATTACK,
		}),
		MAGIC(Prayer.PROTECT_FROM_MAGIC, Color.CYAN, Color.BLUE, new int[]{
			JAL_AK_MAGIC_ATTACK,
			JAL_ZEK_MAGE_ATTACK,
			JALTOK_JAD_MAGE_ATTACK,
		}),
		UNKNOWN(null, Color.WHITE, Color.GRAY, new int[]{});

		private final Prayer prayer;
		private final Color normalColor;
		private final Color criticalColor;
		private final int[] animationIds;

		Attack(Prayer prayer, Color normalColor, Color criticalColor, int[] animationIds)
		{
			this.prayer = prayer;
			this.normalColor = normalColor;
			this.criticalColor = criticalColor;
			this.animationIds = animationIds;
		}

		static Attack attackFromId(int animationId)
		{
			for (Attack attack : values())
			{
				if (contains(attack.animationIds, animationId))
				{
					return attack;
				}
			}
			return null;
		}
	}

	@Getter(AccessLevel.PACKAGE)
	enum Type
	{
		NIBBLER(new int[]{NpcID.JALNIB}, Attack.MELEE, 4, 99, 100),
		BAT(new int[]{NpcID.JALMEJRAH}, Attack.RANGED, 3, 4, 7),
		BLOB(new int[]{NpcID.JALAK}, Attack.UNKNOWN, 6, 15, 4),
		MELEE(new int[]{NpcID.JALIMKOT}, Attack.MELEE, 4, 1, 3),
		RANGER(new int[]{NpcID.JALXIL, NpcID.JALXIL_7702}, Attack.RANGED, 4, 98, 2),
		MAGE(new int[]{NpcID.JALZEK, NpcID.JALZEK_7703}, Attack.MAGIC, 4, 98, 1),
		JAD(new int[]{NpcID.JALTOKJAD, NpcID.JALTOKJAD_7704, 10623}, Attack.UNKNOWN, 3, 99, 0),
		HEALER_JAD(new int[]{NpcID.YTHURKOT, NpcID.YTHURKOT_7701, NpcID.YTHURKOT_7705, 10624}, Attack.MELEE, 4, 1, 6),
		ZUK(new int[]{NpcID.TZKALZUK}, Attack.UNKNOWN, 10, 99, 99),
		HEALER_ZUK(new int[]{NpcID.JALMEJJAK, 10624}, Attack.UNKNOWN, -1, 99, 100);

		private final int[] npcIds;
		private final Attack defaultAttack;
		private final int ticksAfterAnimation;
		private final int range;
		private final int priority;

		Type(int[] npcIds, Attack defaultAttack, int ticksAfterAnimation, int range, int priority)
		{
			this.npcIds = npcIds;
			this.defaultAttack = defaultAttack;
			this.ticksAfterAnimation = ticksAfterAnimation;
			this.range = range;
			this.priority = priority;
		}

		static Type typeFromId(int npcId)
		{
			for (Type type : values())
			{
				if (contains(type.npcIds, npcId))
				{
					return type;
				}
			}
			return null;
		}
	}

	private static boolean contains(int[] values, int target)
	{
		for (int value : values)
		{
			if (value == target)
			{
				return true;
			}
		}
		return false;
	}
}
