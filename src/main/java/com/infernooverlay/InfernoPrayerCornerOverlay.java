package com.infernooverlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.inject.Inject;

import com.infernooverlay.displaymodes.PrayerDisplayMode;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * A single prayer-protection icon in the corner of the screen, showing which prayer
 * matches the next incoming attack. Tinted red if you aren't already praying it. This is
 * a label only - the player still has to click the prayer themselves.
 */
class InfernoPrayerCornerOverlay extends Overlay
{
	private final Client client;
	private final InfernoOverlayPlugin plugin;
	private final InfernoOverlayConfig config;
	private final SpriteManager spriteManager;
	private final PanelComponent panelComponent = new PanelComponent();

	private BufferedImage meleeSprite;
	private BufferedImage rangedSprite;
	private BufferedImage magicSprite;

	@Inject
	private InfernoPrayerCornerOverlay(Client client, InfernoOverlayPlugin plugin, InfernoOverlayConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PrayerDisplayMode mode = config.prayerDisplayMode();
		if (mode != PrayerDisplayMode.BOTTOM_RIGHT && mode != PrayerDisplayMode.BOTH)
		{
			return null;
		}

		InfernoMonster.Attack attack = plugin.getClosestAttack();
		if (attack == null || attack.getPrayer() == null)
		{
			return null;
		}

		BufferedImage sprite = getPrayerImage(attack);
		if (sprite == null)
		{
			return null;
		}

		boolean prayingCorrectly = client.isPrayerActive(attack.getPrayer());

		panelComponent.getChildren().clear();
		panelComponent.setBackgroundColor(prayingCorrectly ? ComponentConstants.STANDARD_BACKGROUND_COLOR : new Color(150, 0, 0, 190));
		panelComponent.getChildren().add(new ImageComponent(sprite));

		return panelComponent.render(graphics);
	}

	private BufferedImage getPrayerImage(InfernoMonster.Attack attack)
	{
		switch (attack)
		{
			case MELEE:
				if (meleeSprite == null)
				{
					meleeSprite = spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MELEE, 0);
				}
				return meleeSprite;
			case RANGED:
				if (rangedSprite == null)
				{
					rangedSprite = spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0);
				}
				return rangedSprite;
			case MAGIC:
				if (magicSprite == null)
				{
					magicSprite = spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0);
				}
				return magicSprite;
			default:
				return null;
		}
	}
}
