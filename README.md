# Alloy Shulker View

Fabric mod for Minecraft 1.21.11 that shows what's inside shulker boxes. Purely visual: shulkers store,
move and behave exactly like vanilla.

## What it shows

- **Shulker items** (inventory, hotbar, chests): a small icon of the item inside when every stack is the same
  item (diamonds, spruce logs...), and a fill bar along the bottom. Light blue while filling, green when 100% full (colours can be changed).
- **Placed shulkers**: the same icon and fill bar as a label on the four side faces and the lid (the lid label hides
  while the box is open; can be turned off),
  like an item frame. Faces turned away from you and shulkers further than 24 blocks are skipped.

Mixed shulkers get only the fill bar by default, or (setting) their main item or the top four as a 2x2; empty
shulkers get nothing.

- **Tooltip preview**: hovering a shulker item (inventory, chests, creative) shows all 27 slots as a grid with counts,
  durability bars and glint, tinted to the shulker's colour, instead of vanilla's text list. Options: only while
  holding Shift, and compact (same items merged with their total). Turned off when ShulkerBoxTooltip is installed.

- **Named shulkers** (with Alloy Pack on): placed shulkers named `Trash`, `Tools`, `Food` or `Blocks` (any case) use
  the same texture Alloy Pack gives the item. Without the pack they look vanilla. With Optimized Block Entities these
  boxes are drawn as normal block entities so the texture can show.

Placed labels are drawn on their own, so they also work with mods that render closed shulkers themselves
(e.g. Optimized Block Entities). Test with one: `./gradlew runClientGameTest -PwithObe=<path to its jar>`.

## Server or client?

One jar, install it on both for the full effect.

- **Server + client**: the server adds a tiny summary (icon item + fill) to each shulker's block entity data, so every
  placed shulker shows its label and updates live (players, hoppers, droppers). Vanilla clients ignore the extra data.
- **Client only** (server without the mod): shulker items work fully. Placed shulkers show their label after you open
  them once (remembered until you leave the server). Same for named-shulker textures: the client learns the name
  from the shulker screen title when you open it.
- **Server only**: harmless, nothing to see.

## Config

`config/cytra-shulkerview.json`:

| Key | Default | |
|---|---|---|
| `itemIcon` | `true` | Icon on shulker items |
| `itemBar` | `true` | Fill bar on shulker items |
| `placedLabels` | `true` | Labels on placed shulkers |
| `mixedIcon` | `NONE` | Mixed shulkers: `NONE`, `MAIN` (most items) or `TOP4` (2x2) |
| `placedLabelsLid` | `true` | Label on the lid of placed shulkers too |
| `placedDistance` | `24` | Max label distance in blocks (1-128) |
| `barColor` | light blue | Fill bar colour while filling (ARGB int) |
| `fullBarColor` | green | Fill bar colour when 100% full (ARGB int) |
| `itemIconSize` | `0.625` | Icon size on items, fraction of the slot (0.25-1) |
| `placedIconSize` | `0.4` | Icon size on placed labels, fraction of the face (0.2-0.8) |
| `namedTextures` | `true` | Alloy Pack textures on placed shulkers named Trash / Tools / Food / Blocks |
| `tooltipPreview` | `true` | Slot-grid preview in shulker item tooltips |
| `tooltipShiftOnly` | `false` | Preview only while Shift is held |
| `tooltipCompact` | `false` | Preview merges same items into one slot with the total |

All of these are also in the mod's own settings screen (Mod Menu's config button, or bind "Open settings" under
Controls → Alloy Shulker View; no key by default) and on the Shulker View page in Alloy Hub. The screen uses Alloy UI
(bundled inside the jar), so it follows the theme picked in the Hub.

## Building

Needs a Java 21 JDK: `./gradlew build` → `build/libs/cytra-shulkerview-<version>.jar`.
`./gradlew runClientGameTest` runs the in-game test (places shulkers, checks sync, takes screenshots).
Add `-PalloyPack=<Alloy Pack zip>` for the named-shulker test (`-PnamedOnly` runs just that one). A test also plays a
server without the mod (labels and named textures only after opening). `-PwithSbt=<ShulkerBoxTooltip jar>` runs the tests
with ShulkerBoxTooltip loaded. `-Ponly=preview|settings|view|named|vanilla|mixed` runs one test.
