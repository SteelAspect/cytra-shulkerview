package dev.steelaspect.shulkerview.smoke;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.steelaspect.shulkerview.ShulkerSummary;
import dev.steelaspect.shulkerview.ShulkerSummaryHolder;
import dev.steelaspect.shulkerview.client.ContainerContents;
import dev.steelaspect.shulkerview.client.EnderChestContents;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import dev.steelaspect.shulkerview.client.preview.ShulkerPreviewData;
import dev.steelaspect.shulkerview.client.screen.SettingsScreen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.20.4 stand-in for the client game tests (fabric-client-gametest-api-v1 starts at 1.21.4). Run with
 * {@code ./headless.sh ./smoketest.sh}: joins a dedicated 1.20.4 server running the built jar as op and checks the
 * container contents reader (coloured shulker, chest, hopper, dispenser, loot chest, bundle), the compact and full
 * previews under both key styles, the hidden vanilla lines and hints, shulker colours, the loot table line, the ender
 * chest preview the server syncs, placed-shulker summaries and the settings screen; then joins a server without the mod
 * and checks everything client-only still works (ender chest read from its screen). Logs {@code [ShulkerSmoke] PASS}
 * or {@code FAIL: ...}, saves screenshots to build/run/smoke/screenshots and quits.
 */
public class ShulkerSmokeTest implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("ShulkerSmoke");
    private static final int TIMEOUT_TICKS = 20 * 90;
    private static final String WITH_MOD = "localhost:25596", PLAIN = "localhost:25597";

    private final List<String> failures = new ArrayList<>();
    private final ArrayDeque<String> commands = new ArrayDeque<>();
    private int commandWait;
    private int step;
    private int wait = 40;
    private BooleanSupplier until;
    private int untilTicks;
    private String untilWhat;
    private boolean done;
    private BlockPos placed = BlockPos.ZERO;

    // Test stacks, built from NBT like /give or Ctrl+pick block would make them.
    private ItemStack redShulker, plainShulker, chest, hopper, dispenser, lootChest, bundle, enderChest;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("shulkerview.smoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (done) return;
        if (!commands.isEmpty()) {
            if (--commandWait <= 0 && mc.player != null) {
                mc.player.connection.sendCommand(commands.poll());
                commandWait = 10;
            }
            return;
        }
        if (until != null) {
            if (until.getAsBoolean()) {
                until = null;
            } else if (++untilTicks > TIMEOUT_TICKS) {
                fail("timed out waiting for " + untilWhat);
                finish(mc);
                return;
            } else {
                return;
            }
        }
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            run(mc);
        } catch (Throwable t) {
            LOG.error("[ShulkerSmoke] step {} threw", step - 1, t);
            fail("step " + (step - 1) + " threw " + t);
            finish(mc);
        }
    }

    private void run(Minecraft mc) {
        switch (step++) {
            case 0 -> waitFor("title screen", () -> mc.screen instanceof TitleScreen);
            case 1 -> {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                makeStacks();
                checkContents();
                join(mc, WITH_MOD);
            }
            case 2 -> {
                command("gamemode creative");
                command("item replace entity @s enderchest.0 with minecraft:diamond 5");
                command("item replace entity @s enderchest.13 with minecraft:ender_pearl 16");
                placed = mc.player.blockPosition().offset(2, 0, 0);
                command("setblock " + placed.getX() + " " + placed.getY() + " " + placed.getZ()
                        + " minecraft:red_shulker_box{Items:[{Slot:0b,id:\"minecraft:stone\",Count:64b}]}");
                waitFor("the ender chest from the server", () -> EnderChestContents.fromServer()
                        && EnderChestContents.get() != null && EnderChestContents.get().slots().get(0).is(Items.DIAMOND));
            }
            case 3 -> {
                ContainerContents.Contents ender = ContainerContents.of(enderChest);
                check(ender != null && ender.kind() == ContainerContents.Kind.ENDER_CHEST && ender.slots().size() == 27
                        && ender.slots().get(13).is(Items.ENDER_PEARL), "ender chest contents from the server: " + ender);
                waitFor("the placed shulker's summary from the server", () -> mc.level.getBlockEntity(placed) instanceof ShulkerSummaryHolder h
                        && h.cytra$getSummary() != null && !h.cytra$getSummary().empty());
            }
            case 4 -> {
                // Look at the placed shulker for a screenshot of its side labels.
                mc.player.setYRot(-90f);
                mc.player.setXRot(25f);
                mc.gui.getChat().clearMessages(false);
                wait = 20;
            }
            case 5 -> {
                shot(mc, "placed-labels");
                ShulkerSummary s = ((ShulkerSummaryHolder) mc.level.getBlockEntity(placed)).cytra$getSummary();
                check(s.icon().is(Items.STONE) && s.fill() > 0.03f && ShulkerSummary.serverSyncSeen, "placed shulker summary " + s);
                checkPreviews(mc);
                mc.setScreen(new Showcase());
                ShulkerPreview.testKeys = 0;
                wait = 5;
            }
            case 6 -> {
                shot(mc, "alloy-compact");
                ShulkerPreview.testKeys = 1;
                wait = 5;
            }
            case 7 -> {
                shot(mc, "alloy-full");
                ShulkerViewConfig.INSTANCE.tooltipSbtKeys = true;
                ShulkerPreview.testKeys = 0;
                wait = 5;
            }
            case 8 -> {
                shot(mc, "sbt-no-key");
                ShulkerPreview.testKeys = 1;
                wait = 5;
            }
            case 9 -> {
                shot(mc, "sbt-compact");
                ShulkerPreview.testKeys = 3;
                wait = 5;
            }
            case 10 -> {
                shot(mc, "sbt-full");
                ShulkerViewConfig.INSTANCE.tooltipSbtKeys = false;
                ShulkerPreview.testKeys = 0;
                mc.setScreen(new SettingsScreen(null));
                wait = 10;
            }
            case 11 -> {
                shot(mc, "settings");
                check(((SettingsScreen) mc.screen).rowForTest("tooltipKeyStyle") != null, "settings row for the key style");
                check(((SettingsScreen) mc.screen).rowForTest("enderChestPreview") != null, "settings row for the ender chest preview");
                mc.setScreen(null);
                mc.level.disconnect();
                mc.disconnect();
                join(mc, PLAIN);
            }
            case 12 -> {
                check(EnderChestContents.get() == null && !ShulkerSummary.serverSyncSeen, "state cleared after leaving the modded server");
                command("gamemode creative");
                command("item replace entity @s enderchest.2 with minecraft:emerald 7");
                placed = mc.player.blockPosition().offset(2, 0, 0);
                command("setblock " + placed.getX() + " " + placed.getY() + " " + placed.getZ() + " minecraft:ender_chest");
                waitFor("the ender chest block", () -> mc.level.getBlockState(placed).is(net.minecraft.world.level.block.Blocks.ENDER_CHEST));
            }
            case 13 -> {
                mc.player.getInventory().selected = 8;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(placed), Direction.UP, placed, false));
                waitFor("the ender chest screen", () -> mc.screen instanceof ContainerScreen
                        && ((ContainerScreen) mc.screen).getMenu().getSlot(2).getItem().is(Items.EMERALD));
            }
            case 14 -> {
                mc.player.closeContainer();
                wait = 5;
            }
            case 15 -> {
                ContainerContents.Contents ender = ContainerContents.of(enderChest);
                check(!EnderChestContents.fromServer() && ender != null && ender.slots().get(2).is(Items.EMERALD),
                        "ender chest read from its screen on a server without the mod: " + ender);
                // Everything item-based keeps working client-only.
                checkPreviews(mc);
                mc.setScreen(new Showcase());
                ShulkerPreview.testKeys = 0;
                wait = 5;
            }
            case 16 -> {
                shot(mc, "plain-server-compact");
                ShulkerPreview.testKeys = -1;
                mc.setScreen(null);
                finish(mc);
            }
            default -> finish(mc);
        }
    }

    private void makeStacks() {
        redShulker = stack("{id:\"minecraft:red_shulker_box\",Count:1b,tag:{BlockEntityTag:{Items:["
                + "{Slot:0b,id:\"minecraft:stone\",Count:64b},{Slot:5b,id:\"minecraft:dirt\",Count:10b},"
                + "{Slot:6b,id:\"minecraft:stone\",Count:3b},{Slot:26b,id:\"minecraft:diamond_pickaxe\",Count:1b,tag:{Damage:500}}]}}}");
        plainShulker = stack("{id:\"minecraft:shulker_box\",Count:1b,tag:{BlockEntityTag:{Items:["
                + "{Slot:3b,id:\"minecraft:oak_log\",Count:64b},{Slot:4b,id:\"minecraft:oak_log\",Count:64b}]}}}");
        chest = stack("{id:\"minecraft:chest\",Count:1b,tag:{BlockEntityTag:{Items:["
                + "{Slot:0b,id:\"minecraft:iron_ingot\",Count:20b},{Slot:26b,id:\"minecraft:gold_ingot\",Count:5b}]}}}");
        hopper = stack("{id:\"minecraft:hopper\",Count:1b,tag:{BlockEntityTag:{Items:[{Slot:4b,id:\"minecraft:coal\",Count:9b}]}}}");
        dispenser = stack("{id:\"minecraft:dispenser\",Count:1b,tag:{BlockEntityTag:{Items:[{Slot:4b,id:\"minecraft:arrow\",Count:64b}]}}}");
        lootChest = stack("{id:\"minecraft:chest\",Count:1b,tag:{BlockEntityTag:{LootTable:\"minecraft:chests/simple_dungeon\"}}}");
        bundle = stack("{id:\"minecraft:bundle\",Count:1b,tag:{Items:[{id:\"minecraft:apple\",Count:3b},"
                + "{id:\"minecraft:arrow\",Count:10b},{id:\"minecraft:apple\",Count:2b}]}}");
        enderChest = new ItemStack(Items.ENDER_CHEST);
    }

    private static ItemStack stack(String snbt) {
        try {
            return ItemStack.of(TagParser.parseTag(snbt));
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(snbt, e);
        }
    }

    /** What the NBT reader makes of each kind of item. */
    private void checkContents() {
        ContainerContents.Contents red = ContainerContents.of(redShulker);
        check(red != null && red.kind() == ContainerContents.Kind.SHULKER && red.color() == DyeColor.RED && red.slots().size() == 27
                && red.slots().get(26).is(Items.DIAMOND_PICKAXE) && red.columns() == 9, "red shulker contents " + red);
        check(red != null && !red.summary().hasIcon(), "mixed shulker has no single icon");
        ContainerContents.Contents plain = ContainerContents.of(plainShulker);
        check(plain != null && plain.color() == null && plain.summary().icon().is(Items.OAK_LOG), "plain shulker contents " + plain);
        check(ContainerContents.of(plainShulker) == plain, "contents are cached per stack");
        ContainerContents.Contents c = ContainerContents.of(chest);
        check(c != null && c.kind() == ContainerContents.Kind.CONTAINER && c.slots().size() == 27 && c.slots().get(26).is(Items.GOLD_INGOT),
                "chest contents " + c);
        ContainerContents.Contents h = ContainerContents.of(hopper);
        check(h != null && h.slots().size() == 5 && h.columns() == 5 && h.slots().get(4).is(Items.COAL), "hopper contents " + h);
        ContainerContents.Contents d = ContainerContents.of(dispenser);
        check(d != null && d.slots().size() == 9 && d.columns() == 3, "dispenser contents " + d);
        ContainerContents.Contents loot = ContainerContents.of(lootChest);
        check(loot != null && !loot.hasItems() && "minecraft:chests/simple_dungeon".equals(loot.lootTable()), "loot chest " + loot);
        ContainerContents.Contents b = ContainerContents.of(bundle);
        check(b != null && b.kind() == ContainerContents.Kind.BUNDLE && b.slots().size() == 3 && b.summary().fill() > 0.2f,
                "bundle contents " + b);
        check(ContainerContents.of(new ItemStack(Items.CHEST)) == null && ContainerContents.of(new ItemStack(Items.STONE)) == null,
                "plain items have no contents");
        ShulkerViewConfig.INSTANCE.otherContainers = false;
        check(ContainerContents.of(chest) == null && ContainerContents.of(redShulker) != null, "other containers can be turned off");
        ShulkerViewConfig.INSTANCE.otherContainers = true;
    }

    /** Views, keys, hidden vanilla lines, hints, colours and the loot table line. */
    private void checkPreviews(Minecraft mc) {
        ShulkerViewConfig cfg = ShulkerViewConfig.INSTANCE;
        cfg.tooltipSbtKeys = false;
        cfg.tooltipFullByDefault = false;
        // Alloy keys: compact on hover, Shift = full.
        ShulkerPreview.testKeys = 0;
        ShulkerPreviewData compact = data(redShulker);
        check(compact != null && compact.compact() && compact.size() == 3, "Alloy keys, no key: compact view of 3 item types");
        check(!hasItemCountLines(mc, redShulker), "vanilla 'Item x N' lines hidden under the preview");
        check(!hasText(mc, redShulker, ": preview"), "no hint line with Alloy keys");
        ShulkerPreview.testKeys = 1;
        ShulkerPreviewData full = data(redShulker);
        check(full != null && !full.compact() && full.size() == 27, "Alloy keys, Shift: full view of 27 slots");
        cfg.tooltipFullByDefault = true;
        ShulkerPreview.testKeys = 0;
        check(data(redShulker) != null && !data(redShulker).compact(), "full by default: full view without a key");
        cfg.tooltipFullByDefault = false;

        // ShulkerBoxTooltip keys: hint only, Shift = compact, Alt+Shift = full.
        cfg.tooltipSbtKeys = true;
        ShulkerPreview.testKeys = 0;
        check(data(redShulker) == null, "SBT keys, no key: no preview");
        check(hasItemCountLines(mc, redShulker) && hasText(mc, redShulker, ": preview"), "SBT keys, no key: vanilla lines + hint");
        ShulkerPreview.testKeys = 1;
        check(data(redShulker) != null && data(redShulker).compact() && !hasItemCountLines(mc, redShulker)
                && hasText(mc, redShulker, ": all slots"), "SBT keys, Shift: compact, vanilla lines hidden, Alt hint");
        ShulkerPreview.testKeys = 3;
        check(data(redShulker) != null && !data(redShulker).compact(), "SBT keys, Alt+Shift: full view");
        cfg.tooltipSbtKeys = false;

        // Colours: the red shulker's window is red, a chest's plain Nocturne; off = no colour.
        ShulkerPreview.testKeys = 0;
        Integer tint = data(redShulker).tint();
        check(tint != null && ((tint >> 16) & 0xFF) > 2 * (tint & 0xFF), "red shulker window is red: " + Integer.toHexString(tint == null ? 0 : tint));
        check(data(chest) != null && data(chest).tint() == null, "chest window uses Nocturne colours");
        cfg.tooltipColored = false;
        check(data(redShulker).tint() == null, "shulker colours can be turned off");
        cfg.tooltipColored = true;

        // Other containers, bundles, ender chest.
        ShulkerPreview.testKeys = 1;
        check(data(chest) != null && data(chest).size() == 27, "chest full preview");
        check(data(hopper) != null && data(hopper).size() == 5 && data(hopper).columns() == 5, "hopper preview: 5 slots in a row");
        check(data(bundle) != null, "bundle preview replaces vanilla's bundle image");
        ShulkerPreview.testKeys = 0;
        check(data(bundle) != null && data(bundle).compact() && data(bundle).size() == 2, "bundle compact: apples merged");
        check(data(enderChest) != null, "ender chest preview");
        check(data(lootChest) == null, "no preview for an unopened loot chest");

        // Loot table line, off by default.
        check(!hasText(mc, lootChest, "Loot table"), "no loot table line by default");
        cfg.tooltipLootTable = true;
        check(hasText(mc, lootChest, "minecraft:chests/simple_dungeon"), "loot table line when on");
        cfg.tooltipLootTable = false;
    }

    private static ShulkerPreviewData data(ItemStack stack) {
        Optional<TooltipComponent> image = stack.getTooltipImage();
        return image.isPresent() && image.get() instanceof ShulkerPreviewData d ? d : null;
    }

    private static boolean hasItemCountLines(Minecraft mc, ItemStack stack) {
        for (Component line : Screen.getTooltipFromItem(mc, stack)) {
            if (line.getContents() instanceof TranslatableContents t && t.getKey().equals("container.shulkerBox.itemCount")) return true;
        }
        return false;
    }

    private static boolean hasText(Minecraft mc, ItemStack stack, String text) {
        for (Component line : Screen.getTooltipFromItem(mc, stack)) {
            if (line.getString().contains(text)) return true;
        }
        return false;
    }

    /** Tooltips of every test item side by side, and a row of the items with their overlays. */
    private final class Showcase extends Screen {
        Showcase() {
            super(Component.literal("Shulker View smoke"));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderBackground(g, mouseX, mouseY, partialTick);
            ItemStack[] row = {redShulker, plainShulker, chest, hopper, dispenser, bundle, enderChest, lootChest};
            for (int i = 0; i < row.length; i++) {
                g.renderItem(row[i], 10 + i * 20, 6);
                g.renderItemDecorations(font, row[i], 10 + i * 20, 6);
            }
            tooltip(g, redShulker, 4, 40);
            tooltip(g, plainShulker, 4, 190);
            tooltip(g, chest, 200, 40);
            tooltip(g, bundle, 200, 190);
            tooltip(g, hopper, 420, 40);
            tooltip(g, enderChest, 420, 150);
        }

        private void tooltip(GuiGraphics g, ItemStack stack, int x, int y) {
            g.renderTooltip(font, Screen.getTooltipFromItem(minecraft, stack), stack.getTooltipImage(), x - 12, y + 12);
        }
    }

    private void join(Minecraft mc, String address) {
        info("joining " + address);
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                new ServerData("smoke", address, ServerData.Type.OTHER), false);
        waitFor("joining " + address, () -> mc.player != null && mc.level != null && mc.screen == null);
    }

    private void command(String command) {
        commands.add(command);
    }

    private void waitFor(String what, BooleanSupplier condition) {
        until = condition;
        untilTicks = 0;
        untilWhat = what;
    }

    private void check(boolean ok, String message) {
        if (!ok) fail(message);
    }

    private static void info(String s) {
        LOG.info("[ShulkerSmoke] {}", s);
    }

    private void fail(String message) {
        LOG.error("[ShulkerSmoke] check failed: {}", message);
        failures.add(message);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), t -> LOG.info("[ShulkerSmoke] {}", t.getString()));
    }

    private void finish(Minecraft mc) {
        done = true;
        ShulkerPreview.testKeys = -1;
        if (failures.isEmpty()) {
            LOG.info("[ShulkerSmoke] PASS");
        } else {
            LOG.error("[ShulkerSmoke] FAIL: {}", failures);
        }
        if (mc.level != null) mc.level.disconnect();
        mc.disconnect();
        mc.stop();
    }
}
