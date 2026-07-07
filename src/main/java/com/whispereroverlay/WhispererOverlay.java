package com.whispereroverlay;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * Draws information only. This overlay never sends a click, key press, or movement to the
 * game - it just renders shapes and text over what the player can already see.
 */
public class WhispererOverlay extends Overlay
{
	private static final Color UNSAFE_TILE_COLOR = new Color(220, 30, 30, 130);
	private static final Color LEECH_COLOR = new Color(30, 200, 30, 130);
	private static final Color VITA_COLOR = new Color(230, 220, 30, 130);
	private static final Color PILLAR_HIGH_COLOR = new Color(30, 200, 30, 130);
	private static final Color PILLAR_MID_COLOR = new Color(230, 220, 30, 130);
	private static final Color PILLAR_LOW_COLOR = new Color(220, 30, 30, 130);

	private final Client client;
	private final WhispererOverlayPlugin plugin;
	private final WhispererOverlayConfig config;

	@Inject
	private WhispererOverlay(Client client, WhispererOverlayPlugin plugin, WhispererOverlayConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showUnsafeTiles())
		{
			renderUnsafeTiles(graphics);
		}

		if (config.showLeechAndVitaTracker())
		{
			renderLeeches(graphics);
			renderVitaAdds(graphics);
		}

		if (config.showPillarRanking())
		{
			renderPillarRanking(graphics);
		}

		if (config.showBindTimer())
		{
			renderBindTimer(graphics);
		}

		if (config.showPrayerIndicator())
		{
			renderPrayerIndicator(graphics);
		}

		return null;
	}

	private void renderUnsafeTiles(Graphics2D graphics)
	{
		for (java.util.Map.Entry<LocalPoint, Integer> entry : plugin.getUnsafeTiles().entrySet())
		{
			int ticksRemaining = entry.getValue() - client.getTickCount();
			if (ticksRemaining < 0)
			{
				continue;
			}

			drawTile(graphics, entry.getKey(), UNSAFE_TILE_COLOR, String.valueOf(ticksRemaining));
		}
	}

	private void renderLeeches(Graphics2D graphics)
	{
		for (LocalPoint leech : plugin.getLeeches())
		{
			drawTile(graphics, leech, LEECH_COLOR, null);
		}
	}

	private void renderVitaAdds(Graphics2D graphics)
	{
		for (NPC vita : plugin.getVitaAdds())
		{
			Shape hull = vita.getConvexHull();
			if (hull != null)
			{
				drawOutlineAndFill(graphics, VITA_COLOR.darker(), VITA_COLOR, 2f, hull);
			}
		}
	}

	private void renderPillarRanking(Graphics2D graphics)
	{
		labelPillar(graphics, plugin.getMostHealthPillar(), "1", PILLAR_HIGH_COLOR);
		labelPillar(graphics, plugin.getNextMostHealthPillar(), "2", PILLAR_MID_COLOR);
		labelPillar(graphics, plugin.getLeastHealthPillar(), "3", PILLAR_LOW_COLOR);
	}

	private void labelPillar(Graphics2D graphics, NPC pillar, String label, Color color)
	{
		if (pillar == null)
		{
			return;
		}

		Shape hull = pillar.getConvexHull();
		if (hull != null)
		{
			drawOutlineAndFill(graphics, color.darker(), color, 2f, hull);
		}

		Point textLocation = pillar.getCanvasTextLocation(graphics, label, pillar.getLogicalHeight() + 40);
		if (textLocation != null)
		{
			drawText(graphics, label, textLocation, color.brighter());
		}
	}

	private void renderBindTimer(Graphics2D graphics)
	{
		int ticksRemaining = plugin.getBindTicksRemaining();
		if (ticksRemaining <= 0 || client.getLocalPlayer() == null)
		{
			return;
		}

		String text = "Bound: " + ticksRemaining;
		Point textLocation = client.getLocalPlayer().getCanvasTextLocation(graphics, text, client.getLocalPlayer().getLogicalHeight() + 40);
		if (textLocation != null)
		{
			drawText(graphics, text, textLocation, Color.WHITE);
		}
	}

	private void renderPrayerIndicator(Graphics2D graphics)
	{
		Prayer prayer = plugin.getIncomingAttackPrayer();
		if (prayer == null || client.getLocalPlayer() == null)
		{
			return;
		}

		String text = "Pray: " + prayerLabel(prayer);
		Point textLocation = client.getLocalPlayer().getCanvasTextLocation(graphics, text, client.getLocalPlayer().getLogicalHeight() + 80);
		if (textLocation != null)
		{
			drawText(graphics, text, textLocation, Color.CYAN);
		}
	}

	private static String prayerLabel(Prayer prayer)
	{
		switch (prayer)
		{
			case PROTECT_FROM_MAGIC:
				return "Magic";
			case PROTECT_FROM_MISSILES:
				return "Missiles";
			case PROTECT_FROM_MELEE:
				return "Melee";
			default:
				return prayer.name();
		}
	}

	private void drawTile(Graphics2D graphics, LocalPoint localPoint, Color color, String label)
	{
		Shape tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null)
		{
			return;
		}

		drawOutlineAndFill(graphics, color.darker(), color, 2f, tilePoly);

		if (label != null)
		{
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
			if (textLocation != null)
			{
				drawText(graphics, label, textLocation, Color.WHITE);
			}
		}
	}

	private static void drawText(Graphics2D graphics, String text, Point location, Color color)
	{
		Color original = graphics.getColor();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
		graphics.setColor(original);
	}

	private static void drawOutlineAndFill(Graphics2D graphics, Color outlineColor, Color fillColor, float strokeWidth, Shape shape)
	{
		Color originalColor = graphics.getColor();
		Stroke originalStroke = graphics.getStroke();

		graphics.setStroke(new BasicStroke(strokeWidth));
		graphics.setColor(outlineColor);
		graphics.draw(shape);

		graphics.setColor(fillColor);
		graphics.fill(shape);

		graphics.setColor(originalColor);
		graphics.setStroke(originalStroke);
	}
}
