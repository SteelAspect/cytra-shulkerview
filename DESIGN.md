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
