package com.kyanite.deeperdarker.content.entities;

import com.kyanite.deeperdarker.content.DDSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class SculkSnapper extends TamableAnimal {
    public final AnimationState idleState = new AnimationState();
    public final AnimationState attackState = new AnimationState();

    public SculkSnapper(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 0.9, 8, 2, false));
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
        return DDSounds.SNAPPER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return DDSounds.SNAPPER_HURT;
    }

    @Override
    public MobType getMobType() {
        return DDMobType.SCULK;
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        level().broadcastEntityEvent(this, (byte) 4);
        this.playSound(DDSounds.SNAPPER_BITE);
        return super.doHurtTarget(pEntity);
    }

    @Override
    public void tick() {
        super.tick();

        if(this.isTame() && this.getOwner() != null) {
            if(this.getOwner().distanceTo(this) < 6 && this.random.nextFloat() < 0.0004f) {
                List<Enchantment> enchantments = new ArrayList<>();
                BuiltInRegistries.ENCHANTMENT.forEach(enchant -> {
                    if(!enchant.isCurse()) enchantments.add(enchant);
                });
                Enchantment enchantment1 = enchantments.remove(this.random.nextInt(enchantments.size()));
                Enchantment enchantment2 = enchantments.get(this.random.nextInt(enchantments.size()));

                ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment1, this.random.nextInt(1, enchantment1.getMaxLevel() + 1)));
                if(this.random.nextFloat() < 0.2f) EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment2, 1));
                if(!book.isEmpty()) this.level().addFreshEntity(new ItemEntity(this.level(), this.blockPosition().getX(), this.blockPosition().getY(), this.blockPosition().getZ(), book));
            }
        }

        if(level().isClientSide()) {
            if(!this.attackState.isStarted() && !this.idleState.isStarted()) {
                this.idleState.start(this.tickCount);
            }
        }
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.NETHERITE_CHESTPLATE) && !pStack.getEnchantmentTags().isEmpty();
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        if(this.isFood(stack) && !this.isTame()) {
            this.usePlayerItem(pPlayer, pHand, stack);
            if(!level().isClientSide()) {
                this.tame(pPlayer);
                this.setOwnerUUID(pPlayer.getUUID());
                setTarget(null);
                level().broadcastEntityEvent(this, (byte) 18);
            }

            return InteractionResult.SUCCESS;
        }

        if(!stack.getEnchantmentTags().isEmpty() && isTame() && this.getHealth() < this.getMaxHealth()) {
            this.usePlayerItem(pPlayer, pHand, stack);
            if(!level().isClientSide()) {
                this.heal(getMaxHealth());
                this.gameEvent(GameEvent.EAT, this);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if(pId == 4) {
            this.idleState.stop();
            this.attackState.start(this.tickCount);
        } else if(pId == 18) {
            for(int i = 0; i < 7; i++) {
                double sX = this.random.nextGaussian() * 0.02;
                double sY = this.random.nextGaussian() * 0.02;
                double sZ = this.random.nextGaussian() * 0.02;
                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1), this.getRandomY() + 0.5, this.getRandomZ(1), sX, sY, sZ);
            }
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
