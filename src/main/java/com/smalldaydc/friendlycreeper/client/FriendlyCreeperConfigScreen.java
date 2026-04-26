package com.smalldaydc.friendlycreeper.client;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperConfigScreen {

    public static Screen create(Screen parent) {
        FriendlyCreeperConfig config = FriendlyCreeperConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Friend Creeper Settings"))
                .setSavingRunnable(FriendlyCreeperConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Allow Owner Damage"),
                        config.allowOwnerDamage)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Whether the owner can damage their own tamed Creeper."))
                .setSaveConsumer(value -> config.allowOwnerDamage = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Follow Owner"),
                        config.followOwner)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether tamed Creepers follow their owner."))
                .setSaveConsumer(value -> config.followOwner = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Revenge Owner"),
                        config.revengeOwner)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether tamed Creepers avenge their owner when attacked."))
                .setSaveConsumer(value -> config.revengeOwner = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Snow Golem Attack"),
                        config.snowGolemAttack)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Whether Snow Golems can target tamed Creepers."))
                .setSaveConsumer(value -> config.snowGolemAttack = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Hurt Sound"),
                        config.hurtSound)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether tamed Creepers play a hurt sound when at low health."))
                .setSaveConsumer(value -> config.hurtSound = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Render Poppy"),
                        config.renderPoppy)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether to render the poppy on tamed Creepers' heads.\nNote: This is a client-side option and is not affected by server configuration."))
                .setSaveConsumer(value -> config.renderPoppy = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.literal("Afraid of Cats"),
                        config.afraidOfCats)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether tamed Creepers are afraid of cats and ocelots."))
                .setSaveConsumer(value -> config.afraidOfCats = value)
                .build());

        return builder.build();
    }
}
