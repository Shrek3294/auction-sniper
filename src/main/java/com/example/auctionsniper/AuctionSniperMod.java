package com.example.auctionsniper;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionSniperMod implements ClientModInitializer {
    public static final String MOD_ID = "auctionsniper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Auction Sniper initialized!");
        SniperConfig.load();
        AutoRefreshHandler.register();
        AutoRelistHandler.register();
        SniperCommand.register();
        LOGGER.info("Auto-refresh enabled with interval: {}ms - {}ms",
                SniperConfig.refreshIntervalMin, SniperConfig.refreshIntervalMax);
    }
}
