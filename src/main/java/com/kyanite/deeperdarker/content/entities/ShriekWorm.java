package com.kyanite.deeperdarker.content.entities;

import com.kyanite.deeperdarker.content.DDBlocks;
import com.kyanite.deeperdarker.content.DDSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation, NullableProblems")
public class ShriekWorm extends Monster {
    private static final EntityDataAccessor<Integer> IDLE_TIMER = SynchedEntityData.defineId(ShriekWorm.class, EntityDataSerializers.INT);
    public final AnimationState idleState = new AnimationState();
    public final AnimationState attackState = new AnimationState();
    public final AnimationState asleepState = new AnimationState();
    public final AnimationState emergeState = new AnimationState();
    public final AnimationState descendState = new AnimationState();

    public ShriekWorm(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 0, true));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.ATTACK_KNOCKBACK, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).build();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DDSounds.SHRIEK_WORM_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DDSounds.SHRIEK_WORM_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return DDSounds.SHRIEK_WORM_HURT;
    }

    @Override
    public MobType getMobType() {
        return DDMobType.SCULK;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IDLE_TIMER, getRandom().nextInt(160, 300));
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        this.level().broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(pEntity);
    }

    @Override
    public void tick() {
        super.tick();

        if(level().isClientSide()) {
            if(this.idleState.isStarted()) {
                this.entityData.set(IDLE_TIMER, this.entityData.get(IDLE_TIMER) - 1);

                if(this.entityData.get(IDLE_TIMER) <= 0) {
                    this.idleState.stop();
                    if(this.random.nextFloat() < 1f) {
                        this.asleepState.start(this.tickCount);
                    }
                    else this.descendState.start(this.tickCount); // chance for descend is 0 bc kill function is broken
                }
            } else if(!this.asleepState.isStarted() && !this.emergeState.isStarted() && !this.descendState.isStarted()) {
                if(!this.attackState.isStarted()) {
                    this.idleState.start(this.tickCount);
                } else if(this.entityData.get(IDLE_TIMER) < 160) {
                    this.entityData.set(IDLE_TIMER, getRandom().nextInt(160, 300));
                }
            }

            if(this.getPose() == Pose.EMERGING) {
                double sX = this.random.nextGaussian() * 0.02;
                double sY = this.random.nextGaussian() * 0.02;
                double sZ = this.random.nextGaussian() * 0.02;
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, this.getBlockStateOn()), this.getX() - this.random.nextDouble(), this.getY() + 1, this.getZ() - this.random.nextDouble(), sX, sY, sZ);
            }
        }

        if(this.noActionTime > 156) this.setPose(Pose.STANDING);

        Player player = level().getNearestPlayer(this, 3);
        if(player == null || player.isDeadOrDying() || player.isCreative()) {
            if(this.attackState.isStarted()) {
                this.attackState.stop();
                this.idleState.start(this.tickCount);
            }
        }

        /*if(this.descendState.isStarted()) {
            this.entityData.set(IDLE_TIMER, this.entityData.get(IDLE_TIMER) - 1);
            if(this.entityData.get(IDLE_TIMER) <= -90) {
                level().setBlock(this.getOnPos(), DDBlocks.INFESTED_SCULK.defaultBlockState(), 3);
                // TODO: kill does not work... make it work (change descent chance once fixed)
                this.kill();
                this.remove(RemovalReason.KILLED);
            }
        }*/
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if(pId == 4) {
            this.idleState.stop();
            this.asleepState.stop();
            this.attackState.start(this.tickCount);
        } else {
            super.handleEntityEvent(pId);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if(pKey.equals(DATA_POSE)) {
            if(this.getPose() == Pose.EMERGING) this.emergeState.start(this.tickCount);
            if(this.getPose() == Pose.STANDING) this.emergeState.stop();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        if(pReason == MobSpawnType.TRIGGERED) this.setPose(Pose.EMERGING);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {
        this.setDeltaMovement(Vec3.ZERO);
    }
}
