package com.autotrade.mixin;

import com.autotrade.config.ConfigManager;
import com.autotrade.engine.AutoTradeEngine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> {
    @Shadow
    private int selectedIndex;

    @Unique
    private ButtonWidget autotrade$toggleButton;

    @Unique
    private ButtonWidget autotrade$setTargetButton;

    @Unique
    private int autotrade$statusX;

    @Unique
    private int autotrade$statusY;

    protected MerchantScreenMixin(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void autotrade$init(CallbackInfo ci) {
        int toggleWidth = 100;
        int targetWidth = 170;
        int maxWidth = Math.max(toggleWidth, targetWidth);

        // Prefer putting controls to the right of the villager GUI; fall back inside if needed.
        int left = this.x + this.backgroundWidth + 6;
        if (left + maxWidth > this.width - 6) {
            left = this.x + this.backgroundWidth - maxWidth - 6;
        }
        int top = this.y + 6;

        autotrade$toggleButton = this.addDrawableChild(ButtonWidget.builder(autotrade$toggleLabel(), button -> {
                    AutoTradeEngine.toggle();
                    autotrade$refreshButtonLabels();
                })
                .dimensions(left, top, toggleWidth, 20)
                .build());

        autotrade$setTargetButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Set Target = Current Selected Trade"), button -> {
                    AutoTradeEngine.setTargetForActiveScope(this.selectedIndex + 1);
                })
                .dimensions(left, top + 24, targetWidth, 20)
                .build());

        autotrade$statusX = left;
        autotrade$statusY = top + 48;
        autotrade$refreshButtonLabels();
    }

    @Inject(method = "renderMain", at = @At("TAIL"))
    private void autotrade$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        autotrade$refreshButtonLabels();
        if (ConfigManager.getConfig().showInScreenStatusText) {
            context.drawText(
                    this.textRenderer,
                    AutoTradeEngine.getStatusText(),
                    autotrade$statusX,
                    autotrade$statusY,
                    0xFFFFFF,
                    true
            );
        }
    }

    @Unique
    private Text autotrade$toggleLabel() {
        return Text.literal(AutoTradeEngine.isEnabled() ? "Auto: ON" : "Auto: OFF");
    }

    @Unique
    private void autotrade$refreshButtonLabels() {
        if (autotrade$toggleButton != null) {
            autotrade$toggleButton.setMessage(autotrade$toggleLabel());
        }
        if (autotrade$setTargetButton != null) {
            autotrade$setTargetButton.active = this.selectedIndex >= 0;
        }
    }
}
