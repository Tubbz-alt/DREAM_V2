package objects;

import utilities.Constants;

/**
 * Simple class to store whether or not a detection has been inferred for a particular scenario.
 * @author port091
 */

public class InferenceResult {

	private boolean inferred;
	private float goodness;
	
	public InferenceResult(boolean inferred, float goodness) {
		this.inferred = inferred;
		this.goodness = goodness;
	}

	@Override
	public String toString() {
		return "Inferred: " + inferred + "\tgoodness:" + Constants.decimalFormat.format(goodness);
	}
	
	public boolean isInferred() {
		return inferred;
	}

	public float getGoodness() {
		return goodness;
	}	
	
}
