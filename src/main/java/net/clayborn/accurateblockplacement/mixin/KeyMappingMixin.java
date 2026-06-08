package net.clayborn.accurateblockplacement.mixin;

import net.clayborn.accurateblockplacement.IKeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyMappingAccessor {

    @Shadow(aliases = {"clickCount"})
    private int f_90818_;

    @Override
    public int accurateblockplacement_getClickCount() {
        return this.f_90818_;
    }
}
