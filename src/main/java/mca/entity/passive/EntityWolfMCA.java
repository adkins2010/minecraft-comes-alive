/**
 * 
 */
package mca.entity.passive;

import java.util.UUID;

import mca.core.Constants;
import mca.core.MCA;
import mca.data.NBTPlayerData;
import mca.entity.PetAttributes;
import mca.enums.EnumGender;
import mca.enums.EnumRace;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLLog;

/**
 * @author Michael M. Adkins
 *
 */
public class EntityWolfMCA extends EntityWolf implements EntityPet {
	// private static final DataParameter<Boolean> BABY =
	// EntityDataManager.<Boolean>createKey(EntityAgeable.class,
	// DataSerializers.BOOLEAN);
	// private EnumGender ownerGender = EnumGender.UNASSIGNED;
	// private EnumRace ownerRace = EnumRace.Villager;
	boolean ownedByPlayer = true;
	public PetAttributes attributes;
	Entity rider;
	/**
	 * @param worldIn
	 */
	public EntityWolfMCA(World worldIn) {
		super(worldIn);
		attributes = new PetAttributes(this);
		attributes.initialize();
		setSitting(false);
	}

	/**
	 * @param worldIn
	 * @param owner
	 */
	public EntityWolfMCA(World worldIn, EntityPlayer owner) {
		super(worldIn);
		this.setOwnerId(owner.getUniqueID());
		attributes = new PetAttributes(this);
		attributes.initialize();
		NBTPlayerData playerData = MCA.getPlayerData(owner);
		ownedByPlayer = true;
		EnumGender ownerGender = playerData.getGender();
		EnumRace ownerRace = playerData.getRace();
		attributes.setGender(ownerGender);
		attributes.setName(getName());
		if (ownerGender == EnumGender.FEMALE) {
			setTamed(false);
			super.setCollarColor(EnumDyeColor.PINK);
		}
		else if (ownerRace == EnumRace.Orc) {
			setTamed(false);
			super.setCollarColor(EnumDyeColor.BROWN);
		}
		else if (ownerRace == EnumRace.Elf) {
			setTamed(false);
			super.setCollarColor(EnumDyeColor.GREEN);
		}
		else {
			setTamed(true);
			super.setCollarColor(EnumDyeColor.BLUE);
		}
		setSitting(false);
	}

	/**
	 * @param worldIn
	 * @param owner
	 */
	public EntityWolfMCA(World worldIn, EntityVillagerMCA owner) {
		super(worldIn);
		this.setOwnerId(owner.getUniqueID());
		attributes = new PetAttributes(this);
		attributes.initialize();
		ownedByPlayer = false;
		if (owner.attributes.getGender() == EnumGender.FEMALE) {
			super.setCollarColor(EnumDyeColor.PINK);
		}
		else if (owner.attributes.getRace() == EnumRace.Orc) {
			super.setCollarColor(EnumDyeColor.BROWN);
		}
		else if (owner.attributes.getRace() == EnumRace.Elf) {
			super.setCollarColor(EnumDyeColor.GREEN);
		}
		else {
			super.setCollarColor(EnumDyeColor.BLUE);
		}
	}

	public boolean isOwnedByPlayer() {
		return ownedByPlayer;
	}

	/**
	 * @return the ownerGender
	 */
	public EnumGender getOwnerGender() {
		EnumGender ownerGender;
		if (ownedByPlayer) {
			NBTPlayerData playerData = MCA.getPlayerData((EntityPlayer) getOwner());
			ownerGender = playerData.getGender();
		}
		else {
			EntityVillagerMCA owner = getVillagerOwnerInstance();
			ownerGender = owner.attributes.getGender();
		}
		return ownerGender;
	}


	@Override
	public EntityVillagerMCA getVillagerOwnerInstance() {
		if (this.getOwnerId() != null && this.getOwnerId() != Constants.EMPTY_UUID) {
			for (Object obj : world.loadedEntityList) {
				if (obj instanceof EntityVillagerMCA) {
					EntityVillagerMCA villager = (EntityVillagerMCA) obj;

					if (villager.getUniqueID().equals(getOwnerId())) {
						return villager;
					}
				}
			}
		}

		return null;
	}

	@Override
	public EntityPlayer getOwnerPlayer() {
		try {
			UUID uuid = this.getOwnerId();
			return uuid == null || uuid == Constants.EMPTY_UUID ? null : this.world.getPlayerEntityByUUID(uuid);
		}
		catch (IllegalArgumentException var2) {
			return null;
		}
	}

	@Override
	public EntityLivingBase getOwner() {
		if (ownedByPlayer) {
			return getOwnerPlayer();
		}
		return getVillagerOwnerInstance();
	}

	/**
	 * @param owner
	 */
	@Override
	public void setOwner(EntityLivingBase owner) {
		this.setOwnerId(owner.getUniqueID());
		ownedByPlayer = owner instanceof EntityPlayerMP;
		// if (getOwnerGender() == EnumGender.FEMALE) {
		// super.setCollarColor(EnumDyeColor.PINK);
		// }
		// else if (getOwnerRace() == EnumRace.Orc) {
		// super.setCollarColor(EnumDyeColor.BROWN);
		// }
		// else if (getOwnerRace() == EnumRace.Elf) {
		// super.setCollarColor(EnumDyeColor.GREEN);
		// }
		// else {
		// super.setCollarColor(EnumDyeColor.BLUE);
		// }
	}

	// @Override
	// public void onLivingUpdate() {
	// if (getOwner() == null || getOwner().isDead) {
	// setSitting(false);
	// EntityChicken chicken = RadixLogic.getClosestEntityExclusive(this, 15,
	// EntityChicken.class);
	// if (chicken != null) {
	// setAttackTarget(chicken);
	// }
	// else {
	// EntityMob monster = RadixLogic.getClosestEntityExclusive(this, 5,
	// EntityMob.class);
	// if (monster != null) {
	// setAttackTarget(monster);
	// }
	// }
	// }
	// super.onLivingUpdate();
	// }

	@Override
	public boolean isChild() {
		if (ownedByPlayer) {
			return super.isChild();
		}
		else {
			EntityVillagerMCA owner = getVillagerOwnerInstance();
			if (owner != null) {
				return owner.attributes.getIsChild();
			}
		}
		return super.isChild();
	}

	@Override
	public int getGrowingAge() {
		if (ownedByPlayer) {
			return super.getGrowingAge();
		}
		else {
			EntityVillagerMCA owner = getVillagerOwnerInstance();
			if (owner != null) {
				return owner.attributes.getIsChild() ? -100 : super.growingAge;
			}
		}
		return super.growingAge;
	}

	/**
	 * @see net.minecraft.entity.passive.EntityWolf#onLivingUpdate()
	 */
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		EntityVillagerMCA owner = getVillagerOwnerInstance();
		if (owner != null) {
			if (this.getNavigator().noPath()) {
				this.getNavigator().tryMoveToEntityLiving(owner, Constants.SPEED_WALK);
			}
			this.setGrowingAge(owner.getGrowingAge());
		}
		else {
			setSitting(false);
		}
	}

	/**
	 * @see net.minecraft.entity.passive.EntityTameable#onDeath(net.minecraft.util.DamageSource)
	 */
	@Override
	public void onDeath(DamageSource damageSource) {
		try {
			super.onDeath(damageSource);
		}
		catch (Exception e) {
			String msg = String.format("Exception occurred!%nMessage: %s%n", e.getLocalizedMessage());
			FMLLog.severe(msg, e);
			// java.util.logging.LogManager.getLogManager().getLogger(this.getClass().getName()).severe(msg);
			org.apache.logging.log4j.LogManager.getLogger(this.getClass().getName()).error(msg, e);
			// java.util.logging.Logger.getLogger(this.getClass().getName()).severe(msg);
		}
		if (!ownedByPlayer) {
			EntityVillagerMCA owner = getVillagerOwnerInstance();
			if (owner != null) {
				owner.setPet(null);
			}
		}
	}

	/**
	 * @see net.minecraft.entity.passive.EntityTameable#setOwnerId(java.util.UUID)
	 */
	@Override
	public void setOwnerId(UUID uniqueId) {
		super.setOwnerId(uniqueId);

	}

	/**
	 * @see net.minecraft.entity.Entity#addPassenger(net.minecraft.entity.Entity)
	 */
	@Override
	protected void addPassenger(Entity passenger) {
		if (passenger.getRidingEntity() != this) {
			if (passenger instanceof EntityVillagerMCA) {
				((EntityVillagerMCA) passenger).setRidingEntity(this);
			}
		}
		super.addPassenger(passenger);
	}

	/**
	 * 
	 */
	public void removeRider() {
		super.removePassenger(rider);
	}

	/**
	 * @param rider
	 *            of the wolf's back
	 */
	public void setRider(Entity rider) {
		this.rider = rider;
		addPassenger(rider);
	}
}
