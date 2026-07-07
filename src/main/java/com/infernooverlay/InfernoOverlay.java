package com.infernooverlay;

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

import com.infernooverlay.displaymodes.PrayerDisplayMode;
import com.infernooverlay.displaymodes.SafespotDisplayMode;
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
 * Draws every Inferno indicator: safespot tiles, monster attack timers, obstacle tiles,
 * blob death markers, nibbler/healer highlights, and the prayer-tab indicator. This class
 * only reads state computed by {@link InfernoOverlayPlugin} and paints it - it never sends
 * input to the game.
 */
class InfernoOverlay extends Overlay
{
	private static final int TICK_PIXEL_SIZE = 60;
	private static final int BOX_WIDTH = 10;
	private static final int BOX_HEIGHT = 5;
	private static final int FONT_STYLE = Font.BOLD;
	private static final int TEXT_SIZE = 32;

	private final Client client;
	private final InfernoOverlayPlugin plugin;
	private final InfernoOverlayConfig config;

	@Inject
	private InfernoOverlay(Client client, InfernoOverlayPlugin plugin, InfernoOverlayConfig config)
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

		if (config.safespotDisplayMode() == SafespotDisplayMode.AREA)
		{
			renderAreaSafespots(graphics);
		}
		else if (config.safespotDisplayMode() == SafespotDisplayMode.INDIVIDUAL_TILES)
		{
			renderIndividualTileSafespots(graphics);
		}

		if (config.indicateBlobDeathLocation())
		{
			renderBlobDeathSpots(graphics);
		}

		for (InfernoMonster monster : plugin.getInfernoMonsters())
		{
			renderMonsterOutline(graphics, monster);

			if (plugin.isIndicateNpcPosition(monster))
			{
				renderNpcLocation(graphics, monster.getNpc());
			}

			if (plugin.isTicksOnNpc(monster) && monster.getTicksTillNextAttack() > 0)
			{
				renderTicksOnNpc(graphics, monster, monster.getNpc());
			}

			if (config.ticksOnNpcZukShield() && monster.getType() == InfernoMonster.Type.ZUK
				&& plugin.getZukShield() != null && monster.getTicksTillNextAttack() > 0)
			{
				renderTicksOnNpc(graphics, monster, plugin.getZukShield());
			}

			if (config.digTimer() && monster.getType() == InfernoMonster.Type.MELEE
				&& monster.getIdleTicks() >= config.digTimerThreshold() && monster.getTicksTillNextAttack() == 0)
			{
				renderDigTimer(graphics, monster);
			}
		}

		renderPrayerHelper(graphics);

		return null;
	}

	private void renderMonsterOutline(Graphics2D graphics, InfernoMonster monster)
	{
		Shape hull = monster.getNpc().getConvexHull();
		if (hull == null)
		{
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (config.indicateNonSafespotted() && plugin.isNormalSafespots(monster) && monster.canAttack(client, playerLocation))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.RED);
		}
		if (config.indicateTemporarySafespotted() && plugin.isNormalSafespots(monster)
			&& monster.canMoveToAttack(client, playerLocation, plugin.getObstacles()))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.YELLOW);
		}
		if (config.indicateSafespotted() && plugin.isNormalSafespots(monster))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.GREEN);
		}
		if (config.indicateNibblers() && monster.getType() == InfernoMonster.Type.NIBBLER
			&& (!config.indicateCentralNibbler() || plugin.getCentralNibbler() != monster))
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.CYAN);
		}
		if (config.indicateCentralNibbler() && monster.getType() == InfernoMonster.Type.NIBBLER && plugin.getCentralNibbler() == monster)
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.BLUE);
		}
		if (config.indicateActiveHealerJad() && monster.getType() == InfernoMonster.Type.HEALER_JAD
			&& monster.getNpc().getInteracting() != client.getLocalPlayer())
		{
			OverlayUtil.renderPolygon(graphics, hull, Color.CYAN);
		}
		if (config.indicateActiveHealerZuk() && monster.getType() == InfernoMonster.Type.HEALER_ZUK
			&& monster.getNpc().getInteracting() != client.getLocalPlayer())
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

	private void renderBlobDeathSpots(Graphics2D graphics)
	{
		for (InfernoBlobDeathSpot deathSpot : plugin.getBlobDeathSpots())
		{
			Polygon area = Perspective.getCanvasTileAreaPoly(client, deathSpot.getLocation(), 3);
			if (area == null)
			{
				continue;
			}

			Color color = config.blobDeathLocationColor();
			renderOutlinePolygon(graphics, area, color);

			String ticks = String.valueOf(deathSpot.getTicksUntilDone());
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, deathSpot.getLocation(), ticks, 0);
			renderText(graphics, ticks, TEXT_SIZE, color, textLocation);
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

	private void renderAreaSafespots(Graphics2D graphics)
	{
		for (Map.Entry<Integer, List<WorldPoint>> entry : plugin.getSafeSpotAreas().entrySet())
		{
			int safespotId = entry.getKey();
			if (safespotId > 6)
			{
				continue;
			}

			Color edge1;
			Color edge2 = null;
			Color fill;

			switch (safespotId)
			{
				case 0:
					edge1 = Color.WHITE;
					fill = Color.WHITE;
					break;
				case 1:
					edge1 = Color.RED;
					fill = Color.RED;
					break;
				case 2:
					edge1 = Color.GREEN;
					fill = Color.GREEN;
					break;
				case 3:
					edge1 = Color.BLUE;
					fill = Color.BLUE;
					break;
				case 4:
					edge1 = Color.RED;
					edge2 = Color.GREEN;
					fill = Color.YELLOW;
					break;
				case 5:
					edge1 = Color.RED;
					edge2 = Color.BLUE;
					fill = new Color(255, 0, 255);
					break;
				case 6:
					edge1 = Color.GREEN;
					edge2 = Color.BLUE;
					fill = new Color(0, 255, 255);
					break;
				default:
					continue;
			}

			renderSafespotArea(graphics, entry.getValue(), edge1, edge2, fill);
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

			renderTranslucentFill(graphics, tilePoly, fill);

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

		int tolerance = (int) Math.ceil(edgeSizeSquaredTotal / (double) allEdges.size() / 6);

		for (int i = 0; i < allEdges.size(); i++)
		{
			int[][] baseEdge = allEdges.get(i);
			boolean duplicate = false;

			for (int j = 0; j < allEdges.size(); j++)
			{
				if (i != j && edgeEqualsEdge(baseEdge, allEdges.get(j), tolerance))
				{
					duplicate = true;
					break;
				}
			}

			if (!duplicate)
			{
				renderLine(graphics, baseEdge, edge1, false);
				if (edge2 != null)
				{
					renderLine(graphics, baseEdge, edge2, true);
				}
			}
		}
	}

	private void renderTicksOnNpc(Graphics2D graphics, InfernoMonster monster, NPC renderOnNpc)
	{
		Color color = (monster.getTicksTillNextAttack() == 1 || (monster.getType() == InfernoMonster.Type.BLOB && monster.getTicksTillNextAttack() == 4))
			? monster.getNextAttack().getCriticalColor() : monster.getNextAttack().getNormalColor();

		String text = String.valueOf(monster.getTicksTillNextAttack());
		Point canvasPoint = renderOnNpc.getCanvasTextLocation(graphics, text, 0);
		renderText(graphics, text, TEXT_SIZE, color, canvasPoint);
	}

	private void renderDigTimer(Graphics2D graphics, InfernoMonster monster)
	{
		String text = String.valueOf(monster.getIdleTicks());
		Point canvasPoint = monster.getNpc().getCanvasTextLocation(graphics, text, 0);
		Color color = monster.getIdleTicks() < config.digTimerDangerThreshold() ? Color.LIGHT_GRAY : Color.ORANGE;
		renderText(graphics, text, 11, color, canvasPoint);
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
		PrayerDisplayMode mode = config.prayerDisplayMode();
		if (mode != PrayerDisplayMode.PRAYER_TAB && mode != PrayerDisplayMode.BOTH)
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
		InfernoMonster.Attack closest = plugin.getClosestAttack();
		if (closest == null)
		{
			return;
		}

		InfernoMonster.Attack activePrayerAttack = null;
		if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
		{
			activePrayerAttack = InfernoMonster.Attack.MAGIC;
		}
		else if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES))
		{
			activePrayerAttack = InfernoMonster.Attack.RANGED;
		}
		else if (client.isPrayerActive(Prayer.PROTECT_FROM_MELEE))
		{
			activePrayerAttack = InfernoMonster.Attack.MELEE;
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
		renderOutlinePolygon(graphics, bounds, closest == activePrayerAttack ? Color.GREEN : Color.RED);
	}

	private void renderDescendingBoxes(Graphics2D graphics)
	{
		for (Map.Entry<Integer, Map<InfernoMonster.Attack, Integer>> tickEntry : plugin.getUpcomingAttacks().entrySet())
		{
			int tick = tickEntry.getKey();
			Map<InfernoMonster.Attack, Integer> attacksAtTick = tickEntry.getValue();

			InfernoMonster.Attack bestAttack = null;
			int bestPriority = 999;
			for (Map.Entry<InfernoMonster.Attack, Integer> attackEntry : attacksAtTick.entrySet())
			{
				if (attackEntry.getValue() < bestPriority)
				{
					bestAttack = attackEntry.getKey();
					bestPriority = attackEntry.getValue();
				}
			}

			for (InfernoMonster.Attack attack : attacksAtTick.keySet())
			{
				Widget widget = prayerWidget(attack);
				if (widget == null)
				{
					continue;
				}

				Color color = (tick == 1 && attack == bestAttack) ? Color.RED : Color.ORANGE;

				int baseX = (int) widget.getBounds().getX() + widget.getBounds().width / 2 - BOX_WIDTH / 2;
				int baseY = (int) widget.getBounds().getY() - tick * TICK_PIXEL_SIZE - BOX_HEIGHT;
				baseY += TICK_PIXEL_SIZE - ((plugin.getLastTickMillis() + 600 - System.currentTimeMillis()) / 600.0 * TICK_PIXEL_SIZE);

				Rectangle box = new Rectangle(BOX_WIDTH, BOX_HEIGHT);
				box.translate(baseX, baseY);

				if (attack == bestAttack)
				{
					renderFilledPolygon(graphics, box, color);
				}
				else if (config.indicateNonPriorityBoxes())
				{
					renderOutlinePolygon(graphics, box, color);
				}
			}
		}
	}

	private Widget prayerWidget(InfernoMonster.Attack attack)
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

	private static void renderTranslucentFill(Graphics2D graphics, Shape shape, Color color)
	{
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 10));
		graphics.fill(shape);
	}

	private static void renderLine(Graphics2D graphics, int[][] line, Color color, boolean dashed)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(dashed ? new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0) : new BasicStroke(2));
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

	private static void renderText(Graphics2D graphics, String text, int fontSize, Color color, Point location)
	{
		if (text == null || text.isEmpty() || location == null)
		{
			return;
		}

		graphics.setFont(new Font("Arial", FONT_STYLE, fontSize));
		Color original = graphics.getColor();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
		graphics.setColor(original);
	}
}
