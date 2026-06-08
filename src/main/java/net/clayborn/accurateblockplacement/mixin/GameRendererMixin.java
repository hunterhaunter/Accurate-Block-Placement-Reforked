package net.clayborn.accurateblockplacement.mixin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import net.clayborn.accurateblockplacement.AccurateBlockPlacement;
import net.clayborn.accurateblockplacement.IKeyMappingAccessor;
import net.clayborn.accurateblockplacement.IMinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private static final String blockUseMethodName = accurateblockplacement$getBlockUseMethodName();
    @Unique
    private static final String itemUseMethodName = accurateblockplacement$getItemUseMethodName();

    @Unique
    private BlockPos lastSeenBlockPos = null;
    @Unique
    private BlockPos lastPlacedBlockPos = null;
    @Unique
    private Vec3 lastPlayerPlacedBlockPos = null;
    @Unique
    private Boolean autoRepeatWaitingOnCooldown = true;
    @Unique
    private Vec3 lastFreshPressMouseRatio = null;
    @Unique
    private ArrayList<HitResult> backFillList = new ArrayList<>();
    @Unique
    private Item lastItemInUse = null;
    @Unique
    private InteractionHand handOfCurrentItemInUse;

    @Unique
    private Item accurateblockplacement$getItemInUse(Minecraft mc) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                this.handOfCurrentItemInUse = hand;
                return stack.getItem();
            }
        }
        return null;
    }

    @Unique
    private static String accurateblockplacement$getBlockUseMethodName() {
        for (Method method : Block.class.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 6
                    && types[0] == BlockState.class
                    && types[1] == Level.class
                    && types[2] == BlockPos.class
                    && types[3] == Player.class
                    && types[4] == InteractionHand.class
                    && types[5] == BlockHitResult.class) {
                return method.getName();
            }
        }
        return null;
    }

    @Unique
    private Boolean accurateblockplacement$doesBlockHaveOverriddenUseMethod(Block block) {
        if (blockUseMethodName == null) {
            System.out.println("[AccurateBlockPlacement] ERROR: blockUseMethodName is null!");
            return false;
        }
        try {
            return !block.getClass().getMethod(blockUseMethodName,
                    BlockState.class, Level.class, BlockPos.class,
                    Player.class, InteractionHand.class, BlockHitResult.class)
                    .getDeclaringClass().equals(BlockBehaviour.class);
        } catch (Exception e) {
            System.out.println("[AccurateBlockPlacement] ERROR: Unable to find block " + block.getClass().getName() + " use method!");
            return false;
        }
    }

    @Unique
    private static String accurateblockplacement$getItemUseMethodName() {
        for (Method method : Item.class.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 3
                    && types[0] == Level.class
                    && types[1] == Player.class
                    && types[2] == InteractionHand.class) {
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
                    Level.class, Player.class, InteractionHand.class)
                    .getDeclaringClass().equals(Item.class);
        } catch (Exception e) {
            System.out.println("[AccurateBlockPlacement] ERROR: Unable to find item " + item.getClass().getName() + " use method!");
            return false;
        }
    }

    @Inject(method = "m_109087_", at = @At("RETURN"))
    private void onPickComplete(float partialTick, CallbackInfo ci) {
        if (!AccurateBlockPlacement.isEnabled) {
            AccurateBlockPlacement.disableNormalItemUse = false;
            this.lastSeenBlockPos = null;
            this.lastPlacedBlockPos = null;
            this.lastPlayerPlacedBlockPos = null;
            this.autoRepeatWaitingOnCooldown = true;
            this.backFillList.clear();
            this.lastFreshPressMouseRatio = null;
            this.lastItemInUse = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.options.keyUse == null
                || mc.hitResult == null || mc.player == null
                || mc.level == null || mc.mouseHandler == null
                || mc.getWindow() == null) {
            return;
        }

        AccurateBlockPlacement.disableNormalItemUse = false;

        IKeyMappingAccessor keyUseAccessor = (IKeyMappingAccessor) mc.options.keyUse;
        boolean freshKeyPress = keyUseAccessor.accurateblockplacement_getClickCount() > 0;

        Item currentItem = accurateblockplacement$getItemInUse(mc);

        if (freshKeyPress) {
            this.lastSeenBlockPos = null;
            this.lastPlacedBlockPos = null;
            this.lastPlayerPlacedBlockPos = null;
            this.autoRepeatWaitingOnCooldown = true;
            this.backFillList.clear();
            this.lastFreshPressMouseRatio = (mc.getWindow().getWidth() > 0 && mc.getWindow().getHeight() > 0)
                    ? new Vec3(mc.mouseHandler.xpos() / (double) mc.getWindow().getWidth(),
                    mc.mouseHandler.ypos() / (double) mc.getWindow().getHeight(), 0.0)
                    : null;
            this.lastItemInUse = currentItem;
        }

        if (currentItem == null) {
            return;
        }

        if (!(currentItem instanceof BlockItem) && !(currentItem instanceof BucketItem)) {
            return;
        }

        if (currentItem.isEdible() || accurateblockplacement$doesItemHaveOverriddenUseMethod(currentItem)) {
            return;
        }

        if (mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        InteractionHand otherHand = (handOfCurrentItemInUse == InteractionHand.MAIN_HAND)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherHandStack = mc.player.getItemInHand(otherHand);
        if (!otherHandStack.isEmpty()
                && (otherHandStack.getItem().isEdible() || accurateblockplacement$doesItemHaveOverriddenUseMethod(otherHandStack.getItem()))
                && mc.player.isUsingItem()) {
            return;
        }

        BlockHitResult blockHitResult = (BlockHitResult) mc.hitResult;
        BlockPos blockHitPos = blockHitResult.getBlockPos();
        Block targetBlock = mc.level.getBlockState(blockHitPos).getBlock();

        boolean isTargetBlockActivatable = accurateblockplacement$doesBlockHaveOverriddenUseMethod(targetBlock);
        if (isTargetBlockActivatable && !mc.player.isShiftKeyDown()) {
            return;
        }

        if (freshKeyPress || mc.options.keyUse.isDown()) {
            AccurateBlockPlacement.disableNormalItemUse = true;

            BlockPlaceContext targetPlacement = new BlockPlaceContext(
                    new UseOnContext(mc.player, handOfCurrentItemInUse, blockHitResult));
            Block oldBlock = mc.level.getBlockState(targetPlacement.getClickedPos()).getBlock();

            double facingAxisPlayerPos = 0.0;
            double facingAxisPlayerLastPos = 0.0;
            double facingAxisLastPlacedPos = 0.0;

            if (this.lastPlacedBlockPos != null && this.lastPlayerPlacedBlockPos != null) {
                Direction.Axis axis = targetPlacement.getClickedFace().getAxis();
                facingAxisPlayerPos = mc.player.position().get(axis);
                facingAxisPlayerLastPos = this.lastPlayerPlacedBlockPos.get(axis);
                facingAxisLastPlacedPos = new Vec3(
                        this.lastPlacedBlockPos.getX(),
                        this.lastPlacedBlockPos.getY(),
                        this.lastPlacedBlockPos.getZ()).get(axis);

                String faceName = targetPlacement.getClickedFace().getName();
                if (faceName.equals("west") || faceName.equals("north")) {
                    facingAxisLastPlacedPos += 1.0;
                }
            }

            IMinecraftAccessor mcAccessor = (IMinecraftAccessor) mc;

            Vec3 currentMouseRatio = null;
            if (mc.getWindow().getWidth() > 0 && mc.getWindow().getHeight() > 0) {
                currentMouseRatio = new Vec3(
                        mc.mouseHandler.xpos() / (double) mc.getWindow().getWidth(),
                        mc.mouseHandler.ypos() / (double) mc.getWindow().getHeight(),
                        0.0);
            }

            boolean isPlacementTargetFresh =
                    ((this.lastSeenBlockPos == null || !this.lastSeenBlockPos.equals(blockHitPos))
                            && (this.lastPlacedBlockPos == null || !this.lastPlacedBlockPos.equals(blockHitPos)))
                            || (this.lastPlacedBlockPos != null && this.lastPlayerPlacedBlockPos != null
                            && this.lastPlacedBlockPos.equals(blockHitPos)
                            && Math.abs(facingAxisPlayerLastPos - facingAxisPlayerPos) >= 0.99
                            && Math.abs(facingAxisPlayerLastPos - facingAxisLastPlacedPos)
                            < Math.abs(facingAxisPlayerPos - facingAxisLastPlacedPos));

            boolean hasMouseMoved = currentMouseRatio != null
                    && this.lastFreshPressMouseRatio != null
                    && this.lastFreshPressMouseRatio.distanceTo(currentMouseRatio) >= 0.1;

            boolean isOnCooldown = this.autoRepeatWaitingOnCooldown
                    && mcAccessor.accurateblockplacement_getRightClickDelay() > 0
                    && !hasMouseMoved;

            if (this.lastItemInUse == currentItem) {
                if (freshKeyPress || (isPlacementTargetFresh && !isOnCooldown)) {
                    if (this.autoRepeatWaitingOnCooldown && !freshKeyPress) {
                        this.autoRepeatWaitingOnCooldown = false;
                        HitResult currentHitResult = mc.hitResult;
                        Iterator<HitResult> iterator = this.backFillList.iterator();
                        while (iterator.hasNext()) {
                            HitResult prevHitResult = iterator.next();
                            mc.hitResult = prevHitResult;
                            mcAccessor.accurateblockplacement_doItemUseBypassDisable();
                        }
                        this.backFillList.clear();
                        mc.hitResult = currentHitResult;
                    }

                    boolean runOnceFlag = !freshKeyPress;
                    while (runOnceFlag || mc.options.keyUse.consumeClick()) {
                        mcAccessor.accurateblockplacement_doItemUseBypassDisable();
                        if (!oldBlock.equals(mc.level.getBlockState(targetPlacement.getClickedPos()).getBlock())) {
                            this.lastPlacedBlockPos = targetPlacement.getClickedPos();
                            if (this.lastPlayerPlacedBlockPos == null) {
                                this.lastPlayerPlacedBlockPos = mc.player.position();
                            } else {
                                Vec3 normal = new Vec3(
                                        targetPlacement.getClickedFace().getNormal().getX(),
                                        targetPlacement.getClickedFace().getNormal().getY(),
                                        targetPlacement.getClickedFace().getNormal().getZ());
                                Vec3 summedLastPlayerPos = this.lastPlayerPlacedBlockPos.add(normal);
                                Vec3 newLastPlayerPlacedPos = null;
                                switch (targetPlacement.getClickedFace().getAxis()) {
                                    case X:
                                        newLastPlayerPlacedPos = new Vec3(
                                                summedLastPlayerPos.x,
                                                mc.player.position().y,
                                                mc.player.position().z);
                                        break;
                                    case Y:
                                        newLastPlayerPlacedPos = new Vec3(
                                                mc.player.position().x,
                                                summedLastPlayerPos.y,
                                                mc.player.position().z);
                                        break;
                                    case Z:
                                        newLastPlayerPlacedPos = new Vec3(
                                                mc.player.position().x,
                                                mc.player.position().y,
                                                summedLastPlayerPos.z);
                                        break;
                                }
                                this.lastPlayerPlacedBlockPos = newLastPlayerPlacedPos;
                            }
                        }
                        runOnceFlag = false;
                    }
                } else if (isPlacementTargetFresh) {
                    this.backFillList.add(mc.hitResult);
                }
            }

            this.lastSeenBlockPos = blockHitResult.getBlockPos();
        }
    }
}
