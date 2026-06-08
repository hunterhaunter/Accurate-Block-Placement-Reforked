package net.clayborn.accurateblockplacement.mixin;

import net.clayborn.accurateblockplacement.AccurateBlockPlacement;
import net.clayborn.accurateblockplacement.IMinecraftAccessor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements IMinecraftAccessor {

    @Shadow(aliases = {"rightClickDelay"})
    private int f_91011_;

    @Shadow(aliases = {"startUseItem"})
    protected abstract void m_91277_();

    @Override
    public void accurateblockplacement_doItemUseBypassDisable() {
        Boolean oldValue = AccurateBlockPlacement.disableNormalItemUse;
        AccurateBlockPlacement.disableNormalItemUse = false;
        this.m_91277_();
        AccurateBlockPlacement.disableNormalItemUse = oldValue;
    }

    @Inject(method = "m_91277_", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (AccurateBlockPlacement.disableNormalItemUse) {
            ci.cancel();
        }
    }

    @Override
    public void accurateblockplacement_setRightClickDelay(int delay) {
        this.f_91011_ = delay;
    }

    @Override
    public int accurateblockplacement_getRightClickDelay() {
        return this.f_91011_;
    }
}
