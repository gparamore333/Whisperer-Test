package com.infernooverlay;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import com.infernooverlay.displaymodes.NpcNamingMode;
import com.infernooverlay.displaymodes.WaveDisplayMode;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * A text panel listing which monsters spawn on the current/next wave. This is static
 * reference data (see {@link InfernoWaveMappings}) rendered for convenience - it is not
 * live tracking of anything and does not affect gameplay.
 */
class InfernoWaveOverlay extends Overlay
{
	private final InfernoOverlayPlugin plugin;
	private final InfernoOverlayConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private InfernoWaveOverlay(InfernoOverlayPlugin plugin, InfernoOverlayConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WaveDisplayMode mode = config.waveDisplay();
		if (mode == WaveDisplayMode.NONE)
		{
			return null;
		}

		panelComponent.getChildren().clear();

		if (mode == WaveDisplayMode.CURRENT || mode == WaveDisplayMode.BOTH)
		{
			addWavePanel(plugin.getCurrentWaveNumber(), "Wave " + plugin.getCurrentWaveNumber());
		}

		if (mode == WaveDisplayMode.NEXT || mode == WaveDisplayMode.BOTH)
		{
			int nextWave = plugin.getNextWaveNumber();
			if (nextWave != -1)
			{
				addWavePanel(nextWave, "Wave " + nextWave + " (next)");
			}
		}

		if (panelComponent.getChildren().isEmpty())
		{
			return null;
		}

		return panelComponent.render(graphics);
	}

	private void addWavePanel(int waveNumber, String header)
	{
		int[] composition = InfernoWaveMappings.WAVE_MAPPING.get(waveNumber);
		if (composition == null)
		{
			return;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left(header)
			.leftColor(config.waveHeaderColor())
			.build());

		Map<Integer, Integer> counts = new LinkedHashMap<>();
		for (int monsterCode : composition)
		{
			counts.merge(monsterCode, 1, Integer::sum);
		}

		boolean complex = config.npcNaming() == NpcNamingMode.COMPLEX;
		for (Map.Entry<Integer, Integer> entry : counts.entrySet())
		{
			String name = complex ? InfernoWaveMappings.COMPLEX_NAMES.get(entry.getKey()) : InfernoWaveMappings.SIMPLE_NAMES.get(entry.getKey());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(name)
				.right("x" + entry.getValue())
				.leftColor(config.waveTextColor())
				.rightColor(config.waveTextColor())
				.build());
		}
	}
}
