package com.kotoriinfernooverlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kotoriinfernooverlay.displaymodes.KotoriWaveDisplayMode;
import lombok.AccessLevel;
import lombok.Setter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;

@Singleton
class KotoriInfernoWaveOverlay extends Overlay
{
	private final KotoriInfernoOverlayPlugin plugin;
	private final PanelComponent panelComponent;

	@Setter(AccessLevel.PACKAGE)
	private Color waveHeaderColor;

	@Setter(AccessLevel.PACKAGE)
	private Color waveTextColor;

	@Setter(AccessLevel.PACKAGE)
	private KotoriWaveDisplayMode displayMode;

	@Inject
	private KotoriInfernoOverlayConfig config;

	@Inject
	KotoriInfernoWaveOverlay(KotoriInfernoOverlayPlugin plugin)
	{
		this.plugin = plugin;
		this.panelComponent = new PanelComponent();
		setPosition(OverlayPosition.TOP_RIGHT);
		setPriority(PRIORITY_HIGHEST);
		panelComponent.setPreferredSize(new Dimension(160, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();

		if (displayMode == KotoriWaveDisplayMode.CURRENT || displayMode == KotoriWaveDisplayMode.BOTH)
		{
			KotoriInfernoWaveMappings.addWaveComponent(config, panelComponent,
				"Current Wave (Wave " + plugin.getCurrentWaveNumber() + ")", plugin.getCurrentWaveNumber(), waveHeaderColor, waveTextColor);
		}

		if (displayMode == KotoriWaveDisplayMode.NEXT || displayMode == KotoriWaveDisplayMode.BOTH)
		{
			KotoriInfernoWaveMappings.addWaveComponent(config, panelComponent,
				"Next Wave (Wave " + plugin.getNextWaveNumber() + ")", plugin.getNextWaveNumber(), waveHeaderColor, waveTextColor);
		}

		return panelComponent.render(graphics);
	}
}
