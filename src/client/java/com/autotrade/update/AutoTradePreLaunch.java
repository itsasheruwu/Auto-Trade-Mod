package com.autotrade.update;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class AutoTradePreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        GitHubAutoUpdater.runBlockingPreLaunchCheck();
    }
}
