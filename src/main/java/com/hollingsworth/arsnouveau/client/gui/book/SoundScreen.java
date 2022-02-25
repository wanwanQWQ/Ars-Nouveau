package com.hollingsworth.arsnouveau.client.gui.book;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SliderButton;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SoundScreen extends BaseBook{

    public SoundScreen() {
        super();
    }

    SliderButton redW;
    SliderButton greenW;
    SliderButton blueW;

    double volume;
    @Override
    public void init() {
        super.init();
        redW = (SliderButton)buildSlider(new TranslatableComponent("ars_nouveau.color_gui.red_slider").getString(), s -> volume, (settings, d) -> volume = d).createButton(Minecraft.getInstance().options, bookLeft + 28, bookTop + 49, 100);
//        greenW = (SliderButton)buildSlider(new TranslatableComponent("ars_nouveau.color_gui.green_slider").getString(), s -> green, (settings, d) -> green = d).createButton(Minecraft.getInstance().options, bookLeft + 28, bookTop + 89, 100);
//        blueW = (SliderButton)buildSlider(new TranslatableComponent("ars_nouveau.color_gui.blue_slider").getString(), s -> blue, (settings, d) -> blue = d).createButton(Minecraft.getInstance().options, bookLeft + 28, bookTop + 129, 100);
        addRenderableWidget(redW);
        addRenderableWidget(greenW);
        addRenderableWidget(blueW);
        addRenderableWidget(new GuiImageButton(bookLeft+ 55, bookBottom - 36, 0,0,37, 12, 37, 12, "textures/gui/save_icon.png", this::onSaveClick));
        addPresets();
    }

    public void addPresets(){
        // Default
        addRenderableWidget(new GuiImageButton(bookRight - 131, bookTop + 44, 0,0,48, 11, 48, 11,
                "textures/gui/default_color_icon.png", (_2) -> setFromPreset(255, 25, 180)));
        // Purple
        addRenderableWidget(new GuiImageButton(bookRight - 131, bookTop + 68, 0,0,48, 11, 48, 11,
                "textures/gui/purple_color_icon.png", (_2) -> setFromPreset(80, 25, 255)));

        // Blue
        addRenderableWidget(new GuiImageButton(bookRight - 131, bookTop + 92, 0,0,48, 11, 48, 11,
                "textures/gui/blue_color_icon.png", (_2) -> setFromPreset(30, 25, 255)));

        // Red
        addRenderableWidget(new GuiImageButton(bookRight - 131, bookTop + 116, 0,0,48, 11, 48, 11,
                "textures/gui/red_color_icon.png", (_2) -> setFromPreset(255, 25, 25)));

        // Green
        addRenderableWidget(new GuiImageButton(bookRight - 131, bookTop + 140, 0,0,48, 11, 48, 11,
                "textures/gui/green_color_icon.png", (_2) -> setFromPreset(25, 255, 25)));

        // Yellow
        addRenderableWidget(new GuiImageButton(bookRight - 73, bookTop + 44, 0,0,48, 11, 48, 11,
                "textures/gui/yellow_color_icon.png", (_2) -> setFromPreset(255, 255, 25)));
        // White
        addRenderableWidget(new GuiImageButton(bookRight - 73, bookTop + 68, 0,0,48, 11, 48, 11,
                "textures/gui/white_color_icon.png", (_2) -> setFromPreset(255, 255, 255)));

        // Orange
        addRenderableWidget(new GuiImageButton(bookRight - 73, bookTop + 92, 0,0,48, 11, 48, 11,
                "textures/gui/orange_color_icon.png", (_2) -> setFromPreset(255, 90, 1)));

        // Cyan
        addRenderableWidget(new GuiImageButton(bookRight - 73, bookTop + 116, 0,0,48, 11, 48, 11,
                "textures/gui/cyan_color_icon.png", (_2) -> setFromPreset(25, 255, 255)));




    }
    public void setFromPreset(int r, int g, int b){
    }

    public void onSaveClick(Button button){
        //Networking.INSTANCE.sendToServer(new PacketUpdateSpellColors(slot, red, green, blue));
    }

    @Override
    public void drawBackgroundElements(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundElements(stack, mouseX, mouseY, partialTicks);
        drawFromTexture(new ResourceLocation(ArsNouveau.MODID, "textures/gui/slider_gilding.png"), 22, 47, 0, 0, 112, 104,112,104, stack);
        int color = -8355712;
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.title").getString(), 51, 24,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.presets").getString(), 159, 24,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.default").getString(), 170, 46,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.purple").getString(), 170, 70,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.blue").getString(), 170, 94,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.red").getString(), 170, 118,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.green").getString(), 170, 142,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.yellow").getString(), 228, 46,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.white").getString(), 228, 70,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.orange").getString(), 228, 94,  color);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.cyan").getString(), 228, 118,  color);
        // minecraft.fontRenderer.drawString(stack, "Ice", 218, 115,  0);
        minecraft.font.draw(stack, new TranslatableComponent("ars_nouveau.color_gui.save").getString(), 67, 160,  color);


    }

    @Override
    public void drawForegroundElements(int mouseX, int mouseY, float partialTicks) {
        super.drawForegroundElements(mouseX, mouseY, partialTicks);
    }

    protected ProgressOption buildSlider(String key, Function<Options, Double> getter, BiConsumer<Options, Double> setter){
        return new ProgressOption(key, 1.0D, 255.0D, 1.0F, getter, setter, (settings, optionValues) -> new TextComponent(key + (int)optionValues.get(settings)));
    }
}
