package com.whispereroverlay;

import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Purely informational overlay for the Whisperer boss fight (DT2).
 * <p>
 * This plugin never interacts with the game on the player's behalf: it does not attack,
 * walk, activate items, or toggle prayers automatically. It only reads game state that is
 * already visible on screen (NPCs, projectiles, chat messages, game objects) and draws
 * indicators so the player can react manually.
 */
@PluginDescriptor(
	name = "Whisperer Overlay",
	description = "Visual-only overlays for the Whisperer boss fight - no automation",
	tags = {"whisperer", "dt2", "overlay", "boss"},
	enabledByDefault = false
)
public class WhispererOverlayPlugin extends Plugin
{
	static final int TENTACLE_NPC_ID = NpcID.WHISPERER_TENTACLE;
	// The pillars appear to be tracked under two IDs depending on game phase (normal vs.
	// shadow realm) - both are treated as pillars so tracking doesn't silently miss one phase.
	private static final int PILLAR_NPC_ID_A = NpcID.WHISPERER_SCREECH_SAFESPOT;
	private static final int PILLAR_NPC_ID_B = NpcID.WHISPERER_SCREECH_SAFESPOT_SHADOW;
	static final int LEECH_OBJECT_ACTIVE_ID = ObjectID.WHISPERER_SEED_SHADOW_REALM_WEAK;

	private static final int MAGE_PROJECTILE_ID = SpotanimID.PROJ_WHISPERER_01_MAGIC_01;
	private static final int RANGED_PROJECTILE_ID = SpotanimID.PROJ_WHISPERER_01_RANGED_01;
	// Note: there is no verified melee auto-attack projectile for this boss - the ID a
	// prior version of this plugin used (2467) actually corresponds to the "entangle"
	// bind effect, not melee damage, so no protect-from-melee mapping is included.

	private static boolean isPillar(int npcId)
	{
		return npcId == PILLAR_NPC_ID_A || npcId == PILLAR_NPC_ID_B;
	}

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WhispererOverlay overlay;

	@Inject
	private WhispererOverlayConfig config;

	@Provides
	WhispererOverlayConfig getConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(WhispererOverlayConfig.class);
	}

	private final List<Projectile> trackedProjectiles = new ArrayList<>();

	@Getter
	private final Map<LocalPoint, Integer> unsafeTiles = new HashMap<>();

	@Getter
	private final List<LocalPoint> leeches = new ArrayList<>();

	@Getter
	private final List<NPC> vitaAdds = new ArrayList<>();

	@Getter
	private NPC mostHealthPillar = null;

	@Getter
	private NPC nextMostHealthPillar = null;

	@Getter
	private NPC leastHealthPillar = null;

	@Getter
	private int bindTicksRemaining = 0;

	private int enrageStartTick = 0;
	private int pillarSpawnTick = 0;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		trackedProjectiles.clear();
		unsafeTiles.clear();
		leeches.clear();
		vitaAdds.clear();
		mostHealthPillar = null;
		nextMostHealthPillar = null;
		leastHealthPillar = null;
		bindTicksRemaining = 0;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		vitaAdds.clear();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getOverheadText() != null && npc.getOverheadText().equals("Vita!"))
			{
				vitaAdds.add(npc);
			}
		}

		List<NPC> activePillars = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && isPillar(npc.getId()) && getHpPercent(npc) > 0)
			{
				activePillars.add(npc);
			}
		}

		if (mostHealthPillar == null && nextMostHealthPillar == null && leastHealthPillar == null)
		{
			if (!activePillars.isEmpty())
			{
				rankPillars(activePillars);
			}
		}
		else if (client.getTickCount() - pillarSpawnTick > 23)
		{
			mostHealthPillar = null;
			nextMostHealthPillar = null;
			leastHealthPillar = null;
		}

		if (bindTicksRemaining > 0)
		{
			bindTicksRemaining--;
		}

		NPC whisperer = findWhisperer();
		if (whisperer == null)
		{
			enrageStartTick = 0;
		}
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		trackedProjectiles.removeIf(proj -> proj.getRemainingCycles() < 1);
		unsafeTiles.values().removeIf(expiryTick -> client.getTickCount() > expiryTick);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();

		if (message.contains("blackstone fragment loses"))
		{
			leeches.clear();
		}

		if (message.contains("binds you in place"))
		{
			bindTicksRemaining = 8;
		}

		if (message.contains("pulls you into the"))
		{
			enrageStartTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (gameObject.getId() == LEECH_OBJECT_ACTIVE_ID)
		{
			leeches.add(gameObject.getLocalLocation());
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		int id = event.getProjectile().getId();
		if (id != MAGE_PROJECTILE_ID && id != RANGED_PROJECTILE_ID)
		{
			return;
		}

		for (Projectile tracked : trackedProjectiles)
		{
			if (tracked == event.getProjectile())
			{
				return;
			}
		}

		trackedProjectiles.add(event.getProjectile());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getId() != TENTACLE_NPC_ID)
		{
			return;
		}

		int dx = 0;
		int dy = 0;

		switch (npc.getOrientation())
		{
			case 0: // south
				dy = -1;
				break;
			case 256: // south-west
				dx = -1;
				dy = -1;
				break;
			case 512: // west
				dx = -1;
				break;
			case 768: // north-west
				dx = -1;
				dy = 1;
				break;
			case 1024: // north
				dy = 1;
				break;
			case 1280: // north-east
				dx = 1;
				dy = 1;
				break;
			case 1536: // east
				dx = 1;
				break;
			case 1792: // south-east
				dx = 1;
				dy = -1;
				break;
			default:
				return;
		}

		int ticksUntilHit = getTicksSinceEnrageStarted() < 200 ? 3 : 2;
		for (int i = 0; i < 5; i++)
		{
			WorldPoint tileCenter = npc.getWorldLocation().dx(dx * i).dy(dy * i).dx(1).dy(1);
			LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), tileCenter);
			if (localPoint != null)
			{
				unsafeTiles.put(localPoint, client.getTickCount() + ticksUntilHit);
			}
		}
	}

	/**
	 * The protection prayer matching the soonest-arriving tracked projectile, or null if
	 * nothing is incoming. This is exposed only so the overlay can label it - the plugin
	 * never activates it.
	 */
	Prayer getIncomingAttackPrayer()
	{
		Prayer prayer = null;
		int lowestRemaining = Integer.MAX_VALUE;

		for (Projectile projectile : trackedProjectiles)
		{
			if (projectile.getRemainingCycles() < lowestRemaining)
			{
				prayer = prayerForProjectile(projectile.getId());
				lowestRemaining = projectile.getRemainingCycles();
			}
		}

		return prayer;
	}

	private Prayer prayerForProjectile(int id)
	{
		if (id == MAGE_PROJECTILE_ID)
		{
			return Prayer.PROTECT_FROM_MAGIC;
		}
		if (id == RANGED_PROJECTILE_ID)
		{
			return Prayer.PROTECT_FROM_MISSILES;
		}
		return null;
	}

	private void rankPillars(List<NPC> activePillars)
	{
		NPC mostHealth = null;
		for (NPC npc : activePillars)
		{
			if (getHpPercent(npc) == 100)
			{
				mostHealth = npc;
				break;
			}
		}

		if (mostHealth == null)
		{
			return;
		}

		NPC nextMostHealth = closestPillarWithHealth(activePillars, mostHealth, 67, null);
		if (nextMostHealth == null)
		{
			return;
		}

		NPC leastHealth = closestPillarAtOrBelow(activePillars, mostHealth, nextMostHealth, 67);
		if (leastHealth == null)
		{
			return;
		}

		mostHealthPillar = mostHealth;
		nextMostHealthPillar = nextMostHealth;
		leastHealthPillar = leastHealth;
		pillarSpawnTick = client.getTickCount();
	}

	private NPC closestPillarWithHealth(List<NPC> candidates, NPC reference, int hpPercent, NPC exclude)
	{
		float leastDistance = Float.MAX_VALUE;
		NPC closest = null;

		for (NPC npc : candidates)
		{
			if (npc == exclude || getHpPercent(npc) != hpPercent)
			{
				continue;
			}

			float distance = (float) reference.getWorldLocation().distanceTo(npc.getWorldLocation());
			if (distance < leastDistance)
			{
				closest = npc;
				leastDistance = distance;
			}
		}

		return closest;
	}

	private NPC closestPillarAtOrBelow(List<NPC> candidates, NPC reference, NPC exclude, int maxHpPercent)
	{
		float leastDistance = Float.MAX_VALUE;
		NPC closest = null;

		for (NPC npc : candidates)
		{
			if (npc == exclude || getHpPercent(npc) > maxHpPercent)
			{
				continue;
			}

			float distance = (float) reference.getWorldLocation().distanceTo(npc.getWorldLocation());
			if (distance < leastDistance)
			{
				closest = npc;
				leastDistance = distance;
			}
		}

		return closest;
	}

	private NPC findWhisperer()
	{
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getName() != null && npc.getName().contains("Whisperer"))
			{
				return npc;
			}
		}
		return null;
	}

	private int getTicksSinceEnrageStarted()
	{
		return client.getTickCount() - enrageStartTick;
	}

	private static int getHpPercent(NPC npc)
	{
		int scale = npc.getHealthScale();
		if (scale <= 0)
		{
			return 0;
		}
		return (int) Math.floor((double) npc.getHealthRatio() / (double) scale * 100);
	}
}
