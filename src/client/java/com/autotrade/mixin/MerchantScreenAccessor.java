package com.autotrade.mixin;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MerchantScreen.class)
public interface MerchantScreenAccessor {
    @Accessor("selectedIndex")
    void autotrade$setSelectedIndex(int selectedIndex);

    @Invoker("syncRecipeIndex")
    void autotrade$invokeSyncRecipeIndex();
}
