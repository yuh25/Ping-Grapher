package com.pinggraph;

import com.google.inject.Provides;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ExecutorServiceExceptionLogger;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PluginDescriptor(
        name = "Ping Grapher"
)
public class PingGraphPlugin extends Plugin {
    private final int numCells = 100;
    @Getter
    private final LinkedList<Integer> pingList = new LinkedList<>();
    @Getter
    private final LinkedList<Integer> tickTimeList = new LinkedList<>();
    @Inject
    private Client client;
    @Inject
    private PingGraphConfig config;
    @Inject
    private WorldService worldService;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PingGraphOverlay pingGrpahOverlay;
    private ScheduledFuture<?> pingFuture, currPingFuture;
    @Getter
    private int currentPing = 1;
    @Getter
    private int maxPing = -1;
    @Getter
    private int minPing = Integer.MAX_VALUE;
    @Getter
    private int currentTick = 600;
    @Getter
    private int maxTick = -1;
    @Getter
    private int minTick = Integer.MAX_VALUE;
    @Getter
    private boolean isLagging;
    private long lastTickTime;
    @Setter
    private int graphStart;
    private ScheduledExecutorService pingExecutorService;

    @Override
    protected void startUp() throws Exception {

        pingList.clear();
        for (int i = 0; i < numCells; i++) pingList.add(1);

        tickTimeList.clear();
        for (int i = 0; i < numCells; i++) tickTimeList.add(600);

        log.info("Ping Graph started!");
        overlayManager.add(pingGrpahOverlay);
        pingExecutorService = new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
        currPingFuture = pingExecutorService.scheduleWithFixedDelay(this::pingCurrentWorld, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        currPingFuture.cancel(true);
        currPingFuture = null;
        overlayManager.remove(pingGrpahOverlay);
        pingExecutorService.shutdown();
        pingExecutorService = null;
        pingList.clear();
        tickTimeList.clear();
        log.info("Ping Graph stopped!");
    }


    @Provides
    PingGraphConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PingGraphConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        long tickTime = new Date().getTime();
        int tickDiff = (int) (tickTime - lastTickTime);
        if (tickDiff < 10000) { // should be enough to hide initial tick on startup
            tickTimeList.add(tickDiff);
        } else {
            tickTimeList.add(600);
        }
        currentTick = tickDiff;
        tickTimeList.remove();
        lastTickTime = new Date().getTime();
    }

    @Subscribe
    public void onClientTick(ClientTick tick) {
        long now = new Date().getTime();
        isLagging = (now - lastTickTime) > 700;
        int[] temp;

        //update Max min values
        temp = getMaxMinFromList(tickTimeList, graphStart);
        maxTick = temp[0];
        minTick = temp[1];

        temp = getMaxMinFromList(pingList, graphStart);
        maxPing = temp[0];
        minPing = temp[1];
    }

    // Code used from runelites worldhopper
    private void pingCurrentWorld() {
        WorldResult worldResult = worldService.getWorlds();
        // There is no reason to ping the current world if not logged in, as the overlay doesn't draw
        if (worldResult == null || client.getGameState() != GameState.LOGGED_IN) return;
        final World currentWorld = worldResult.findWorld(client.getWorld());
        if (currentWorld == null) return;

        currentPing = Ping.ping(currentWorld);
        pingList.add(currentPing);
        pingList.remove();// remove the first ping
        int[] temp = getMaxMinFromList(pingList, graphStart);
        if (!config.graphTicks()) {
            maxPing = temp[0];
            minPing = temp[1];
        }
    }

    public int[] getMaxMinFromList(LinkedList<Integer> list, int start) {
        int maxVal = -1;
        int minVal = Integer.MAX_VALUE;

        for (int i = start; i < list.size(); i++) {
            int val = list.get(i);
            if (val > 0) {
                if (maxVal < val)
                    maxVal = val;
                if (minVal > val)
                    minVal = val;
            }
        }
        return new int[]{maxVal, minVal};
    }
}
