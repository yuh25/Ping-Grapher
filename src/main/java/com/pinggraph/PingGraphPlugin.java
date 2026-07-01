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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private static final int TCP_PORT = 43594;
    private static final int TCP_TIMEOUT_MS = 2000;

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
        // Fall back to TCP should ICMP ping fail. Workaround for presumed upstream bug.
        // Also handles worlds whose hostname resolves only to an IPv6 address (e.g. Brazil
        // worlds 692-695), where Ping.ping() returns -1 before attempting TCP because it
        // calls InetAddress.getByName() and rejects non-Inet4Address results immediately.
        currentPing = pingWorld(currentWorld);

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

    /**
     * Ping the given world, falling back gracefully for IPv6-only hosts.
     *
     * Ping.ping() resolves a hostname with InetAddress.getByName(), which returns
     * a single address. If that address is IPv6 it immediately returns -1 — the TCP
     * fallback never runs. This affects worlds whose DNS only advertises an IPv6
     * address (e.g. Brazil worlds 692-695 hosted on AWS sa-east-1).
     *
     * Fix: resolve all addresses with getAllByName() and prefer the first IPv4 result.
     * If none exists, fall back to a TCP connect over whatever address is available
     * (Socket handles IPv6 natively).
     */
    private static int pingWorld(World world)
    {
        InetAddress[] addresses;
        try
        {
            addresses = InetAddress.getAllByName(world.getAddress());
        }
        catch (UnknownHostException ex)
        {
            log.debug("Could not resolve host for world {}: {}", world.getId(), ex.getMessage());
            return -1;
        }

        // Look for an IPv4 address first — Ping.ping() handles those well.
        for (InetAddress addr : addresses)
        {
            if (addr instanceof Inet4Address)
            {
                // Delegate to the existing Ping utility with TCP fallback enabled.
                // We pass a synthetic world-like object isn't possible, so we just
                // re-use the original world; the IPv4 address will be the one
                // InetAddress.getByName returns when an A record exists alongside AAAA.
                // If this world reliably returns IPv4 we'll hit the fast path in Ping.ping().
                int result = Ping.ping(world, true);
                if (result >= 0)
                {
                    return result;
                }
                // ICMP and TCP both failed on IPv4 — no point trying IPv6.
                return -1;
            }
        }

        // No IPv4 address — IPv6-only world. Use a direct TCP connect.
        // Socket.connect() supports IPv6 natively on all platforms.
        log.debug("World {} has no IPv4 address, using TCP ping via IPv6 ({})",
            world.getId(), addresses[0].getHostAddress());
        return tcpPing(addresses[0]);
    }

    /**
     * Measure a TCP connect time to the game server port.
     * Used as a fallback when only an IPv6 address is available.
     */
    private static int tcpPing(InetAddress address)
    {
        try (Socket socket = new Socket())
        {
            socket.setSoTimeout(TCP_TIMEOUT_MS);
            long start = System.nanoTime();
            socket.connect(new InetSocketAddress(address, TCP_PORT), TCP_TIMEOUT_MS);
            long end = System.nanoTime();
            return (int) ((end - start) / 1_000_000L);
        }
        catch (Exception ex)
        {
            log.debug("TCP ping failed for {}: {}", address.getHostAddress(), ex.getMessage());
            return -1;
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