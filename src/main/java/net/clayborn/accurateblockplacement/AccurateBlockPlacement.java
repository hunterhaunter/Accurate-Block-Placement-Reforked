package net.clayborn.accurateblockplacement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = AccurateBlockPlacement.MODID, name = "Accurate Block Placement",
        version = "1.3.0", clientSideOnly = true,
        dependencies = "required-after:mixinbooter@[9.0,);")
public class AccurateBlockPlacement {
    public static final String MODID = "accurateblockplacement";

    public static Boolean disableNormalItemUse = false;
    public static boolean isEnabled = true;
    public static boolean freshPressThisTick = false;

    private static KeyBinding toggleKey;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding(
                "key.accurateblockplacement.toggle",
                Keyboard.KEY_NONE,
                "key.categories.accurateblockplacement"
        );
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        while (toggleKey != null && toggleKey.isPressed()) {
            isEnabled = !isEnabled;
            mc.player.sendStatusMessage(
                    new TextComponentTranslation(isEnabled
                            ? "message.accurateblockplacement.enabled"
                            : "message.accurateblockplacement.disabled"),
                    true
            );
        }
    }
}
