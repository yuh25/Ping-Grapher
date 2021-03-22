package com.pinggraph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;


import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

public class PingGrpahOverlay extends Overlay{

    private final Client client;
    private final PingGraphPlugin pingGraphPlugin;
    private final PingGraphConfig pingGraphConfig;

    @Inject
    private PingGrpahOverlay(Client client, PingGraphPlugin pingGraphPlugin, PingGraphConfig pingGraphConfig)
    {
        this.client = client;
        this.pingGraphPlugin = pingGraphPlugin;
        this.pingGraphConfig = pingGraphConfig;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        final int ping = pingGraphPlugin.getCurrentPing();
        if (ping < 0)
        {
            return null;
        }

        final String text = ping + " ms";
        final int textWidth = graphics.getFontMetrics().stringWidth(text);
        final int textHeight = graphics.getFontMetrics().getAscent() - graphics.getFontMetrics().getDescent();

        // Adjust ping offset for logout button
        Widget logoutButton = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_LOGOUT_BUTTON);
        int xOffset = 20;
        if (logoutButton != null && !logoutButton.isHidden())
        {
            xOffset += logoutButton.getWidth();
        }

        final int width = (int) client.getRealDimensions().getWidth();
        final Point point = new Point(width - textWidth - xOffset, textHeight + 20);
        OverlayUtil.renderTextLocation(graphics, point, text, Color.YELLOW);
        return null;
    }
}
