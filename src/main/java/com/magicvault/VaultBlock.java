package com.magicvault;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VaultBlock extends BaseEntityBlock {
    public static final MapCodec<VaultBlock> CODEC = simpleCodec(VaultBlock::new);

    public VaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<VaultBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaultBlockEntity(pos, state);
    }

    /** A Vault Link used on a fully upgraded vault binds to it. On any other vault the menu opens as usual. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(MagicVault.VAULT_LINK_ITEM)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof VaultBlockEntity be && be.storage().isMaxTier()) {
            VaultLinkItem.bind(stack, level, pos);
            player.displayClientMessage(Component.literal("Vault Link bound"), true);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Component.literal("Only a fully upgraded vault can be linked"), true);
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }

        return InteractionResult.SUCCESS;
    }

    /** Restores tier (and contents, for iron/gold/diamond vaults) from the item. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VaultBlockEntity be) {
            StorageSnapshot data = stack.get(MagicVault.STORAGE_DATA);

            if (data != null) {
                be.storage().load(data);
                be.setChanged();
            }
        }
    }

    /**
     * Iron/gold/diamond vaults keep their contents inside the dropped item (like a shulker box).
     * Coal/none vaults drop their contents as normal items; the tier is still remembered by the item.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VaultBlockEntity be) {
            VaultStorage st = be.storage();
            boolean keepContents = st.keepsContents();
            boolean hasContents = keepContents && st.size() > 0;

            ItemStack item = new ItemStack(MagicVault.VAULT_ITEM);

            if (st.hasProgress()) {
                item.set(MagicVault.STORAGE_DATA, st.snapshot(keepContents));
            }

            if (keepContents) {
                st.clear();
            } else {
                for (ItemStack s : st.drain()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
                }
            }

            if (!player.isCreative() || hasContents) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }
}
