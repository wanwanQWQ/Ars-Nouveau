package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.client.gui.SpellTooltip;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class SpellParchment extends ModItem implements ICasterTool {

    public SpellParchment(Properties properties) {
        super(properties);
    }

    public SpellParchment() {
        super(ItemsRegistry.defaultItemProperties().component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()));
    }

    @Override
    public Component getName(ItemStack pStack) {
        SpellCaster caster = getSpellCaster(pStack);
        return caster.getSpellName().isEmpty() ? super.getName(pStack) : Component.literal(caster.getSpellName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip2, TooltipFlag flagIn) {
        stack.addToTooltip(DataComponentRegistry.SPELL_CASTER, context, tooltip2::add, flagIn);
        super.appendHoverText(stack, context, tooltip2, flagIn);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        SpellCaster caster = getSpellCaster(pStack);
        if (Config.GLYPH_TOOLTIPS.get() && !Screen.hasShiftDown() && !caster.isSpellHidden() && !caster.getSpell().isEmpty())
            return Optional.of(new SpellTooltip(caster));
        return Optional.empty();
    }
}
