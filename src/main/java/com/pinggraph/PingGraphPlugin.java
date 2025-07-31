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
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

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
    private final ReadWriteLock pingLock = new ReentrantReadWriteLock();
    @Getter
    private final ReadWriteLock tickLock = new ReentrantReadWriteLock();
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
    private PingGraphOverlay pingGraphOverlay;
    private ScheduledFuture<?> currPingFuture;
    @Getter
    private volatile int currentPing = 1;
    @Getter
    private volatile int maxPing = -1;
    @Getter
    private volatile int minPing = Integer.MAX_VALUE;
    @Getter
    private volatile int currentTick = 600;
    @Getter
    private volatile int maxTick = -1;
    @Getter
    private volatile int minTick = Integer.MAX_VALUE;
    @Getter
    private boolean isLagging;
    private long lastTickTime;
    @Setter
    private volatile int graphStart;
    @Getter
    private int noResponseCount;

    private int lastPing = 1;

    private boolean resetGraphToggle = false;

    @Inject
    private ScheduledExecutorService pingExecutorService;

    @Override
    protected void startUp() throws Exception {
        write(pingLock, () -> {
            pingList.clear();
            for (int i = 0; i < numCells; i++) pingList.add(1);
            return null;
        });

        write(tickLock, () -> {
            tickTimeList.clear();
            for (int i = 0; i < numCells; i++) tickTimeList.add(600);
            return null;
        });

        log.info("Ping Graph started!");
        resetGraphToggle = config.enablePingSpikes();
        overlayManager.add(pingGraphOverlay);
        currPingFuture = pingExecutorService.scheduleWithFixedDelay(this::pingCurrentWorld, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        currPingFuture.cancel(true);
        currPingFuture = null;
        overlayManager.remove(pingGraphOverlay);
        write(pingLock, () -> {
            pingList.clear();
            return null;
        });
        write(tickLock, () -> {
            tickTimeList.clear();
            return null;
        });
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
        currentTick = tickDiff;
        write(tickLock, () -> {
            if (tickDiff < 10000) { // should be enough to hide initial tick on startup
                tickTimeList.add(tickDiff);
            } else {
                tickTimeList.add(600);
            }
            return tickTimeList.remove();
        });
        lastTickTime = new Date().getTime();
    }

    @Subscribe
    public void onClientTick(ClientTick tick) {
        long now = new Date().getTime();
        isLagging = (now - lastTickTime) > 700;

        //update Max min values
        int[] temp = read(tickLock, () -> getMaxMinFromList(tickTimeList, graphStart));
        maxTick = temp[0];
        minTick = temp[1];

        temp = read(pingLock, () -> getMaxMinFromList(pingList, graphStart));
        maxPing = temp[0];
        minPing = temp[1];

        if(config.enablePingSpikes() != resetGraphToggle){
            write(pingLock, () -> {
                pingList.clear();
                for (int i = 0; i < numCells; i++) pingList.add(1);
                return null;
            });
            resetGraphToggle = !resetGraphToggle;
        }
    }

    // Code used from runelites worldhopper
    private void pingCurrentWorld() {
        WorldResult worldResult = worldService.getWorlds();
        // There is no reason to ping the current world if not logged in, as the overlay doesn't draw
        if (worldResult == null || client.getGameState() != GameState.LOGGED_IN) return;
        final World currentWorld = worldResult.findWorld(client.getWorld());
        if (currentWorld == null) return;

        lastPing = currentPing;
        currentPing = Ping.ping(currentWorld);

        if(currentPing < 0) {
            noResponseCount++;
            if(config.enablePingSpikes()){
                write(pingLock, () -> {
                    pingList.add(currentPing);
                    return pingList.remove();
                });
            }
            currentPing = lastPing;
        } else {
            noResponseCount = 0;
            write(pingLock, () -> {
                pingList.add(currentPing);
                return pingList.remove(); // remove the first ping
            });
        }

        if (!config.graphTicks()) {
            int[] temp = read(pingLock, () -> getMaxMinFromList(pingList, graphStart));
            maxPing = temp[0];
            minPing = temp[1];
        }
    }

    private static int[] getMaxMinFromList(List<Integer> list, int start) {
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
        return new int[] { maxVal, minVal };
    }

    public static <T> T read(ReadWriteLock lock, Supplier<T> supplier) {
        return supplyLocked(lock.readLock(), supplier);
    }

    public static <T> T write(ReadWriteLock lock, Supplier<T> supplier) {
        return supplyLocked(lock.writeLock(), supplier);
    }

    private static <T> T supplyLocked(Lock lock, Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
