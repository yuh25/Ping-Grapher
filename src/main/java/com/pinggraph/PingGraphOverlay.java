package com.pinggraph;

import java.awt.Color;
import java.awt.Dimension;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingGraphOverlay extends OverlayPanel {

    private final Client client;
    private final PingGraphPlugin pingGraphPlugin;
    private final PingGraphConfig pingGraphConfig;

    private final int marginGraphWidth = 10;
    private final int marginGraphHeight = 15;

    private final double round = 50.0; //used for rounding maxPing, looks nicer

    @Inject
    private PingGraphOverlay(Client client, PingGraphPlugin pingGraphPlugin, PingGraphConfig pingGraphConfig)
    {
        this.client = client;
        this.pingGraphPlugin = pingGraphPlugin;
        this.pingGraphConfig = pingGraphConfig;
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.BOTTOM_LEFT);

    }

    LayoutableRenderableEntity graphEntity = new LayoutableRenderableEntity() {
        @Override
        public Dimension render(Graphics2D graphics) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);

            int overlayWidth = pingGraphConfig.graphWidth();
            int overlayHeight = pingGraphConfig.graphHeight();

            int width = overlayWidth - marginGraphWidth * 2;
            int height = overlayHeight - marginGraphHeight * 2;

            //background rect
            graphics.setColor(pingGraphConfig.graphBackgroundColor());
            graphics.fillRect(0, 0, overlayWidth, overlayHeight);

            //overlay border box
            graphics.setColor(pingGraphConfig.graphBorderColor());
            graphics.drawRect(0, 0, overlayWidth, overlayHeight);                //outside border
            graphics.drawRect(marginGraphWidth-1, marginGraphHeight+1, width, height);//inside border

            int oldX = -1;
            int oldY = -1;
            int currPing = pingGraphPlugin.getCurrentPing();

            // round maxPing up to nearest 50ms
            int maxPing = (int)(Math.ceil((double)pingGraphPlugin.getMaxPing() / round) * round);

            if(maxPing <= 0) {
                // change maxPing to 100, prevents div by 0 incase of error
                maxPing = 100;
            }

            if((maxPing - pingGraphPlugin.getMaxPing()) <= (0.2 * maxPing)) {
                // increase the max ping to move the graph away from the top
                maxPing += round;
            }

            //drawing line graph
            graphics.setColor(pingGraphConfig.graphLineColor());
            for (int x = 0; x < pingGraphPlugin.getPingList().size(); x++) {

                int y = pingGraphPlugin.getPingList().get(x);

                // change a "timed out" to spike rather than drop
                if(y < 0){
                    y = maxPing - 1;
                }

                //scale the y values between 0 and max ping
                y = height - (height * y / maxPing) + marginGraphHeight;

                //scale the x values between to graph
                int tempX = width * x / 100 + marginGraphWidth;

                if (y >= 0) {
                    graphics.drawLine(tempX, y, tempX, y);
                }

                if (oldX != -1 && y >= 0) {
                    graphics.drawLine(oldX, oldY, tempX, y);
                }
                oldX = tempX;
                oldY = y;
            }

            graphics.setColor(pingGraphConfig.graphTextColor());
            String temp = "Latency: " + currPing + "ms";
            if(currPing < 0) temp = "Latency: Timed out";
            graphics.drawString(temp, marginGraphWidth, marginGraphHeight-1); //current Ping

            int strWidth = graphics.getFontMetrics().stringWidth("0ms");
            graphics.drawString("0ms",overlayWidth - strWidth, overlayHeight); //0

            strWidth = graphics.getFontMetrics().stringWidth(maxPing + "ms");
            graphics.drawString(maxPing + "ms",overlayWidth - strWidth, marginGraphHeight-1);//Max Ping

            //Fixed runelite border - no idea why it works
            return new Dimension(overlayWidth - 8, overlayHeight - 8);
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(pingGraphConfig.graphWidth(), pingGraphConfig.graphHeight());
        }

        @Override
        public void setPreferredLocation(java.awt.Point position) {

        }

        @Override
        public void setPreferredSize(Dimension dimension) {

        }
    };


    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.render(graphics);
        panelComponent.getChildren().add(graphEntity);
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
        return super.render(graphics);
    }
}
