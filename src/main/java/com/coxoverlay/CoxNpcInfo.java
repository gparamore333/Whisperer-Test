package com.coxoverlay;

import java.util.HashMap;
import java.util.Map;

import net.runelite.api.Prayer;
import net.runelite.api.gameval.NpcID;

/**
 * Maps every Chambers of Xeric combat/puzzle-room NPC ID (verified against RuneLite's own
 * gameval NpcID table) to its room, a human-readable state label, and its attack style where
 * that style is unambiguous. This is state observation only - "what is this NPC doing right
 * now" - not a predictive tick countdown, since exact attack-cycle timings for most of these
 * aren't reliably documented data.
 */
enum CoxNpcInfo
{
	TEKTON_WAITING(NpcID.RAIDS_TEKTON_WAITING, CoxRoom.TEKTON, "Waiting", null),
	TEKTON_WALKING(NpcID.RAIDS_TEKTON_WALKING_STANDARD, CoxRoom.TEKTON, "Walking back to anvil", Prayer.PROTECT_FROM_MELEE),
	TEKTON_FIGHTING(NpcID.RAIDS_TEKTON_FIGHTING_STANDARD, CoxRoom.TEKTON, "Fighting", Prayer.PROTECT_FROM_MELEE),
	TEKTON_WALKING_ENRAGED(NpcID.RAIDS_TEKTON_WALKING_ENRAGED, CoxRoom.TEKTON, "Walking back (enraged)", Prayer.PROTECT_FROM_MELEE),
	TEKTON_FIGHTING_ENRAGED(NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED, CoxRoom.TEKTON, "Fighting (enraged)", Prayer.PROTECT_FROM_MELEE),
	TEKTON_HAMMERING(NpcID.RAIDS_TEKTON_HAMMERING, CoxRoom.TEKTON, "Healing at anvil - safe to reposition", null),

	VANGUARD_DORMANT(NpcID.RAIDS_VANGUARD_DORMANT, CoxRoom.VANGUARDS, "Dormant", null),
	VANGUARD_WALKING(NpcID.RAIDS_VANGUARD_WALKING, CoxRoom.VANGUARDS, "Repositioning", null),
	VANGUARD_MELEE(NpcID.RAIDS_VANGUARD_MELEE, CoxRoom.VANGUARDS, "Melee Vanguard", Prayer.PROTECT_FROM_MELEE),
	VANGUARD_RANGED(NpcID.RAIDS_VANGUARD_RANGED, CoxRoom.VANGUARDS, "Ranged Vanguard", Prayer.PROTECT_FROM_MISSILES),
	VANGUARD_MAGIC(NpcID.RAIDS_VANGUARD_MAGIC, CoxRoom.VANGUARDS, "Magic Vanguard", Prayer.PROTECT_FROM_MAGIC),

	VESPULA_FLYING(NpcID.RAIDS_VESPULA_FLYING, CoxRoom.VESPULA, "Flying", null),
	VESPULA_ENRAGED(NpcID.RAIDS_VESPULA_ENRAGED, CoxRoom.VESPULA, "Enraged", null),
	VESPULA_WALKING(NpcID.RAIDS_VESPULA_WALKING, CoxRoom.VESPULA, "Walking", null),
	VESPULA_PORTAL(NpcID.RAIDS_VESPULA_PORTAL, CoxRoom.VESPULA, "Portal phase - use your configured quick-prayer", null),

	MUTTADILE_SUBMERGED(NpcID.RAIDS_DOGODILE_SUBMERGED, CoxRoom.MUTTADILES, "Submerged", null),
	MUTTADILE_JUNIOR(NpcID.RAIDS_DOGODILE_JUNIOR, CoxRoom.MUTTADILES, "Small Muttadile active (melee/ranged)", null),
	MUTTADILE_BIG(NpcID.RAIDS_DOGODILE, CoxRoom.MUTTADILES, "Big Muttadile active (any style + shockwave)", null),

	VASA_DORMANT(NpcID.RAIDS_VASANISTIRIO_DORMANT, CoxRoom.VASA, "Dormant", null),
	VASA_WALKING(NpcID.RAIDS_VASANISTIRIO_WALKING, CoxRoom.VASA, "Active (ranged/magic)", null),
	VASA_HEALING(NpcID.RAIDS_VASANISTIRIO_HEALING, CoxRoom.VASA, "Healing", null),
	VASA_CRYSTAL(NpcID.RAIDS_VASANISTIRIO_CRYSTAL, CoxRoom.VASA, "Glowing crystal - break it to force Vasa out", null),

	GUARDIAN_LEFT(NpcID.RAIDS_STONEGUARDIANS_LEFT, CoxRoom.GUARDIANS, "Guardian (left)", Prayer.PROTECT_FROM_MELEE),
	GUARDIAN_RIGHT(NpcID.RAIDS_STONEGUARDIANS_RIGHT, CoxRoom.GUARDIANS, "Guardian (right)", Prayer.PROTECT_FROM_MELEE),

	SHAMAN_A(NpcID.RAIDS_LIZARDSHAMAN_A, CoxRoom.SHAMANS, "Lizardman shaman", null),
	SHAMAN_B(NpcID.RAIDS_LIZARDSHAMAN_B, CoxRoom.SHAMANS, "Lizardman shaman", null),
	SHAMAN_BLOCKER(NpcID.RAIDS_LIZARDSHAMAN_BLOCKER, CoxRoom.SHAMANS, "Spawn - move away before it explodes", null),

	CRAB_GREY(NpcID.RAIDS_LASERCRABS_CRAB_GREY, CoxRoom.CRABS, "Crab (uncoloured)", null),
	CRAB_RED(NpcID.RAIDS_LASERCRABS_CRAB_RED, CoxRoom.CRABS, "Crab (red - melee)", null),
	CRAB_GREEN(NpcID.RAIDS_LASERCRABS_CRAB_GREEN, CoxRoom.CRABS, "Crab (green - ranged)", null),
	CRAB_BLUE(NpcID.RAIDS_LASERCRABS_CRAB_BLUE, CoxRoom.CRABS, "Crab (blue - magic)", null),
	CRYSTAL_WHITE(NpcID.RAIDS_LASERCRABS_ENERGY_WHITE, CoxRoom.CRABS, "Crystal (needs colour)", null),
	CRYSTAL_RED(NpcID.RAIDS_LASERCRABS_ENERGY_RED, CoxRoom.CRABS, "Crystal (red)", null),
	CRYSTAL_GREEN(NpcID.RAIDS_LASERCRABS_ENERGY_GREEN, CoxRoom.CRABS, "Crystal (green)", null),
	CRYSTAL_BLUE(NpcID.RAIDS_LASERCRABS_ENERGY_BLUE, CoxRoom.CRABS, "Crystal (blue)", null),

	ICE_DEMON_FROZEN(NpcID.RAIDS_ICEDEMON_NONCOMBAT, CoxRoom.ICE_DEMON, "Frozen - keep chopping", null),
	ICE_DEMON_THAWED(NpcID.RAIDS_ICEDEMON_COMBAT, CoxRoom.ICE_DEMON, "THAWED - attackable", null),
	ICEFIEND(NpcID.RAIDS_ICEFIEND, CoxRoom.ICE_DEMON, "Icefiend", null),

	SKELETAL_MYSTIC_A(NpcID.RAIDS_SKELETONMYSTIC_A, CoxRoom.MYSTICS, "Skeletal Mystic", Prayer.PROTECT_FROM_MAGIC),
	SKELETAL_MYSTIC_B(NpcID.RAIDS_SKELETONMYSTIC_B, CoxRoom.MYSTICS, "Skeletal Mystic", Prayer.PROTECT_FROM_MAGIC),
	SKELETAL_MYSTIC_C(NpcID.RAIDS_SKELETONMYSTIC_C, CoxRoom.MYSTICS, "Skeletal Mystic", Prayer.PROTECT_FROM_MAGIC),

	TIGHTROPE_RANGER(NpcID.RAIDS_TIGHTROPE_RANGER, CoxRoom.TIGHTROPE, "Deathly ranger", Prayer.PROTECT_FROM_MISSILES),
	TIGHTROPE_MAGE(NpcID.RAIDS_TIGHTROPE_MAGE, CoxRoom.TIGHTROPE, "Deathly mage", Prayer.PROTECT_FROM_MAGIC),

	OLM_HEAD_SPAWNING(NpcID.OLM_HEAD_SPAWNING, CoxRoom.OLM, "Head - spawning", null),
	OLM_HEAD(NpcID.OLM_HEAD, CoxRoom.OLM, "Head active", null),
	OLM_HAND_LEFT_SPAWNING(NpcID.OLM_HAND_LEFT_SPAWNING, CoxRoom.OLM, "Left hand (melee) - spawning", null),
	OLM_HAND_LEFT(NpcID.OLM_HAND_LEFT, CoxRoom.OLM, "Left hand (melee) active", Prayer.PROTECT_FROM_MELEE),
	OLM_HAND_LEFT_DYING(NpcID.OLM_HAND_LEFT_DYING, CoxRoom.OLM, "Left hand disabled", null),
	OLM_HAND_RIGHT_SPAWNING(NpcID.OLM_HAND_RIGHT_SPAWNING, CoxRoom.OLM, "Right hand (magic) - spawning", null),
	OLM_HAND_RIGHT(NpcID.OLM_HAND_RIGHT, CoxRoom.OLM, "Right hand (magic) active", Prayer.PROTECT_FROM_MAGIC),
	OLM_HAND_RIGHT_DYING(NpcID.OLM_HAND_RIGHT_DYING, CoxRoom.OLM, "Right hand disabled", null);

	private static final Map<Integer, CoxNpcInfo> BY_ID = new HashMap<>();

	static
	{
		for (CoxNpcInfo info : values())
		{
			BY_ID.put(info.npcId, info);
		}
	}

	private final int npcId;
	private final CoxRoom room;
	private final String stateLabel;
	private final Prayer prayer;

	CoxNpcInfo(int npcId, CoxRoom room, String stateLabel, Prayer prayer)
	{
		this.npcId = npcId;
		this.room = room;
		this.stateLabel = stateLabel;
		this.prayer = prayer;
	}

	CoxRoom getRoom()
	{
		return room;
	}

	String getStateLabel()
	{
		return stateLabel;
	}

	/**
	 * The protection prayer this NPC's current state calls for, or {@code null} if this NPC
	 * doesn't have a single unambiguous style (hybrid/reactive monsters - Vasa, the
	 * Muttadiles, the lizardman shamans, Vespula) or isn't currently attacking.
	 */
	Prayer getPrayer()
	{
		return prayer;
	}

	static CoxNpcInfo fromId(int npcId)
	{
		return BY_ID.get(npcId);
	}
}
