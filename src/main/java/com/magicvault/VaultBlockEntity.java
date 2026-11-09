package com.magicvault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

public class VaultBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    private static final String KEY = "Vault";

    private final VaultStorage storage = new VaultStorage();

    public VaultBlockEntity(BlockPos pos, BlockState state) {
        super(MagicVault.VAULT_BE, pos, state);
    }

    public VaultStorage storage() {
        return storage;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storage.load(input.read(KEY, StorageSnapshot.CODEC).orElse(StorageSnapshot.EMPTY));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(KEY, StorageSnapshot.CODEC, storage.snapshot(true));
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magicvault.vault");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new VaultMenu(syncId, inventory, worldPosition);
    }

    /** Menu provider for opening this vault from afar (Vault Link): no distance check, chunk kept loaded. */
    public ExtendedScreenHandlerFactory<BlockPos> remoteFactory() {
        return new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public BlockPos getScreenOpeningData(ServerPlayer player) {
                return VaultBlockEntity.this.getBlockPos();
            }

            @Override
            public Component getDisplayName() {
                return VaultBlockEntity.this.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
                return new VaultMenu(syncId, inventory, VaultBlockEntity.this.getBlockPos(), VaultBlockEntity.this);
            }
        };
    }
}
