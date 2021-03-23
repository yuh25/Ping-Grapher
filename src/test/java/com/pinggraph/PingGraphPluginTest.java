package com.pinggraph;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;


public class PingGraphPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PingGraphPlugin.class);
		RuneLite.main(args);
	}
}