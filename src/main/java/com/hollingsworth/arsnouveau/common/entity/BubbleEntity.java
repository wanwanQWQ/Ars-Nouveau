package com.hollingsworth.arsnouveau.common.entity;

import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.setup.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BubbleEntity extends Projectile implements GeoEntity {
    int maxAge;
    int age;
    float damage;

    public BubbleEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BubbleEntity(Level pLevel, int maxAge, float damage) {
        super(ModEntities.BUBBLE.get(), pLevel);
        this.maxAge = maxAge;
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public void tick() {
        super.tick();
        if(!level.isClientSide){
            age++;
            if(age > maxAge) {
                this.pop();
            }
            if(this.getPassengers().isEmpty()) {
                for (Entity entity1 : level.getEntities(this, this.getBoundingBox().inflate(0.5f), this::canHitEntity)) {
                    entity1.startRiding(this);
                }
            }
        }

        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.setDeltaMovement(ParticleUtil.inRange(-0.01, 0.01), 0.1, ParticleUtil.inRange(-0.01, 0.01));
        this.setPos(getNextHitPosition());

    }

    public Vec3 getNextHitPosition() {
        return this.position().add(this.getDeltaMovement());
    }

    public void pop(){
        if(this.level.isClientSide)
            return;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, this.getSoundSource(), 3.0F, 1.0F);
        this.remove(RemovalReason.DISCARDED);
        if(damage > 0 && this.getFirstPassenger() instanceof LivingEntity living){
            living.hurt(this.damageSources().magic(), this.damage);
        }
    }

    // The only purpose of this is to prevent the default attack noise that occurs.
    public static void onAttacked(AttackEntityEvent event){
        if(event.getTarget() instanceof BubbleEntity bubble){
            bubble.pop();
            event.setCanceled(true);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if(this.getPassengers().isEmpty() && this.canHitEntity(pResult.getEntity())){
            pResult.getEntity().startRiding(this);
        }
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        this.pop();
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return !(pTarget instanceof BubbleEntity);
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity pEntity) {
        return pEntity instanceof ItemEntity ? this.position.add(0, 0.5, 0) : this.position;
    }

    @Override
    protected boolean canRide(Entity pVehicle) {
        return !(pVehicle instanceof BubbleEntity) && super.canRide(pVehicle);
    }

    public AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean save(CompoundTag pCompound) {
        pCompound.putInt("maxAge", this.maxAge);
        pCompound.putFloat("damage", this.damage);
        pCompound.putInt("age", this.age);
        return super.save(pCompound);
    }

    @Override
    public void load(CompoundTag pCompound) {
        super.load(pCompound);
        this.maxAge = pCompound.getInt("maxAge");
        this.damage = pCompound.getFloat("damage");
        this.age = pCompound.getInt("age");
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isAlive() && age > 1;
    }

    @Override
    public boolean mayInteract(Level pLevel, BlockPos pPos) {
        return true;
    }
}
