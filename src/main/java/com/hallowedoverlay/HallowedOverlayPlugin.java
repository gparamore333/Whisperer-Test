package com.hallowedoverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * Visual-only overlays for the Hallowed Sepulchre. Reads NPC/object/animation state (via
 * RuneLite's current gameval ID tables) and draws indicators - never a click, key press, or
 * movement. Ported from OreoCupcakes' kotori-plugins {@code hallowedhelper}, re-verified ID by
 * ID against current gameval tables (nothing was found stale) and the OSRS Wiki. The original's
 * fragile, hand-tuned "predict the whole room's fire pattern from memorized tile grids" system
 * was replaced with live per-statue animation tracking - see {@link HallowedWizardStatue} and
 * the README for why. Two cosmetic client-side animation-override toggles ("GlitchyHit"/
 * "GlitchyGrapple") were dropped entirely - see README.
 */
@PluginDescriptor(
	name = "Hallowed Sepulchre Overlay",
	description = "Visual-only Hallowed Sepulchre overlays - no automation",
	tags = {"hallowed", "sepulchre", "overlay", "thieving", "agility", "minigame"},
	enabledByDefault = false
)
public class HallowedOverlayPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HallowedOverlay overlay;

	@Inject
	private HallowedOverlayConfig config;

	private static final Set<Integer> SEPULCHRE_REGION_IDS = Set.of(
		8794, 8795, 8796, 8797, 8798,
		9050, 9051, 9052, 9053, 9054,
		9306, 9307, 9308, 9309, 9310,
		9562, 9563, 9564, 9565, 9566,
		9818, 9819, 9820, 9821, 9822,
		10074, 10075, 10076, 10077, 10078,
		10330, 10331, 10332, 10333, 10334
	);

	// T3 fire-trap statues cycle 1 tick faster than the rest, per the wiki's own line about
	// floor 5 ("Flame statues on this floor change phases 1 tick faster than on floors 1-4").
	private static final int FIRE_CYCLE_TICKS_T3 = 2;
	private static final int FIRE_CYCLE_TICKS_DEFAULT = 3;

	private static final Set<Integer> WIZARD_STATUE_2TICK_IDS = Set.of(
		ObjectID.HALLOWED_FIRE_TRAP_T3,
		ObjectID.HALLOWED_FIRE_TRAP_T3_V2,
		ObjectID.HALLOWED_FIRE_TRAP_T3_V3,
		ObjectID.HALLOWED_FIRE_TRAP_T3_V4,
		ObjectID.HALLOWED_FIRE_TRAP_T3_V5
	);

	private static final Set<Integer> WIZARD_STATUE_3TICK_IDS = Set.of(
		ObjectID.HALLOWED_FIRE_TRAP,
		ObjectID.HALLOWED_FIRE_TRAP_V2,
		ObjectID.HALLOWED_FIRE_TRAP_V3,
		ObjectID.HALLOWED_FIRE_TRAP_V4,
		ObjectID.HALLOWED_FIRE_TRAP_V5,
		ObjectID.HALLOWED_FIRE_TRAP_V6,
		ObjectID.HALLOWED_FIRE_TRAP_V7,
		ObjectID.HALLOWED_FIRE_TRAP_T2,
		ObjectID.HALLOWED_FIRE_TRAP_T2_V2,
		ObjectID.HALLOWED_FIRE_TRAP_T2_V3,
		ObjectID.HALLOWED_FIRE_TRAP_T2_V4,
		ObjectID.HALLOWED_FIRE_TRAP_T2_V5
	);

	private static final Set<Integer> SWORD_STATUE_IDS = Set.of(
		ObjectID.HALLOWED_SWORD_TRAP,
		ObjectID.HALLOWED_SWORD_TRAP_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_OWL,
		ObjectID.HALLOWED_SWORD_TRAP_OWL_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_LION,
		ObjectID.HALLOWED_SWORD_TRAP_LION_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_UNICORN,
		ObjectID.HALLOWED_SWORD_TRAP_UNICORN_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_T2,
		ObjectID.HALLOWED_SWORD_TRAP_T2_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_T2_LION,
		ObjectID.HALLOWED_SWORD_TRAP_T2_LION_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_T3,
		ObjectID.HALLOWED_SWORD_TRAP_T3_THROWN,
		ObjectID.HALLOWED_SWORD_TRAP_T3_LION,
		ObjectID.HALLOWED_SWORD_TRAP_T3_LION_THROWN
	);

	private static final Set<Integer> CROSSBOW_STATUE_IDS = Set.of(
		ObjectID.HALLOWED_PROJECTILE_TRAP,
		ObjectID.HALLOWED_PROJECTILE_TRAP_T2,
		ObjectID.HALLOWED_PROJECTILE_TRAP_T3
	);

	private static final Set<Integer> SWORD_NPC_IDS = Set.of(
		NpcID.HALLOWED_SWORD_NPC,
		NpcID.HALLOWED_SWORD_NPC_T2,
		NpcID.HALLOWED_SWORD_NPC_T3
	);

	private static final Set<Integer> ARROW_NPC_IDS = Set.of(
		NpcID.HALLOWED_PROJECTILE_NPC,
		NpcID.HALLOWED_PROJECTILE_NPC_T2,
		NpcID.HALLOWED_PROJECTILE_NPC_T3
	);

	private static final Set<Integer> CHEST_SPAWN_IDS = Set.of(
		ObjectID.HALLOWED_REWARD_COFFIN_A,
		ObjectID.HALLOWED_REWARD_COFFIN_B,
		ObjectID.HALLOWED_F5_GRAPPLE_COFFIN,
		ObjectID.HALLOWED_F5_PORTAL_COFFIN,
		ObjectID.HALLOWED_F5_BRIDGE_COFFIN,
		ObjectID.HALLOWED_FINAL_CHEST
	);

	private static final Set<Integer> CHEST_CLOSED_IDS = Set.of(
		ObjectID.HALLOWED_REWARD_COFFIN_UNLOOTED,
		ObjectID.HALLOWED_F5_GRAPPLE_COFFIN_UNLOOTED,
		ObjectID.HALLOWED_F5_PORTAL_COFFIN_UNLOOTED,
		ObjectID.HALLOWED_F5_BRIDGE_COFFIN_UNLOOTED,
		ObjectID.HALLOWED_FINAL_CHEST_UNLOOTED
	);

	private static final int BRIDGE_ID = ObjectID.HALLOWED_CONSTRUCTION_MULTI;
	private static final int BRIDGE_BUILT_ID = ObjectID.HALLOWED_TREASURE_CONSTRUCTION_FINAL;
	private static final int PORTAL_ID = ObjectID.HALLOWED_MAGIC_MULTI;
	private static final int PORTAL_BUILT_ID = ObjectID.HALLOWED_TREASURE_MAGIC_FINAL;

	private static final Set<Integer> STAIRS_IDS = Set.of(
		ObjectID.HALLOWED_FLOOR_1_NORTHPATH_DROP,
		ObjectID.HALLOWED_FLOOR_1_SOUTHPATH_STAIRS,
		ObjectID.HALLOWED_FLOOR_1_WESTPATH_DROP,
		ObjectID.HALLOWED_FLOOR_2_NORTHPATH_STAIRS,
		ObjectID.HALLOWED_FLOOR_2_EASTPATH_STAIRS,
		ObjectID.HALLOWED_FLOOR_2_SOUTHPATH_STAIRS,
		ObjectID.HALLOWED_FLOOR_2_WESTPATH_DROP,
		ObjectID.HALLOWED_FLOOR_3_EASTPATH_DROP,
		ObjectID.HALLOWED_FLOOR_3_WESTPATH_DROP
	);

	// Jagex's current gameval name for these is "STAIRS_FLOORx", not "gate" - these are the
	// barrier objects tied to the "door to the next floor closes" chat message, matching
	// exactly how the legacy plugin used them as the floor-advance gate.
	private static final Set<Integer> FLOOR_GATE_IDS = Set.of(
		ObjectID.HALLOWED_STAIRS_FLOOR1,
		ObjectID.HALLOWED_STAIRS_FLOOR2,
		ObjectID.HALLOWED_STAIRS_FLOOR3,
		ObjectID.HALLOWED_STAIRS_FLOOR4
	);

	private static final int LIGHTNING_SPOTANIM_ID = SpotanimID.HALLOWED_STATUE_LIGHTNING_START_STRIKE;
	private static final int BLUE_PORTAL_SPOTANIM_ID = SpotanimID.HALLOWED_TELEPAD_READY_FORWARD;
	private static final int YELLOW_PORTAL_SPOTANIM_ID = SpotanimID.HALLOWED_TELEPAD_READY_BACKWARD;

	private static final int SWORD_THROW_ANIMATION = AnimationID.HALLOWED_STATUE_SWORD_START_ATTACK;
	private static final int CROSSBOW_IDLE_ANIMATION = AnimationID.HALLOWED_STATUE_PROJECTILE_IDLE;
	private static final int CROSSBOW_FINISH_ANIMATION = AnimationID.HALLOWED_STATUE_PROJECTILE_FINISH_ATTACK;

	// The lockpicking-stage animation IDs read off the LOCAL PLAYER (not the coffin itself) -
	// that's how the legacy plugin identified opening progress too. HUMAN_PICKLOCK_CAGEDOOR and
	// HALLOWED_COFFIN_OPEN are unambiguous; the middle-stage animation's current gameval name
	// (NTK_TOMB_PUSH_LOOP) doesn't mention Hallowed Sepulchre at all - Jagex reuses generic
	// interaction animations across unrelated content, so the ID is confirmed to still exist,
	// just not confirmed to be semantically correct for this exact use.
	private static final int CHEST_LOCKPICK_ANIMATION = AnimationID.HUMAN_PICKLOCK_CAGEDOOR;
	private static final int CHEST_OPENING_ANIMATION = AnimationID.NTK_TOMB_PUSH_LOOP;
	private static final int CHEST_OPEN_ANIMATION = AnimationID.HALLOWED_COFFIN_OPEN;
	// Same caveat as above, stronger: this spotanim's current gameval name is
	// "MORTMYRE_SWAMPSTENCH" - a Mort Myre Swamp effect, not anything Hallowed-Sepulchre-named.
	// It's plausible Jagex reuses a generic poison-gas graphic across unrelated content (as
	// they do for many particle effects), which is what the legacy plugin assumed, but this
	// is unverified - test in-game before trusting it.
	private static final int CHEST_FAIL_SPOTANIM = SpotanimID.MORTMYRE_SWAMPSTENCH;

	@Getter(AccessLevel.PACKAGE)
	private boolean playerInSepulchre;

	@Getter(AccessLevel.PACKAGE)
	private boolean doorOpen = true;

	@Getter(AccessLevel.PACKAGE)
	private final Set<NPC> swords = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<NPC> arrows = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> swordStatues = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> crossbowStatues = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<HallowedWizardStatue> wizardStatues = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> chests = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> endPortals = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GroundObject> bridges = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> stairs = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GameObject> floorGates = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> lightningTiles = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private int ticksSinceLightning = 100;

	@Getter(AccessLevel.PACKAGE)
	private final Map<LocalPoint, HallowedTeleportPortal> bluePortals = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<LocalPoint, HallowedTeleportPortal> yellowPortals = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private int localPlayerAnimation = -1;

	@Provides
	HallowedOverlayConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HallowedOverlayConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (isInSepulchre())
		{
			enter();
		}
	}

	@Override
	protected void shutDown()
	{
		leave();
	}

	private boolean isInSepulchre()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		return SEPULCHRE_REGION_IDS.contains(
			WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID());
	}

	private void enter()
	{
		playerInSepulchre = true;
		doorOpen = true;
		overlayManager.add(overlay);
	}

	private void leave()
	{
		playerInSepulchre = false;
		overlayManager.remove(overlay);
		swords.clear();
		arrows.clear();
		swordStatues.clear();
		crossbowStatues.clear();
		wizardStatues.clear();
		chests.clear();
		endPortals.clear();
		bridges.clear();
		stairs.clear();
		floorGates.clear();
		lightningTiles.clear();
		ticksSinceLightning = 100;
		bluePortals.clear();
		yellowPortals.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			if (playerInSepulchre)
			{
				leave();
			}
			return;
		}

		boolean inSepulchre = isInSepulchre();
		if (inSepulchre && !playerInSepulchre)
		{
			enter();
		}
		else if (!inSepulchre && playerInSepulchre)
		{
			leave();
		}
	}

	private static final String MESSAGE_DOOR_CLOSES = "<col=ef1020>You hear a loud rumbling noise as the door to the next floor closes.";
	private static final String MESSAGE_BARRIER_ACTIVATES = "<col=ef1020>You hear the sound of a magical barrier activating.";
	private static final String MESSAGE_ENTER_LOBBY_1 = "You make your way back to the lobby of the Hallowed Sepulchre.";
	private static final String MESSAGE_ENTER_LOBBY_2 = "The obelisk teleports you back to the lobby of the Hallowed Sepulchre.";
	private static final String MESSAGE_ENTER_SEPULCHRE_1 = "You venture further down into the Hallowed Sepulchre.";
	private static final String MESSAGE_ENTER_SEPULCHRE_2 = "You venture down into the Hallowed Sepulchre.";

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!playerInSepulchre || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();
		if (message.equals(MESSAGE_DOOR_CLOSES) || message.equals(MESSAGE_BARRIER_ACTIVATES))
		{
			doorOpen = false;
		}
		else if (message.equals(MESSAGE_ENTER_LOBBY_1) || message.equals(MESSAGE_ENTER_LOBBY_2))
		{
			leave();
		}
		else if (message.equals(MESSAGE_ENTER_SEPULCHRE_1) || message.equals(MESSAGE_ENTER_SEPULCHRE_2))
		{
			doorOpen = true;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!playerInSepulchre)
		{
			return;
		}

		NPC npc = event.getNpc();
		int id = npc.getId();
		if (SWORD_NPC_IDS.contains(id))
		{
			swords.add(npc);
		}
		else if (ARROW_NPC_IDS.contains(id))
		{
			arrows.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		swords.remove(event.getNpc());
		arrows.remove(event.getNpc());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!playerInSepulchre)
		{
			return;
		}

		GameObject gameObject = event.getGameObject();
		int id = gameObject.getId();

		if (WIZARD_STATUE_2TICK_IDS.contains(id))
		{
			wizardStatues.add(new HallowedWizardStatue(gameObject, FIRE_CYCLE_TICKS_T3));
		}
		else if (WIZARD_STATUE_3TICK_IDS.contains(id))
		{
			wizardStatues.add(new HallowedWizardStatue(gameObject, FIRE_CYCLE_TICKS_DEFAULT));
		}
		else if (SWORD_STATUE_IDS.contains(id))
		{
			swordStatues.add(gameObject);
		}
		else if (CROSSBOW_STATUE_IDS.contains(id))
		{
			crossbowStatues.add(gameObject);
		}
		else if (CHEST_SPAWN_IDS.contains(id))
		{
			chests.add(gameObject);
		}
		else if (id == PORTAL_ID || id == PORTAL_BUILT_ID)
		{
			endPortals.add(gameObject);
		}
		else if (STAIRS_IDS.contains(id))
		{
			stairs.add(gameObject);
		}
		else if (FLOOR_GATE_IDS.contains(id))
		{
			floorGates.add(gameObject);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		wizardStatues.removeIf(statue -> statue.getGameObject() == gameObject);
		swordStatues.remove(gameObject);
		crossbowStatues.remove(gameObject);
		chests.remove(gameObject);
		endPortals.remove(gameObject);
		stairs.remove(gameObject);
		floorGates.remove(gameObject);
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (!playerInSepulchre)
		{
			return;
		}

		GroundObject groundObject = event.getGroundObject();
		if (groundObject.getId() == BRIDGE_ID || groundObject.getId() == BRIDGE_BUILT_ID)
		{
			bridges.add(groundObject);
		}
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		bridges.remove(event.getGroundObject());
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (!playerInSepulchre)
		{
			return;
		}

		GraphicsObject graphicsObject = event.getGraphicsObject();
		int id = graphicsObject.getId();
		LocalPoint location = graphicsObject.getLocation();

		if (id == LIGHTNING_SPOTANIM_ID)
		{
			// A single strike spawns several of these graphics in the same tick (the wiki's
			// "3x3 chequerboard pattern") - only clear the previous batch when this is the
			// first tile of a genuinely new strike, not a sibling tile of the current one.
			if (ticksSinceLightning > 0)
			{
				lightningTiles.clear();
			}
			ticksSinceLightning = 0;
			WorldPoint worldPoint = WorldPoint.fromLocal(client, location);
			if (!lightningTiles.contains(worldPoint))
			{
				lightningTiles.add(worldPoint);
			}
		}
		else if (id == BLUE_PORTAL_SPOTANIM_ID)
		{
			bluePortals.put(location, new HallowedTeleportPortal(graphicsObject));
		}
		else if (id == YELLOW_PORTAL_SPOTANIM_ID)
		{
			yellowPortals.put(location, new HallowedTeleportPortal(graphicsObject));
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!playerInSepulchre || event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		localPlayerAnimation = client.getLocalPlayer().getAnimation();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!playerInSepulchre)
		{
			return;
		}

		for (HallowedWizardStatue statue : wizardStatues)
		{
			statue.updateTick();
		}

		ticksSinceLightning++;

		bluePortals.values().forEach(HallowedTeleportPortal::decrementTicksUntilDespawn);
		bluePortals.values().removeIf(portal -> portal.getTicksUntilDespawn() <= 0);
		yellowPortals.values().forEach(HallowedTeleportPortal::decrementTicksUntilDespawn);
		yellowPortals.values().removeIf(portal -> portal.getTicksUntilDespawn() <= 0);
	}

	boolean isChestClosed(GameObject chest)
	{
		ObjectComposition composition = client.getObjectDefinition(chest.getId());
		ObjectComposition impostor = composition.getImpostor();
		int resolvedId = impostor != null ? impostor.getId() : composition.getId();
		return CHEST_CLOSED_IDS.contains(resolvedId);
	}

	boolean isPortalBuilt(GameObject portal)
	{
		ObjectComposition composition = client.getObjectDefinition(portal.getId());
		ObjectComposition impostor = composition.getImpostor();
		int resolvedId = impostor != null ? impostor.getId() : composition.getId();
		return resolvedId == PORTAL_BUILT_ID;
	}

	boolean isBridgeBuilt(GroundObject bridge)
	{
		ObjectComposition composition = client.getObjectDefinition(bridge.getId());
		ObjectComposition impostor = composition.getImpostor();
		int resolvedId = impostor != null ? impostor.getId() : composition.getId();
		return resolvedId == BRIDGE_BUILT_ID;
	}

	boolean isSwordStatueThrowing(GameObject statue)
	{
		return getObjectAnimation(statue) == SWORD_THROW_ANIMATION;
	}

	boolean isCrossbowStatueFiring(GameObject statue)
	{
		int animation = getObjectAnimation(statue);
		return animation != -1 && animation != CROSSBOW_IDLE_ANIMATION && animation != CROSSBOW_FINISH_ANIMATION;
	}

	private int getObjectAnimation(GameObject gameObject)
	{
		if (!(gameObject.getRenderable() instanceof DynamicObject))
		{
			return -1;
		}
		DynamicObject dynamicObject = (DynamicObject) gameObject.getRenderable();
		return dynamicObject.getAnimation() == null ? -1 : dynamicObject.getAnimation().getId();
	}

	/**
	 * Chest opening stage, read off the LOCAL PLAYER's own current animation/graphic (not the
	 * chest object) - matching how the legacy plugin identified it. 0 = closed/idle, 1 =
	 * lockpicking, 2 = opening, 3 = open, -1 = just failed (poisoned).
	 */
	int getChestOpeningStage()
	{
		if (client.getLocalPlayer().getGraphic() == CHEST_FAIL_SPOTANIM)
		{
			return -1;
		}
		if (localPlayerAnimation == CHEST_OPEN_ANIMATION)
		{
			return 3;
		}
		if (localPlayerAnimation == CHEST_OPENING_ANIMATION)
		{
			return 2;
		}
		if (localPlayerAnimation == CHEST_LOCKPICK_ANIMATION)
		{
			return 1;
		}
		return 0;
	}
}
