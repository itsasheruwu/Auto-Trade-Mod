package com.autotrade.modmenu;

import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return com.autotrade.ui.ConfigScreenFactory::create;
    }
}
