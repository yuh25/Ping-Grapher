package com.pinggraph;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ExecutorServiceExceptionLogger;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@Slf4j
@PluginDescriptor(
	name = "Ping Grapher"
)
public class PingGraphPlugin extends Plugin
{
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
	private final LinkedList<Integer> pingList = new LinkedList<>();

	@Getter
	private int currentPing;

	private final int numCells = 100;

	@Getter
	private int maxPing = -1;

	private ScheduledExecutorService pingExecutorService;

	@Override
	protected void startUp() throws Exception
	{
		for(int i = 0; i<numCells; i++){
			pingList.add(0);
		}
		log.info("Ping Graph started!");
		overlayManager.add(pingGrpahOverlay);
		pingExecutorService = new ExecutorServiceExceptionLogger(Executors.newSingleThreadScheduledExecutor());
		currPingFuture = pingExecutorService.scheduleWithFixedDelay(this::pingCurrentWorld, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown() throws Exception
	{
		currPingFuture.cancel(true);
		currPingFuture = null;

		overlayManager.remove(pingGrpahOverlay);

		pingExecutorService.shutdown();
		pingExecutorService = null;
		pingList.clear();
		log.info("Ping Graph stopped!");
	}


	@Provides
	PingGraphConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PingGraphConfig.class);
	}

	// Code used from runelites worldhopper
	private void pingCurrentWorld()
	{
		WorldResult worldResult = worldService.getWorlds();
		// There is no reason to ping the current world if not logged in, as the overlay doesn't draw
		if (worldResult == null || client.getGameState() != GameState.LOGGED_IN) return;

		final World currentWorld = worldResult.findWorld(client.getWorld());
		if (currentWorld == null) return;

		currentPing = Ping.ping(currentWorld);
		pingList.add(currentPing);
		pingList.remove();// remove the first ping
		maxPing = -1;
		for (int tempMax: pingList) {
			if(maxPing < tempMax)
				maxPing = tempMax;
		}
	}
}
