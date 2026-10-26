package dev.feldsher.deconstructedcore.renderdata;

/** A shown battery: how charged it is, in the quarters the model shows. */
public final class BatteryRenderInfo {
	public static final int CHARGE_STEPS = 4;
	private final float chargeRatio;

	public BatteryRenderInfo(float chargeRatio) {
		this.chargeRatio = chargeRatio;
	}

	public static BatteryRenderInfo of(int energyStored, int maxEnergyStored) {
		return new BatteryRenderInfo(maxEnergyStored <= 0 ? 0 : (float) Math.floor((float) energyStored / maxEnergyStored * CHARGE_STEPS) / CHARGE_STEPS);
	}

	public float getChargeRatio() {
		return chargeRatio;
	}
}
