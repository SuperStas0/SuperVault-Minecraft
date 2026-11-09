package com.magicvault.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.magicvault.ActionPayload;
import com.magicvault.StorageSnapshot;
import com.magicvault.VaultMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class VaultScreen extends AbstractContainerScreen<VaultMenu> {
    // All art comes from vanilla textures/sprites, so resource packs (e.g. Night UI) re-skin this screen.
    private static final Identifier CONTAINER_TEX = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier TAB_SPRITE = Identifier.withDefaultNamespace("recipe_book/tab");
    private static final Identifier TAB_SELECTED_SPRITE = Identifier.withDefaultNamespace("recipe_book/tab_selected");
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HOVER_SPRITE = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier BUTTON_OFF_SPRITE = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");

    private static final int W = 176;
    private static final int H = 222;
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int CELL = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int HEADER_CROP = 9;

    private static final int TAB_W = 32;
    private static final int TAB_H = 27;
    private static final int TAB_Y0 = 2;
    private static final int TAB_RIGHT_X = -32;
    private static final int TAB_LEFT_X = -64;
    private static final int TAB_ROWS = 6;

    private static final int PANEL_X = 172;
    private static final int PANEL_W = 88;
    private static final int BTN_X = 181;
    private static final int BTN_W = 70;
    private static final int BTN_H = 20;
    private static final int BLOCK_H = 46;

    private static final int SCROLLER_W = 12;
    private static final int SCROLLER_H = 15;
    private static final int TRACK_X = 179;
    private static final int TRACK_H = ROWS * CELL;

    private static final int GLFW_KEY_ESCAPE = 256;

    private static final String[] CAT_NAMES = {
        "All", "Blocks", "Furniture", "Tools", "Combat", "Food", "Resources", "Potions", "Mobs", "Redstone", "Misc"
    };

    private static final ItemStack[] CAT_ICONS = {
        new ItemStack(Items.CHEST), new ItemStack(Items.GRASS_BLOCK), new ItemStack(Items.OAK_STAIRS),
        new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.APPLE),
        new ItemStack(Items.IRON_INGOT), new ItemStack(Items.BREWING_STAND), new ItemStack(Items.BONE),
        new ItemStack(Items.REDSTONE), new ItemStack(Items.COMPASS)
    };

    private static final Set<String> TOOL_IDS = Set.of(
        "shears", "fishing_rod", "flint_and_steel", "bucket", "compass", "recovery_compass", "clock", "spyglass",
        "brush", "lead", "name_tag", "carrot_on_a_stick", "warped_fungus_on_a_stick", "saddle");
    private static final Set<String> COMBAT_IDS = Set.of(
        "mace", "trident", "bow", "crossbow", "shield", "elytra", "wolf_armor", "fire_charge");
    private static final Set<String> MOB_IDS = Set.of(
        "rotten_flesh", "bone", "gunpowder", "spider_eye", "string", "slime_ball", "ender_pearl", "ender_eye",
        "blaze_rod", "blaze_powder", "ghast_tear", "magma_cream", "phantom_membrane", "leather", "feather",
        "ink_sac", "glow_ink_sac", "rabbit_hide", "rabbit_foot", "nautilus_shell", "shulker_shell",
        "prismarine_shard", "prismarine_crystals", "turtle_scute", "armadillo_scute", "nether_star", "breeze_rod",
        "wind_charge", "egg", "totem_of_undying", "heart_of_the_sea", "fermented_spider_eye");
    private static final Set<String> RESOURCE_IDS = Set.of(
        "coal", "charcoal", "diamond", "emerald", "lapis_lazuli", "quartz", "amethyst_shard", "flint", "clay_ball",
        "brick", "nether_brick", "ancient_debris", "echo_shard", "netherite_scrap");
    private static final Set<String> REDSTONE_IDS = Set.of(
        "redstone", "repeater", "comparator", "piston", "sticky_piston", "observer", "hopper", "dropper", "dispenser",
        "lever", "tripwire_hook", "redstone_torch", "redstone_lamp", "redstone_block", "daylight_detector", "target",
        "tnt", "note_block", "jukebox", "slime_block", "honey_block", "lightning_rod", "sculk_sensor",
        "calibrated_sculk_sensor", "crafter", "rail", "powered_rail", "detector_rail", "activator_rail", "bell");
    private static final Set<String> FURNITURE_IDS = Set.of(
        "chest", "trapped_chest", "ender_chest", "barrel", "bookshelf", "chiseled_bookshelf", "ladder", "lantern",
        "soul_lantern", "flower_pot", "painting", "item_frame", "glow_item_frame", "armor_stand", "crafting_table",
        "furnace", "blast_furnace", "smoker", "loom", "cartography_table", "fletching_table", "smithing_table",
        "grindstone", "stonecutter", "lectern", "composter", "cauldron", "anvil", "enchanting_table", "brewing_stand",
        "campfire", "soul_campfire", "torch", "soul_torch", "end_rod", "chain", "scaffolding", "cake");
    private static final String[] FURNITURE_SUFFIX = {
        "_stairs", "_slab", "_fence", "_fence_gate", "_wall", "_door", "_trapdoor", "_bed", "_sign", "_hanging_sign",
        "_carpet", "_banner", "_candle", "_pane", "_shelf", "_lantern", "_chest", "_shulker_box", "_glazed_terracotta"
    };
    private static final String[] ARMOR_SUFFIX = {"_helmet", "_chestplate", "_leggings", "_boots", "_horse_armor", "_spear"};

    private static final java.util.Map<Item, Integer> CATEGORY_CACHE = new java.util.HashMap<>();

    private static int category(ItemStack s) {
        return CATEGORY_CACHE.computeIfAbsent(s.getItem(), item -> computeCategory(s));
    }

    private static int computeCategory(ItemStack s) {
        String p = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();

        if (p.endsWith("potion") || p.equals("glass_bottle") || p.equals("dragon_breath")) {
            return 7;
        }

        if (p.endsWith("_pickaxe") || p.endsWith("_axe") || p.endsWith("_shovel") || p.endsWith("_hoe")
            || p.endsWith("_bucket") || TOOL_IDS.contains(p)) {
            return 3;
        }

        if (p.endsWith("_sword") || p.endsWith("arrow") || endsWithAny(p, ARMOR_SUFFIX) || COMBAT_IDS.contains(p)) {
            return 4;
        }

        if (MOB_IDS.contains(p) || p.endsWith("_spawn_egg") || p.endsWith("_skull") || p.endsWith("_head")) {
            return 8;
        }

        if (p.endsWith("_ingot") || p.endsWith("_nugget") || p.startsWith("raw_") || p.endsWith("_ore")
            || RESOURCE_IDS.contains(p)) {
            return 6;
        }

        if (s.has(DataComponents.FOOD)) {
            return 5;
        }

        if (p.endsWith("_button") || p.endsWith("_pressure_plate") || p.endsWith("minecart") || REDSTONE_IDS.contains(p)) {
            return 9;
        }

        if (endsWithAny(p, FURNITURE_SUFFIX) || FURNITURE_IDS.contains(p)) {
            return 2;
        }

        if (s.getItem() instanceof BlockItem) {
            return 1;
        }

        return 10;
    }

    private static boolean endsWithAny(String s, String[] suffixes) {
        for (String x : suffixes) {
            if (s.endsWith(x)) {
                return true;
            }
        }

        return false;
    }

    private record Btn(int up, int x, int y, int iconX, int iconY) {
    }

    private static final Item[] COST_ITEMS = {Items.COPPER_INGOT, Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND};

    /** Fixed tab layout: right column (attached to the window) has All + 5 filters, left column has 5 more. */
    private static final int[] RIGHT_COL = {0, 1, 2, 3, 4, 5};
    private static final int[] LEFT_COL = {-1, 6, 7, 8, 9, 10};
    private int selectedCat = 0;

    private EditBox searchBox;
    private String savedSearch = "";
    private int scroll = 0;
    private boolean draggingScrollbar = false;

    private int[] visible = new int[0];
    private final long[] haveCache = new long[COST_ITEMS.length];
    private StorageSnapshot cachedView = null;
    private boolean dirty = true;

    public VaultScreen(VaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.inventoryLabelY = H - 94;
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();

        searchBox = new EditBox(this.font, leftPos + 8, topPos + 4, 160, 12, Component.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(text -> {
            savedSearch = text;
            searchBox.setSuggestion(text.isEmpty() ? "Search..." : null);
            dirty = true;
            scroll = 0;
        });
        searchBox.setValue(savedSearch);
        searchBox.setSuggestion(savedSearch.isEmpty() ? "Search..." : null);
        searchBox.visible = gold();
        addRenderableWidget(searchBox);

        if (gold()) {
            this.setFocused(searchBox);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean gold = gold();

        if (searchBox != null && searchBox.visible != gold) {
            searchBox.visible = gold;
            this.setFocused(gold ? searchBox : null);
        }
    }

    private boolean has(int flag) {
        return menu.view.has(flag);
    }

    private boolean gold() {
        return menu.view.hasSearch();
    }

    private int goldLevel() {
        return menu.view.goldLevel();
    }

    private int unlockedCells() {
        return menu.view.unlockedCells();
    }

    /** Tab categories 0..goldLevel-1 are unlocked (level 1 = search bar + All). */
    private boolean tabUnlocked(int cat) {
        return cat >= 0 && cat < goldLevel();
    }

    private int topOffset() {
        return gold() ? 0 : HEADER_CROP;
    }

    private boolean matches(ItemStack stack, String query) {
        if (query.startsWith("@")) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(query.substring(1));
        }

        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void refilter() {
        StorageSnapshot view = menu.view;
        boolean gold = view.hasSearch();
        String query = gold && searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        int cat = gold ? selectedCat : 0;
        List<StorageSnapshot.CellData> cells = view.cells();

        java.util.Arrays.fill(haveCache, 0);
        long[] keys = new long[cells.size()];
        int n = 0;

        for (int i = 0; i < cells.size(); i++) {
            StorageSnapshot.CellData data = cells.get(i);
            ItemStack stack = data.stack();

            for (int f = 0; f < COST_ITEMS.length; f++) {
                if (stack.is(COST_ITEMS[f])) {
                    haveCache[f] += data.count();
                    break;
                }
            }

            if (cat != 0 && category(stack) != cat) {
                continue;
            }

            if (!query.isEmpty() && !matches(stack, query)) {
                continue;
            }

            // registry order like the creative inventory; index in the low bits keeps equal items stable
            keys[n++] = ((long) BuiltInRegistries.ITEM.getId(stack.getItem()) << 32) | i;
        }

        java.util.Arrays.sort(keys, 0, n);
        int[] out = new int[n];

        for (int i = 0; i < n; i++) {
            out[i] = (int) (keys[i] & 0xFFFFFFFFL);
        }

        visible = out;
        cachedView = view;
        dirty = false;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int totalRows() {
        return (visible.length + COLS - 1) / COLS;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - ROWS);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Display slot 0..53, -1 outside the grid, -2 on a locked row. */
    private int displayAt(double mx, double my) {
        double lx = mx - leftPos - GRID_X;
        double ly = my - topPos - GRID_Y;

        if (lx < 0 || ly < 0 || lx >= COLS * CELL || ly >= ROWS * CELL) {
            return -1;
        }

        int d = (int) (ly / CELL) * COLS + (int) (lx / CELL);
        return d >= unlockedCells() ? -2 : d;
    }

    private int realIndex(int display) {
        int v = scroll * COLS + display;
        return (v >= 0 && v < visible.length) ? visible[v] : -1;
    }

    private boolean scrollbarVisible() {
        return has(StorageSnapshot.UP_DIAMOND) && maxScroll() > 0;
    }

    private int trackX() {
        return leftPos + TRACK_X;
    }

    private int trackY() {
        return topPos + GRID_Y;
    }

    private void scrollFromMouse(double my) {
        int max = maxScroll();

        if (max <= 0) {
            return;
        }

        double range = TRACK_H - SCROLLER_H;
        double frac = (my - trackY() - SCROLLER_H / 2.0) / range;
        scroll = (int) Math.round(Math.max(0, Math.min(1, frac)) * max);
    }

    /** Switching to a different tab clears the search. */
    private void selectTab(int cat) {
        if (cat != selectedCat) {
            selectedCat = cat;

            if (searchBox != null) {
                searchBox.setValue("");
            }
        }

        scroll = 0;
        dirty = true;
    }

    private int tabX(boolean right) {
        return leftPos + (right ? TAB_RIGHT_X : TAB_LEFT_X);
    }

    private int tabY(int row) {
        return topPos + TAB_Y0 + row * TAB_H;
    }

    private List<Integer> offered() {
        StorageSnapshot v = menu.view;
        List<Integer> out = new ArrayList<>();

        if (v.canUpgrade(StorageSnapshot.UP_CELL)) {
            out.add(StorageSnapshot.UP_CELL);
            return out;
        }

        for (int up : new int[] {StorageSnapshot.UP_IRON, StorageSnapshot.UP_GOLD, StorageSnapshot.UP_DIAMOND}) {
            if (v.canUpgrade(up)) {
                out.add(up);
            }
        }

        return out;
    }

    private List<Btn> upgradeButtons() {
        List<Btn> out = new ArrayList<>();
        int y0 = topPos + topOffset();
        List<Integer> ups = offered();

        for (int k = 0; k < ups.size(); k++) {
            int by = y0 + 6 + k * BLOCK_H;
            out.add(new Btn(ups.get(k), leftPos + BTN_X, by + 20, leftPos + BTN_X + (BTN_W - 16) / 2, by));
        }

        return out;
    }

    private long have(Item item) {
        for (int i = 0; i < COST_ITEMS.length; i++) {
            if (COST_ITEMS[i] == item) {
                return haveCache[i];
            }
        }

        return 0;
    }

    private String effect(int up) {
        StorageSnapshot v = menu.view;

        return switch (up) {
            case StorageSnapshot.UP_CELL -> "Unlock 1 cell (" + v.unlockedCells() + "/" + StorageSnapshot.MAX_CELLS + ")";
            case StorageSnapshot.UP_IRON -> "Unlimited items per cell";
            case StorageSnapshot.UP_GOLD -> v.goldLevel() == 0
                ? "Search bar and All tab"
                : "Unlock tab: " + CAT_NAMES[v.goldLevel()] + " (" + v.goldLevel() + "/" + StorageSnapshot.GOLD_MAX + ")";
            default -> "Unlimited cells and scrolling";
        };
    }

    private static String format(long n) {
        if (n < 100_000) {
            return Long.toString(n);
        }

        String[] suffix = {"K", "M", "B", "T", "Q"};
        double v = n;
        int i = -1;

        while (v >= 1000 && i < suffix.length - 1) {
            v /= 1000;
            i++;
        }

        return (v >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v)) + suffix[i];
    }

    private static void tex(GuiGraphics g, int x, int y, float u, float v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEX, x, y, u, v, w, h, 256, 256);
    }

    /** Builds an arbitrary-size panel from the corners/edges of the vanilla chest texture. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        for (int yy = 4; yy < h - 4; yy += 13) {
            int th = Math.min(13, h - 4 - yy);

            for (int xx = 4; xx < w - 4; xx += 160) {
                tex(g, x + xx, y + yy, 8, 4, Math.min(160, w - 4 - xx), th);
            }

            tex(g, x, y + yy, 0, 4, 4, th);
            tex(g, x + w - 4, y + yy, 172, 4, 4, th);
        }

        for (int xx = 4; xx < w - 4; xx += 160) {
            int tw = Math.min(160, w - 4 - xx);
            tex(g, x + xx, y, 4, 0, tw, 4);
            tex(g, x + xx, y + h - 4, 4, 218, tw, 4);
        }

        tex(g, x, y, 0, 0, 4, 4);
        tex(g, x + w - 4, y, 172, 0, 4, 4);
        tex(g, x, y + h - 4, 0, 218, 4, 4);
        tex(g, x + w - 4, y + h - 4, 172, 218, 4, 4);
    }

    private void drawTab(GuiGraphics g, boolean right, int row, int cat, boolean selected) {
        int x = tabX(right) - (selected ? 2 : 0);
        int y = tabY(row);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? TAB_SELECTED_SPRITE : TAB_SPRITE, x, y, TAB_W + 3, TAB_H);
        g.renderItem(CAT_ICONS[cat], x + 8, y + 5);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (dirty || cachedView != menu.view) {
            refilter();
        }

        int x0 = leftPos;
        int y0 = topPos;
        boolean gold = gold();
        List<Btn> buttons = upgradeButtons();

        // tabs (left column first so the attached column overlaps it)
        if (gold) {
            for (int r = 1; r < TAB_ROWS; r++) {
                if (tabUnlocked(LEFT_COL[r]) && LEFT_COL[r] != selectedCat) {
                    drawTab(g, false, r, LEFT_COL[r], false);
                }
            }

            for (int r = 0; r < TAB_ROWS; r++) {
                if (tabUnlocked(RIGHT_COL[r]) && RIGHT_COL[r] != selectedCat) {
                    drawTab(g, true, r, RIGHT_COL[r], false);
                }
            }

            for (int r = 0; r < TAB_ROWS; r++) {
                if (tabUnlocked(RIGHT_COL[r]) && RIGHT_COL[r] == selectedCat) {
                    drawTab(g, true, r, RIGHT_COL[r], true);
                }
            }

            for (int r = 1; r < TAB_ROWS; r++) {
                if (tabUnlocked(LEFT_COL[r]) && LEFT_COL[r] == selectedCat) {
                    drawTab(g, false, r, LEFT_COL[r], true);
                }
            }
        }

        // side panels (drawn before the window so the window edge overlaps them)
        if (!buttons.isEmpty()) {
            panel(g, x0 + PANEL_X, y0 + topOffset(), PANEL_W, buttons.size() * BLOCK_H + 6);
        }

        if (scrollbarVisible()) {
            panel(g, x0 + PANEL_X, y0 + 14, 26, TRACK_H + 8);
        }

        // window
        if (gold) {
            tex(g, x0, y0, 0, 0, W, 125);
        } else {
            tex(g, x0, y0 + HEADER_CROP, 0, 0, W, 8);
            tex(g, x0, y0 + 17, 0, 17, W, 108);
        }

        tex(g, x0, y0 + 125, 0, 126, W, 96);

        // locked cells
        int unlocked = Math.min(unlockedCells(), ROWS * COLS);

        for (int d = unlocked; d < ROWS * COLS; d++) {
            int cx = x0 + GRID_X + (d % COLS) * CELL;
            int cy = y0 + GRID_Y + (d / COLS) * CELL;
            g.fill(cx, cy, cx + 16, cy + 16, 0xA0000000);
        }

        // hover highlight
        int hovered = displayAt(mouseX, mouseY);

        if (hovered >= 0 && realIndex(hovered) >= 0) {
            int cx = x0 + GRID_X + (hovered % COLS) * CELL;
            int cy = y0 + GRID_Y + (hovered / COLS) * CELL;
            g.fill(cx, cy, cx + 16, cy + 16, 0x80FFFFFF);
        }

        // cells
        for (int d = 0; d < unlocked; d++) {
            int real = realIndex(d);

            if (real < 0) {
                continue;
            }

            StorageSnapshot.CellData data = menu.view.cells().get(real);
            int ix = x0 + GRID_X + (d % COLS) * CELL;
            int iy = y0 + GRID_Y + (d / COLS) * CELL;
            g.renderItem(data.stack(), ix, iy);
            g.renderItemDecorations(this.font, data.stack(), ix, iy, "");

            if (data.count() > 1) {
                String text = format(data.count());
                int tw = this.font.width(text);
                float s = tw > 17 ? 17f / tw : 1f;
                g.pose().pushMatrix();
                g.pose().translate(ix + 17 - tw * s, iy + 17 - 8 * s);
                g.pose().scale(s, s);
                g.drawString(this.font, text, 0, 0, 0xFFFFFFFF, true);
                g.pose().popMatrix();
            }
        }

        // scrollbar thumb
        if (scrollbarVisible()) {
            int thumbY = trackY() + (TRACK_H - SCROLLER_H) * scroll / Math.max(1, maxScroll());
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, trackX(), thumbY, SCROLLER_W, SCROLLER_H);
        }

        // upgrade buttons
        for (Btn b : buttons) {
            Item cost = menu.view.costItem(b.up());
            int need = menu.view.costAmount(b.up());
            ItemStack costStack = new ItemStack(cost);
            g.renderItem(costStack, b.iconX(), b.iconY());
            g.renderItemDecorations(this.font, costStack, b.iconX(), b.iconY(), Integer.toString(need));

            boolean affordable = have(cost) >= need;
            boolean hover = in(mouseX, mouseY, b.x(), b.y(), BTN_W, BTN_H);
            Identifier sprite = !affordable ? BUTTON_OFF_SPRITE : (hover ? BUTTON_HOVER_SPRITE : BUTTON_SPRITE);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, b.x(), b.y(), BTN_W, BTN_H);

            String label = b.up() == StorageSnapshot.UP_CELL ? "Unlock" : "Upgrade";
            g.drawString(this.font, label, b.x() + (BTN_W - this.font.width(label)) / 2, b.y() + 6, affordable ? 0xFFFFFFFF : 0xFFA0A0A0, true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        int hovered = displayAt(mouseX, mouseY);

        if (hovered >= 0) {
            int real = realIndex(hovered);

            if (real >= 0) {
                StorageSnapshot.CellData data = menu.view.cells().get(real);
                List<Component> lines = new ArrayList<>(data.stack().getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL));
                lines.add(Component.literal("Stored: " + String.format(Locale.ROOT, "%,d", data.count())).withStyle(ChatFormatting.GRAY));
                g.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
        }

        if (gold()) {
            for (int r = 0; r < TAB_ROWS; r++) {
                if (tabUnlocked(RIGHT_COL[r]) && in(mouseX, mouseY, tabX(true), tabY(r), TAB_W, TAB_H)) {
                    g.setTooltipForNextFrame(this.font, Component.literal(CAT_NAMES[RIGHT_COL[r]]), mouseX, mouseY);
                }

                if (r > 0 && tabUnlocked(LEFT_COL[r]) && in(mouseX, mouseY, tabX(false), tabY(r), TAB_W, TAB_H)) {
                    g.setTooltipForNextFrame(this.font, Component.literal(CAT_NAMES[LEFT_COL[r]]), mouseX, mouseY);
                }
            }
        }

        for (Btn b : upgradeButtons()) {
            if (in(mouseX, mouseY, b.x(), b.y(), BTN_W, BTN_H)) {
                Item cost = menu.view.costItem(b.up());
                long have = have(cost);
                int need = menu.view.costAmount(b.up());
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(effect(b.up())).withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal("Consumes " + need + " ").append(new ItemStack(cost).getHoverName()).append(Component.literal(" from the vault")));
                lines.add(Component.literal("In vault: " + have).withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
                g.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void sendCell(int realIndex, int button) {
        ItemStack proto = realIndex >= 0 ? menu.view.cells().get(realIndex).stack() : ItemStack.EMPTY;
        int flags = (button == 1 ? 1 : 0) | (Minecraft.getInstance().hasShiftDown() ? 2 : 0);
        ClientPlayNetworking.send(new ActionPayload(menu.containerId, ActionPayload.CELL, realIndex, flags, proto));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (button == 0) {
            if (gold()) {
                for (int r = 0; r < TAB_ROWS; r++) {
                    if (tabUnlocked(RIGHT_COL[r]) && in(mx, my, tabX(true), tabY(r), TAB_W, TAB_H)) {
                        selectTab(RIGHT_COL[r]);
                        return true;
                    }

                    if (r > 0 && tabUnlocked(LEFT_COL[r]) && in(mx, my, tabX(false), tabY(r), TAB_W, TAB_H)) {
                        selectTab(LEFT_COL[r]);
                        return true;
                    }
                }
            }

            for (Btn b : upgradeButtons()) {
                if (in(mx, my, b.x(), b.y(), BTN_W, BTN_H)) {
                    if (have(menu.view.costItem(b.up())) >= menu.view.costAmount(b.up())) {
                        ClientPlayNetworking.send(new ActionPayload(menu.containerId, ActionPayload.UPGRADE, b.up(), 0, ItemStack.EMPTY));
                    }

                    return true;
                }
            }

            if (scrollbarVisible() && in(mx, my, trackX(), trackY(), SCROLLER_W, TRACK_H)) {
                draggingScrollbar = true;
                scrollFromMouse(my);
                return true;
            }
        }

        int d = displayAt(mx, my);

        if (d == -2) {
            return true;
        }

        if (d >= 0) {
            int real = realIndex(d);

            if (real >= 0 || !menu.getCarried().isEmpty()) {
                sendCell(real, button);
            }

            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            draggingScrollbar = false;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            scrollFromMouse(event.y());
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (maxScroll() > 0 && (displayAt(mx, my) != -1 || in(mx, my, trackX(), trackY(), SCROLLER_W, TRACK_H))) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
            return true;
        }

        return super.mouseScrolled(mx, my, dx, dy);
    }

    private boolean searchActive() {
        return searchBox != null && searchBox.visible;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW_KEY_ESCAPE) {
            return super.keyPressed(event);
        }

        if (searchActive()) {
            if (!searchBox.isFocused()) {
                this.setFocused(searchBox);
            }

            if (searchBox.keyPressed(event)) {
                return true;
            }

            if (searchBox.canConsumeInput()) {
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchActive()) {
            if (!searchBox.isFocused()) {
                this.setFocused(searchBox);
            }

            if (searchBox.charTyped(event)) {
                return true;
            }
        }

        return super.charTyped(event);
    }
}
