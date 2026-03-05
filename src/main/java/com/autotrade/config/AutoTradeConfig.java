package com.autotrade.config;

import java.util.HashMap;
import java.util.Map;

public final class AutoTradeConfig {
    public SelectionScope selectionScope = SelectionScope.GLOBAL;
    public int globalTradeIndex = 1;
    public Map<String, Integer> tradeIndexByProfession = new HashMap<>();
    public Map<String, Integer> tradeIndexByVillager = new HashMap<>();
    public RateMode rateMode = RateMode.MODERATE;
    public boolean showInScreenStatusText = true;
    public boolean pauseRetryOnUnavailable = true;
    public boolean keepEnabledAcrossScreens = false;
    public boolean autoCloseUiOnMissingInput = false;
    public boolean autoUpdateEnabled = true;
    public String autoUpdateGithubOwner = "itsasheruwu";
    public String autoUpdateGithubRepo = "Auto-Trade-Mod";

    public enum SelectionScope {
        GLOBAL,
        PROFESSION,
        VILLAGER
    }

    public enum RateMode {
        CONSERVATIVE(5),
        MODERATE(2),
        FAST(1);

        private final int tickInterval;

        RateMode(int tickInterval) {
            this.tickInterval = tickInterval;
        }

        public int tickInterval() {
            return tickInterval;
        }
    }
}
