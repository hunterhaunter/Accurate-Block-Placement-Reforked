package net.clayborn.accurateblockplacement.mixin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import net.clayborn.accurateblockplacement.AccurateBlockPlacement;
import net.clayborn.accurateblockplacement.IMinecraftAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Unique
    private static final String blockActivateMethodName = accurateblockplacement$getBlockActivateMethodName();
    @Unique
    private static final String itemUseMethodName = accurateblockplacement$getItemUseMethodName();

    @Unique
    private BlockPos lastSeenBlockPos = null;
    @Unique
    private BlockPos lastPlacedBlockPos = null;
    @Unique
    private Vec3d lastPlayerPlacedBlockPos = null;
    @Unique
    private Boolean autoRepeatWaitingOnCooldown = true;
    @Unique
    private ArrayList<RayTraceResult> backFillList = new ArrayList<RayTraceResult>();
    @Unique
    private Item lastItemInUse = null;
    @Unique
    private EnumHand handOfCurrentItemInUse;
    @Unique
    private boolean wasUseKeyDown = false;

    @Unique
    private Item accurateblockplacement$getItemInUse(Minecraft mc) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = mc.player.getHeldItem(hand);
            if (!stack.isEmpty()) {
                this.handOfCurrentItemInUse = hand;
                return stack.getItem();
            }
        }
        return null;
    }

    @Unique
    private static double accurateblockplacement$getAxisValue(Vec3d vec, EnumFacing.Axis axis) {
        switch (axis) {
            case X: return vec.x;
            case Y: return vec.y;
            case Z: return vec.z;
            default: return 0;
        }
    }

    @Unique
    private static String accurateblockplacement$getBlockActivateMethodName() {
        for (Method method : Block.class.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 9
                    && types[0] == World.class
                    && types[1] == BlockPos.class
                    && types[2] == IBlockState.class
                    && types[3] == EntityPlayer.class
                    && types[4] == EnumHand.class
                    && types[5] == EnumFacing.class
                    && types[6] == float.class
                    && types[7] == float.class
                    && types[8] == float.class) {
                return method.getName();
            }
        }
        return null;
    }

    @Unique
    private Boolean accurateblockplacement$doesBlockHaveOverriddenActivateMethod(Block block) {
        if (blockActivateMethodName == null) {
            System.out.println("[AccurateBlockPlacement] ERROR: blockActivateMethodName is null!");
            return false;
        }
        try {
            return !block.getClass().getMethod(blockActivateMethodName,
                    World.class, BlockPos.class, IBlockState.class,
                    EntityPlayer.class, EnumHand.class, EnumFacing.class,
                    float.class, float.class, float.class)
                    .getDeclaringClass().equals(Block.class);
        } catch (Exception e) {
            System.out.println("[AccurateBlockPlacement] ERROR: Unable to find block " + block.getClass().getName() + " activate method!");
            return false;
        }
    }

    @Unique
    private static String accurateblockplacement$getItemUseMethodName() {
        for (Method method : Item.class.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 3
                    && types[0] == World.class
                    && types[1] == EntityPlayer.class
                    && types[2] == EnumHand.class) {
                return method.getName();
            }
        }
        return null;
    }

    @Unique
    private Boolean accurateblockplacement$doesItemHaveOverriddenUseMethod(Item item) {
        if (itemUseMethodName == null) {
            System.out.println("[AccurateBlockPlacement] ERROR: itemUseMethodName is null!");
            return false;
        }
        try {
            return !item.getClass().getMethod(itemUseMethodName,
                    World.class, EntityPlayer.class, EnumHand.class)
                    .getDeclaringClass().equals(Item.class);
        } catch (Exception e) {
            System.out.println("[AccurateBlockPlacement] ERROR: Unable to find item " + item.getClass().getName() + " use method!");
            return false;
        }
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void onPickComplete(float partialTick, CallbackInfo ci) {
        if (!AccurateBlockPlacement.isEnabled) {
            AccurateBlockPlacement.disableNormalItemUse = false;
            this.lastSeenBlockPos = null;
            this.lastPlacedBlockPos = null;
            this.lastPlayerPlacedBlockPos = null;
            this.autoRepeatWaitingOnCooldown = true;
            this.backFillList.clear();
            this.lastItemInUse = null;
            this.wasUseKeyDown = false;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.keyBindUseItem == null
                || mc.objectMouseOver == null || mc.player == null
                || mc.world == null) {
            return;
        }

        AccurateBlockPlacement.disableNormalItemUse = false;

        boolean useKeyDown = mc.gameSettings.keyBindUseItem.isKeyDown();
        boolean freshKeyPress = useKeyDown && !this.wasUseKeyDown;
        this.wasUseKeyDown = useKeyDown;

        Item currentItem = accurateblockplacement$getItemInUse(mc);

        if (freshKeyPress) {
            this.lastSeenBlockPos = null;
            this.lastPlacedBlockPos = null;
            this.lastPlayerPlacedBlockPos = null;
            this.autoRepeatWaitingOnCooldown = true;
            this.backFillList.clear();
            this.lastItemInUse = currentItem;
        }

        if (currentItem == null) {
            return;
        }

        if (!(currentItem instanceof ItemBlock) && !(currentItem instanceof ItemBucket)) {
            return;
        }

        if (currentItem instanceof ItemFood || accurateblockplacement$doesItemHaveOverriddenUseMethod(currentItem)) {
            return;
        }

        if (mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        EnumHand otherHand = (handOfCurrentItemInUse == EnumHand.MAIN_HAND)
                ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack otherHandStack = mc.player.getHeldItem(otherHand);
        if (!otherHandStack.isEmpty()
                && (otherHandStack.getItem() instanceof ItemFood
                    || accurateblockplacement$doesItemHaveOverriddenUseMethod(otherHandStack.getItem()))
                && mc.player.isHandActive()) {
            return;
        }

        RayTraceResult rayTrace = mc.objectMouseOver;
        BlockPos blockHitPos = rayTrace.getBlockPos();
        Block targetBlock = mc.world.getBlockState(blockHitPos).getBlock();

        boolean isTargetBlockActivatable = accurateblockplacement$doesBlockHaveOverriddenActivateMethod(targetBlock);
        if (isTargetBlockActivatable && !mc.player.isSneaking()) {
            return;
        }

        if (freshKeyPress || mc.gameSettings.keyBindUseItem.isKeyDown()) {
            AccurateBlockPlacement.disableNormalItemUse = true;

            EnumFacing clickedFace = rayTrace.sideHit;
            BlockPos placePos;
            IBlockState hitState = mc.world.getBlockState(rayTrace.getBlockPos());
            if (hitState.getBlock().isReplaceable(mc.world, rayTrace.getBlockPos())) {
                placePos = rayTrace.getBlockPos();
            } else {
                placePos = rayTrace.getBlockPos().offset(rayTrace.sideHit);
            }

            Block oldBlock = mc.world.getBlockState(placePos).getBlock();

            double facingAxisPlayerPos = 0.0;
            double facingAxisPlayerLastPos = 0.0;
            double facingAxisLastPlacedPos = 0.0;

            if (this.lastPlacedBlockPos != null && this.lastPlayerPlacedBlockPos != null) {
                EnumFacing.Axis axis = clickedFace.getAxis();
                facingAxisPlayerPos = accurateblockplacement$getAxisValue(mc.player.getPositionVector(), axis);
                facingAxisPlayerLastPos = accurateblockplacement$getAxisValue(this.lastPlayerPlacedBlockPos, axis);
                facingAxisLastPlacedPos = accurateblockplacement$getAxisValue(new Vec3d(
                        this.lastPlacedBlockPos.getX(),
                        this.lastPlacedBlockPos.getY(),
                        this.lastPlacedBlockPos.getZ()), axis);

                String faceName = clickedFace.getName();
                if (faceName.equals("west") || faceName.equals("north")) {
                    facingAxisLastPlacedPos += 1.0;
                }
            }

            IMinecraftAccessor mcAccessor = (IMinecraftAccessor) mc;

            boolean isPlacementTargetFresh =
                    ((this.lastSeenBlockPos == null || !this.lastSeenBlockPos.equals(blockHitPos))
                            && (this.lastPlacedBlockPos == null || !this.lastPlacedBlockPos.equals(blockHitPos)))
                            || (this.lastPlacedBlockPos != null && this.lastPlayerPlacedBlockPos != null
                            && this.lastPlacedBlockPos.equals(blockHitPos)
                            && Math.abs(facingAxisPlayerLastPos - facingAxisPlayerPos) >= 0.99
                            && Math.abs(facingAxisPlayerLastPos - facingAxisLastPlacedPos)
                            < Math.abs(facingAxisPlayerPos - facingAxisLastPlacedPos));

            boolean isOnCooldown = mcAccessor.accurateblockplacement_getRightClickDelay() > 0;

            if (this.lastItemInUse == currentItem) {
                if (!freshKeyPress && isPlacementTargetFresh && !isOnCooldown) {
                    if (this.autoRepeatWaitingOnCooldown) {
                        this.autoRepeatWaitingOnCooldown = false;
                        RayTraceResult currentHitResult = mc.objectMouseOver;
                        Iterator<RayTraceResult> iterator = this.backFillList.iterator();
                        while (iterator.hasNext()) {
                            RayTraceResult prevHitResult = iterator.next();
                            mc.objectMouseOver = prevHitResult;
                            mcAccessor.accurateblockplacement_doItemUseBypassDisable();
                        }
                        this.backFillList.clear();
                        mc.objectMouseOver = currentHitResult;
                    }

                    boolean runOnceFlag = true;
                    while (runOnceFlag) {
                        mcAccessor.accurateblockplacement_doItemUseBypassDisable();
                        if (!oldBlock.equals(mc.world.getBlockState(placePos).getBlock())) {
                            this.lastPlacedBlockPos = placePos;
                            if (this.lastPlayerPlacedBlockPos == null) {
                                this.lastPlayerPlacedBlockPos = mc.player.getPositionVector();
                            } else {
                                Vec3i dirVec = clickedFace.getDirectionVec();
                                Vec3d normal = new Vec3d(dirVec.getX(), dirVec.getY(), dirVec.getZ());
                                Vec3d summedLastPlayerPos = this.lastPlayerPlacedBlockPos.add(normal);
                                Vec3d newLastPlayerPlacedPos = null;
                                switch (clickedFace.getAxis()) {
                                    case X:
                                        newLastPlayerPlacedPos = new Vec3d(
                                                summedLastPlayerPos.x,
                                                mc.player.getPositionVector().y,
                                                mc.player.getPositionVector().z);
                                        break;
                                    case Y:
                                        newLastPlayerPlacedPos = new Vec3d(
                                                mc.player.getPositionVector().x,
                                                summedLastPlayerPos.y,
                                                mc.player.getPositionVector().z);
                                        break;
                                    case Z:
                                        newLastPlayerPlacedPos = new Vec3d(
                                                mc.player.getPositionVector().x,
                                                mc.player.getPositionVector().y,
                                                summedLastPlayerPos.z);
                                        break;
                                }
                                this.lastPlayerPlacedBlockPos = newLastPlayerPlacedPos;
                            }
                        }
                        runOnceFlag = false;
                    }
                } else if (!freshKeyPress && isPlacementTargetFresh && this.autoRepeatWaitingOnCooldown) {
                    this.backFillList.add(mc.objectMouseOver);
                }
            }

            this.lastSeenBlockPos = rayTrace.getBlockPos();
        }
    }
}
