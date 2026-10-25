package dev.steelaspect.shulkerview.client;

import dev.steelaspect.alloyui.widget.Options;
import dev.steelaspect.shulkerview.client.preview.ShulkerPreview;
import net.minecraft.network.chat.Component;

/**
 * Every setting in one Alloy UI {@link Options} list, for the settings screen. Getters go through
 * {@link ShulkerViewConfig#INSTANCE} each time because loading the config replaces it. Every change saves at once.
 */
public final class ShulkerViewOptions {
    private ShulkerViewOptions() {}

    public static Options create() {
        Options o = new Options("cytra-shulkerview.option.");
        o.section("section.items")
                .toggle("itemIcon", () -> c().itemIcon, v -> c().itemIcon = v, true).onChange(ShulkerViewConfig::save)
                .toggle("itemBar", () -> c().itemBar, v -> c().itemBar = v, true).onChange(ShulkerViewConfig::save)
                .choice("mixedIcon", () -> java.util.List.of(dev.steelaspect.shulkerview.ShulkerSummary.MixedIcon.values()),
                        v -> Component.translatable("cytra-shulkerview.option.mixedIcon." + v.name().toLowerCase(java.util.Locale.ROOT)),
                        () -> c().mixedIcon, v -> c().mixedIcon = v, dev.steelaspect.shulkerview.ShulkerSummary.MixedIcon.NONE)
                .onChange(ShulkerViewConfig::save)
                .slider("itemIconSize", 0.25, 1.0, 0.05, () -> c().itemIconSize, v -> c().itemIconSize = v, 0.625)
                .onChange(ShulkerViewConfig::save);
        o.section("section.tooltip")
                .toggle("tooltipPreview", () -> c().tooltipPreview, v -> c().tooltipPreview = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerPreview::otherModPreviews, Component.translatable("cytra-shulkerview.option.tooltipPreview.other"))
                .toggle("tooltipShiftOnly", () -> c().tooltipShiftOnly, v -> c().tooltipShiftOnly = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"))
                .choice("tooltipCompact", () -> java.util.List.of(true, false),
                        v -> Component.translatable("cytra-shulkerview.option.tooltipCompact." + (v ? "compact" : "full")),
                        () -> c().tooltipCompact, v -> c().tooltipCompact = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"))
                .choice("tooltipStyle", () -> java.util.List.of(ShulkerViewConfig.TooltipStyle.values()),
                        v -> Component.translatable("cytra-shulkerview.option.tooltipStyle." + v.name().toLowerCase(java.util.Locale.ROOT)),
                        () -> c().tooltipStyle, v -> c().tooltipStyle = v, ShulkerViewConfig.TooltipStyle.VANILLA).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"))
                .toggle("tooltipTint", () -> c().tooltipTint, v -> c().tooltipTint = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"))
                .toggle("tooltipOtherContainers", () -> c().tooltipOtherContainers, v -> c().tooltipOtherContainers = v, true)
                .onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"))
                .toggle("tooltipEnderChest", () -> c().tooltipEnderChest, v -> c().tooltipEnderChest = v, true).onChange(ShulkerViewConfig::save)
                .disabledWhen(ShulkerViewOptions::previewOff, Component.translatable("cytra-shulkerview.option.tooltipPreview.off"));
        o.section("section.placed")
                .toggle("placedLabels", () -> c().placedLabels, v -> c().placedLabels = v, true).onChange(ShulkerViewConfig::save)
                .toggle("placedLabelsLid", () -> c().placedLabelsLid, v -> c().placedLabelsLid = v, true).onChange(ShulkerViewConfig::save)
                .number("placedDistance", 1, 128, 1, true, () -> c().placedDistance, v -> c().placedDistance = v.intValue(), 24)
                .onChange(ShulkerViewConfig::save)
                .slider("placedIconSize", 0.2, 0.8, 0.05, () -> c().placedIconSize, v -> c().placedIconSize = v, 0.4)
                .onChange(ShulkerViewConfig::save)
                .toggle("namedTextures", () -> c().namedTextures, v -> c().namedTextures = v, true).onChange(ShulkerViewConfig::save);
        o.section("section.colors")
                .color("barColor", () -> c().barColor, v -> c().barColor = v, 0xFF55BBFF, true).onChange(ShulkerViewConfig::save)
                .color("fullBarColor", () -> c().fullBarColor, v -> c().fullBarColor = v, 0xFF55FF55, true)
                .onChange(ShulkerViewConfig::save);
        return o;
    }

    private static boolean previewOff() {
        return ShulkerPreview.otherModPreviews() || !c().tooltipPreview;
    }

    private static ShulkerViewConfig c() {
        return ShulkerViewConfig.INSTANCE;
    }
}
