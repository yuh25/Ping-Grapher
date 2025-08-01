package com.pinggraph;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;


import java.awt.*;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingGraphOverlay extends OverlayPanel {

    private final Client client;
    private final PingGraphPlugin pingGraphPlugin;
    private final PingGraphConfig pingGraphConfig;

    public int marginGraphHeight;
    public int marginGraphWidth = 10;
    LayoutableRenderableEntity graphEntity = new LayoutableRenderableEntity() {
        @Override
        public Dimension render(Graphics2D graphics) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int tempPing  = pingGraphPlugin.getCurrentPing();
            int tempTick  = pingGraphPlugin.getCurrentTick();
            if(pingGraphConfig.warnMaxToggle()) {
                tempTick  = pingGraphPlugin.getMaxTick();
                tempPing = pingGraphPlugin.getMaxPing();
            }

            boolean warning = (tempPing > pingGraphConfig.warnPingVal() || tempTick > pingGraphConfig.warnTickVal());
            warning = warning || (pingGraphPlugin.getCurrentPing() < 0); //warn if ping timed out

            if (pingGraphConfig.toggleBehind()) {
                setLayer(OverlayLayer.ABOVE_SCENE);
            } else {
                setLayer(OverlayLayer.ABOVE_WIDGETS);
            }

            int overlayWidth, overlayHeight;                // width and height of the entire overlay
            try {
                overlayWidth = getPreferredSize().width;
                overlayHeight = getPreferredSize().height;
            } catch (NullPointerException e) {
                overlayWidth = 180;                         // Default settings for first time
                overlayHeight = 60;
                PingGraphOverlay.this.setPreferredSize(new Dimension(overlayWidth, overlayHeight));
            }

            boolean hasBottomLabels = !pingGraphConfig.bottomRightLabel().equals(PingGraphConfig.Labels.NONE) ||
                    !pingGraphConfig.bottomLeftLabel().equals(PingGraphConfig.Labels.NONE);

            boolean hasTopLabels = !pingGraphConfig.rightLabel().equals(PingGraphConfig.Labels.NONE) ||
                    !pingGraphConfig.leftLabel().equals(PingGraphConfig.Labels.NONE);


            marginGraphHeight = pingGraphConfig.fontSize();

            int xOrigin = marginGraphWidth - 1;
            int yOrigin = marginGraphHeight + 1;

            int graphWidth = overlayWidth - marginGraphWidth * 2;
            int graphHeight = overlayHeight - marginGraphHeight * 2;

            if (pingGraphConfig.hideMargin()) {
                graphWidth = overlayWidth;                           // set graph width to whole plugin width
                if (!hasTopLabels) {
                    graphHeight += marginGraphHeight;                // remove the extra height for top and bottom text
                }
                if (!hasBottomLabels){
                    graphHeight += marginGraphHeight;
                }
            }

            if (pingGraphConfig.hideGraph()) {
                graphWidth = 0;
                graphHeight = 0;
                overlayHeight = pingGraphConfig.fontSize();

                if (hasBottomLabels) {
                    overlayHeight += pingGraphConfig.fontSize();
                }
            }

            //background rect
            if (pingGraphConfig.warningBGToggle() && warning) {
                graphics.setColor(pingGraphConfig.warningBGColor());
            } else {
                graphics.setColor(pingGraphConfig.overlayBackgroundColor());
            }
            graphics.fillRect(0, 0, overlayWidth, overlayHeight);

            //outside border
            graphics.setColor(pingGraphConfig.overlayBorderColor());
            graphics.drawRect(0, 0, overlayWidth, overlayHeight);

            if (!pingGraphConfig.toggleLineOnly()) {
                //inside border
                graphics.setColor(pingGraphConfig.graphBorderColor());

                if(pingGraphConfig.hideMargin()){
                    xOrigin = 0;
                    if(!hasTopLabels)
                        yOrigin = 1;
                }

                graphics.drawRect(xOrigin, yOrigin, graphWidth, graphHeight);


                //inside rect
                if (pingGraphConfig.warningGraphBGToggle() && warning) {
                    graphics.setColor(pingGraphConfig.warningGraphBGColor());
                } else {
                    graphics.setColor(pingGraphConfig.graphBackgroundColor());
                }
                graphics.fillRect(xOrigin, yOrigin, graphWidth, graphHeight);

                //Font Settings
                if (pingGraphConfig.warningFontToggle() && warning){
                    graphics.setColor(pingGraphConfig.warningFontColor());
                } else {
                    graphics.setColor(pingGraphConfig.graphTextColor());
                }
                String fontName = pingGraphConfig.fontName();
                if (pingGraphConfig.fontName().equals("")) {
                    fontName = "Runescape Small";           // Default name if the font name is empty
                }

                Font userFont = new Font(fontName, pingGraphConfig.fontStyle().getValue(), pingGraphConfig.fontSize());

                if (userFont.getFamily().equals("Dialog")) { // Can't find the font, change to default
                    userFont = new Font("Runescape Small", pingGraphConfig.fontStyle().getValue(), pingGraphConfig.fontSize());
                }
                graphics.setFont(userFont);

                String rightLabel = labelText(pingGraphConfig.rightLabel());

                //Right label
                int strWidth = graphics.getFontMetrics().stringWidth(rightLabel);
                graphics.drawString(rightLabel, overlayWidth - strWidth - marginGraphWidth, marginGraphHeight - 1);

                //Left label
                String leftLabel = labelText(pingGraphConfig.leftLabel());
                graphics.drawString(leftLabel, marginGraphWidth, marginGraphHeight - 1);

                //Right label
                rightLabel = labelText(pingGraphConfig.bottomRightLabel());
                strWidth = graphics.getFontMetrics().stringWidth(rightLabel);
                graphics.drawString(rightLabel, overlayWidth - strWidth - marginGraphWidth, overlayHeight);

                //Left label
                leftLabel = labelText(pingGraphConfig.bottomLeftLabel());
                graphics.drawString(leftLabel, marginGraphWidth, overlayHeight);


            } else {
                graphWidth = overlayWidth;
                graphHeight = overlayHeight;
                xOrigin = 0;
                yOrigin = 0;
            }

            LinkedList<Integer> data;
            ReadWriteLock lock;
            if (pingGraphConfig.graphTicks()) {
                data = pingGraphPlugin.getTickTimeList();
                lock = pingGraphPlugin.getTickLock();
            } else {
                data = pingGraphPlugin.getPingList();
                lock = pingGraphPlugin.getPingLock();
            }

            int dataSize = PingGraphPlugin.read(lock, data::size);
            int dataStart = (dataSize > overlayWidth) ? (dataSize - overlayWidth) : 0;
            pingGraphPlugin.setGraphStart(dataStart);

            int maxValue;
            int minValue;
            if (pingGraphConfig.graphTicks()) {
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
            //if checked, the graph will scale between min and max ping
            int tempMax = maxValue;
            if (!pingGraphConfig.toggleRange()) {
                minValue = 0;
                double round = maxValue > 50 ? 50 : 10; // round up to nearest 50 ms if > 50 else 10 ms
                maxValue = (int) (Math.ceil((double) tempMax / round) * round);

                if ((maxValue - tempMax) <= (0.2 * maxValue)) {
                    maxValue += (int) round; // increase the max value to move the graph away from the top
                }
            }

            if (maxValue == minValue) {
                maxValue++;
                minValue--;
            }
            if (!pingGraphConfig.hideGraph()) {
                Lock l = lock.readLock();
                l.lock();
                try {
                    //drawing line graph
                    drawGraph(graphics, dataStart, data, xOrigin, yOrigin, graphHeight, graphWidth, maxValue, minValue);
                } finally {
                    l.unlock();
                }
            }
            return new Dimension(overlayWidth - 8, overlayHeight - 8);
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(getPreferredSize().width, getPreferredSize().height);
        }

        @Override
        public void setPreferredLocation(Point position) {
        }

        @Override
        public void setPreferredSize(Dimension dimension) {
        }
    };

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

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.render(graphics);
        panelComponent.getChildren().add(graphEntity);
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
        return super.render(graphics);
    }

    // returns a string based on user settings
    private String labelText(PingGraphConfig.Labels setting) {
        String tempLabel = "";
        switch (setting) {
            case LATENCY:
            case PING:
                String labelType = (setting == PingGraphConfig.Labels.LATENCY) ? "Latency:" : "Ping:";
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = labelType;

                tempLabel += pingGraphPlugin.getCurrentPing() + "ms";

                if (pingGraphPlugin.getNoResponseCount() >= pingGraphConfig.noResponseLimit()) {
                    tempLabel = "";
                    if(!pingGraphConfig.simpleLabels())
                        tempLabel = labelType;
                    tempLabel += pingGraphConfig.noResponseMsg();
                }
            break;
            case PINGMAX:
                if(!pingGraphConfig.simpleLabels()) {
                    tempLabel = "Max(P):";
                }
                tempLabel += pingGraphPlugin.getMaxPing() + "ms";
                break;
            case PINGMIN:
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = "Min(P):";
                tempLabel +=  pingGraphPlugin.getMinPing() + "ms";
                break;
            case TICK:
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = "Tick:";
                tempLabel += pingGraphPlugin.getCurrentTick() + "ms";
                break;
            case TICKMAX:
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = "Max(T)";
                tempLabel += pingGraphPlugin.getMaxTick() + "ms";
                break;
            case TICKDEV:
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = "Tick: +/-";
                tempLabel += (Math.abs(pingGraphPlugin.getCurrentTick() - 600)) + "ms";
                break;
            case TICKDEVMAX:
                if(!pingGraphConfig.simpleLabels())
                    tempLabel = "Max(T): +/-";
                tempLabel += (Math.abs(pingGraphPlugin.getMaxTick() - 600)) + "ms";
                break;
            case NONE:
                tempLabel = "";
                break;
        }
        return tempLabel;
    }

    private void drawGraph(Graphics2D graphics, int dataStart, LinkedList<Integer> data,int xOrigin, int yOrigin, int height, int width, int maxValue, int minValue){
        //drawing line graph
        int tempX;
        graphics.setColor(pingGraphConfig.graphLineColor());
        int oldX, oldY = oldX = -1;

        for (int x = dataStart; x < data.size(); x++) {
            int y = data.get(x);
            y = y < 0 ? maxValue : y; // change a "timed out" to spike rather than drop

            //((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
            //scale the x and y values to fit to the plugin
            y = height - (height * (y - minValue) / (maxValue - minValue) - yOrigin);
            tempX = ((width - 1) * (x - dataStart) / (data.size() - dataStart - 1));

            if (!pingGraphConfig.hideMargin()) {
                tempX += marginGraphWidth;
            }

            if (pingGraphConfig.toggleLineOnly()) {
                if (!pingGraphConfig.hideMargin())
                    tempX -= marginGraphWidth;
            }

            if (oldX != -1 && y >= 0) {
                graphics.drawLine(oldX, oldY, tempX, y);
            }

            oldX = tempX;
            oldY = y;
        }
    }




}
