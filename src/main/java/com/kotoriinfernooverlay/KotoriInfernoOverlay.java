package com.kotoriinfernooverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.kotoriinfernooverlay.displaymodes.KotoriPrayerDisplayMode;
import com.kotoriinfernooverlay.displaymodes.KotoriSafespotDisplayMode;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws every Kotori Inferno indicator. Reads state computed by
 * {@link KotoriInfernoOverlayPlugin} and paints it - never sends input to the game.
 */
class KotoriInfernoOverlay extends Overlay
{
	private static final int TICK_PIXEL_SIZE = 60;
	private static final int BOX_WIDTH = 10;
	private static final int BOX_HEIGHT = 5;
	private static final int TEXT_SIZE = 32;

	private final Client client;
	private final KotoriInfernoOverlayPlugin plugin;
	private final KotoriInfernoOverlayConfig config;

	@Inject
	private KotoriInfernoOverlay(Client client, KotoriInfernoOverlayPlugin plugin, KotoriInfernoOverlayConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.indicateObstacles())
		{
			renderObstacles(graphics);
		}

		if (config.safespotDisplayMode() == KotoriSafespotDisplayMode.AREA)
		{
			renderAreaSafespots(graphics);
		}
		else if (config.safespotDisplayMode() == KotoriSafespotDisplayMode.INDIVIDUAL_TILES)
		{
			renderIndividualTileSafespots(graphics);
		}

		if (config.indicateBlobDeathLocation())
		{
			renderBlobDeathSpots(graphics);
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		for (KotoriInfernoNPC infernoNPC : plugin.getInfernoNpcs())
		{
			renderMonsterOutline(graphics, infernoNPC, playerLocation);

			if (plugin.isIndicateNpcPosition(infernoNPC))
			{
				renderNpcLocation(graphics, infernoNPC.getNpc());
			}

			if (plugin.isTicksOnNpc(infernoNPC) && infernoNPC.getTicksTillNextAttack() > 0)
			{
				renderTicksOnNpc(graphics, infernoNPC, infernoNPC.getNpc());
			}

			if (config.ticksOnNpcZukShield() && infernoNPC.getType() == KotoriInfernoNPC.Type.ZUK
				&& plugin.getZukShield() != null && infernoNPC.getTicksTillNextAttack() > 0)
			{
				renderTicksOnNpc(graphics, infernoNPC, plugin.getZukShield());
			}

			if (config.ticksOnNpcMeleerDig() && infernoNPC.getType() == KotoriInfernoNPC.Type.MELEE
				&& infernoNPC.getIdleTicks() >= config.digTimerThreshold() && infernoNPC.getTicksTillNextAttack() == 0)
			{
				renderDigTimer(graphics, infernoNPC);
			}
		}

		renderPrayerHelper(graphics);

		return null;
	}

	private void renderMonsterOutline(Graphics2D graphics, KotoriInfernoNPC infernoNPC, WorldPoint playerLocation)
	{
		Shape hull = infernoNPC.getNpc().getConvexHull();
		if (hull == null)
		{
			return;
		}

		if (config.indicateNonSafespotted() && plugin.isNormalSafespots(infernoNPC) && infernoNPC.canAttack(client, playerLocation))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.RED);
		}
		if (config.indicateTemporarySafespotted() && plugin.isNormalSafespots(infernoNPC)
			&& infernoNPC.canMoveToAttack(client, playerLocation, plugin.getObstacles()))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.YELLOW);
		}
		if (config.indicateSafespotted() && plugin.isNormalSafespots(infernoNPC))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.GREEN);
		}
		if (config.indicateNibblers() && infernoNPC.getType() == KotoriInfernoNPC.Type.NIBBLER
			&& (!config.indicateCentralNibbler() || plugin.getCentralNibbler() != infernoNPC))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.CYAN);
		}
		if (config.indicateCentralNibbler() && infernoNPC.getType() == KotoriInfernoNPC.Type.NIBBLER && plugin.getCentralNibbler() == infernoNPC)
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.BLUE);
		}
		if (config.indicateActiveHealerJad() && infernoNPC.getType() == KotoriInfernoNPC.Type.HEALER_JAD
			&& infernoNPC.getNpc().getInteracting() != client.getLocalPlayer())
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.CYAN);
		}
		if (config.indicateActiveHealerZuk() && infernoNPC.getType() == KotoriInfernoNPC.Type.HEALER_ZUK
			&& infernoNPC.getNpc().getInteracting() != client.getLocalPlayer())
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.CYAN);
		}
	}

	private void renderObstacles(Graphics2D graphics)
	{
		for (WorldPoint worldPoint : plugin.getObstacles())
		{
			Polygon tilePoly = tilePolygon(worldPoint);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, Color.BLUE);
			}
		}
	}

	private void renderAreaSafespots(Graphics2D graphics)
	{
		for (Map.Entry<Integer, List<WorldPoint>> entry : plugin.getSafeSpotAreas().entrySet())
		{
			int safeSpotId = entry.getKey();
			if (safeSpotId > 6)
			{
				continue;
			}

			Color colorEdge1;
			Color colorEdge2 = null;
			Color colorFill;

			switch (safeSpotId)
			{
				case 0:
					colorEdge1 = Color.WHITE;
					colorFill = Color.WHITE;
					break;
				case 1:
					colorEdge1 = Color.RED;
					colorFill = Color.RED;
					break;
				case 2:
					colorEdge1 = Color.GREEN;
					colorFill = Color.GREEN;
					break;
				case 3:
					colorEdge1 = Color.BLUE;
					colorFill = Color.BLUE;
					break;
				case 4:
					colorEdge1 = Color.RED;
					colorEdge2 = Color.GREEN;
					colorFill = Color.YELLOW;
					break;
				case 5:
					colorEdge1 = Color.RED;
					colorEdge2 = Color.BLUE;
					colorFill = new Color(255, 0, 255);
					break;
				case 6:
					colorEdge1 = Color.GREEN;
					colorEdge2 = Color.BLUE;
					colorFill = new Color(0, 255, 255);
					break;
				default:
					continue;
			}

			renderSafespotArea(graphics, entry.getValue(), colorEdge1, colorEdge2, colorFill);
		}
	}

	private void renderSafespotArea(Graphics2D graphics, List<WorldPoint> tiles, Color edge1, Color edge2, Color fill)
	{
		List<int[][]> allEdges = new ArrayList<>();
		long edgeSizeSquaredTotal = 0;

		for (WorldPoint worldPoint : tiles)
		{
			Polygon tilePoly = tilePolygon(worldPoint);
			if (tilePoly == null)
			{
				continue;
			}

			renderAreaTilePolygon(graphics, tilePoly, fill);

			for (int i = 0; i < 4; i++)
			{
				int next = (i + 1) % 4;
				int[][] edge = new int[][]{{tilePoly.xpoints[i], tilePoly.ypoints[i]}, {tilePoly.xpoints[next], tilePoly.ypoints[next]}};
				edgeSizeSquaredTotal += Math.pow(tilePoly.xpoints[i] - tilePoly.xpoints[next], 2) + Math.pow(tilePoly.ypoints[i] - tilePoly.ypoints[next], 2);
				allEdges.add(edge);
			}
		}

		if (allEdges.isEmpty())
		{
			return;
		}

		int toleranceSquared = (int) Math.ceil(edgeSizeSquaredTotal / (double) allEdges.size() / 6);

		for (int i = 0; i < allEdges.size(); i++)
		{
			int[][] baseEdge = allEdges.get(i);
			boolean duplicate = false;

			for (int j = 0; j < allEdges.size(); j++)
			{
				if (i != j && edgeEqualsEdge(baseEdge, allEdges.get(j), toleranceSquared))
				{
					duplicate = true;
					break;
				}
			}

			if (!duplicate)
			{
				renderFullLine(graphics, baseEdge, edge1);
				if (edge2 != null)
				{
					renderDashedLine(graphics, baseEdge, edge2);
				}
			}
		}
	}

	private void renderDigTimer(Graphics2D graphics, KotoriInfernoNPC infernoNPC)
	{
		String tickString = Integer.toString(infernoNPC.getIdleTicks());
		Point canvasLocation = infernoNPC.getNpc().getCanvasTextLocation(graphics, tickString, 0);
		if (canvasLocation == null)
		{
			return;
		}

		// The exact dig trigger isn't confirmed data - counts up to the danger threshold as a
		// rough indicator, per the original plugin's own approach.
		Color digColor = infernoNPC.getIdleTicks() < config.digTimerDangerThreshold() ? config.meleeDigSafeColor() : config.meleeDigDangerColor();

		renderTextLocation(graphics, tickString, config.meleeDigFontSize(), digColor, canvasLocation);
	}

	private void renderBlobDeathSpots(Graphics2D graphics)
	{
		for (KotoriInfernoBlobDeathSpot deathSpot : plugin.getBlobDeathSpots())
		{
			Polygon area = Perspective.getCanvasTileAreaPoly(client, deathSpot.getLocation(), 3);
			if (area == null)
			{
				continue;
			}

			Color color = config.blobDeathLocationColor();
			if (config.blobDeathLocationFade())
			{
				color = new Color(color.getRed(), color.getGreen(), color.getBlue(), deathSpot.fillAlpha());
			}

			renderOutlinePolygon(graphics, area, color);

			String ticks = String.valueOf(deathSpot.getTicksUntilDone());
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, deathSpot.getLocation(), ticks, 0);
			renderTextLocation(graphics, ticks, TEXT_SIZE, config.blobDeathLocationColor(), textLocation);
		}
	}

	private void renderIndividualTileSafespots(Graphics2D graphics)
	{
		for (Map.Entry<WorldPoint, Integer> entry : plugin.getSafeSpotMap().entrySet())
		{
			Color color = safespotColor(entry.getValue());
			if (color == null)
			{
				continue;
			}

			Polygon tilePoly = tilePolygon(entry.getKey());
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, color);
			}
		}
	}

	private void renderTicksOnNpc(Graphics2D graphics, KotoriInfernoNPC infernoNPC, NPC renderOnNpc)
	{
		Color color = (infernoNPC.getTicksTillNextAttack() == 1
			|| (infernoNPC.getType() == KotoriInfernoNPC.Type.BLOB && infernoNPC.getTicksTillNextAttack() == 4))
			? infernoNPC.getNextAttack().getCriticalColor() : infernoNPC.getNextAttack().getNormalColor();

		String text = String.valueOf(infernoNPC.getTicksTillNextAttack());
		Point canvasPoint = renderOnNpc.getCanvasTextLocation(graphics, text, 0);
		renderTextLocation(graphics, text, TEXT_SIZE, color, canvasPoint);
	}

	private void renderNpcLocation(Graphics2D graphics, NPC npc)
	{
		Polygon tilePoly = tilePolygon(npc.getWorldLocation());
		if (tilePoly != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePoly, Color.BLUE);
		}
	}

	private void renderPrayerHelper(Graphics2D graphics)
	{
		KotoriPrayerDisplayMode mode = config.prayerDisplayMode();
		if (mode != KotoriPrayerDisplayMode.PRAYER_TAB && mode != KotoriPrayerDisplayMode.BOTH)
		{
			return;
		}

		Widget meleeWidget = client.getWidget(InterfaceID.Prayerbook.PRAYER19);
		Widget rangeWidget = client.getWidget(InterfaceID.Prayerbook.PRAYER18);
		Widget magicWidget = client.getWidget(InterfaceID.Prayerbook.PRAYER17);

		boolean hidden = meleeWidget == null || rangeWidget == null || magicWidget == null
			|| meleeWidget.isHidden() || rangeWidget.isHidden() || magicWidget.isHidden();

		if (hidden && !config.alwaysShowPrayerHelper())
		{
			return;
		}

		renderPrayerIconOutline(graphics);

		if (config.descendingBoxes())
		{
			renderDescendingBoxes(graphics);
		}
	}

	private void renderPrayerIconOutline(Graphics2D graphics)
	{
		KotoriInfernoNPC.Attack closest = plugin.getClosestAttack();
		if (closest == null)
		{
			return;
		}

		KotoriInfernoNPC.Attack activePrayerAttack = null;
		if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
		{
			activePrayerAttack = KotoriInfernoNPC.Attack.MAGIC;
		}
		else if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES))
		{
			activePrayerAttack = KotoriInfernoNPC.Attack.RANGED;
		}
		else if (client.isPrayerActive(Prayer.PROTECT_FROM_MELEE))
		{
			activePrayerAttack = KotoriInfernoNPC.Attack.MELEE;
		}

		if (closest == activePrayerAttack && !config.indicateWhenPrayingCorrectly())
		{
			return;
		}

		Widget widget = prayerWidget(closest);
		if (widget == null)
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		Color prayerColor = closest == activePrayerAttack ? config.correctPrayerColor() : config.mustPrayNextTickColor();
		renderOutlinePolygon(graphics, bounds, prayerColor);
	}

	private void renderDescendingBoxes(Graphics2D graphics)
	{
		for (Map.Entry<Integer, Map<KotoriInfernoNPC.Attack, Integer>> tickEntry : plugin.getUpcomingAttacks().entrySet())
		{
			int tick = tickEntry.getKey();
			Map<KotoriInfernoNPC.Attack, Integer> attacksAtTick = tickEntry.getValue();

			KotoriInfernoNPC.Attack priorityAttack = null;
			int bestPriority = 999;
			for (Map.Entry<KotoriInfernoNPC.Attack, Integer> attackEntry : attacksAtTick.entrySet())
			{
				if (attackEntry.getValue() < bestPriority)
				{
					priorityAttack = attackEntry.getKey();
					bestPriority = attackEntry.getValue();
				}
			}

			for (KotoriInfernoNPC.Attack currentAttack : attacksAtTick.keySet())
			{
				renderDescendingBox(graphics, currentAttack, priorityAttack, tick);
			}
		}
	}

	private void renderDescendingBox(Graphics2D graphics, KotoriInfernoNPC.Attack currentAttack, KotoriInfernoNPC.Attack priorityAttack, int ticksUntilAttack)
	{
		Color color = getCurrentAttackDescendingBarColor(currentAttack, priorityAttack, ticksUntilAttack);
		Widget prayerWidget = prayerWidget(currentAttack);

		if (prayerWidget == null)
		{
			return;
		}

		int baseX = (int) prayerWidget.getBounds().getX() + prayerWidget.getBounds().width / 2 - BOX_WIDTH / 2;
		int baseY = (int) prayerWidget.getBounds().getY() - ticksUntilAttack * TICK_PIXEL_SIZE - BOX_HEIGHT;
		baseY += (int) (TICK_PIXEL_SIZE - ((plugin.getLastTick() + 600 - System.currentTimeMillis()) / 600.0 * TICK_PIXEL_SIZE));

		Rectangle box = new Rectangle(BOX_WIDTH, BOX_HEIGHT);
		box.translate(baseX, baseY);

		if (currentAttack == priorityAttack)
		{
			renderFilledPolygon(graphics, box, color);
		}
		else if (config.indicateNonPriorityDescendingBoxes())
		{
			renderOutlinePolygon(graphics, box, color);
		}
	}

	private Color getCurrentAttackDescendingBarColor(KotoriInfernoNPC.Attack currentAttack, KotoriInfernoNPC.Attack priorityAttack, int ticksUntilAttack)
	{
		if (currentAttack != priorityAttack)
		{
			return config.nonPriorityPrayerColor();
		}
		if (client.isPrayerActive(priorityAttack.getPrayer()))
		{
			return config.correctPrayerColor();
		}
		if (ticksUntilAttack == 1)
		{
			return config.mustPrayNextTickColor();
		}
		return config.upcomingPrayerNotPrayedColor();
	}

	private Widget prayerWidget(KotoriInfernoNPC.Attack attack)
	{
		switch (attack)
		{
			case MELEE:
				return client.getWidget(InterfaceID.Prayerbook.PRAYER19);
			case RANGED:
				return client.getWidget(InterfaceID.Prayerbook.PRAYER18);
			case MAGIC:
				return client.getWidget(InterfaceID.Prayerbook.PRAYER17);
			default:
				return null;
		}
	}

	private Polygon tilePolygon(WorldPoint worldPoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		return localPoint == null ? null : Perspective.getCanvasTilePoly(client, localPoint);
	}

	private static Color safespotColor(int safespotId)
	{
		switch (safespotId)
		{
			case 0:
				return Color.WHITE;
			case 1:
				return Color.RED;
			case 2:
				return Color.GREEN;
			case 3:
				return Color.BLUE;
			case 4:
				return new Color(255, 255, 0);
			case 5:
				return new Color(255, 0, 255);
			case 6:
				return new Color(0, 255, 255);
			default:
				return null;
		}
	}

	private static boolean edgeEqualsEdge(int[][] edge1, int[][] edge2, int toleranceSquared)
	{
		return (pointEqualsPoint(edge1[0], edge2[0], toleranceSquared) && pointEqualsPoint(edge1[1], edge2[1], toleranceSquared))
			|| (pointEqualsPoint(edge1[0], edge2[1], toleranceSquared) && pointEqualsPoint(edge1[1], edge2[0], toleranceSquared));
	}

	private static boolean pointEqualsPoint(int[] a, int[] b, int toleranceSquared)
	{
		double distanceSquared = Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2);
		return distanceSquared <= toleranceSquared;
	}

	private static void renderAreaTilePolygon(Graphics2D graphics, Shape shape, Color color)
	{
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 10));
		graphics.fill(shape);
	}

	private static void renderFullLine(Graphics2D graphics, int[][] line, Color color)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2));
		graphics.drawLine(line[0][0], line[0][1], line[1][0], line[1][1]);
		graphics.setStroke(originalStroke);
	}

	private static void renderDashedLine(Graphics2D graphics, int[][] line, Color color)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
		graphics.drawLine(line[0][0], line[0][1], line[1][0], line[1][1]);
		graphics.setStroke(originalStroke);
	}

	private static void renderOutlinePolygon(Graphics2D graphics, Shape shape, Color color)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(shape);
		graphics.setStroke(originalStroke);
	}

	private static void renderFilledPolygon(Graphics2D graphics, Shape shape, Color color)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(shape);
		graphics.fill(shape);
		graphics.setStroke(originalStroke);
	}

	private static void renderTextLocation(Graphics2D graphics, String text, int fontSize, Color color, Point location)
	{
		if (text == null || text.isEmpty() || location == null)
		{
			return;
		}

		graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
	}
}
