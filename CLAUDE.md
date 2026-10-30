# Alloy Shulker View

Shows what is inside shulker boxes (icon, fill bar, placed-box labels, tooltip preview, ender chest preview). Purely visual: shulkers behave like vanilla. Workspace rules: `../CLAUDE.md`.

## What it is
- id `cytra-shulkerview`, display name "Alloy Shulker View", package `dev.steelaspect.shulkerview`, jar `alloy-shulkerview-<mod_version>.jar` (1.7.0 at last check). `environment: *`: client does everything; the server entrypoint only sends the ender chest contents to players who have the mod.
- Repo `SteelAspect/cytra-shulkerview`, branch `main` only (no `dev`). Docs: `README.md`, `DESIGN.md` (1.3.0 settings screen), `PROGRESS.txt`.

## Stack
- MC 1.21.11, Loader 0.19.5, Fabric API 0.141.6+1.21.11, Loom 1.17.21, **Mojang mappings**, Java 21 (`~/.local/share/PrismLauncher/java/java-runtime-delta`).
- Alloy UI `alloyui_version=1.3.0` jar-in-jar. Mod Menu 17.0.1 compile-only. No split source sets: client classes live in the `client` package, the server never loads them.
- Test-only flags: `-PwithObe=<Optimized Block Entities jar>`, `-PwithSbt=<ShulkerBoxTooltip jar>`, `-PalloyPack=<Alloy Pack zip>`, `-PnamedOnly`, `-Ponly=preview|settings|view|named|vanilla`.

## Code map
- Entrypoints `ShulkerView` (main: registers `EnderChestPayload` S2C, sends a player's ender chest every second only on change and only to clients that can receive it), `client.ShulkerViewClient`, `client.compat.ModMenuIntegration`.
- `ShulkerSummary` / `ShulkerSummaryHolder` (what a box holds, synced in the block entity update tag, incl. top-4 item types as `top`; older clients ignore it).
- Client: `ShulkerItemOverlay` + `ShulkerItems` (icon and fill bar on items), `PlacedShulkerLabels` (labels, optionally on the lid), `NamedShulkerTextures` + `NamedMaterialHolder` (textures by custom name), `preview/` (`ShulkerPreview`, `ClientShulkerPreview`, `ShulkerPreviewData`: tooltip like ShulkerBoxTooltip, Shift = compact, Alt+Shift = full), `OpenedShulkerCache`, `EnderChestCache`, `ShulkerViewConfig` (`config/cytra-shulkerview.json`, rewritten with all keys on load), `ShulkerViewOptions` (one option list), `screen/SettingsScreen`.
- Mixins (`cytra-shulkerview.mixins.json`): `ShulkerBoxBlockEntityMixin` (`loadAdditional` TAIL), `BlockEntityMixin` (`setChanged` TAIL), `ContainerItemsAccessor`; client `GuiGraphicsMixin` (`renderItemDecorations`), `ShulkerBoxRenderStateMixin` + `ShulkerBoxRendererMixin` (`extractRenderState`, `submit` ModifyVariable), `ItemTooltipImageMixin` (`getTooltipImage` HEAD), `ItemStackTooltipMixin` (`addToTooltip` HEAD).
- Key "Open settings" is unbound by default (user choice). Lang `assets/cytra-shulkerview/lang`.
- Hub page lives in cytra-hub (`integration/shulkerview`): it reads `ShulkerViewConfig` fields by reflection (names like `tooltipPreview`, `tooltipShiftOnly`, `tooltipStyle`, `mixedIcon`, `placedLabelsLid`, `tooltipEnderChest`), so older versions just hide rows.

## Build and test
```bash
export JAVA_HOME=~/.local/share/PrismLauncher/java/java-runtime-delta
./gradlew build
./gradlew runClientGameTest                       # all client game tests (muted, headless)
./gradlew runClientGameTest -Ponly=preview        # one of: preview, settings, view, named, vanilla
./gradlew runClientGameTest -PwithSbt=/path/ShulkerBoxTooltip.jar   # clash check with SBT
```
Tests: `ShulkerViewClientGameTest`, `PreviewClientGameTest`, `SettingsScreenClientGameTest`, `MixedIconClientGameTest`, `NamedShulkerClientGameTest`, `EnderChestClientGameTest`, `VanillaServerClientGameTest` (vanilla server without the mod). No unit tests.

## Cross-mod links
- Hub page: **hub** chat; it follows this mod's config field names by reflection, so renaming a config field breaks the Hub row silently. Alloy Pack (alloy-resource-pack) provides named shulker textures; Alloy UI: **alloy-ui** chat.

## Decisions and gotchas
- Never change shulker behaviour; only drawing and tooltips. Greyed-out/hidden when ShulkerBoxTooltip is loaded (Hub greys the tooltip toggles).
- Works on vanilla servers (client-computed summary); the ender chest preview needs the mod on the server.
- Sync format additions must be ignorable by older clients (the `top` key).
- Config clamp ranges, Hub page ranges and the options list must stay the same.

## Release
- Bump `mod_version`, `python ../cytra-releases/release.py build/libs/alloy-shulkerview-<v>.jar "<changelog>"`. Never release without asking.

## Gaps and improvement ideas
- Alloy UI 1.3.0 vs 1.4.0.
- No `dev` branch; every change lands on `main`.
- `DESIGN.md` only covers 1.3.0; 1.4.0-1.7.0 features (tooltip preview, mixed icon, ender chest) are documented only in PROGRESS/README. Add design notes for the ender chest payload.
- No unit tests for `ShulkerSummary` (pure data, easy to cover) or tooltip layout maths.
- Hub reflection on config field names has no cross-repo test; a rename here is not caught until the Hub's page tests run.
