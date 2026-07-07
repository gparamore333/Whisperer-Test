package com.infernooverlay;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * A countdown to the next mage/ranger spawn set during the Zuk fight. It's just a
 * stopwatch driven by wall-clock time - it doesn't read or predict anything beyond
 * when to start/pause, both decided from Zuk's health ratio by the plugin.
 */
class InfernoSpawnTimerInfobox extends InfoBox
{
	private static final int SPAWN_DURATION_SECONDS = 210;
	private static final int WARNING_THRESHOLD_SECONDS = 120;
	private static final int DANGER_THRESHOLD_SECONDS = 30;

	private long resumedAtEpochSecond = -1;
	private int elapsedSecondsAtPause = 0;
	private boolean running = false;

	InfernoSpawnTimerInfobox(BufferedImage image, Plugin plugin)
	{
		super(image, plugin);
		setTooltip("Time until next Zuk spawn set");
	}

	boolean isRunning()
	{
		return running;
	}

	void run()
	{
		running = true;
		resumedAtEpochSecond = System.currentTimeMillis() / 1000;
	}

	void pause()
	{
		if (running)
		{
			elapsedSecondsAtPause = elapsedSeconds();
			running = false;
		}
	}

	void reset()
	{
		running = false;
		elapsedSecondsAtPause = 0;
		resumedAtEpochSecond = -1;
	}

	private int elapsedSeconds()
	{
		if (!running)
		{
			return elapsedSecondsAtPause;
		}
		return elapsedSecondsAtPause + (int) (System.currentTimeMillis() / 1000 - resumedAtEpochSecond);
	}

	private int secondsRemaining()
	{
		return Math.max(0, SPAWN_DURATION_SECONDS - elapsedSeconds());
	}

	@Override
	public String getText()
	{
		int remaining = secondsRemaining();
		return String.format("%d:%02d", remaining / 60, remaining % 60);
	}

	@Override
	public Color getTextColor()
	{
		int remaining = secondsRemaining();
		if (remaining <= DANGER_THRESHOLD_SECONDS)
		{
			return Color.RED;
		}
		if (remaining <= WARNING_THRESHOLD_SECONDS)
		{
			return Color.ORANGE;
		}
		return Color.GREEN;
	}
}
