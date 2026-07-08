package com.osrsoverlaytest;

import com.infernooverlay.InfernoOverlayPlugin;
import com.whispereroverlay.WhispererOverlayPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches the real RuneLite client locally with both overlay plugins registered, so both
 * can be tested in the same session without going through the Plugin Hub. Run this class's
 * main method; log in with your own account, then enable "Whisperer Overlay" and/or
 * "Inferno Overlay" in the plugin list as needed.
 */
public class AllOverlaysTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhispererOverlayPlugin.class, InfernoOverlayPlugin.class);
		RuneLite.main(args);
	}
}
