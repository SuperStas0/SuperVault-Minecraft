package com.magicvault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class VaultMenu extends AbstractContainerMenu {
    public static final int PLAYER_X = 8;
    public static final int INV_Y = 139;
    public static final int HOTBAR_Y = 197;

    private final ContainerLevelAccess access;
    private final Player player;
    @Nullable
    private final VaultBlockEntity blockEntity;
    @Nullable
    private final VaultStorage storage;
    private int lastSentVersion = -1;
    /** Opened through a Vault Link: no distance limit, and the vault's chunk is kept loaded. */
    private final boolean remote;
    private int ticketCountdown = 0;

    /** Client-side copy of the vault state, updated by {@link SyncPayload}. */
    public StorageSnapshot view = StorageSnapshot.EMPTY;

    public VaultMenu(int syncId, Inventory inventory, BlockPos pos) {
        this(syncId, inventory, pos, null);
    }

    /** {@code remoteVault} is only set on the server, when opened through a Vault Link (it may be in another dimension). */
    public VaultMenu(int syncId, Inventory inventory, BlockPos pos, @Nullable VaultBlockEntity remoteVault) {
        super(MagicVault.VAULT_MENU, syncId);
        this.player = inventory.player;
        Level level = inventory.player.level();
        this.remote = remoteVault != null;

        if (remoteVault != null) {
            Level vaultLevel = remoteVault.getLevel();
            this.access = ContainerLevelAccess.create(vaultLevel != null ? vaultLevel : level, pos);
            this.blockEntity = remoteVault;
            this.storage = remoteVault.storage();
        } else if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VaultBlockEntity be) {
            this.access = ContainerLevelAccess.create(level, pos);
            this.blockEntity = be;
            this.storage = be.storage();
        } else {
            this.access = ContainerLevelAccess.create(level, pos);
            this.blockEntity = null;
            this.storage = null;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, PLAYER_X + col * 18, INV_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (remote && blockEntity != null && !blockEntity.isRemoved() && blockEntity.getLevel() instanceof ServerLevel vaultLevel
            && ticketCountdown-- <= 0) {
            ticketCountdown = 20;
            VaultLinkItem.keepLoaded(vaultLevel, blockEntity.getBlockPos());
        }

        if (storage != null && player instanceof ServerPlayer sp && storage.version() != lastSentVersion) {
            lastSentVersion = storage.version();
            ServerPlayNetworking.send(sp, new SyncPayload(containerId, storage.snapshot(true)));
        }
    }

    /** Server side only. */
    public void handleAction(ActionPayload payload) {
        if (storage == null || blockEntity == null || blockEntity.isRemoved()) {
            return;
        }

        switch (payload.action()) {
            case ActionPayload.CELL -> cellClick(payload);
            case ActionPayload.UPGRADE -> storage.tryUpgrade(payload.index());
            default -> {
            }
        }

        blockEntity.setChanged();
        broadcastChanges();
    }

    private void cellClick(ActionPayload p) {
        boolean right = (p.flags() & 1) != 0;
        boolean shift = (p.flags() & 2) != 0;
        ItemStack carried = getCarried();

        if (!carried.isEmpty()) {
            int amount = right ? 1 : carried.getCount();
            int inserted = storage.insert(carried.copyWithCount(amount));
            carried.shrink(inserted);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            return;
        }

        int index = p.index();

        if (index < 0 || index >= storage.size()) {
            return;
        }

        VaultStorage.Cell cell = storage.cell(index);

        if (!ItemStack.isSameItemSameComponents(cell.proto, p.proto())) {
            return;
        }

        int perStack = (int) Math.min((long) cell.proto.getMaxStackSize(), cell.count);
        int want = right ? (shift ? 1 : (perStack + 1) / 2) : perStack;
        ItemStack out = storage.extract(index, want);

        if (out.isEmpty()) {
            return;
        }

        if (shift) {
            moveItemStackTo(out, 0, 36, true);

            if (!out.isEmpty()) {
                storage.insert(out);
            }
        } else {
            setCarried(out);
        }
    }

    /** Shift-click on a player inventory slot: deposit into the vault. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (storage == null || blockEntity == null || blockEntity.isRemoved()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inserted = storage.insert(stack);

        if (inserted <= 0) {
            return ItemStack.EMPTY;
        }

        stack.shrink(inserted);

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        blockEntity.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (remote) {
            return blockEntity != null && !blockEntity.isRemoved();
        }

        return stillValid(access, player, MagicVault.VAULT_BLOCK);
    }
}
