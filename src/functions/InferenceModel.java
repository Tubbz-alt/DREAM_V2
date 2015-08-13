package functions;

import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.Scenario;
import objects.ScenarioSet;

public interface InferenceModel {

	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, Scenario scenario);
}
