package com.pinggraph;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

import lombok.AllArgsConstructor;

@ConfigGroup("pinggraph")
public interface PingGraphConfig extends Config {

    @ConfigSection(
            name = "Font Settings",
            description = "Font Settings",
            position = 98
    )
    String fontSection = "fontSection";

    @ConfigSection(
            name = "Warning Settings",
            description = "Change the colors of the overlay when lagging",
            position = 99
    )
    String warnSection = "warnSection";


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
        return new Color(0, 0, 0, 100);
    }



    @Alpha
    @ConfigItem(
            position = 3,
            keyName = "OverlayBorderColor",
            name = "Overlay Border Color",
            description = "The border color of the overlay"
    )
    default Color overlayBorderColor() {
        return new Color(0, 0, 0, 70);
    }



    @Alpha
    @ConfigItem(
            position = 4,
            keyName = "graphBackgroundColor",
            name = "Graph Background Color",
            description = "The background color of the graph"
    )
    default Color graphBackgroundColor() {
        return new Color(0, 0, 0, 120);
    }



    @Alpha
    @ConfigItem(
            position = 5,
            keyName = "graphBorderColor",
            name = "Graph Border Color",
            description = "The border color of the graph"
    )
    default Color graphBorderColor() {
        return new Color(0, 0, 0, 70);
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



    @ConfigItem(
            position = 10,
            keyName = "hideGraph",
            name = "Hide Graph",
            description = "Hides the Graph leaving only labels"
    )
    default boolean hideGraph() {
        return false;
    }



    @ConfigItem(
            position = 11,
            keyName = "leftLabel",
            name = "Left Label",
            description = "Default: \"Current Latency\"",
            section = fontSection
    )
    default Labels leftLabel() {
        return Labels.LATENCY;
    }



    @ConfigItem(
            position = 12,
            keyName = "rightLabel",
            name = "Right Label",
            description = "Default: \"Max Ping Value\"",
            section = fontSection
    )
    default Labels rightLabel() {
        return Labels.PINGMAX;
    }



    @ConfigItem(
            position = 13,
            keyName = "fontName",
            name = "Font Name",
            description = "Default: \"Runescape Small\"",
            section = fontSection
    )
    default String fontName() {
        return "Runescape Small";
    }



    @ConfigItem(
            position = 14,
            keyName = "fontSize",
            name = "Font Size",
            description = "Default: 16",
            section = fontSection
    )
    default int fontSize() {
        return 16;
    }



    @ConfigItem(
            position = 15,
            keyName = "fontStyle",
            name = "Font Style",
            description = "Default: Regular",
            section = fontSection
    )
    default FontStyle fontStyle() {
        return FontStyle.REGULAR;
    }



    enum FontStyle {
        REGULAR(0),
        BOLD(1),
        ITALICS(2);

        private final int value;

        FontStyle(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }



    @AllArgsConstructor
    enum Labels {
        PING("Current Ping"),
        LATENCY("Current Latency"),
        PINGMAX("Max Ping Value"),
        PINGMIN("Min Ping Value"),
        TICK("Current tick"),
        TICKMAX("Max Tick Value"),
        TICKMIN("Min Tick Value"),
        NONE("Blank");


        private final String name;

        @Override
        public String toString() {
            return name;
        }
    }



    @ConfigItem(
            position = 16,
            keyName = "warnPingVal",
            name = "Ping Threshold (ms)",
            description = "Warns you when the ping exceeds this threshold",
            section = warnSection
    )
    default int warnPingVal() {
        return 100;
    }



    @ConfigItem(
            position = 17,
            keyName = "warnTickVal",
            name = "Tick Threshold (ms)",
            description = "Warns you when the tick exceeds this threshold",
            section = warnSection
    )
    default int warnTickVal() {
        return 800;
    }



    @ConfigItem(
            position = 18,
            keyName = "warningFontToggle",
            name = "Swap Font Color on Warning",
            description = "Change the fonts color when the warning value is too high",
            section = warnSection
    )
    default boolean warningFontToggle() {
        return false;
    }



    @ConfigItem(
            position = 19,
            keyName = "warningGraphBGToggle",
            name = "Swap Graph BG Color on Warning",
            description = "Change the Graph background color when the warning value is too high",
            section = warnSection
    )
    default boolean warningGraphBGToggle() {
        return false;
    }



    @ConfigItem(
            position = 20,
            keyName = "warningBGToggle",
            name = "Swap Overlay BG Color on Warning",
            description = "Change the Overlays background color when the warning value is too high",
            section = warnSection
    )
    default boolean warningBGToggle() {
        return false;
    }



    @ConfigItem(
            position = 21,
            keyName = "warnMaxToggle",
            name = "Persisting Warning",
            description = "The warning will persist until the all displayed values are below the warning values",
            section = warnSection
    )
    default boolean warnMaxToggle() {
        return false;
    }



    @Alpha
    @ConfigItem(
            position = 22,
            keyName = "warningBGColor",
            name = "BG Warning Color",
            description = "The color the Overlays background will change to while exceeding the threshold",
            section = warnSection
    )
    default Color warningBGColor() {
        return new Color(255, 30, 30, 64);
    }



    @Alpha
    @ConfigItem(
            position = 23,
            keyName = "warningGraphBGColor",
            name = "Graph BG Warning Color",
            description = "The color the Graph Background will change to while exceeding the threshold",
            section = warnSection
    )
    default Color warningGraphBGColor() {
        return new Color(255, 30, 30, 64);
    }



    @Alpha
    @ConfigItem(
            position = 24,
            keyName = "warningFontColor",
            name = "Text Warning Color",
            description = "The color text will change to while exceeding the threshold",
            section = warnSection
    )
    default Color warningFontColor() {
        return new Color(255, 30, 30, 255);
    }

}
