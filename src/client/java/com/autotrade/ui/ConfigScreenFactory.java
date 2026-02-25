package com.autotrade.ui;

import com.autotrade.config.AutoTradeConfig;
import com.autotrade.config.ConfigManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        AutoTradeConfig config = ConfigManager.getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Auto Trade Mod Settings"));
        builder.setSavingRunnable(ConfigManager::save);

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory targetCategory = builder.getOrCreateCategory(Text.literal("Target Trade"));
        targetCategory.addEntry(entries.startIntField(Text.literal("Global Trade Row (1-based)"), config.globalTradeIndex)
                .setDefaultValue(1)
                .setMin(1)
                .setSaveConsumer(value -> config.globalTradeIndex = value)
                .build());
        targetCategory.addEntry(entries.startTextDescription(Text.literal("Row numbers are 1-based: first row is 1.")) .build());

        ConfigCategory selectionCategory = builder.getOrCreateCategory(Text.literal("Selection Scope"));
        selectionCategory.addEntry(entries.startEnumSelector(
                Text.literal("Scope Mode"),
                AutoTradeConfig.SelectionScope.class,
                config.selectionScope)
            .setDefaultValue(AutoTradeConfig.SelectionScope.GLOBAL)
            .setSaveConsumer(value -> config.selectionScope = value)
            .build());

        ConfigCategory speedCategory = builder.getOrCreateCategory(Text.literal("Trade Speed"));
        speedCategory.addEntry(entries.startEnumSelector(
                        Text.literal("Rate Mode"),
                        AutoTradeConfig.RateMode.class,
                        config.rateMode)
                .setDefaultValue(AutoTradeConfig.RateMode.MODERATE)
                .setSaveConsumer(value -> config.rateMode = value)
                .build());
        speedCategory.addEntry(entries.startTextDescription(Text.literal("Conservative=5 ticks, Moderate=2 ticks, Fast=1 tick"))
                .build());

        ConfigCategory statusCategory = builder.getOrCreateCategory(Text.literal("Status Display"));
        statusCategory.addEntry(entries.startBooleanToggle(Text.literal("Show in-screen status text"), config.showInScreenStatusText)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showInScreenStatusText = value)
                .build());
        statusCategory.addEntry(entries.startBooleanToggle(Text.literal("Keep auto ON when trading UI closes"), config.keepEnabledAcrossScreens)
                .setDefaultValue(false)
                .setTooltip(Text.literal("If enabled, Auto stays armed between villager screen reopen."))
                .setSaveConsumer(value -> config.keepEnabledAcrossScreens = value)
                .build());

        ConfigCategory updateCategory = builder.getOrCreateCategory(Text.literal("Auto Update"));
        updateCategory.addEntry(entries.startBooleanToggle(Text.literal("Enable GitHub auto update"), config.autoUpdateEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.autoUpdateEnabled = value)
                .build());
        updateCategory.addEntry(entries.startStrField(Text.literal("GitHub owner"), config.autoUpdateGithubOwner)
                .setDefaultValue("itsasheruwu")
                .setSaveConsumer(value -> config.autoUpdateGithubOwner = value.trim())
                .build());
        updateCategory.addEntry(entries.startStrField(Text.literal("GitHub repo"), config.autoUpdateGithubRepo)
                .setDefaultValue("Auto-Trade-Mod")
                .setSaveConsumer(value -> config.autoUpdateGithubRepo = value.trim())
                .build());
        updateCategory.addEntry(entries.startTextDescription(Text.literal("Updater checks at startup/pre-launch and installs new jar automatically."))
                .build());
        updateCategory.addEntry(entries.startTextDescription(Text.literal("Minecraft still must relaunch to run new mod code."))
                .build());

        ConfigCategory scopeCategory = builder.getOrCreateCategory(Text.literal("Stored Indexes"));
        scopeCategory.addEntry(entries.startTextDescription(Text.literal("Profession mappings: " + config.tradeIndexByProfession.size()))
                .build());
        scopeCategory.addEntry(entries.startTextDescription(Text.literal("Villager mappings: " + config.tradeIndexByVillager.size()))
                .build());
        scopeCategory.addEntry(entries.startTextDescription(Text.literal("pauseRetryOnUnavailable is always enabled."))
                .build());

        ConfigCategory resetCategory = builder.getOrCreateCategory(Text.literal("Reset Scope Mappings"));
        resetCategory.addEntry(entries.startBooleanToggle(Text.literal("Reset profession mappings"), false)
                .setDefaultValue(false)
                .setSaveConsumer(value -> {
                    if (value) {
                        config.tradeIndexByProfession.clear();
                    }
                })
                .build());
        resetCategory.addEntry(entries.startBooleanToggle(Text.literal("Reset villager mappings"), false)
                .setDefaultValue(false)
                .setSaveConsumer(value -> {
                    if (value) {
                        config.tradeIndexByVillager.clear();
                    }
                })
                .build());

        return builder.build();
    }
}
