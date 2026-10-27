package dev.feldsher.deconstructedcore.upgrades.pump;

import dev.feldsher.deconstructedcore.api.IStorageFluidHandler;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import dev.feldsher.deconstructedcore.util.FluidContainerHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pumps the fluids its filter lets through into the storage's tanks, or out of them: from or to the fluid handlers
 * next to the storage, the fluid blocks around it, and the fluid containers in the hands of the players near it or
 * of the one carrying it.
 */
public class PumpUpgradeWrapper extends UpgradeWrapperBase<PumpUpgradeWrapper, PumpUpgradeItem> implements ITickableUpgrade {
	private static final String IS_INPUT_TAG = "isInput";
	private static final String INTERACT_WITH_HAND_TAG = "interactWithHand";
	private static final String INTERACT_WITH_WORLD_TAG = "interactWithWorld";
	private static final int DID_NOTHING_COOLDOWN_TIME = 40;
	private static final int HAND_INTERACTION_COOLDOWN_TIME = 3;
	private static final int WORLD_INTERACTION_COOLDOWN_TIME = 20;
	private static final int FLUID_HANDLER_INTERACTION_COOLDOWN_TIME = 20;
	private static final int PLAYER_SEARCH_RANGE = 3;
	private static final int PUMP_IN_WORLD_RANGE = 4;
	private static final int PUMP_IN_WORLD_RANGE_SQR = PUMP_IN_WORLD_RANGE * PUMP_IN_WORLD_RANGE;

	private long lastHandActionTime = -1;
	private final FluidFilterLogic fluidFilterLogic;
	private final IPumpUpgradeConfig pumpUpgradeConfig;

	public PumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		pumpUpgradeConfig = upgradeItem.getPumpUpgradeConfig();
		fluidFilterLogic = new FluidFilterLogic(pumpUpgradeConfig.getFilterSlots(), upgrade, this.upgradeSaveHandler);
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (isInCooldown(world)) {
			return;
		}
		setCooldown(world, storageWrapper.getFluidHandler().map(storageFluidHandler -> tick(new FilteredFluidHandler(storageFluidHandler, fluidFilterLogic), entity, world, pos))
				.orElse(DID_NOTHING_COOLDOWN_TIME));
	}

	private int tick(IStorageFluidHandler storageFluidHandler, @Nullable Entity entity, World world, BlockPos pos) {
		if (entity instanceof EntityPlayer && shouldInteractWithHand() && handleFluidContainerInHands((EntityPlayer) entity, storageFluidHandler)) {
			lastHandActionTime = world.getTotalWorldTime();
			return HAND_INTERACTION_COOLDOWN_TIME;
		}
		Optional<Integer> newCooldown = handleInWorldInteractions(storageFluidHandler, entity, world, pos);
		if (newCooldown.isPresent()) {
			return newCooldown.get();
		}
		return lastHandActionTime + 10 * HAND_INTERACTION_COOLDOWN_TIME > world.getTotalWorldTime() ? HAND_INTERACTION_COOLDOWN_TIME : DID_NOTHING_COOLDOWN_TIME;
	}

	/** The hands of the players around only for a placed storage: one that's carried works with its wearer's alone. */
	private Optional<Integer> handleInWorldInteractions(IStorageFluidHandler storageFluidHandler, @Nullable Entity entity, World world, BlockPos pos) {
		if (entity == null && shouldInteractWithHand() && handleFluidContainersInHandsOfNearbyPlayers(world, pos, storageFluidHandler)) {
			lastHandActionTime = world.getTotalWorldTime();
			return Optional.of(HAND_INTERACTION_COOLDOWN_TIME);
		}

		if (shouldInteractWithWorld()) {
			Optional<Integer> newCooldown = interactWithWorld(world, pos, storageFluidHandler);
			if (newCooldown.isPresent()) {
				return newCooldown;
			}
		}

		return interactWithAttachedFluidHandlers(world, pos, storageFluidHandler);
	}

	private Optional<Integer> interactWithAttachedFluidHandlers(World world, BlockPos pos, IStorageFluidHandler storageFluidHandler) {
		for (EnumFacing dir : EnumFacing.values()) {
			TileEntity te = world.getTileEntity(pos.offset(dir));
			if (te == null || !te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {
				continue;
			}
			IFluidHandler fluidHandler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
			if (fluidHandler != null && (isInput() ? FluidContainerHelper.moveFluid(fluidHandler, storageFluidHandler, getMaxInOut())
					: FluidContainerHelper.moveFluid(storageFluidHandler, fluidHandler, getMaxInOut()))) {
				return Optional.of(FLUID_HANDLER_INTERACTION_COOLDOWN_TIME);
			}
		}

		return Optional.empty();
	}

	private int getMaxInOut() {
		return Math.max(Fluid.BUCKET_VOLUME, pumpUpgradeConfig.getMaxInputOutput() * storageWrapper.getNumberOfSlotRows() * getAdjustedStackMultiplier());
	}

	private int getAdjustedStackMultiplier() {
		double stackMultiplier = StackUpgradeItem.getInventorySlotLimit(storageWrapper) / 64D;
		return 1 + (int) (pumpUpgradeConfig.getStackMultiplierRatio() * (stackMultiplier - 1));
	}

	private Optional<Integer> interactWithWorld(World world, BlockPos pos, IStorageFluidHandler storageFluidHandler) {
		if (isInput()) {
			return fillFromBlockInRange(world, pos, storageFluidHandler);
		}
		for (EnumFacing dir : EnumFacing.values()) {
			if (dir != EnumFacing.UP && placeFluidInWorld(world, storageFluidHandler, pos.offset(dir))) {
				return Optional.of(WORLD_INTERACTION_COOLDOWN_TIME);
			}
		}
		return Optional.empty();
	}

	private boolean placeFluidInWorld(World world, IStorageFluidHandler storageFluidHandler, BlockPos offsetPos) {
		if (!isValidForFluidPlacement(world, offsetPos)) {
			return false;
		}
		for (IFluidTankProperties tank : storageFluidHandler.getTankProperties()) {
			FluidStack tankFluid = tank.getContents();
			if (tankFluid != null && tankFluid.amount >= Fluid.BUCKET_VOLUME && fluidFilterLogic.fluidMatches(tankFluid)
					&& FluidUtil.tryPlaceFluid(null, world, offsetPos, storageFluidHandler, new FluidStack(tankFluid, Fluid.BUCKET_VOLUME))) {
				return true;
			}
		}
		return false;
	}

	/** Air, or a fluid that isn't a source. */
	private static boolean isValidForFluidPlacement(World world, BlockPos pos) {
		if (world.isAirBlock(pos)) {
			return true;
		}
		if (!isFluidBlock(world.getBlockState(pos))) {
			return false;
		}
		IFluidHandler blockFluidHandler = FluidUtil.getFluidHandler(world, pos, null);
		return blockFluidHandler == null || blockFluidHandler.drain(Integer.MAX_VALUE, false) == null;
	}

	private static boolean isFluidBlock(IBlockState state) {
		Block block = state.getBlock();
		return block instanceof IFluidBlock || state.getMaterial().isLiquid();
	}

	/** The nearest fluid source within the range, nearer ones first: the farther it is, the longer until the next. */
	private Optional<Integer> fillFromBlockInRange(World world, BlockPos basePos, IStorageFluidHandler storageFluidHandler) {
		LinkedList<BlockPos> nextPositions = new LinkedList<>();
		Set<BlockPos> searchedPositions = new HashSet<>();
		nextPositions.add(basePos);
		searchedPositions.add(basePos);

		while (!nextPositions.isEmpty()) {
			BlockPos pos = nextPositions.poll();
			if (fillFromBlock(world, pos, storageFluidHandler)) {
				return Optional.of((int) (Math.max(1, Math.sqrt(basePos.distanceSq(pos))) * WORLD_INTERACTION_COOLDOWN_TIME));
			}

			for (EnumFacing dir : EnumFacing.values()) {
				BlockPos offsetPos = pos.offset(dir);
				if (searchedPositions.add(offsetPos) && basePos.distanceSq(offsetPos) < PUMP_IN_WORLD_RANGE_SQR) {
					nextPositions.add(offsetPos);
				}
			}
		}
		return Optional.empty();
	}

	private boolean fillFromBlock(World world, BlockPos pos, IStorageFluidHandler storageFluidHandler) {
		if (!world.isBlockLoaded(pos) || !isFluidBlock(world.getBlockState(pos))) {
			return false;
		}
		IFluidHandler blockFluidHandler = FluidUtil.getFluidHandler(world, pos, null);
		return blockFluidHandler != null && FluidContainerHelper.moveFluid(blockFluidHandler, storageFluidHandler, Fluid.BUCKET_VOLUME);
	}

	private boolean handleFluidContainersInHandsOfNearbyPlayers(World world, BlockPos pos, IStorageFluidHandler storageFluidHandler) {
		AxisAlignedBB searchBox = new AxisAlignedBB(pos).grow(PLAYER_SEARCH_RANGE);
		for (EntityPlayer player : world.playerEntities) {
			if (searchBox.contains(player.getPositionVector()) && handleFluidContainerInHands(player, storageFluidHandler)) {
				return true;
			}
		}
		return false;
	}

	private boolean handleFluidContainerInHands(EntityPlayer player, IStorageFluidHandler storageFluidHandler) {
		return handleFluidContainerInHand(storageFluidHandler, player, EnumHand.MAIN_HAND) || handleFluidContainerInHand(storageFluidHandler, player, EnumHand.OFF_HAND);
	}

	/** A bucket's worth into or out of one container of what the hand holds, as by hand; never the storage itself. */
	private boolean handleFluidContainerInHand(IStorageFluidHandler storageFluidHandler, EntityPlayer player, EnumHand hand) {
		ItemStack itemInHand = player.getHeldItem(hand);
		if (itemInHand.isEmpty() || itemInHand == storageWrapper.getWrappedStorageStack()) {
			return false;
		}
		FluidActionResult result = isInput() ? FluidContainerHelper.emptyContainer(itemInHand, storageFluidHandler, Fluid.BUCKET_VOLUME, false)
				: FluidContainerHelper.fillContainer(itemInHand, storageFluidHandler, Fluid.BUCKET_VOLUME, false);
		if (!result.isSuccess()) {
			return false;
		}
		FluidContainerHelper.settleResult(player, itemInHand, result.getResult(), stack -> player.setHeldItem(hand, stack));
		return true;
	}

	private NBTTagCompound getTag() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null ? new NBTTagCompound() : tag;
	}

	private boolean getBoolean(String key, boolean defaultValue) {
		NBTTagCompound tag = getTag();
		return tag.hasKey(key) ? tag.getBoolean(key) : defaultValue;
	}

	private void setBoolean(String key, boolean value) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(key, value);
		save();
	}

	public void setIsInput(boolean input) {
		setBoolean(IS_INPUT_TAG, input);
	}

	public boolean isInput() {
		return getBoolean(IS_INPUT_TAG, true);
	}

	public FluidFilterLogic getFluidFilterLogic() {
		return fluidFilterLogic;
	}

	public void setInteractWithHand(boolean interactWithHand) {
		setBoolean(INTERACT_WITH_HAND_TAG, interactWithHand);
	}

	public boolean shouldInteractWithHand() {
		return getBoolean(INTERACT_WITH_HAND_TAG, upgradeItem.getInteractWithHandDefault());
	}

	public void setInteractWithWorld(boolean interactWithWorld) {
		setBoolean(INTERACT_WITH_WORLD_TAG, interactWithWorld);
	}

	public boolean shouldInteractWithWorld() {
		return getBoolean(INTERACT_WITH_WORLD_TAG, upgradeItem.getInteractWithWorldDefault());
	}
}
