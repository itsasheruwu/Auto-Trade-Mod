package com.autotrade;

import com.autotrade.config.ConfigManager;
import com.autotrade.engine.AutoTradeEngine;
import com.autotrade.state.TradeContextTracker;
import com.autotrade.update.GitHubAutoUpdater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.ActionResult;

public final class AutoTradeClientMod implements ClientModInitializer {
    private static final TradeContextTracker TRADE_CONTEXT_TRACKER = new TradeContextTracker();

    @Override
    public void onInitializeClient() {
        ConfigManager.getConfig();
        AutoTradeEngine.initialize(TRADE_CONTEXT_TRACKER);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof VillagerEntity villager) {
                TRADE_CONTEXT_TRACKER.capture(villager);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(AutoTradeEngine::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(GitHubAutoUpdater::onClientTick);
    }
}
