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

    @Shadow
    private int rightClickDelayTimer;

    @Shadow
    protected abstract void rightClickMouse();

    @Override
    public void accurateblockplacement_doItemUseBypassDisable() {
        Boolean oldValue = AccurateBlockPlacement.disableNormalItemUse;
        AccurateBlockPlacement.disableNormalItemUse = false;
        this.rightClickMouse();
        AccurateBlockPlacement.disableNormalItemUse = oldValue;
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClickMouse(CallbackInfo ci) {
        if (AccurateBlockPlacement.disableNormalItemUse) {
            ci.cancel();
        }
    }

    @Override
    public void accurateblockplacement_setRightClickDelay(int delay) {
        this.rightClickDelayTimer = delay;
    }

    @Override
    public int accurateblockplacement_getRightClickDelay() {
        return this.rightClickDelayTimer;
    }
}
