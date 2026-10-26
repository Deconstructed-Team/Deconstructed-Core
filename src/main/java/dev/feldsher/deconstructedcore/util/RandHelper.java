package dev.feldsher.deconstructedcore.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RandHelper {
	private RandHelper() {}

	/** As many of the elements as asked for, or all there are, in random order. */
	public static <T> List<T> getNRandomElements(List<T> input, int numberOfElements, Random rand) {
		if (input.size() == numberOfElements) {
			return input;
		}

		List<T> randomizedList = new ArrayList<>(input);
		Collections.shuffle(randomizedList, rand);
		return new ArrayList<>(randomizedList.subList(0, Math.min(numberOfElements, randomizedList.size())));
	}

	/** One of the elements, each by its weight; empty when there are none. */
	public static <T> Optional<T> getRandomWeightedElement(Random random, List<WeightedElement<T>> weightedElements) {
		int totalWeight = 0;
		for (WeightedElement<T> weightedElement : weightedElements) {
			int weight = weightedElement.getWeight();
			if (weight < 0) {
				throw new IllegalArgumentException("Negative weight element passed in");
			}
			totalWeight += weight;
		}
		if (totalWeight == 0) {
			return Optional.empty();
		}

		int rndValue = random.nextInt(totalWeight);
		for (WeightedElement<T> weightedElement : weightedElements) {
			rndValue -= weightedElement.getWeight();
			if (rndValue < 0) {
				return Optional.of(weightedElement.getElement());
			}
		}
		return Optional.empty();
	}
}
