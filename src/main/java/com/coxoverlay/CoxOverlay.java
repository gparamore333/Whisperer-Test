package com.coxoverlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws Chambers of Xeric state labels and the prayer-tab reminder. Reads state computed by
 * {@link CoxOverlayPlugin} and paints it - never sends input to the game.
 */
class CoxOverlay extends Overlay
{
	private static final Color SALVE_REMINDER_COLOR = Color.YELLOW;
	private static final Color SHAMAN_ACID_COLOR = new Color(69, 200, 44);

	private final Client client;
	private final CoxOverlayPlugin plugin;
	private final CoxOverlayConfig config;

	@Inject
	private CoxOverlay(Client client, CoxOverlayPlugin plugin, CoxOverlayConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.enableStateLabels())
		{
			renderStateLabels(graphics);
		}

		renderPrayerReminder(graphics);
		renderSalveReminder(graphics);
		renderShamanAcidWarnings(graphics);

		return null;
	}

	private void renderShamanAcidWarnings(Graphics2D graphics)
	{
		if (!config.shamanAcidWarning())
		{
			return;
		}

		for (CoxShamanAcid acid : plugin.getShamanAcidWarnings())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), acid.getTargetPoint());
			if (localPoint == null)
			{
				continue;
			}

			Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, SHAMAN_ACID_COLOR);
			}

			String ticksText = String.valueOf(acid.ticksUntilImpact());
			Point canvasPoint = Perspective.localToCanvas(client, localPoint, acid.getTargetPoint().getPlane());
			if (canvasPoint != null)
			{
				renderText(graphics, ticksText, SHAMAN_ACID_COLOR, canvasPoint);
			}
		}
	}

	private void renderStateLabels(Graphics2D graphics)
	{
		for (CoxTrackedNpc tracked : plugin.getTrackedNpcs())
		{
			if (!plugin.isRoomEnabled(tracked.getInfo().getRoom()) || tracked.getInfo().getStateLabel() == null)
			{
				continue;
			}

			String text = tracked.getInfo().getStateLabel();
			Color color = Color.WHITE;
			if (tracked.getInfo() == CoxNpcInfo.OLM_HAND_LEFT && plugin.isOlmHandClenched())
			{
				text += " - CLENCHED (temporarily resistant)";
				color = Color.ORANGE;
			}

			Point canvasPoint = tracked.getNpc().getCanvasTextLocation(graphics, text, 0);
			renderText(graphics, text, color, canvasPoint);
		}
	}

	private void renderPrayerReminder(Graphics2D graphics)
	{
		Prayer prayer = plugin.getClosestPrayer();
		if (prayer == null)
		{
			return;
		}

		Widget widget = prayerWidget(prayer);
		if (widget == null)
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		Color color = client.isPrayerActive(prayer) ? Color.GREEN : Color.RED;

		graphics.setColor(color);
		graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	private void renderSalveReminder(Graphics2D graphics)
	{
		if (!plugin.isShowSalveReminder())
		{
			return;
		}

		String text = "No Salve amulet equipped";
		Point canvasPoint = client.getLocalPlayer().getCanvasTextLocation(graphics, text, 40);
		renderText(graphics, text, SALVE_REMINDER_COLOR, canvasPoint);
	}

	private Widget prayerWidget(Prayer prayer)
	{
		if (prayer == Prayer.PROTECT_FROM_MELEE)
		{
			return client.getWidget(InterfaceID.Prayerbook.PRAYER19);
		}
		if (prayer == Prayer.PROTECT_FROM_MISSILES)
		{
			return client.getWidget(InterfaceID.Prayerbook.PRAYER18);
		}
		if (prayer == Prayer.PROTECT_FROM_MAGIC)
		{
			return client.getWidget(InterfaceID.Prayerbook.PRAYER17);
		}
		return null;
	}

	private static void renderText(Graphics2D graphics, String text, Color color, Point location)
	{
		if (text == null || location == null)
		{
			return;
		}

		graphics.setFont(new Font("Arial", Font.BOLD, 16));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
	}
}
