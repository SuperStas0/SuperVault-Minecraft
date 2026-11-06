package com.magicvault;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-side mutable vault contents and upgrade state. */
public final class VaultStorage {
	public static final long UNLIMITED = Long.MAX_VALUE / 4;

	public static final class Cell {
		public final ItemStack proto;
		public long count;

		Cell(ItemStack proto, long count) {
			this.proto = proto;
			this.count = count;
		}
	}

	private int upgrades;
	private int unlockedCells = StorageSnapshot.START_CELLS;
	private int goldLevel;
	private final List<Cell> cells = new ArrayList<>();
	private int version;

	public int upgrades() {
		return upgrades;
	}

	public boolean has(int flag) {
		return (upgrades & flag) != 0;
	}

	public int unlockedCells() {
		return unlockedCells;
	}

	public int goldLevel() {
		return goldLevel;
	}

	/** Contents stay inside the dropped item once iron or any gold upgrade is bought. */
	public boolean keepsContents() {
		return has(StorageSnapshot.UP_IRON) || goldLevel > 0;
	}

	public boolean hasProgress() {
		return snapshot(false).hasProgress();
	}

	public boolean isMaxTier() {
		return snapshot(false).isMaxTier();
	}

	public int version() {
		return version;
	}

	public int size() {
		return cells.size();
	}

	public Cell cell(int index) {
		return cells.get(index);
	}

	public int capacity() {
		return has(StorageSnapshot.UP_DIAMOND) ? Integer.MAX_VALUE : unlockedCells;
	}

	private long limitFor(ItemStack proto) {
		return has(StorageSnapshot.UP_IRON) ? UNLIMITED : proto.getMaxStackSize();
	}

	/** Inserts as much of the stack as fits. Does not modify the stack. Returns the amount inserted. */
	public int insert(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}

		int remaining = stack.getCount();

		for (Cell c : cells) {
			if (remaining <= 0) {
				break;
			}

			if (!ItemStack.isSameItemSameComponents(c.proto, stack)) {
				continue;
			}

			long space = limitFor(c.proto) - c.count;

			if (space <= 0) {
				continue;
			}

			int add = (int) Math.min(space, remaining);
			c.count += add;
			remaining -= add;
		}

		while (remaining > 0 && cells.size() < capacity()) {
			ItemStack proto = stack.copyWithCount(1);
			int add = (int) Math.min(limitFor(proto), remaining);
			cells.add(new Cell(proto, add));
			remaining -= add;
		}

		int inserted = stack.getCount() - remaining;

		if (inserted > 0) {
			version++;
		}

		return inserted;
	}

	/** Removes up to {@code amount} (capped at one max stack) from a cell. */
	public ItemStack extract(int index, int amount) {
		if (index < 0 || index >= cells.size() || amount <= 0) {
			return ItemStack.EMPTY;
		}

		Cell c = cells.get(index);
		int n = (int) Math.min(Math.min((long) amount, c.count), c.proto.getMaxStackSize());
		ItemStack out = c.proto.copyWithCount(n);
		c.count -= n;

		if (c.count <= 0) {
			cells.remove(index);
		}

		version++;
		return out;
	}

	public long totalOf(Item item) {
		long total = 0;

		for (Cell c : cells) {
			if (c.proto.is(item)) {
				total += c.count;
			}
		}

		return total;
	}

	private boolean consume(Item item, int amount) {
		if (totalOf(item) < amount) {
			return false;
		}

		long left = amount;

		for (int i = 0; i < cells.size() && left > 0; ) {
			Cell c = cells.get(i);

			if (!c.proto.is(item)) {
				i++;
				continue;
			}

			long take = Math.min(left, c.count);
			c.count -= take;
			left -= take;

			if (c.count <= 0) {
				cells.remove(i);
			} else {
				i++;
			}
		}

		return true;
	}

	public boolean tryUpgrade(int up) {
		StorageSnapshot state = snapshot(false);

		if (!state.canUpgrade(up) || !consume(state.costItem(up), state.costAmount(up))) {
			return false;
		}

		switch (up) {
			case StorageSnapshot.UP_CELL -> unlockedCells++;
			case StorageSnapshot.UP_GOLD -> goldLevel++;
			case StorageSnapshot.UP_IRON -> {
				upgrades |= StorageSnapshot.UP_IRON;
				consolidate();
			}
			case StorageSnapshot.UP_DIAMOND -> upgrades |= StorageSnapshot.UP_DIAMOND;
			default -> {
			}
		}

		version++;
		return true;
	}

	/** Merges cells holding the same item (needed when stacks become unlimited). */
	private void consolidate() {
		for (int i = 0; i < cells.size(); i++) {
			Cell a = cells.get(i);

			for (int j = cells.size() - 1; j > i; j--) {
				Cell b = cells.get(j);

				if (ItemStack.isSameItemSameComponents(a.proto, b.proto)) {
					a.count += b.count;
					cells.remove(j);
				}
			}
		}
	}

	public StorageSnapshot snapshot(boolean withContents) {
		List<StorageSnapshot.CellData> list = new ArrayList<>();

		if (withContents) {
			for (Cell c : cells) {
				list.add(new StorageSnapshot.CellData(c.proto.copy(), c.count));
			}
		}

		return new StorageSnapshot(upgrades, unlockedCells, goldLevel, list);
	}

	public void load(StorageSnapshot snapshot) {
		upgrades = snapshot.upgrades();
		unlockedCells = snapshot.unlockedCells();
		goldLevel = snapshot.goldLevel();
		cells.clear();

		for (StorageSnapshot.CellData d : snapshot.cells()) {
			cells.add(new Cell(d.stack().copyWithCount(1), d.count()));
		}

		version++;
	}

	public void clear() {
		cells.clear();
		version++;
	}

	/** Empties the vault into ordinary stacks (only used for low tiers, where counts are small). */
	public List<ItemStack> drain() {
		List<ItemStack> out = new ArrayList<>();

		for (Cell c : cells) {
			long left = c.count;
			int max = Math.max(1, c.proto.getMaxStackSize());

			while (left > 0) {
				int n = (int) Math.min(max, left);
				out.add(c.proto.copyWithCount(n));
				left -= n;
			}
		}

		cells.clear();
		version++;
		return out;
	}
}
