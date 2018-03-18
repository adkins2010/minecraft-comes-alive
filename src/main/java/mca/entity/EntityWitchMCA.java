/**
 * 
 */
package mca.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import mca.core.Constants;
import mca.core.minecraft.SoundsMCA;
import mca.enums.EnumGender;
import mca.util.Utilities;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.passive.EntityFlying;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityDragonFireball;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import radixcore.modules.RadixLogic;
import radixcore.modules.RadixMath;

/**
 * @author Michael M. Adkins
 *
 */
public class EntityWitchMCA extends EntityWitch implements EntityFlying {
	// private static final DataParameter<Boolean> IS_ANGRY =
	// EntityDataManager.<Boolean>createKey(EntityWitchMCA.class,
	// DataSerializers.BOOLEAN);
	private static final UUID MODIFIER_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
	private static final DataParameter<Integer> ATTACK_STATE = EntityDataManager
			.<Integer>createKey(EntityWitchMCA.class, DataSerializers.VARINT);
	public static int counterEntity;
	private static final DataParameter<Boolean> DO_DISPLAY = EntityDataManager
			.<Boolean>createKey(EntityWitchMCA.class, DataSerializers.BOOLEAN);
	// private static final int MAX_WAIT_TIME = Time.SECOND / 2;
	private static final AttributeModifier MODIFIER = (new AttributeModifier(MODIFIER_UUID, "Drinking speed penalty",
			-0.25D, 0)).setSaved(false);
	private static final DataParameter<Integer> STATE_TRANSITION_COOLDOWN = EntityDataManager
			.<Integer>createKey(EntityWitchMCA.class, DataSerializers.VARINT);
	private EntityAINearestAttackableTarget aiNearestAttackableTarget = new EntityAINearestAttackableTarget(this,
			EntityPlayer.class, true);

	protected int burningTime;
	private int witchAttackTimer;
	private ResourceLocation texture;
	public WitchAttributes attributes;
	private Entity ridingEntity;
	private List<EntityLiving> minions;

	/**
	 * @param worldIn
	 */
	public EntityWitchMCA(World worldIn) {
		super(worldIn);
		minions = new Vector<EntityLiving>();
		setHealth(10.0f);
		attributes = new WitchAttributes(this);
		attributes.initialize();
		attributes.setGender(EnumGender.FEMALE);
		this.getRidingEntity();
	}

	/**
	 * @param worldIn
	 * @param gender
	 */
	public EntityWitchMCA(World worldIn, EnumGender gender) {
		super(worldIn);
		minions = new ArrayList<EntityLiving>();
		attributes = new WitchAttributes(this);
		attributes.initialize();
		attributes.setGender(gender);
		if (gender == EnumGender.MALE) {
			setHealth(30.0f);
		}
		else {
			setHealth(10.0f);
		}
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(ATTACK_STATE, Integer.valueOf(0));
		this.dataManager.register(STATE_TRANSITION_COOLDOWN, Integer.valueOf(0));
	}

	@Override
	protected void initEntityAI() {
		this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackRanged(this, 1.0D, 60, 10.0F));
        this.tasks.addTask(2, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(3, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
	}

	@Override
	protected final void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30F);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.5F);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(225.0F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundsMCA.villager_female_heh;
	}

	@Override
	protected SoundEvent getDeathSound() {
		if (attributes.getGender() == EnumGender.FEMALE) {
			return new Random().nextBoolean() ? SoundsMCA.evil_female_death_1 : SoundsMCA.evil_female_death_2;
		}
		else {
			return new Random().nextBoolean() ? SoundsMCA.evil_male_death_1 : SoundsMCA.evil_male_death_2;
		}
	}

	public boolean getDoDisplay() {
		return dataManager.get(DO_DISPLAY);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		if (attributes.getGender() == EnumGender.FEMALE) {
			return new Random().nextBoolean() ? SoundsMCA.villager_female_hurt_1 : SoundsMCA.villager_female_hurt_2;
		}
		else {
			return super.getHurtSound(source);
		}
	}

	@Override
	public void onDeath(DamageSource cause) {
		if (RadixLogic.getBooleanWithProbability(58)) {
			Vec3d looking = this.getLookVec();
			if (looking != null) {
				EntityLightningBolt lightning = new EntityLightningBolt(world, looking.x, looking.y, looking.z, false);
				lightning.setPosition(looking.x, looking.y, looking.z);
				world.spawnEntity(lightning);
			}
		}
		else if (RadixLogic.getBooleanWithProbability(24)) {
			EntityFireball fireball = new EntitySmallFireball(world);
			fireball.setPosition(this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ());
			Vec3d looking = this.getLookVec();
			if (looking != null) {
				fireball.motionX = looking.x;
				fireball.motionY = looking.y;
				fireball.motionZ = looking.z;
				fireball.accelerationX = fireball.motionX * 0.1D;
				fireball.accelerationY = fireball.motionY * 0.1D;
				fireball.accelerationZ = fireball.motionZ * 0.1D;
			}
			world.spawnEntity(fireball);
		}
		else if (RadixLogic.getBooleanWithProbability(12)) {
			EntityFireball fireball = new EntityLargeFireball(world);
			fireball.setPosition(this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ());
			Vec3d looking = this.getLookVec();
			if (looking != null) {
				fireball.motionX = looking.x;
				fireball.motionY = looking.y;
				fireball.motionZ = looking.z;
				fireball.accelerationX = fireball.motionX * 0.1D;
				fireball.accelerationY = fireball.motionY * 0.1D;
				fireball.accelerationZ = fireball.motionZ * 0.1D;
			}
			world.spawnEntity(fireball);
		}
		else if (RadixLogic.getBooleanWithProbability(6)) {
			EntityFireball fireball = new EntityDragonFireball(world);
			fireball.setPosition(this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ());
			Vec3d looking = this.getLookVec();
			if (looking != null) {
				fireball.motionX = looking.x;
				fireball.motionY = looking.y;
				fireball.motionZ = looking.z;
				fireball.accelerationX = fireball.motionX * 0.1D;
				fireball.accelerationY = fireball.motionY * 0.1D;
				fireball.accelerationZ = fireball.motionZ * 0.1D;
			}
			world.spawnEntity(fireball);
		}
		// EntityFireball fireball = new EntityDragonFireball(world);
		// fireball.setPosition(this.getPosition().getX(), this.getPosition().getY(),
		// this.getPosition().getZ());
		// Vec3d looking = this.getLookVec();
		// if (looking != null) {
		// fireball.motionX = looking.x;
		// fireball.motionY = looking.y;
		// fireball.motionZ = looking.z;
		// fireball.accelerationX = fireball.motionX * 0.1D;
		// fireball.accelerationY = fireball.motionY * 0.1D;
		// fireball.accelerationZ = fireball.motionZ * 0.1D;
		// }
		// world.spawnEntity(fireball);
		Utilities.spawnParticlesAroundPointS(EnumParticleTypes.CLOUD, world, posX, posY, posZ, 10);
		for (EntityLiving minion : minions) {
			if (!minion.isDead && minion instanceof EntityOcelot) {
				// minions.remove(minion);
				EntityOcelot cat = (EntityOcelot) minion;
				cat.setTamed(false);
				cat.setOwnerId(null);
			}
		}
		super.onDeath(cause);
	}

	// @Override
	// public void onUpdate() {
	// super.onUpdate();
	// for (EntityLiving minion : minions) {
	// if (minion.isDead) {
	// minions.remove(minion);
	// }
	// }
	// }

	/**
	 * 
	 * @see net.minecraft.entity.monster.EntityWitch#onLivingUpdate()
	 */
	@Override
	public void onLivingUpdate() {
		if (world.isDaytime()) {
			float f = getBrightness();
			if (f > 0.5F && world.canBlockSeeSky(this.getPosition()) && rand.nextFloat() * 30F < (f - 0.4F) * 2.0F) {
				super.setHealth(super.getHealth() - 0.01f);
			}
		}
		else {
			float f = getBrightness();
			if (f < 0.5F && rand.nextFloat() * 30F < (f - 0.4F) * 2.0F) {
				super.setHealth(super.getHealth() + 0.01f);
			}
		}
		if (!this.world.isRemote) {
			if (this.isDrinkingPotion()) {
				if (this.witchAttackTimer-- <= 0) {
					this.setAggressive(false);
					ItemStack itemstack = this.getHeldItemMainhand();
					this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);

					if (itemstack.getItem() == Items.POTIONITEM) {
						List<PotionEffect> list = PotionUtils.getEffectsFromStack(itemstack);

						if (list != null) {
							for (PotionEffect potioneffect : list) {
								this.addPotionEffect(new PotionEffect(potioneffect));
							}
						}
					}
					this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(MODIFIER);

				}
				else {
					for (int i = 0; i < minions.size(); i++) {
						EntityLiving minion = minions.get(i);
						if (!minion.isDead) {
							if (minion.getNavigator().noPath()) {
								minion.getNavigator().tryMoveToEntityLiving(this, Constants.SPEED_WALK);
							}
							else {
								List<EntityVillagerMCA> villagers = RadixLogic
										.getEntitiesWithinDistance(EntityVillagerMCA.class, this, 10);
								if (villagers != null && villagers.size() > 0) {
									EntityVillagerMCA villager = villagers
											.get(RadixMath.getNumberInRange(0, villagers.size() - 1));
									minion.setAttackTarget(villager);
								}
							}
						}
						else {
							minions.remove(i);
							EntityOcelot cat = new EntityOcelot(world);
							cat.setPosition(this.posX, this.posY, this.posZ);
							cat.setTamed(false);
							cat.setOwnerId(this.getUniqueID());
							cat.setTameSkin(1);
							EntityAIBase aiFollowOwner = new EntityAIFollowOwner(cat, 1.0D, 10.0F, 2.0F);
							cat.tasks.addTask(1, aiFollowOwner);
							cat.setCustomNameTag(String.format("%s's cat", this.getName()));
							world.spawnEntity(cat);
							// this.addMinion(cat);
							minions.add(i, cat);
						}
					}
				}
			}
			else {
				PotionType potiontype = null;

				if (this.rand.nextFloat() < 0.15F && this.isInsideOfMaterial(Material.WATER)
						&& !this.isPotionActive(MobEffects.WATER_BREATHING)) {
					potiontype = PotionTypes.WATER_BREATHING;
				}
				else if (this.rand.nextFloat() < 0.15F
						&& (this.isBurning()
								|| this.getLastDamageSource() != null && this.getLastDamageSource().isFireDamage())
						&& !this.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
					potiontype = PotionTypes.FIRE_RESISTANCE;
				}
				else if (this.rand.nextFloat() < 0.05F && this.getHealth() < this.getMaxHealth()) {
					potiontype = PotionTypes.HEALING;
				}
				else if (this.rand.nextFloat() < 0.5F && this.getAttackTarget() != null
						&& !this.isPotionActive(MobEffects.SPEED)
						&& this.getAttackTarget().getDistanceSqToEntity(this) > 121.0D) {
					potiontype = PotionTypes.SWIFTNESS;
				}

				if (potiontype != null) {
					this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
							PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), potiontype));
					this.witchAttackTimer = this.getHeldItemMainhand().getMaxItemUseDuration();
					this.setAggressive(true);
					this.playSound(this.getAmbientSound(), 1.0f, 0.9f);
				}
			}

			if (this.rand.nextFloat() < 7.5E-4F) {
				this.world.setEntityState(this, (byte) 15);
			}
		}

		super.onLivingUpdate();
	}
//
//	/**
//	 * @param angry
//	 *            the anger to set
//	 */
//	public void setAngry(boolean angry) {
//		this.getDataManager().set(IS_ANGRY, Boolean.valueOf(angry));
//	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		counterEntity = nbttagcompound.getInteger("CounterEntity");
	}

	public void setDoDisplay(boolean value) {
		dataManager.set(DO_DISPLAY, value);
	}

	public void setEntityDead() {
		counterEntity--;
		super.setDead();
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setInteger("CounterEntity", counterEntity);
	}

	/**
	 * @return the texture
	 */
	public ResourceLocation getTexture() {
		return texture;
	}

	/**
	 * @param texture
	 *            the texture to set
	 */
	public void setTexture(ResourceLocation texture) {
		this.texture = texture;
	}

	/**
	 * @return the ridingEntity
	 */
	@Override
	public Entity getRidingEntity() {
		return this.ridingEntity;
	}

	/**
	 * @param ridingEntity
	 *            the ridingEntity to set
	 */
	public void setRidingEntity(Entity ridingEntity) {
		this.ridingEntity = ridingEntity;
	}

	@Override
	public void updateRidden() {
		Entity ride = this.getRidingEntity();

		if (this.isRiding() && ride.isDead) {
			this.dismountRidingEntity();
		}
		else {
			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
			if (!updateBlocked)
				this.onUpdate();

			if (this.isRiding()) {
				ride.updatePassenger(this);
			}
		}
	}

	@Override
	public void dismountRidingEntity() {
		if (this.ridingEntity != null) {
			Entity ride = this.ridingEntity;
			if (!net.minecraftforge.event.ForgeEventFactory.canMountEntity(this, ride, false))
				return;
			this.ridingEntity = null;
			super.dismountRidingEntity();
		}
	}

	/**
	 * @param minion
	 */
	public synchronized void addMinion(EntityLiving minion) {
		minion.setHealth(getHealth());
		minions.add(minion);
	}

	protected synchronized List<EntityLiving> getMinions() {
		return minions;
	}
}
