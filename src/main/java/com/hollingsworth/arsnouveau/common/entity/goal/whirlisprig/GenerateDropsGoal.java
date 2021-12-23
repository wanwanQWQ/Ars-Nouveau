package com.hollingsworth.arsnouveau.common.entity.goal.whirlisprig;

import com.hollingsworth.arsnouveau.api.util.DropDistribution;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.entity.Whirlisprig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.hollingsworth.arsnouveau.common.entity.goal.whirlisprig.EvaluateGroveGoal.getScore;

public class GenerateDropsGoal extends Goal {
    Whirlisprig sylph;
    public List<BlockPos> locList;
    int timeGathering;
    public GenerateDropsGoal(Whirlisprig sylph){
        this.sylph = sylph;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public void stop() {
        timeGathering = 100;
        locList = null;
    }

    public int getDropsByDiversity(){
        return  sylph.diversityScore / 2;
    }

    public int getTimerByMood(){
        int mood = sylph.getEntityData().get(Whirlisprig.MOOD_SCORE);
        if(mood >= 1000)
            return 20;

        if(mood >= 750)
            return 40;

        if(mood >= 500)
            return 60;

        if(mood >= 250)
            return 80;

        return 100;
    }

    @Override
    public void tick() {
        Level world = sylph.getCommandSenderWorld();
        timeGathering--;

        if(locList == null || timeGathering <= 0)
            return;

        for(BlockPos growPos : locList) {
            ((ServerLevel) world).sendParticles(ParticleTypes.COMPOSTER, growPos.getX() + 0.5, growPos.getY() + 0.5, growPos.getZ() + 0.5, 1, ParticleUtil.inRange(-0.2, 0.2), 0, ParticleUtil.inRange(-0.2, 0.2), 0.01);
        }
        timeGathering--;

        if(timeGathering == 0 && sylph.removeManaForDrops()){
            sylph.timeUntilGather = getTimerByMood() * 20;
            DropDistribution<BlockState> blockDropDistribution = new DropDistribution<>(sylph.genTable);
            int numDrops = getDropsByDiversity() + 3;
            for(int i = 0; i < numDrops; i++){
                BlockState block = blockDropDistribution.nextDrop();
                if(block == null)
                    return;

                for(ItemStack s : getDrops(blockDropDistribution)){
                    sylph.onPickup(s);
                }
            }

        }
    }

    public List<ItemStack> getDrops(DropDistribution<BlockState> blockDropDistribution){
        Level world = sylph.getCommandSenderWorld();
        Supplier<List<ItemStack>> getDrops = () -> Block.getDrops(blockDropDistribution.nextDrop(), (ServerLevel) world, sylph.blockPosition(), null);

        List<ItemStack> successfulDrops;
        boolean bonusReroll = false;
        for(int numRerolls = 0; numRerolls < (bonusReroll ? 16 : 8); numRerolls++) {
            List<ItemStack> drops = getDrops.get();
            if (drops.isEmpty()) continue;
            successfulDrops = drops.stream().filter(s -> sylph.isValidReward(s)).collect(Collectors.toCollection(ArrayList::new));
            bonusReroll = true;
            if (successfulDrops.isEmpty()) continue;
            return successfulDrops;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean canContinueToUse() {
        return sylph.timeUntilGather <= 0  && timeGathering >= 0 && locList != null && sylph.crystalPos != null;
    }

    @Override
    public boolean canUse() {
        return sylph.crystalPos != null  && sylph.genTable != null && sylph.timeUntilGather <= 0 && (sylph.drops == null || sylph.drops.isEmpty()) && sylph.enoughManaForTask();
    }

    @Override
    public void start() {
        Level world = sylph.getCommandSenderWorld();
        if(locList == null){
            locList = new ArrayList<>();
            for(BlockPos b : BlockPos.betweenClosed(sylph.blockPosition().north(4).west(4).below(3),sylph.blockPosition().south(4).east(4).above(3))){
                if(b.getY() >= 256)
                    continue;
                BlockState state = world.getBlockState(b);
                int points = getScore(state);
                if(points == 0)
                    continue;
                locList.add(b.immutable());
            }
            Collections.shuffle(locList);
            if(locList.size() > 6)
                locList = locList.subList(0, 6);
        }
    }
}