package dev.steelaspect.shulkerview.client.screen;

import dev.steelaspect.alloyui.AlloyScreen;
import dev.steelaspect.alloyui.UiContext;
import dev.steelaspect.alloyui.render.Draw;
import dev.steelaspect.alloyui.theme.Theme;
import dev.steelaspect.alloyui.widget.Button;
import dev.steelaspect.alloyui.widget.Container;
import dev.steelaspect.alloyui.widget.ScrollPanel;
import dev.steelaspect.alloyui.widget.SettingRow;
import dev.steelaspect.alloyui.widget.Widget;
import dev.steelaspect.shulkerview.client.ShulkerViewConfig;
import dev.steelaspect.shulkerview.client.ShulkerViewOptions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Shulker View's own settings (Mod Menu, the unbound "Open settings" key): every option in one scrolling list. */
public final class SettingsScreen extends AlloyScreen {
    private final Frame frame;

    public SettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("cytra-shulkerview.settings.title"), parent);
        frame = new Frame(title, ShulkerViewOptions.create().build());
        frame.button(new Button(Component.translatable("gui.done"), this::onClose).style(Button.Style.PRIMARY));
        setRoot(frame);
    }

    /** For tests: the row whose label is this option's ({@code cytra-shulkerview.option.<id>}), or null. */
    public @Nullable SettingRow rowForTest(String id) {
        return find(root(), Component.translatable("cytra-shulkerview.option." + id).getString());
    }

    private static @Nullable SettingRow find(@Nullable Widget w, String label) {
        if (w == null) return null;
        if (w instanceof SettingRow r && r.visible() && r.label().getString().equals(label)) return r;
        for (Widget c : w.children()) {
            SettingRow r = find(c, label);
            if (r != null) return r;
        }
        return null;
    }

    @Override
    public void removed() {
        super.removed();
        ShulkerViewConfig.save();
    }

    /** Centred panel: title, scrolling body, buttons at the bottom right (the last is the main one). */
    private static final class Frame extends Container {
        private static final float TITLE_H = 22, FOOTER_H = 26, PAD = 8, BUTTON_GAP = 6, MAX_WIDTH = 420;

        private final Component title;
        private final ScrollPanel body;
        private final java.util.List<Button> buttons = new java.util.ArrayList<>();
        private float px, py, pw, ph;

        Frame(Component title, Widget content) {
            this.title = title;
            this.body = add(new ScrollPanel(content));
        }

        void button(Button b) {
            buttons.add(add(b));
        }

        @Override
        public float measure(float width) {
            return 0;
        }

        @Override
        public boolean contains(double mx, double my) {
            return mx >= px && mx < px + pw && my >= py && my < py + ph;
        }

        @Override
        protected void layout() {
            float[] f = AlloyScreen.frameBounds(w, h);
            pw = Math.min(f[2], MAX_WIDTH);
            ph = f[3];
            px = x + (w - pw) / 2;
            py = y + f[1];
            body.setBounds(px + PAD, py + TITLE_H, pw - 2 * PAD, ph - TITLE_H - FOOTER_H);
            float bx = px + pw - PAD;
            for (int i = buttons.size() - 1; i >= 0; i--) {
                Button b = buttons.get(i);
                float bw = Math.max(80, b.preferredWidth());
                bx -= bw;
                b.setBounds(bx, py + ph - FOOTER_H + 4, bw, b.measure(bw));
                bx -= BUTTON_GAP;
            }
        }

        @Override
        protected void renderWidget(UiContext ctx) {
            Theme t = ctx.theme;
            Draw.elevation(ctx.g, px, py, pw, ph, t.metricF("radiusLarge"), 3);
            Draw.panel(ctx.g, px, py, pw, ph, t.metricF("radiusLarge"), t.color("panel"), t.color("border"), t.metricF("borderWidth"));
            Draw.heading(ctx.g, title, px + PAD + 2, py + 8, t.color("text"));
            Draw.fadeRule(ctx.g, px + PAD, py + TITLE_H - 3, pw - 2 * PAD, t.color("border"));
            Draw.fadeRule(ctx.g, px + PAD, py + ph - FOOTER_H, pw - 2 * PAD, t.color("border"));
            super.renderWidget(ctx);
        }
    }
}
