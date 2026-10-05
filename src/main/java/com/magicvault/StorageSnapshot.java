package com.magicvault;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Immutable copy of a vault's upgrade state and contents. Used as item component and network payload.
 * Also holds the upgrade rules, so the server and the client screen always agree on costs.
 */
public record StorageSnapshot(int upgrades, int unlockedCells, int goldLevel, List<CellData> cells) {
	/** Upgrade ids. IRON and DIAMOND double as bits in {@link #upgrades()}. */
	public static final int UP_CELL = 1;
	public static final int UP_IRON = 2;
	public static final int UP_GOLD = 4;
	public static final int UP_DIAMOND = 8;

	public static final int START_CELLS = 9;
	public static final int COPPER_CELLS_END = 27;
	public static final int MAX_CELLS = 54;
	/** 1 upgrade for the search bar and All tab, then one per filter tab (10 filters). */
	public static final int GOLD_MAX = 11;

	public static final StorageSnapshot EMPTY = new StorageSnapshot(0, START_CELLS, 0, List.of());

	public static final Codec<StorageSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("upgrades", 0).forGetter(StorageSnapshot::upgrades),
		Codec.INT.optionalFieldOf("unlocked", START_CELLS).forGetter(StorageSnapshot::unlockedCells),
		Codec.INT.optionalFieldOf("gold", 0).forGetter(StorageSnapshot::goldLevel),
		CellData.CODEC.listOf().optionalFieldOf("cells", List.of()).forGetter(StorageSnapshot::cells)
	).apply(i, StorageSnapshot::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, StorageSnapshot> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, StorageSnapshot::upgrades,
		ByteBufCodecs.VAR_INT, StorageSnapshot::unlockedCells,
		ByteBufCodecs.VAR_INT, StorageSnapshot::goldLevel,
		CellData.STREAM_CODEC.apply(ByteBufCodecs.list()), StorageSnapshot::cells,
		StorageSnapshot::new
	);

	public boolean has(int flag) {
		return (upgrades & flag) != 0;
	}

	public boolean hasSearch() {
		return goldLevel >= 1;
	}

	public boolean hasProgress() {
		return upgrades != 0 || unlockedCells != START_CELLS || goldLevel != 0;
	}

	/** Iron and gold become available once every cell is unlocked. */
	private boolean cellsDone() {
		return unlockedCells >= MAX_CELLS;
	}

	public boolean canUpgrade(int up) {
		return switch (up) {
			case UP_CELL -> unlockedCells < MAX_CELLS;
			case UP_IRON -> cellsDone() && !has(UP_IRON);
			case UP_GOLD -> cellsDone() && goldLevel < GOLD_MAX;
			case UP_DIAMOND -> has(UP_IRON) && goldLevel >= GOLD_MAX && !has(UP_DIAMOND);
			default -> false;
		};
	}

	public Item costItem(int up) {
		return switch (up) {
			case UP_CELL -> unlockedCells < COPPER_CELLS_END ? Items.COPPER_INGOT : Items.COAL;
			case UP_IRON -> Items.IRON_INGOT;
			case UP_GOLD -> Items.GOLD_INGOT;
			default -> Items.DIAMOND;
		};
	}

	public int costAmount(int up) {
		return switch (up) {
			case UP_CELL -> 4;
			case UP_IRON -> 64;
			case UP_GOLD -> 8;
			default -> 64;
		};
	}

	/** One cell: a single item type (stack count 1 used as prototype) and its total count. */
	public record CellData(ItemStack stack, long count) {
		public static final Codec<CellData> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.CODEC.fieldOf("item").forGetter(CellData::stack),
			Codec.LONG.fieldOf("count").forGetter(CellData::count)
		).apply(i, CellData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, CellData> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, CellData::stack,
			ByteBufCodecs.VAR_LONG, CellData::count,
			CellData::new
		);
	}
}
