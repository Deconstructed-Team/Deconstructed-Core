package dev.feldsher.deconstructedcore.upgrades;

/** An upgrade that changes which upgrades its storage finds, by wrapping how it finds them. */
public interface IUpgradeAccessModifier {
	IUpgradeWrapperAccessor wrapAccessor(IUpgradeWrapperAccessor upgradeWrapperAccessor);
}
