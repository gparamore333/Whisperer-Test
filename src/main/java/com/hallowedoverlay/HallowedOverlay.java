package com.hallowedoverlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Map;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws every Hallowed Sepulchre indicator. Reads state computed by
 * {@link HallowedOverlayPlugin} and paints it - never sends input to the game.
 */
class HallowedOverlay extends Overlay
{
	// A batch of lightning tiles is only worth showing for a short window after it strikes -
	// past this, the tiles are stale (waiting for the next strike) and are hidden rather than
	// left rendering an always-green tile indefinitely.
	private static final int LIGHTNING_DISPLAY_TICKS = 6;
	private static final int[][] CARDINAL_OFFSETS = {
		{0, -1}, // orientation 0-511: south
		{-1, 0}, // 512-1023: west
		{0, 1},  // 1024-1535: north
		{1, 0}   // 1536-2047: east
	};

	private final Client client;
	private final HallowedOverlayPlugin plugin;
	private final HallowedOverlayConfig config;

	@Inject
	private HallowedOverlay(Client client, HallowedOverlayPlugin plugin, HallowedOverlayConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isPlayerInSepulchre() || client.getLocalPlayer() == null)
		{
			return null;
		}

		renderWizardStatues(graphics);
		renderLightning(graphics);
		renderSwords(graphics);
		renderSwordStatues(graphics);
		renderArrows(graphics);
		renderCrossbowStatues(graphics);
		renderChests(graphics);

		if (plugin.isDoorOpen())
		{
			renderEndPortals(graphics);
			renderBridges(graphics);
			renderFloorGates(graphics);
		}

		renderStairs(graphics);
		renderTeleportPortals(graphics);
		renderServerTile(graphics);

		return null;
	}

	private void renderWizardStatues(Graphics2D graphics)
	{
		for (HallowedWizardStatue statue : plugin.getWizardStatues())
		{
			if (!statue.isKnown())
			{
				continue;
			}

			int ticksUntilFire = statue.getTicksUntilFire();
			Color color;
			if (ticksUntilFire <= 1)
			{
				if (!config.showFireUnsafe())
				{
					continue;
				}
				color = config.fireUnsafeColor();
			}
			else if (ticksUntilFire == 2)
			{
				if (!config.showFireRisky())
				{
					continue;
				}
				color = config.fireRiskyColor();
			}
			else
			{
				if (!config.showFireSafe())
				{
					continue;
				}
				color = config.fireSafeColor();
			}

			WorldPoint origin = statue.getGameObject().getWorldLocation();
			int[] offset = cardinalOffset(statue.getGameObject().getOrientation());
			for (int step = 1; step <= 3; step++)
			{
				WorldPoint tile = origin.dx(offset[0] * step).dy(offset[1] * step);
				renderTile(graphics, tile, color);
			}

			if (config.showFireTickCounter())
			{
				renderWorldText(graphics, origin, String.valueOf(ticksUntilFire), color, 40);
			}
		}
	}

	private void renderLightning(Graphics2D graphics)
	{
		if (!config.showLightningTiles() || plugin.getTicksSinceLightning() >= LIGHTNING_DISPLAY_TICKS)
		{
			return;
		}

		// Countdown shape carried over from the legacy plugin - the exact cycle length isn't
		// wiki-published, only that "there is one tick between cycles with no flames present".
		int countdown = Math.abs(plugin.getTicksSinceLightning() - 5);
		Color color;
		if (countdown == 2)
		{
			color = Color.ORANGE;
		}
		else if (countdown == 1)
		{
			color = Color.RED;
		}
		else
		{
			color = Color.GREEN;
		}

		for (WorldPoint tile : plugin.getLightningTiles())
		{
			renderTile(graphics, tile, color);
			if (config.showLightningCountdown())
			{
				renderWorldText(graphics, tile, String.valueOf(countdown), color, 0);
			}
		}
	}

	private void renderSwords(Graphics2D graphics)
	{
		if (!config.showSwordDanger())
		{
			return;
		}

		for (NPC sword : plugin.getSwords())
		{
			renderTile(graphics, sword.getWorldLocation(), config.swordDangerColor());
		}
	}

	private void renderSwordStatues(Graphics2D graphics)
	{
		if (!config.showSwordStatue())
		{
			return;
		}

		for (GameObject statue : plugin.getSwordStatues())
		{
			if (plugin.isSwordStatueThrowing(statue))
			{
				renderObjectOutline(graphics, statue, config.swordStatueColor());
			}
		}
	}

	private void renderArrows(Graphics2D graphics)
	{
		if (!config.showArrows())
		{
			return;
		}

		for (NPC arrow : plugin.getArrows())
		{
			WorldPoint origin = arrow.getWorldLocation();
			renderTile(graphics, origin, config.arrowDangerColor());

			int[] offset = cardinalOffset(arrow.getOrientation());
			for (int step = 1; step <= 2; step++)
			{
				WorldPoint tile = origin.dx(offset[0] * step).dy(offset[1] * step);
				renderTile(graphics, tile, config.arrowPathColor());
			}
		}
	}

	private void renderCrossbowStatues(Graphics2D graphics)
	{
		if (!config.showCrossbowStatue())
		{
			return;
		}

		for (GameObject statue : plugin.getCrossbowStatues())
		{
			if (plugin.isCrossbowStatueFiring(statue))
			{
				renderObjectOutline(graphics, statue, config.crossbowStatueColor());
			}
		}
	}

	private void renderChests(Graphics2D graphics)
	{
		if (!config.showChests())
		{
			return;
		}

		for (GameObject chest : plugin.getChests())
		{
			Color color;
			if (plugin.isChestClosed(chest))
			{
				int stage = plugin.getChestOpeningStage();
				switch (stage)
				{
					case -1:
						color = config.chestFailColor();
						break;
					case 1:
					case 2:
						color = config.chestOpeningColor();
						break;
					case 3:
						color = config.chestOpenColor();
						break;
					default:
						color = config.chestClosedColor();
						break;
				}
			}
			else
			{
				color = config.chestOpenColor();
			}

			renderClickbox(graphics, chest, color);
		}
	}

	private void renderEndPortals(Graphics2D graphics)
	{
		if (!config.showEndPortal())
		{
			return;
		}

		for (GameObject portal : plugin.getEndPortals())
		{
			Color color = plugin.isPortalBuilt(portal) ? config.portalOpenColor() : config.portalClosedColor();
			renderClickbox(graphics, portal, color);
		}
	}

	private void renderBridges(Graphics2D graphics)
	{
		if (!config.showBridge())
		{
			return;
		}

		for (GroundObject bridge : plugin.getBridges())
		{
			Color color = plugin.isBridgeBuilt(bridge) ? config.bridgeBuiltColor() : config.bridgeUnbuiltColor();
			renderClickbox(graphics, bridge, color);
		}
	}

	private void renderStairs(Graphics2D graphics)
	{
		if (!config.showStairs())
		{
			return;
		}

		for (GameObject stairsObject : plugin.getStairs())
		{
			renderClickbox(graphics, stairsObject, config.stairsColor());
		}
	}

	private void renderFloorGates(Graphics2D graphics)
	{
		if (!config.showFloorGates())
		{
			return;
		}

		for (GameObject gate : plugin.getFloorGates())
		{
			renderClickbox(graphics, gate, config.floorGateColor());
		}
	}

	private void renderTeleportPortals(Graphics2D graphics)
	{
		if (!config.showTeleporterTiles())
		{
			return;
		}

		renderTeleportPortalMap(graphics, plugin.getBluePortals(), Color.CYAN);
		renderTeleportPortalMap(graphics, plugin.getYellowPortals(), Color.YELLOW);
	}

	private void renderTeleportPortalMap(Graphics2D graphics, Map<LocalPoint, HallowedTeleportPortal> portals, Color color)
	{
		for (Map.Entry<LocalPoint, HallowedTeleportPortal> entry : portals.entrySet())
		{
			LocalPoint localPoint = entry.getKey();
			Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, color);
			}

			if (config.showTeleporterTimer())
			{
				String text = String.valueOf(entry.getValue().getTicksUntilDespawn());
				Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
				if (canvasPoint != null)
				{
					renderText(graphics, text, color, canvasPoint);
				}
			}
		}
	}

	private void renderServerTile(Graphics2D graphics)
	{
		if (!config.showServerTile())
		{
			return;
		}

		renderTile(graphics, client.getLocalPlayer().getWorldLocation(), config.serverTileColor());
	}

	private static int[] cardinalOffset(int orientation)
	{
		return CARDINAL_OFFSETS[(orientation / 512) & 3];
	}

	private void renderTile(Graphics2D graphics, WorldPoint worldPoint, Color color)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (localPoint == null)
		{
			return;
		}

		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePoly, color);
		}
	}

	private void renderObjectOutline(Graphics2D graphics, GameObject gameObject, Color color)
	{
		if (gameObject.getConvexHull() != null)
		{
			OverlayUtil.renderPolygon(graphics, gameObject.getConvexHull(), color);
		}
	}

	private void renderClickbox(Graphics2D graphics, TileObject tileObject, Color color)
	{
		Shape clickbox = tileObject.getClickbox();
		if (clickbox == null)
		{
			return;
		}

		graphics.setColor(color.darker());
		graphics.draw(clickbox);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(color.getAlpha(), 50)));
		graphics.fill(clickbox);
	}

	private void renderWorldText(Graphics2D graphics, WorldPoint worldPoint, String text, Color color, int zOffset)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (localPoint == null)
		{
			return;
		}

		Point canvasPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, zOffset);
		if (canvasPoint != null)
		{
			renderText(graphics, text, color, canvasPoint);
		}
	}

	private static void renderText(Graphics2D graphics, String text, Color color, Point location)
	{
		graphics.setFont(new Font("Arial", Font.BOLD, 16));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
	}
}
