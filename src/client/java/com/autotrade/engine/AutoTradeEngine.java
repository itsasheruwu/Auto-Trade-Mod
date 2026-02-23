package com.autotrade.engine;

import com.autotrade.config.AutoTradeConfig;
import com.autotrade.config.ConfigManager;
import com.autotrade.state.AutoTradeState;
import com.autotrade.state.TradeContextTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.village.TradeOfferList;

import java.util.UUID;

public final class AutoTradeEngine {
    private static final AutoTradeState STATE = AutoTradeState.getInstance();
    private static TradeContextTracker contextTracker;
    private static int lastSyncedTradeIndex = -1;
    private static int lastSyncId = -1;
    private static long lastSelectionTick = Long.MIN_VALUE;

    private AutoTradeEngine() {
    }

    public static void initialize(TradeContextTracker tracker) {
        contextTracker = tracker;
    }

    public static void onClientTick(MinecraftClient client) {
        if (!STATE.isEnabled()) {
            return;
        }

        if (!(client.currentScreen instanceof MerchantScreen screen)) {
            stop("Auto: OFF");
            return;
        }

        if (client.player == null || client.interactionManager == null || client.world == null) {
            return;
        }

        AutoTradeConfig config = ConfigManager.getConfig();
        long now = client.world.getTime();
        if (now - STATE.getLastAttemptTick() < config.rateMode.tickInterval()) {
            return;
        }
        STATE.setLastAttemptTick(now);

        MerchantScreenHandler handler = screen.getScreenHandler();
        if (handler.syncId != lastSyncId) {
            lastSyncId = handler.syncId;
            lastSyncedTradeIndex = -1;
            lastSelectionTick = Long.MIN_VALUE;
        }

        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) {
            STATE.setStatusText("Auto: Waiting (no trades)");
            return;
        }

        int targetIndex = resolveTargetIndex(config);
        if (targetIndex < 0 || targetIndex >= offers.size()) {
            STATE.setStatusText("Auto: Waiting (invalid target)");
            return;
        }

        // Don't spam recipe clicks every tick; synchronize target in controlled intervals.
        if (lastSyncedTradeIndex != targetIndex || now - lastSelectionTick >= 40) {
            syncTargetTrade(client, handler, targetIndex, now);
            STATE.setStatusText("Auto: Selecting trade #" + (targetIndex + 1));
            return;
        }

        Slot output = handler.getSlot(2);
        ItemStack before = output.getStack().copy();
        if (before.isEmpty()) {
            // Re-run autofill periodically if result is currently unavailable.
            if (now - lastSelectionTick >= 5) {
                handler.switchTo(targetIndex);
                lastSelectionTick = now;
            }
            STATE.setStatusText("Auto: Waiting (no result)");
            return;
        }

        client.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, client.player);
        ItemStack after = output.getStack();

        if (after.isEmpty() || after.getCount() < before.getCount()) {
            STATE.setStatusText("Auto: ON");
            return;
        }

        STATE.setStatusText("Auto: Paused (inventory full?)");
    }

    public static void toggle() {
        if (STATE.isEnabled()) {
            stop("Auto: OFF");
            return;
        }

        resetSelectionState();
        STATE.setEnabled(true);
        STATE.setStatusText("Auto: ON");
        STATE.setLastAttemptTick(0L);
    }

    public static void stop(String message) {
        resetSelectionState();
        STATE.setEnabled(false);
        STATE.setStatusText(message);
    }

    public static boolean isEnabled() {
        return STATE.isEnabled();
    }

    public static String getStatusText() {
        return STATE.getStatusText();
    }

    public static void setStatusText(String text) {
        STATE.setStatusText(text);
    }

    public static void setTargetForActiveScope(int oneBasedIndex) {
        AutoTradeConfig config = ConfigManager.getConfig();
        int safeIndex = Math.max(1, oneBasedIndex);

        switch (config.selectionScope) {
            case GLOBAL -> config.globalTradeIndex = safeIndex;
            case PROFESSION -> {
                String profession = contextTracker == null ? null : contextTracker.getProfessionId();
                if (profession == null || profession.isBlank()) {
                    config.globalTradeIndex = safeIndex;
                    STATE.setStatusText("Target saved globally (no profession context)");
                } else {
                    config.tradeIndexByProfession.put(profession, safeIndex);
                    STATE.setStatusText("Target saved for profession: " + profession);
                }
            }
            case VILLAGER -> {
                UUID villagerUuid = contextTracker == null ? null : contextTracker.getVillagerUuid();
                if (villagerUuid == null) {
                    config.globalTradeIndex = safeIndex;
                    STATE.setStatusText("Target saved globally (no villager context)");
                } else {
                    config.tradeIndexByVillager.put(villagerUuid.toString(), safeIndex);
                    STATE.setStatusText("Target saved for villager: " + villagerUuid);
                }
            }
        }

        ConfigManager.save();
        resetSelectionState();
    }

    private static int resolveTargetIndex(AutoTradeConfig config) {
        int oneBased = config.globalTradeIndex;

        if (config.selectionScope == AutoTradeConfig.SelectionScope.PROFESSION) {
            String profession = contextTracker == null ? null : contextTracker.getProfessionId();
            if (profession != null && config.tradeIndexByProfession.containsKey(profession)) {
                oneBased = config.tradeIndexByProfession.get(profession);
            }
        } else if (config.selectionScope == AutoTradeConfig.SelectionScope.VILLAGER) {
            UUID villagerUuid = contextTracker == null ? null : contextTracker.getVillagerUuid();
            if (villagerUuid != null && config.tradeIndexByVillager.containsKey(villagerUuid.toString())) {
                oneBased = config.tradeIndexByVillager.get(villagerUuid.toString());
            }
        }

        return Math.max(1, oneBased) - 1;
    }

    private static void syncTargetTrade(MinecraftClient client, MerchantScreenHandler handler, int targetIndex, long now) {
        handler.switchTo(targetIndex);
        client.interactionManager.clickButton(handler.syncId, targetIndex);
        lastSyncedTradeIndex = targetIndex;
        lastSelectionTick = now;
    }

    private static void resetSelectionState() {
        lastSyncedTradeIndex = -1;
        lastSyncId = -1;
        lastSelectionTick = Long.MIN_VALUE;
    }
}
