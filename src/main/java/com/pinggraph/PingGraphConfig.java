package com.pinggraph;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("pinggraph")
public interface PingGraphConfig extends Config
{
	@Alpha
	@ConfigItem(
			position = 0,
			keyName = "graphTextColor",
			name = "Graph Text Color",
			description = "The color of the text"
	)
	default Color graphTextColor() {
		return new Color(255, 255, 24, 255);
	}

	@Alpha
	@ConfigItem(
			position = 1,
			keyName = "graphLineColor",
			name = "Graph Line Color",
			description = "The color of the graph line"
	)
	default Color graphLineColor() {
		return new Color(255, 255, 0, 255);
	}

	@Alpha
	@ConfigItem(
			position = 2,
			keyName = "OverlayBackgroundColor",
			name = "Overlay Background Color",
			description = "The background color of the overlay"
	)
	default Color overlayBackgroundColor() {
		return new Color(0, 0,  0, 100);
	}

	@Alpha
	@ConfigItem(
			position = 3,
			keyName = "OverlayBorderColor",
			name = "Overlay Border Color",
			description = "The border color of the overlay"
	)
	default Color overlayBorderColor() {
		return new Color(0, 0,  0, 70);
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "graphBackgroundColor",
			name = "Graph Background Color",
			description = "The background color of the graph"
	)
	default Color graphBackgroundColor() { return new Color(0, 0,  0, 120);
	}

	@Alpha
	@ConfigItem(
			position = 5,
			keyName = "graphBorderColor",
			name = "Graph Border Color",
			description = "The border color of the graph"
	)
	default Color graphBorderColor() {
		return new Color(0, 0,  0, 70);
	}

    @ConfigItem(
            position = 6,
            keyName = "toggleLineOnly",
            name = "Hide Labels",
            description = "Changes the plugin to only show a line"
    )
    default boolean toggleLineOnly() {
        return false;
    }

	@ConfigItem(
			position = 7,
			keyName = "toggleBehind",
			name = "Hide Behind Interfaces",
			description = "Hides graph behind interfaces i.e bank and map"
	)
	default boolean toggleBehind() {
		return false;
	}

	@ConfigItem(
			position = 8,
			keyName = "toggleMaxMin",
			name = "Scale Between Max and Min Ping",
			description = "Only show range between max and min ping"
	)
	default boolean toggleRange() {
		return false;
	}

	@ConfigItem(
			position = 9,
			keyName = "hideMargin",
			name = "Hide Bottom and Side Margins",
			description = "Removes the margins that surround the graph"
	)
	default boolean hideMargin() {
		return false;
	}

	@ConfigItem(
			position = 10,
			keyName = "graphTicks",
			name = "Graph Game Ticks",
			description = "Changes the graph to show game ticks(normally around 600ms)"
	)
	default boolean graphTicks() {
		return false;
	}
}
