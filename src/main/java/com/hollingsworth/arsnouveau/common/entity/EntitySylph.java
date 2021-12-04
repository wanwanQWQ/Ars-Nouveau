package com.hollingsworth.arsnouveau.common.entity;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.api.entity.IDispellable;
import com.hollingsworth.arsnouveau.api.spell.IPickupResponder;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.NBTUtil;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.tile.SummoningCrystalTile;
import com.hollingsworth.arsnouveau.common.entity.goal.GoBackHomeGoal;
import com.hollingsworth.arsnouveau.common.entity.goal.sylph.*;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketANEffect;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.Config;
import com.hollingsworth.arsnouveau.setup.ItemsRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;

public class EntitySylph extends AbstractFlyingCreature implements IPickupResponder, IAnimatable, ITooltipProvider, IDispellable {
    AnimationFactory manager = new AnimationFactory(this);

    public static Tag.Named<Item> DENIED_DROP =  ItemTags.createOptional(new ResourceLocation(ArsNouveau.MODID, "sylph/denied_drop"));

    public int timeSinceBonemeal = 0;
    public static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(EntitySylph.class, EntityDataSerializers.BOOLEAN);
    /*Strictly used for after a tame event*/
    public int tamingTime = 0;
    public boolean droppingShards; // Strictly used by non-tamed spawns for giving shards
    public static final EntityDataAccessor<Integer> MOOD_SCORE = SynchedEntityData.defineId(EntitySylph.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> COLOR = SynchedEntityData.defineId(EntitySylph.class, EntityDataSerializers.STRING);
    public List<ItemStack> ignoreItems;
    public int timeUntilGather = 0;
    public int timeUntilEvaluation = 0;
    public int diversityScore;
    public Map<BlockState, Integer> genTable;
    public Map<BlockState, Integer> scoreMap;
    public BlockPos crystalPos;
    public List<ItemStack> drops;
    private boolean setBehaviors;
    private <E extends Entity> PlayState idlePredicate(AnimationEvent event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController(this, "idleController", 20, this::idlePredicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return manager;
    }

    @Override
    protected int getExperienceReward(Player player) {
        return 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if(player.getCommandSenderWorld().isClientSide)
            return super.mobInteract(player,hand);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() == ItemsRegistry.DENY_ITEM_SCROLL) {
            List<ItemStack> items = ItemsRegistry.DENY_ITEM_SCROLL.getItems(stack);
            if (!items.isEmpty()) {
                this.ignoreItems = ItemsRegistry.DENY_ITEM_SCROLL.getItems(stack);
                PortUtil.sendMessage(player, new TranslatableComponent("ars_nouveau.sylph.ignore"));
            }
        }
        return super.mobInteract(player,hand);
    }

    public String getColor(){
        return this.entityData.get(COLOR);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        if(hand != InteractionHand.MAIN_HAND || player.getCommandSenderWorld().isClientSide || !this.entityData.get(TAMED))
            return InteractionResult.PASS;
        
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if(Tags.Items.DYES.contains(item)){
            if(Tags.Items.DYES_GREEN.contains(item) && !getColor().equals("summer")){
                this.entityData.set(COLOR, "summer");
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if(Tags.Items.DYES_ORANGE.contains(item) && !getColor().equals("autumn")){
                this.entityData.set(COLOR, "autumn");
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if(Tags.Items.DYES_YELLOW.contains(item) && !getColor().equals("spring")){
                this.entityData.set(COLOR, "spring");
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if(Tags.Items.DYES_WHITE.contains(item) && !getColor().equals("winter")){
                this.entityData.set(COLOR, "winter");
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }

        if(stack.isEmpty()) {
            int moodScore = entityData.get(MOOD_SCORE);
            if(moodScore < 250){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.unhappy"));
            }else if(moodScore <= 500){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.content"));
            }else if(moodScore <= 750){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.happy"));
            }else if(moodScore < 1000){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.very_happy"));
            }else{
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.extremely_happy"));
            }
            int numDrops = diversityScore / 2;
            if(numDrops <= 5){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.okay_diversity"));
            }else if(numDrops <= 10){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.diverse_enough"));
            }else if(numDrops <= 20){
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.very_diverse"));
            }else{
                PortUtil.sendMessage(player, new TranslatableComponent("sylph.extremely_diverse"));
            }
            if(ignoreItems != null && !ignoreItems.isEmpty()) {
                StringBuilder status = new StringBuilder();
                status.append(new TranslatableComponent("ars_nouveau.sylph.ignore_list").getString());
                for (ItemStack i : ignoreItems) {
                    status.append(i.getHoverName().getString()).append(" ");
                }
                PortUtil.sendMessage(player, status.toString());
            }

            return InteractionResult.SUCCESS;
        }
        if(!(stack.getItem() instanceof BlockItem))
            return InteractionResult.PASS;
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        int score = EvaluateGroveGoal.getScore(state);
        if(score > 0 && this.scoreMap != null && this.scoreMap.get(state) != null && this.scoreMap.get(state) >= 50){
            PortUtil.sendMessage(player, new TranslatableComponent("sylph.toomuch"));
            return InteractionResult.SUCCESS;
        }

        if(score == 0) {
            PortUtil.sendMessage(player, new TranslatableComponent("sylph.notinterested"));
        }
        if(score == 1){
            PortUtil.sendMessage(player, new TranslatableComponent("sylph.likes"));
        }

        if(score == 2){
            PortUtil.sendMessage(player, new TranslatableComponent("sylph.excited"));
        }
        return InteractionResult.SUCCESS;
    }

    protected EntitySylph(EntityType<? extends AbstractFlyingCreature> type, Level worldIn) {
        super(type, worldIn);
        MinecraftForge.EVENT_BUS.register(this);
        this.moveControl =  new FlyingMoveControl(this, 10, true);
        addGoalsAfterConstructor();
    }

    public EntitySylph(Level world, boolean isTamed, BlockPos pos) {
        super(ModEntities.ENTITY_SYLPH_TYPE, world);
        MinecraftForge.EVENT_BUS.register(this);
        this.moveControl =  new FlyingMoveControl(this, 10, true);
        this.entityData.set(TAMED, isTamed);
        this.crystalPos = pos;
        addGoalsAfterConstructor();
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.level.isClientSide){
            if(level.getGameTime() % 20 == 0 && this.blockPosition().getY() < 0) {
                this.remove(RemovalReason.DISCARDED);
                return;
            }

            if(Boolean.TRUE.equals(this.entityData.get(TAMED))){
                this.timeUntilEvaluation--;
                this.timeUntilGather--;
            }
            this.timeSinceBonemeal++;
        }

        if(!level.isClientSide && level.getGameTime() % 60 == 0 && isTamed() && crystalPos != null && !(level.getBlockEntity(crystalPos) instanceof SummoningCrystalTile)) {
            this.hurt(DamageSource.playerAttack(FakePlayerFactory.getMinecraft((ServerLevel) level)), 99);
            return;
        }

        if(this.droppingShards) {
            tamingTime++;
            if(tamingTime % 20 == 0 && !level.isClientSide())
                Networking.sendToNearby(level, this, new PacketANEffect(PacketANEffect.EffectType.TIMED_HELIX, blockPosition()));

            if(tamingTime > 60 && !level.isClientSide) {
                ItemStack stack = new ItemStack(ItemsRegistry.sylphShard, 1 + level.random.nextInt(1));
                level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), stack));
                this.remove(RemovalReason.DISCARDED);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1f, 1f );
            }
            else if (tamingTime > 55 && level.isClientSide){
                for(int i =0; i < 10; i++){
                    double d0 = getX();
                    double d1 = getY()+0.1;
                    double d2 = getZ();
                    level.addParticle(ParticleTypes.END_ROD, d0, d1, d2, (level.random.nextFloat() * 1 - 0.5)/3, (level.random.nextFloat() * 1 - 0.5)/3, (level.random.nextFloat() * 1 - 0.5)/3);
                }
            }
        }
    }

    // Cannot add conditional goals in RegisterGoals as it is final and called during the MobEntity super.
    protected void addGoalsAfterConstructor(){
        if(this.level.isClientSide())
            return;

        for(WrappedGoal goal : getGoals()){
            this.goalSelector.addGoal(goal.getPriority(), goal.getGoal());
        }
    }

    public List<WrappedGoal> getGoals(){
        return this.entityData.get(TAMED) ? getTamedGoals() : getUntamedGoals();
    }

    public boolean enoughManaForTask(){
        if(!(level.getBlockEntity(crystalPos) instanceof SummoningCrystalTile))
            return false;
        return ((SummoningCrystalTile) level.getBlockEntity(crystalPos)).enoughMana(Config.SYLPH_MANA_COST.get());
    }

    public boolean removeManaForDrops(){
        if(!(level.getBlockEntity(crystalPos) instanceof SummoningCrystalTile))
            return false;
        return ((SummoningCrystalTile) level.getBlockEntity(crystalPos)).removeManaAround(Config.SYLPH_MANA_COST.get());
    }

    public boolean isTamed(){
        return this.entityData.get(TAMED);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if(source == DamageSource.CACTUS || source == DamageSource.SWEET_BERRY_BUSH || source == DamageSource.DROWN)
            return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if(!level.isClientSide && isTamed()){
            ItemStack stack = new ItemStack(ItemsRegistry.sylphCharm);
            level.addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(), stack));
        }
        super.die(source);
    }

    //MOJANG MAKES THIS SO CURSED WHAT THE HECK

    public List<WrappedGoal> getTamedGoals(){
        List<WrappedGoal> list = new ArrayList<>();
        list.add(new WrappedGoal(3, new RandomLookAroundGoal(this)));
        list.add(new WrappedGoal(2, new BonemealGoal(this, () -> this.crystalPos, 10)));
        list.add(new WrappedGoal(1, new EvaluateGroveGoal(this, 20 * 120 )));
        list.add(new WrappedGoal(2, new InspectPlantGoal(this, () -> this.crystalPos,15)));
        list.add(new WrappedGoal(1, new GoBackHomeGoal(this, () -> this.crystalPos,20)));
        list.add(new WrappedGoal(1, new GenerateDropsGoal(this)));
        list.add(new WrappedGoal(0, new FloatGoal(this)));
        return list;
    }

    public List<WrappedGoal> getUntamedGoals(){
        List<WrappedGoal> list = new ArrayList<>();
        list.add(new WrappedGoal(3, new FollowMobGoalBackoff(this, 1.0D, 3.0F, 7.0F, 0.5f)));
        list.add(new WrappedGoal(5, new FollowPlayerGoal(this, 1.0D, 3.0F, 7.0F)));
        list.add(new WrappedGoal(2, new RandomLookAroundGoal(this)));
        list.add(new WrappedGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D)));
        list.add(new WrappedGoal(1, new BonemealGoal(this)));
        list.add(new WrappedGoal(0, new FloatGoal(this)));
        return list;
    }

    @SubscribeEvent
    public void treeGrow(SaplingGrowTreeEvent event) {
        if(!this.entityData.get(TAMED) && BlockUtil.distanceFrom(this.blockPosition(), event.getPos()) <= 10) {
            this.droppingShards = true;
        }
    }


    @Override
    protected void registerGoals() { /*Do not use. See above*/}

    @Override
    public List<String> getTooltip() {
        List<String> tooltip = new ArrayList<>();
        if(!this.entityData.get(TAMED))
            return tooltip;
        int mood = this.entityData.get(MOOD_SCORE);
        String moodStr = new TranslatableComponent("ars_nouveau.sylph.tooltip_unhappy").getString();
        if(mood >= 1000)
            moodStr = new TranslatableComponent("ars_nouveau.sylph.tooltip_extremely_happy").getString();
        else if(mood >= 750)
            moodStr = new TranslatableComponent("ars_nouveau.sylph.tooltip_very_happy").getString();
        else if(mood >= 500)
            moodStr = new TranslatableComponent("ars_nouveau.sylph.tooltip_happy").getString();
        else if(mood >= 250)
            moodStr = new TranslatableComponent("ars_nouveau.sylph.tooltip_content").getString();
        tooltip.add(new TranslatableComponent("ars_nouveau.sylph.tooltip_mood").getString() + moodStr);
        return tooltip;
    }

    public boolean isValidReward(ItemStack stack){
        if (DENIED_DROP.contains(stack.getItem()))
            return false;
        if (ignoreItems == null || ignoreItems.isEmpty())
            return true;
        return ignoreItems.stream().noneMatch(i -> i.sameItem(stack));
    }


    @Override
    public @Nonnull ItemStack onPickup(ItemStack stack) {
        if(!isValidReward(stack))
            return stack;
        SummoningCrystalTile tile = level.getBlockEntity(crystalPos) instanceof SummoningCrystalTile ? (SummoningCrystalTile) level.getBlockEntity(crystalPos) : null;
        return tile == null ? stack : tile.insertItem(stack);
    }


    @Override
    public boolean removeWhenFarAway(double p_213397_1_) {
        return false;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.FLYING_SPEED, Attributes.FLYING_SPEED.getDefaultValue())
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }


    @Override
    protected PathNavigation createNavigation(Level world) {
        FlyingPathNavigation flyingpathnavigator = new FlyingPathNavigation(this, world);
        flyingpathnavigator.setCanOpenDoors(false);
        flyingpathnavigator.setCanFloat(true);
        flyingpathnavigator.setCanPassDoors(true);
        return flyingpathnavigator;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("summoner_x"))
            crystalPos = new BlockPos(tag.getInt("summoner_x"), tag.getInt("summoner_y"), tag.getInt("summoner_z"));
        timeSinceBonemeal = tag.getInt("bonemeal");
        timeUntilGather = tag.getInt("gather");
        timeUntilEvaluation = tag.getInt("eval");
        this.entityData.set(TAMED, tag.getBoolean("tamed"));
        this.entityData.set(EntitySylph.MOOD_SCORE, tag.getInt("score"));
        if(!setBehaviors){
            tryResetGoals();
            setBehaviors = true;
        }
        ignoreItems = NBTUtil.readItems(tag, "ignored_");
        this.entityData.set(COLOR, tag.getString("color"));
    }
    // A workaround for goals not registering correctly for a dynamic variable on reload as read() is called after constructor.
    public void tryResetGoals(){
        this.goalSelector.availableGoals = new LinkedHashSet<>();
        this.addGoalsAfterConstructor();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(crystalPos != null){
            tag.putInt("summoner_x", crystalPos.getX());
            tag.putInt("summoner_y", crystalPos.getY());
            tag.putInt("summoner_z", crystalPos.getZ());
        }
        tag.putInt("eval", timeUntilEvaluation);
        tag.putInt("bonemeal", timeSinceBonemeal);
        tag.putInt("gather", timeUntilGather);
        tag.putBoolean("tamed", this.entityData.get(TAMED));
        tag.putInt("score", this.entityData.get(EntitySylph.MOOD_SCORE));
        tag.putString("color", this.entityData.get(COLOR));
        if (ignoreItems != null && !ignoreItems.isEmpty())
            NBTUtil.writeItems(tag, "ignored_", ignoreItems);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOOD_SCORE, 0);
        this.entityData.define(TAMED, false);
        this.entityData.define(COLOR, "summer");
    }

    @Override
    public boolean onDispel(@Nullable LivingEntity caster) {
        if(this.isRemoved())
            return false;

        if(!level.isClientSide && isTamed()){
            ItemStack stack = new ItemStack(ItemsRegistry.sylphCharm);
            level.addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(), stack));
            ParticleUtil.spawnPoof((ServerLevel)level, blockPosition());
            this.remove(RemovalReason.DISCARDED);
        }
        return this.isTamed();
    }
}