package com.pinggraph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.LinkedList;

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

    @Inject
    private PingGraphOverlay(Client client, PingGraphPlugin pingGraphPlugin, PingGraphConfig pingGraphConfig){
        this.client = client;
        this.pingGraphPlugin = pingGraphPlugin;
        this.pingGraphConfig = pingGraphConfig;
        if(getPreferredSize() == null){
            PingGraphOverlay.this.setPreferredSize(new Dimension(180, 60));
        }
        setPosition(OverlayPosition.TOP_LEFT);
    }



    LayoutableRenderableEntity graphEntity = new LayoutableRenderableEntity(){
        @Override
        public Dimension render(Graphics2D graphics) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);

            if(pingGraphConfig.toggleBehind()) {
                setLayer(OverlayLayer.ABOVE_SCENE);
            } else {
                setLayer(OverlayLayer.ABOVE_WIDGETS);
            }

            int overlayWidth, overlayHeight;
            try {
                overlayWidth = getPreferredSize().width;
                overlayHeight = getPreferredSize().height;
            } catch(NullPointerException e){
                System.out.println("NullPointerException - PreferredSize not set");
                PingGraphOverlay.this.setPreferredSize(new Dimension(180, 60));
                overlayWidth = 180;
                overlayHeight = 60;
            }

            int width, height, tempX;                       // width and height - size of the graph

            int marginGraphHeight = 15;
            int marginGraphWidth = 10;
            if(pingGraphConfig.hideMargin()) {
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

            if(!pingGraphConfig.toggleLineOnly()) {
                //inside border
                graphics.setColor(pingGraphConfig.graphBorderColor());
                int x = pingGraphConfig.hideMargin() ? 0 : marginGraphWidth - 1;
                graphics.drawRect(x, marginGraphHeight + 1, width, height);

                //inside rect
                graphics.setColor(pingGraphConfig.graphBackgroundColor());
                graphics.fillRect(x, marginGraphHeight + 1, width, height);

                //max Ping label
                graphics.setColor(pingGraphConfig.graphTextColor());
                String temp = "Max: " + pingGraphPlugin.getMaxPing() + "ms";
                int strWidth = graphics.getFontMetrics().stringWidth(temp);
                graphics.drawString(temp, overlayWidth - strWidth - marginGraphWidth, marginGraphHeight - 1);

                //current Ping label
                temp = "Latency: " + pingGraphPlugin.getCurrentPing() + "ms";
                if (pingGraphPlugin.getCurrentPing() < 0) temp = "Latency: Timed out";
                graphics.drawString(temp, marginGraphWidth, marginGraphHeight - 1);
            } else {
                width = overlayWidth;
                height = overlayHeight;
            }

            LinkedList<Integer> data;
            data = pingGraphConfig.graphTicks() ? pingGraphPlugin.getTickTimeList() : pingGraphPlugin.getPingList();
            int dataStart = (data.size() > overlayWidth) ? (data.size() - overlayWidth) : 0;

            int maxPing = pingGraphPlugin.getMaxPing();
            int minPing = pingGraphPlugin.getMinPing();

            if(data.size() > overlayWidth){
                dataStart = (data.size() - overlayWidth);
                int[] temp = pingGraphPlugin.getMaxMinFromList(data, dataStart);
                maxPing = temp[0];
                minPing = temp[1];
            }

            // change maxPing to 100, prevents div by 0 in-case of error
            if (maxPing <= 0) { maxPing = 100; }
            //if checked the graph will scale between min and max ping
            if(!pingGraphConfig.toggleRange()) {

                double round = maxPing > 50 ? 50 : 10; // round ping up to nearest 50ms if > 50 else 10ms
                maxPing = (int) (Math.ceil((double) pingGraphPlugin.getMaxPing() / round) * round);

                if ((maxPing - pingGraphPlugin.getMaxPing()) <= (0.2 * maxPing)) {
                    maxPing += round; // increase the max ping to move the graph away from the top
                }
            }

            if(maxPing == minPing) {
                maxPing++;
                minPing--;
            }

            //drawing line graph
            graphics.setColor(pingGraphConfig.graphLineColor());
            int oldX, oldY = oldX = -1;

            for (int x = dataStart; x < data.size(); x++) {
                int y = data.get(x);

                y = y < 0 ? maxPing - 1 : y; // change a "timed out" to spike rather than drop

                //((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
                //scale the x and y values to fit to the plugin
                if(pingGraphConfig.toggleRange()) { //limit between min ping and max ping
                    y = height - (((height - 2) * (y - minPing)) / (maxPing - minPing) + 1);
                } else {
                    y = height - (height * y / maxPing);
                }

                tempX = ((width) * (x - dataStart) / (data.size() - dataStart));

                y += marginGraphHeight;

                if(!pingGraphConfig.hideMargin()){
                    tempX += marginGraphWidth;
                }

                if(pingGraphConfig.toggleLineOnly()){
                    if(!pingGraphConfig.hideMargin())
                        tempX -= marginGraphWidth;
                    y -= marginGraphHeight;
                }

                if (y >= 0) { graphics.drawLine(tempX, y, tempX, y); }
                if (oldX != -1 && y >= 0) { graphics.drawLine(oldX, oldY, tempX, y); }

                oldX = tempX;
                oldY = y;
            }

            return new Dimension(overlayWidth - 8, overlayHeight - 8);
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(getPreferredSize().width, getPreferredSize().height);
        }

        @Override
        public void setPreferredLocation(java.awt.Point position) {}

        @Override
        public void setPreferredSize(Dimension dimension) {}
    };


    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.render(graphics);
        panelComponent.getChildren().add(graphEntity);
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
        return super.render(graphics);
    }
}
