package com.infernooverlay;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches the real RuneLite client locally with InfernoOverlayPlugin registered, so it
 * can be tested without going through the Plugin Hub. Run this class's main method (e.g.
 * "Run" in your IDE); log in with your own account, then enable "Inferno Overlay" in the
 * plugin list.
 */
public class InfernoOverlayPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InfernoOverlayPlugin.class);
		RuneLite.main(args);
	}
}
