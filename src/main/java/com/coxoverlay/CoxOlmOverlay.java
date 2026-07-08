package com.coxoverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the Great Olm crystal-bomb heatmap/countdown and the acid pool, falling-crystal, and
 * lightning-trail hazard tiles. All read live game objects/graphics - nothing here predicts
 * an attack before it happens.
 */
class CoxOlmOverlay extends Overlay
{
	private static final int BOMB_RADIUS = 7;
	private static final Color BOMB_SAFE = new Color(0, 200, 0);
	private static final Color BOMB_CAUTION = Color.YELLOW;
	private static final Color BOMB_WARNING = new Color(255, 153, 51);
	private static final Color BOMB_DANGER = new Color(255, 102, 0);
	private static final Color BOMB_LETHAL = new Color(200, 0, 0);
	private static final Color ACID_COLOR = new Color(69, 200, 44);
	private static final Color CRYSTAL_COLOR = new Color(255, 0, 84);
	private static final Color LIGHTNING_COLOR = new Color(0, 150, 220);

	private final Client client;
	private final CoxOverlayPlugin plugin;
	private final CoxOverlayConfig config;

	@Inject
	private CoxOlmOverlay(Client client, CoxOverlayPlugin plugin, CoxOverlayConfig config)
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
		if (!config.olmEnable() || client.getLocalPlayer() == null)
		{
			return null;
		}

		if (config.olmBombHeatmap())
		{
			renderBombs(graphics);
		}

		if (config.olmAcidWarning())
		{
			for (GameObject acidPool : plugin.getOlmAcidPools())
			{
				renderHazardTile(graphics, acidPool.getWorldLocation(), ACID_COLOR);
			}
		}

		if (config.olmCrystalWarning())
		{
			for (GameObject crystal : plugin.getOlmCrystalMarkers())
			{
				renderHazardTile(graphics, crystal.getWorldLocation(), CRYSTAL_COLOR);
			}
		}

		if (config.olmLightningWarning())
		{
			for (WorldPoint point : plugin.getOlmLightningTrail())
			{
				renderHazardTile(graphics, point, LIGHTNING_COLOR);
			}
		}

		return null;
	}

	private void renderBombs(Graphics2D graphics)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		int currentTick = client.getTickCount();

		for (CoxOlmBomb bomb : plugin.getOlmBombs())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), bomb.getLocation());
			if (localPoint == null)
			{
				continue;
			}

			int dx = Math.abs(bomb.getLocation().getX() - playerLocation.getX());
			int dy = Math.abs(bomb.getLocation().getY() - playerLocation.getY());
			int chebyshevDistance = Math.max(dx, dy);

			Color color;
			if (chebyshevDistance < 1)
			{
				color = BOMB_LETHAL;
			}
			else if (chebyshevDistance < 2)
			{
				color = BOMB_DANGER;
			}
			else if (chebyshevDistance < 3)
			{
				color = BOMB_WARNING;
			}
			else if (chebyshevDistance < 4)
			{
				color = BOMB_CAUTION;
			}
			else
			{
				color = BOMB_SAFE;
			}

			Polygon area = Perspective.getCanvasTileAreaPoly(client, localPoint, BOMB_RADIUS);
			if (area != null)
			{
				Stroke originalStroke = graphics.getStroke();
				graphics.setColor(color);
				graphics.setStroke(new BasicStroke(1));
				graphics.drawPolygon(area);
				graphics.setColor(new Color(0, 0, 0, 10));
				graphics.fillPolygon(area);
				graphics.setStroke(originalStroke);
			}

			String ticksText = String.valueOf(bomb.ticksUntilDetonation(currentTick));
			Point canvasPoint = Perspective.localToCanvas(client, localPoint, bomb.getLocation().getPlane());
			if (canvasPoint != null)
			{
				renderText(graphics, ticksText, color, canvasPoint);
			}
		}
	}

	private void renderHazardTile(Graphics2D graphics, WorldPoint worldPoint, Color color)
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

	private static void renderText(Graphics2D graphics, String text, Color color, Point location)
	{
		graphics.setFont(new Font("Arial", Font.BOLD, 16));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
	}
}
