package com.pinggraph;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.LinkedList;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingGraphOverlay extends OverlayPanel {

    private final Client client;
    private final PingGraphPlugin pingGraphPlugin;
    private final PingGraphConfig pingGraphConfig;

    @Inject
    private PingGraphOverlay(Client client, PingGraphPlugin pingGraphPlugin, PingGraphConfig pingGraphConfig) {
        this.client = client;
        this.pingGraphPlugin = pingGraphPlugin;
        this.pingGraphConfig = pingGraphConfig;
        if (getPreferredSize() == null) {
            PingGraphOverlay.this.setPreferredSize(new Dimension(180, 60));
        }
        setPosition(OverlayPosition.TOP_LEFT);
        setMinimumSize(15);
    }



    LayoutableRenderableEntity graphEntity = new LayoutableRenderableEntity() {
        @Override
        public Dimension render(Graphics2D graphics) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            if (pingGraphConfig.toggleBehind()) {
                setLayer(OverlayLayer.ABOVE_SCENE);
            } else {
                setLayer(OverlayLayer.ABOVE_WIDGETS);
            }

            int overlayWidth, overlayHeight;
            try {
                overlayWidth = getPreferredSize().width;
                overlayHeight = getPreferredSize().height;
            } catch (NullPointerException e) {
                PingGraphOverlay.this.setPreferredSize(new Dimension(180, 60));
                overlayWidth = 180;
                overlayHeight = 60;
            }

            int width, height, tempX;                       // width and height - size of the graph

            int marginGraphHeight = 15;
            int marginGraphWidth = 10;
            if (pingGraphConfig.hideMargin()) {
                width = overlayWidth;                       // set graph width to whole plugin width
                height = overlayHeight - marginGraphHeight; // remove the extra height
            } else {
                width = overlayWidth - marginGraphWidth * 2;
                height = overlayHeight - marginGraphHeight * 2;
            }

            //background rect
            graphics.setColor(pingGraphConfig.overlayBackgroundColor());
            graphics.fillRect(0, 0, overlayWidth, overlayHeight);

            //outside border
            graphics.setColor(pingGraphConfig.overlayBorderColor());
            graphics.drawRect(0, 0, overlayWidth, overlayHeight);

            if (!pingGraphConfig.toggleLineOnly()) {
                //inside border
                graphics.setColor(pingGraphConfig.graphBorderColor());
                int x = pingGraphConfig.hideMargin() ? 0 : marginGraphWidth - 1;
                graphics.drawRect(x, marginGraphHeight + 1, width, height);

                //inside rect
                graphics.setColor(pingGraphConfig.graphBackgroundColor());
                graphics.fillRect(x, marginGraphHeight + 1, width, height);

                graphics.setColor(pingGraphConfig.graphTextColor());


                String rightLabel = labelText(pingGraphConfig.rightLabel());

                //Right label
                int strWidth = graphics.getFontMetrics().stringWidth(rightLabel);
                graphics.drawString(rightLabel, overlayWidth - strWidth - marginGraphWidth, marginGraphHeight - 1);

                //Left label
                String leftLabel = labelText(pingGraphConfig.leftLabel());
                graphics.drawString(leftLabel, marginGraphWidth, marginGraphHeight - 1);
            } else {
                width = overlayWidth;
                height = overlayHeight;
            }

            LinkedList<Integer> data;
            data = pingGraphConfig.graphTicks() ? pingGraphPlugin.getTickTimeList() : pingGraphPlugin.getPingList();

            int dataStart = (data.size() > overlayWidth) ? (data.size() - overlayWidth) : 0;
            pingGraphPlugin.setGraphStart(dataStart);

            int maxValue;
            int minValue;
            if(pingGraphConfig.graphTicks()) {
                maxValue = pingGraphPlugin.getMaxTick();
                minValue = pingGraphPlugin.getMinTick();
            } else {
                maxValue = pingGraphPlugin.getMaxPing();
                minValue = pingGraphPlugin.getMinPing();
            }


            // change maxPing to 100, prevents div by 0 in-case of error
            if (maxValue <= 0) {
                maxValue = 100;
            }
            //if checked the graph will scale between min and max ping
            if (!pingGraphConfig.toggleRange()) {

                double round = maxValue > 50 ? 50 : 10; // round ping up to nearest 50ms if > 50 else 10ms
                maxValue = (int) (Math.ceil((double) pingGraphPlugin.getMaxPing() / round) * round);

                if ((maxValue - pingGraphPlugin.getMaxPing()) <= (0.2 * maxValue)) {
                    maxValue += round; // increase the max ping to move the graph away from the top
                }
            }

            if (maxValue == minValue) {
                maxValue++;
                minValue--;
            }

            if(!pingGraphConfig.hideGraph()) {
                //drawing line graph
                graphics.setColor(pingGraphConfig.graphLineColor());
                int oldX, oldY = oldX = -1;

                for (int x = dataStart; x < data.size(); x++) {
                    int y = data.get(x);

                    y = y < 0 ? maxValue - 1 : y; // change a "timed out" to spike rather than drop

                    //((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
                    //scale the x and y values to fit to the plugin
                    if (pingGraphConfig.toggleRange()) { //limit between min ping and max ping
                        y = height - (((height - 2) * (y - minValue)) / (maxValue - minValue) + 1);
                    } else {
                        y = height - (height * y / maxValue);
                    }

                    tempX = ((width) * (x - dataStart) / (data.size() - dataStart));

                    y += marginGraphHeight;

                    if (!pingGraphConfig.hideMargin()) {
                        tempX += marginGraphWidth;
                    }

                    if (pingGraphConfig.toggleLineOnly()) {
                        if (!pingGraphConfig.hideMargin())
                            tempX -= marginGraphWidth;
                        y -= marginGraphHeight;
                    }

                    if (y >= 0) {
                        graphics.drawLine(tempX, y, tempX, y);
                    }
                    if (oldX != -1 && y >= 0) {
                        graphics.drawLine(oldX, oldY, tempX, y);
                    }

                    oldX = tempX;
                    oldY = y;
                }
            }
            return new Dimension(overlayWidth - 8, overlayHeight - 8);
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(getPreferredSize().width, getPreferredSize().height);
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

    // returns a string based on user settings
    private String labelText(PingGraphConfig.Labels setting){
        String tempLabel = "Label Error";
        for(int i = 0; i < 1; i++) {
            switch (setting) {
                case LATENCY:
                    tempLabel = "Latency: " + pingGraphPlugin.getCurrentPing()  + "ms";
                    if (pingGraphPlugin.getCurrentPing() < 0)
                        tempLabel = "Timed out";
                    break;
                case PING:
                    tempLabel = "Ping: " + pingGraphPlugin.getCurrentPing() + "ms";
                    if (pingGraphPlugin.getCurrentPing() < 0)
                        tempLabel = "Timed out";
                    break;
                case PINGMAX:
                    tempLabel = "Max: " + pingGraphPlugin.getMaxPing() + "ms";
                    break;
                case PINGMIN:
                    tempLabel = "Min: " + pingGraphPlugin.getMaxPing() + "ms";
                    break;
                case TICK:
                    tempLabel = "Tick: " + pingGraphPlugin.getCurrentTick() + "ms";
                    break;
                case TICKMAX:
                    tempLabel = "Max Tick: " + pingGraphPlugin.getMaxTick() + "ms";
                    break;
                case TICKMIN:
                    tempLabel = "Min Tick: " + pingGraphPlugin.getMinTick() + "ms";
                    break;
                case NONE:
                    tempLabel = "";
                    break;
            }
        }
        return tempLabel;
    }

}
