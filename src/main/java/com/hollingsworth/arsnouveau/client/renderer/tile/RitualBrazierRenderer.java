package com.hollingsworth.arsnouveau.client.renderer.tile;

import com.hollingsworth.arsnouveau.client.renderer.item.GenericItemRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.RitualTile;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class RitualBrazierRenderer extends GeoBlockRenderer<RitualTile> {
    public RitualBrazierRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
        super(rendererDispatcherIn, new RitualBrazierModel());
    }

    public static GenericItemRenderer getISTER(){
        return new GenericItemRenderer(new RitualBrazierModel());
    }
}
