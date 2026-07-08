package com.osrsoverlaytest;

import com.infernooverlay.InfernoOverlayPlugin;
import com.kotoriinfernooverlay.KotoriInfernoOverlayPlugin;
import com.whispereroverlay.WhispererOverlayPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches the real RuneLite client locally with every overlay plugin registered, so they can
 * all be tested in the same session without going through the Plugin Hub. Run this class's
 * main method; log in with your own account, then enable "Whisperer Overlay", "Inferno
 * Overlay", and/or "Kotori Inferno Overlay" in the plugin list as needed - enabling both
 * Inferno plugins at once is useful for comparing them side-by-side.
 */
public class AllOverlaysTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhispererOverlayPlugin.class, InfernoOverlayPlugin.class, KotoriInfernoOverlayPlugin.class);
		RuneLite.main(args);
	}
}
