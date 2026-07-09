package com.coxoverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Map;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws every Great Olm indicator: the crystal-bomb heatmap/countdown, acid pool/falling-
 * crystal/lightning-trail/heal-beam hazard tiles, teleport target/destination markers, and
 * burn/acid-drip victim highlights. All read live game objects, graphics, or (for the phase
 * banner and sphere/hand-clench state) Olm's own chat announcements - nothing here predicts
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
	private static final Color HEAL_BEAM_COLOR = new Color(255, 215, 0);
	private static final Color TELEPORT_COLOR = new Color(193, 255, 245);
	private static final Color ACID_TARGET_COLOR = new Color(69, 200, 44);
	private static final Color BURN_VICTIM_COLOR = new Color(255, 100, 0);
	private static final Color PHASE_BANNER_COLOR = Color.WHITE;
	private static final Color EXPOSED_COLOR = new Color(220, 30, 30);
	private static final Color MIDDLE_COLOR = Color.ORANGE;
	private static final Color SAFE_COLOR = new Color(0, 200, 0);
	private static final Color SPECIAL_WARNING_COLOR = new Color(255, 60, 60);
	private static final Color SPECIAL_DENIED_COLOR = new Color(0, 220, 255);
	private static final Color KITE_COUNTER_COLOR = Color.CYAN;
	private static final Color SAFESPOT_COLOR = new Color(255, 255, 255, 120);
	private static final Color RECOMMENDED_SAFESPOT_COLOR = new Color(0, 255, 120);

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

		if (config.olmHealBeamWarning())
		{
			for (WorldPoint point : plugin.getOlmHealBeamTiles())
			{
				renderHazardTile(graphics, point, HEAL_BEAM_COLOR);
			}
		}

		if (config.olmTeleportWarning())
		{
			for (WorldPoint point : plugin.getOlmTeleportDestinations())
			{
				renderHazardTile(graphics, point, TELEPORT_COLOR);
			}
			for (Player target : plugin.getOlmTeleportTargets())
			{
				renderActorHighlight(graphics, target, TELEPORT_COLOR);
			}
		}

		if (config.olmAcidTargetWarning() && plugin.getOlmAcidTarget() != null)
		{
			renderActorHighlight(graphics, plugin.getOlmAcidTarget(), ACID_TARGET_COLOR);
		}

		if (config.olmBurnVictimWarning())
		{
			for (Player victim : plugin.getOlmBurnVictims())
			{
				renderActorHighlight(graphics, victim, BURN_VICTIM_COLOR);
			}
		}

		if (config.olmPhaseBanner() && plugin.isOlmPhaseBannerActive() && plugin.getOlmPhase() != null)
		{
			renderPhaseBanner(graphics);
		}

		if (config.olmKiteEnable())
		{
			renderKiteAssist(graphics);
		}

		return null;
	}

	private void renderKiteAssist(Graphics2D graphics)
	{
		Player localPlayer = client.getLocalPlayer();

		if (config.olmKiteSafespotTiles())
		{
			renderSafespotTiles(graphics);
		}

		if (config.olmKiteHeadFacing() && plugin.getOlmHeadFacing() != CoxOlmHeadFacing.UNKNOWN)
		{
			Color tileColor = plugin.isOlmPlayerExposed() ? EXPOSED_COLOR
				: plugin.getOlmHeadFacing() == CoxOlmHeadFacing.MIDDLE ? MIDDLE_COLOR : SAFE_COLOR;
			LocalPoint localPoint = localPlayer.getLocalLocation();
			Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, tileColor);
			}

			String headText = "Head: " + plugin.getOlmHeadFacing();
			Point headCanvasPoint = localPlayer.getCanvasTextLocation(graphics, headText, 60);
			renderText(graphics, headText, tileColor, headCanvasPoint);

			if (plugin.isOlmPlayerExposed() && plugin.getOlmRecommendedSafespot() != null)
			{
				String moveText = "Move to spot " + plugin.getOlmRecommendedSafespot().getNumber();
				Point moveCanvasPoint = localPlayer.getCanvasTextLocation(graphics, moveText, 80);
				renderText(graphics, moveText, RECOMMENDED_SAFESPOT_COLOR, moveCanvasPoint);
			}
		}

		if (config.olmKiteActionCountdown() && plugin.isOlmActionTicksConfirmed())
		{
			String text = "Next check: " + plugin.getOlmTicksUntilNextStep();
			Color color = plugin.getOlmTicksUntilNextStep() <= 1 ? SPECIAL_WARNING_COLOR : KITE_COUNTER_COLOR;
			Point canvasPoint = localPlayer.getCanvasTextLocation(graphics, text, 120);
			renderText(graphics, text, color, canvasPoint);
		}

		if (config.olmKiteSpecialWarning())
		{
			renderSpecialWarning(graphics, localPlayer);
		}

		if (config.olmKiteAttackCounter())
		{
			renderAttackCounter(graphics, localPlayer);
		}
	}

	private void renderSafespotTiles(Graphics2D graphics)
	{
		CoxOlmSafespot recommended = plugin.getOlmRecommendedSafespot();
		for (Map.Entry<CoxOlmSafespot, WorldPoint> entry : plugin.getOlmSafespotPositions().entrySet())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), entry.getValue());
			if (localPoint == null)
			{
				continue;
			}

			boolean isRecommended = entry.getKey() == recommended;
			Color color = isRecommended ? RECOMMENDED_SAFESPOT_COLOR : SAFESPOT_COLOR;

			Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
			if (tilePoly != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePoly, color);
			}

			String label = String.valueOf(entry.getKey().getNumber());
			Point canvasPoint = Perspective.localToCanvas(client, localPoint, entry.getValue().getPlane());
			if (canvasPoint != null)
			{
				renderText(graphics, label, color, canvasPoint);
			}
		}
	}

	private void renderSpecialWarning(Graphics2D graphics, Player localPlayer)
	{
		if (plugin.getOlmSpecialDeniedFlashTicks() > 0)
		{
			String text = "Special denied!";
			Point canvasPoint = localPlayer.getCanvasTextLocation(graphics, text, 100);
			renderText(graphics, text, SPECIAL_DENIED_COLOR, canvasPoint);
			return;
		}

		if (!plugin.isOlmSpecialImminent())
		{
			return;
		}

		String text = plugin.getOlmNextSpecial() + " due - force a head turn now";
		Point canvasPoint = localPlayer.getCanvasTextLocation(graphics, text, 100);
		renderText(graphics, text, SPECIAL_WARNING_COLOR, canvasPoint);
	}

	private void renderAttackCounter(Graphics2D graphics, Player localPlayer)
	{
		int meleeCount = plugin.getOlmMeleeAttackCount();
		int mageCount = plugin.getOlmMageAttackCount();

		String text;
		boolean lastAttackOfCycle;
		if (meleeCount > 0)
		{
			int ratio = config.olmKiteMeleeRatio().getAttacks();
			text = "Melee " + meleeCount + " / " + ratio;
			lastAttackOfCycle = meleeCount >= ratio;
		}
		else if (mageCount > 0)
		{
			int ratio = config.olmKiteMageRatio().getAttacks();
			text = "Mage " + mageCount + " / " + ratio;
			lastAttackOfCycle = mageCount >= ratio;
		}
		else
		{
			return;
		}

		Color color = lastAttackOfCycle ? SPECIAL_WARNING_COLOR : KITE_COUNTER_COLOR;
		Point canvasPoint = localPlayer.getCanvasTextLocation(graphics, text, 40);
		renderText(graphics, text, color, canvasPoint);
	}

	private void renderActorHighlight(Graphics2D graphics, Player player, Color color)
	{
		if (player.getConvexHull() != null)
		{
			OverlayUtil.renderPolygon(graphics, player.getConvexHull(), color);
		}
	}

	private void renderPhaseBanner(Graphics2D graphics)
	{
		String text = plugin.getOlmPhase().getLabel();
		Point canvasPoint = client.getLocalPlayer().getCanvasTextLocation(graphics, text, 80);
		if (canvasPoint != null)
		{
			graphics.setFont(new Font("Arial", Font.BOLD, 16));
			graphics.setColor(Color.BLACK);
			graphics.drawString(text, canvasPoint.getX() + 1, canvasPoint.getY() + 1);
			graphics.setColor(PHASE_BANNER_COLOR);
			graphics.drawString(text, canvasPoint.getX(), canvasPoint.getY());
		}
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
		graphics.setFont(new Font("Arial", Font.BOLD, 12));
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, location.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), location.getY());
	}
}
