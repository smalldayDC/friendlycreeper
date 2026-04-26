package com.smalldaydc.friendlycreeper.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperNoConfigScreen extends Screen {

    private static final Text[] MESSAGES = {
        Text.literal("Cloth Config API is not installed"),
        Text.literal("Please install it before configuring via the graphical interface."),
        Text.literal("You can download it from Modrinth or CurseForge.")
    };

    private final Screen parent;

    public FriendlyCreeperNoConfigScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                button -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 75, this.height / 2 + 40, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        for (int i = 0; i < MESSAGES.length; i++) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    MESSAGES[i], this.width / 2, this.height / 2 - 20 + i * 15, 0xFFFFFFFF);
        }
    }
}
