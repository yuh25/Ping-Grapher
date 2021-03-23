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

    private final int marginGraphLeft = 10;
    private final int marginGraphTop = 15;
    private final int marginGraphRight = 10;
    private final int marginGraphBottom = 15;
    private final double round = 50.0; //used for rounding maxPing, looks nicer

    @Inject
    private PingGraphOverlay(Client client, PingGraphPlugin pingGraphPlugin, PingGraphConfig pingGraphConfig)
    {
        this.client = client;
        this.pingGraphPlugin = pingGraphPlugin;
        this.pingGraphConfig = pingGraphConfig;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.BOTTOM_LEFT);
    }

    LayoutableRenderableEntity graphEntity = new LayoutableRenderableEntity() {
        @Override
        public Dimension render(Graphics2D graphics) {

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);

            int width = pingGraphConfig.graphWidth();
            int height = pingGraphConfig.graphHeight();

            graphics.setColor(pingGraphConfig.graphBackgroundColor());
            graphics.fillRect(0, 0, this.getBounds().width, this.getBounds().height);   //background

            //overlay border box
            graphics.setColor(pingGraphConfig.graphBoarderColor());
            graphics.drawRect(0, 0, this.getBounds().width, this.getBounds().height);   //outside boarder
            graphics.drawRect(marginGraphLeft-1, marginGraphTop, width, height);           //inside boarder

            graphics.setColor(pingGraphConfig.graphLineColor());
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
            for (int x = 0; x < pingGraphPlugin.getPingList().size(); x++) {

                int y = pingGraphPlugin.getPingList().get(x);
                y = height - (height * y / maxPing); // scale the y values between 0 and max ping
                int tempX = width * x / 100;//100 - number of cells

                if (y >= 0) {
                    graphics.drawLine(marginGraphLeft + tempX, marginGraphTop + y, marginGraphLeft + tempX, marginGraphTop + y);
                }

                if (oldX != -1 && y >= 0) {
                    graphics.drawLine(marginGraphLeft + oldX, marginGraphTop + oldY, marginGraphLeft + tempX  - 1, marginGraphTop + y);
                }
                oldX = tempX;
                oldY = y;
            }

            //
            graphics.setColor(pingGraphConfig.graphTextColor());
            String temp = currPing + "ms";
            if(currPing < 0) temp = "Timed out";
            graphics.drawString("Latency: " + currPing + "ms",marginGraphLeft, marginGraphTop); //current Ping

            int strWidth = graphics.getFontMetrics().stringWidth("0ms");
            graphics.drawString("0ms",this.getBounds().width - strWidth, this.getBounds().height); //0

            strWidth = graphics.getFontMetrics().stringWidth(maxPing + "ms");
            graphics.drawString(maxPing + "ms",this.getBounds().width - strWidth, marginGraphTop);// Max Ping

            return new Dimension(this.getBounds().width, this.getBounds().height);
        }

        @Override
        public Rectangle getBounds() {
            int boundsWidth = marginGraphLeft+ pingGraphConfig.graphWidth()+marginGraphRight;
            int boundsHeight = marginGraphTop+ pingGraphConfig.graphHeight()+marginGraphBottom;
            return new Rectangle(boundsWidth, boundsHeight);
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

        panelComponent.getChildren().add(graphEntity);
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));

        return super.render(graphics);
    }
}
