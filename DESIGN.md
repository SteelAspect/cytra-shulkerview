# Alloy Shulker View — own settings screen (1.3.0)

Goal: meet the workspace standard (settings in the mod's own Nocturne screen *and* the Alloy Hub page, from the same
config), without changing what the mod draws.

## Decisions (picked by the mod chat, user said "just do")

- **Alloy UI 1.3.0, jar-in-jar.** `AlloyScreen` + `Options`. Client code only (the `client` package; this mod has
  no split source sets, the server never loads those classes).
- **One option list**, `ShulkerViewOptions`, over `ShulkerViewConfig` (same 9 keys, same ranges as the config clamp and
  the Hub page). Every change saves the config at once.
- Sections: **Shulker items** (icon, fill bar, icon size), **Placed shulkers** (labels, distance, icon size, named
  textures), **Fill bar colours** (filling, full). Hints from lang `.hint` keys.
- **Opening it:** Mod Menu's config button (optional, compile-only), and an **unbound** key "Open settings" under
  Controls → Alloy Shulker View (the user chose no default hotkey).
- **Lang file** `assets/cytra-shulkerview/lang/en_us.json` for the screen, options, key and category.
- The Hub page stays in cytra-hub (it already reads the same `ShulkerViewConfig` fields). Moving it onto
  `ShulkerViewOptions` is a later Hub change, not part of this.
- Test: `SettingsScreenClientGameTest` opens the screen, flips a toggle through its row, checks the config, takes a
  screenshot.

# 1.20.4 port + ShulkerBoxTooltip-style preview (1.5.0+1.20.4)

User asked (2026-10-24): port to 1.20.4 and make the preview as close as possible to Shulker Box Tooltip (MIT), with
our own code and look. Branch `mc1.20.4` only; the 1.21.11 mod is unchanged.

## Decisions

- **Contents from NBT.** 1.20.4 items keep contents in NBT: `BlockEntityTag.Items` (with `Slot`) for shulkers, chests,
  barrels, hoppers, furnaces, dispensers and modded blocks; the bundle's own `Items` list. `client/ContainerContents`
  reads them once per NBT list (Guava weak-key cache by identity) for the overlay and the preview. Grid size = the
  block entity's real `getContainerSize()` (a throwaway block entity per item, cached); else from the highest slot.
  9 slots (dispenser/dropper) draw 3x3, fewer than 9 in one row.
- **Two views.** Compact = each item type once with its total, most first; full = every slot where it is.
- **Keys.** Alloy style (default): compact always on hover, the preview key (Left Shift) shows full. ShulkerBoxTooltip
  style: hint line on hover, preview key = compact, full key (Left Alt) + preview key = full. "First view" swaps
  compact/full in both. The keys are config strings with a picker in the settings screen, **not** vanilla key
  mappings: on 1.20.4 `KeyMapping.MAP` holds one mapping per key, so a second Left Shift mapping would steal Sneak.
  Either Shift/Alt/Ctrl counts when the left one is chosen.
- **Colour.** Shulker previews take the dye colour (plain = purple), the ender chest teal; others use Nocturne.
  `tooltipColored` turns colours off.
- **Vanilla lines.** The shulker's "Item x N / and N more" lines are cancelled while a preview shows
  (`ShulkerBoxBlock.appendHoverText`). The tooltip image is hooked on `ItemStack.getTooltipImage`, so the bundle's own
  image is replaced too.
- **Loot table line** (`tooltipLootTable`, off): "Loot table: <id>" for unopened loot containers.
- **Ender chest.** Optional server side (`ShulkerViewServer`, main entrypoint): every 5 ticks it sends a player their
  ender chest when it changed (`cytra-shulkerview:ender_chest`, only to clients that registered the channel). Without
  the server side the client remembers the ender chest from its screen when closed.
- **Overlay** (icon + fill bar) on every container item with items; bundles keep vanilla's bar (icon only), ender chest
  items get none (the contents aren't the item's own).
- Off when ShulkerBoxTooltip itself is installed (as before).
- **Config** (`config/cytra-shulkerview.json`): new `tooltipSbtKeys`, `tooltipFullByDefault`, `previewKey`,
  `fullPreviewKey`, `tooltipColored`, `tooltipLootTable`, `otherContainers`, `bundles`, `enderChestPreview`;
  dropped `tooltipShiftOnly`, `tooltipCompact`. Hub page: update when Hub turns its 1.20.4 integration pages back on.
- **Tests:** `./headless.sh ./smoketest.sh` (two dedicated servers, see the script).
