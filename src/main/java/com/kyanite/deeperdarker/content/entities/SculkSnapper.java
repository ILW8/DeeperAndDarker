package com.kyanite.deeperdarker.content.entities;

import com.kyanite.deeperdarker.content.DDSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullableProblems")
public class SculkSnapper extends TamableAnimal {
    public final AnimationState idleState = new AnimationState();
    public final AnimationState attackState = new AnimationState();
    private int idleTimeout;

    public SculkSnapper(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 0.9, 12, 2, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 12).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.MOVEMENT_SPEED, 0.3).build();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DDSounds.SNAPPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return DDSounds.SNAPPER_HURT.get();
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        this.level().broadcastEntityEvent(this, (byte) 4);
        this.playSound(DDSounds.SNAPPER_BITE.get());
        return super.doHurtTarget(pEntity);
    }

    @Override
    public void tick() {
        super.tick();

        if(level().isClientSide()) {
            if(this.idleTimeout <= 0) {
                this.idleTimeout = this.random.nextInt(40, 120);
                this.idleState.start(this.tickCount);
            } else {
                this.idleTimeout--;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if(pId == 4) {
            this.idleState.stop();
            this.attackState.start(this.tickCount);
        } else {
            super.handleEntityEvent(pId);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }
}
