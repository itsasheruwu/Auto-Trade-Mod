package com.autotrade.state;

public final class AutoTradeState {
    private static final AutoTradeState INSTANCE = new AutoTradeState();

    private boolean enabled;
    private String statusText = "Auto: OFF";
    private long lastAttemptTick;

    private AutoTradeState() {
    }

    public static AutoTradeState getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.lastAttemptTick = 0L;
        }
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public long getLastAttemptTick() {
        return lastAttemptTick;
    }

    public void setLastAttemptTick(long lastAttemptTick) {
        this.lastAttemptTick = lastAttemptTick;
    }
}
