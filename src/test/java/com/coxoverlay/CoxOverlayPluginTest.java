package com.coxoverlay;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches the real RuneLite client locally with CoxOverlayPlugin registered, so it can be
 * tested without going through the Plugin Hub. Run this class's main method (e.g. "Run" in
 * your IDE); log in with your own account, then enable "CoX Overlay" in the plugin list.
 */
public class CoxOverlayPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CoxOverlayPlugin.class);
		RuneLite.main(args);
	}
}
