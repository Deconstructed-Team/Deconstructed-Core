package dev.feldsher.deconstructedcore.api;

import dev.feldsher.deconstructedcore.upgrades.FilterLogic;

import java.util.Optional;

public interface IIOFilterUpgrade {
	Optional<FilterLogic> getInputFilter();

	Optional<FilterLogic> getOutputFilter();
}
