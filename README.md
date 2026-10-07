# Cytra Shulker View

Fabric mod for Minecraft 1.21.11 that shows what's inside shulker boxes. Purely visual: shulkers store,
move and behave exactly like vanilla.

## What it shows

- **Shulker items** (inventory, hotbar, chests): a small icon of the item inside when every stack is the same
  item (diamonds, spruce logs...), and a fill bar along the bottom. Light blue while filling, green when 100% full.
- **Placed shulkers**: the same icon and fill bar as a label on the four side faces (the faces around the lid),
  like an item frame. Faces turned away from you and shulkers further than 24 blocks are skipped.

Mixed shulkers get only the fill bar; empty shulkers get nothing.

## Server or client?

One jar, install it on both for the full effect.

- **Server + client**: the server adds a tiny summary (icon item + fill) to each shulker's block entity data, so every
  placed shulker shows its label and updates live (players, hoppers, droppers). Vanilla clients ignore the extra data.
- **Client only** (server without the mod): shulker items work fully. Placed shulkers show their label after you open
  them once (remembered until you leave the server).
- **Server only**: harmless, nothing to see.

## Config

`config/cytra-shulkerview.json`:

| Key | Default | |
|---|---|---|
| `itemIcon` | `true` | Icon on shulker items |
| `itemBar` | `true` | Fill bar on shulker items |
| `placedLabels` | `true` | Labels on placed shulkers |
| `placedDistance` | `24` | Max label distance in blocks (1-128) |

## Building

Needs a Java 21 JDK: `./gradlew build` → `build/libs/cytra-shulkerview-<version>.jar`.
`./gradlew runClientGameTest` runs the in-game test (places shulkers, checks sync, takes screenshots).
