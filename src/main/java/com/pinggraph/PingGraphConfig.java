package com.pinggraph;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("pinggraph")
public interface PingGraphConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "graphWidth",
			name = "Graph Width",
			description = "Configures the width of the graph."
	)
	default int graphWidth()
	{
		return 170;
	}

	@ConfigItem(
			position = 1,
			keyName = "graphHeight",
			name = "Graph Height",
			description = "Configures the height of the graph."
	)
	default int graphHeight()
	{
		return 60;
	}

	@Alpha
	@ConfigItem(
			position = 2,
			keyName = "graphTextColor",
			name = "Graph Text Color",
			description = "The color of the text"
	)
	default Color graphTextColor() {
		return new Color(255, 255, 24, 255);
	}

	@Alpha
	@ConfigItem(
			position = 3,
			keyName = "graphLineColor",
			name = "Graph Line Color",
			description = "The color of the graph line"
	)
	default Color graphLineColor() {
		return new Color(255, 255, 0, 255);
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "graphBackgroundColor",
			name = "Background Color",
			description = "The background color of the graph."
	)
	default Color graphBackgroundColor() {
		return new Color(0, 0,  0, 100);
	}


	@Alpha
	@ConfigItem(
			position = 5,
			keyName = "graphBorderColor",
			name = "Border Color",
			description = "The border color of the plugin."
	)
	default Color graphBorderColor() {
		return new Color(0, 0,  0, 70);
	}
}
