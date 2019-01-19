package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.ScenarioSet;
import objects.ExtendedSensor;
import utilities.Constants;

/**
 * Will handle all the functions as defined in the RIMVA 9.1 Code
 * This is the function that we use by default/exclusively, at the moment
 * @author port091
 * @author whit162
 */

public class SimulatedAnnealing extends Function {

	public SimulatedAnnealing() {
	}

	public SimulatedAnnealing(MUTATE mutate) {
		this.mutate = mutate;
	}

	@Override
	public String toString() {
		return "SimulatedAnnealing";
	}
	
	/*// Inference is basically testing that enough sensors detected their threshold to count as a leak
	// The criteria for inference is set from Page_DetectionCriteria
	@Override
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, String scenario) {
		
		// Count how many of each type of sensor has triggered
		Map<String, Integer> totalByType = new HashMap<String, Integer>();
		Map<String, Integer> triggeredByType = new HashMap<String, Integer>();

		// Loop through sensors in the configuration
		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			
			// Add sensor types to lists
			if(!totalByType.containsKey(sensor.getSensorType())) {
				totalByType.put(sensor.getSensorType(), 0);
				triggeredByType.put(sensor.getSensorType(), 0);
			}
			
			// Only increment triggered totals if the current sensor is triggered
			if(sensor.isTriggering() && sensor.isTriggeredInScenario(scenario)) {
				int triggered = triggeredByType.get(sensor.getSensorType())+1;
				triggeredByType.put(sensor.getSensorType(), triggered);
			}
			
			//Count all sensors
			int count = totalByType.get(sensor.getSensorType())+1;
			totalByType.put(sensor.getSensorType(), count);
		}

		for(String type: totalByType.keySet()) {
			Constants.log(Level.FINEST, "Simulated Annealing - inference", type + " total: " + totalByType.get(type) + "\ttriggering: " + triggeredByType.get(type));
		}

		Boolean inference = set.getInferenceTest().reachedInference(triggeredByType);
		InferenceResult result = new InferenceResult(inference);

		if(inference)
			result = new InferenceResult(inference, set.getInferenceTest().calculateGoodness(totalByType, triggeredByType));

		Constants.log(Level.FINEST, "Simulated Annealing - inference", inference.toString());

		return result;

	}*/
	
	// This calculates an value describing how well the sensors detected for this configuration
	// A large penalty is given to sensors that do not detect
	@Override
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set, boolean runThreaded) {
		
		// Start a timer	
		long startTime = System.currentTimeMillis();		
		List<Thread> threads = new ArrayList<Thread>();
		// Clear out previous information
		for (ExtendedSensor sensor : configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
		}
		final int cores = Runtime.getRuntime().availableProcessors() - 1; //Use all but one core
		for(final String scenario: set.getScenarios()) {
			if(set.getScenarioWeights().get(scenario) <= 0) continue; //Skip any scenarios with a weighting of 0
			if(runThreaded) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							long startTime = System.currentTimeMillis();
							innerLoopParallel(configuration, set, scenario);
							Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				if(threads.size() < cores) {
					thread.start();
					threads.add(thread);
				}
			} else {
				try {
					startTime = System.currentTimeMillis();	
					innerLoopParallel(configuration, set, scenario);
					Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if(runThreaded) {
			for (Thread thread: threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		Constants.timer.addPerConfiguration(System.currentTimeMillis() - startTime);
		
		return configuration.getObjectiveValue();
	}
	
	
	// This basically tests that enough sensors detected their threshold to count as a leak
	// The testing criteria is set from Page_DetectionCriteria
	public void innerLoopParallel(ExtendedConfiguration configuration, ScenarioSet set, String scenario) throws Exception {
		
		boolean inferencePass = false;
		float inferenceValue = 1000000; //Default penalty for no detection
		List<HashMap<String, Integer>> activeTests = set.getInferenceTest().getActiveTests();
		List<ExtendedSensor> sensors = configuration.getExtendedSensors();
		
		// Loop through active tests to calculate the best inference time, if any
		for(Map<String, Integer> test: activeTests) { //Loop through tests
			if(sensors.size() < test.size()) continue; //Not enough sensors to complete test
			boolean testPass = true;
			float testValue = 0;
			for(String testKey: test.keySet()) { //Loop through sensors in a test
				
				// We already checked that there were enough sensors, so we don't need to do anything more here
				if(testKey.equals("Any Sensor")) continue;
				
				// Create a list of ttds for the given sensor from the test
				List<Float> ttds = listOfValidTTDs(sensors, set, scenario, testKey);
				
				// If there are not enough of this sensor, the test fails
				if(ttds.size() < test.get(testKey)) {
					testPass = false;
					break;
				}
				
				// Save the largest TTD at the minimum test requirement
				if(ttds.get(test.get(testKey)-1) > testValue)
					testValue = ttds.get(test.get(testKey)-1);
			}
			
			// The configuration passed this test
			if(testPass) {
				
				// Do a final check to confirm that the "any sensor" requirement doesn't increase the inferenceValue
				List<Float> allTTDs = listOfValidTTDs(sensors, set, scenario, "");
				if(allTTDs.size() < test.size()) continue; //Not enough detecting sensors to complete test
				if(allTTDs.get(test.size()-1) > testValue) //Save the largest TTD at the minimum "All Sensor" test requirement
					testValue = allTTDs.get(test.size()-1);
				
				// Store values globally
				inferencePass = true;
				if(testValue < inferenceValue)
					inferenceValue = testValue;
			}
		}
		
		// Store results in configuration
		if(inferencePass) {
			configuration.addTimeToDetection(scenario, inferenceValue);
			// Store triggering information for the sensors
			for(ExtendedSensor sensor: sensors) {
				String specificType = set.getSensorSettings(sensor.getSensorType()).specificType;
				Float ttd = null;
				if(sensor.getSensorType().contains("Electrical Conductivity")) //Exception because ERT comes from a different matrix
					ttd = E4DSensors.ertGetDetection(scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
				else
					ttd = set.getDetectionMap().get(specificType).get(scenario).get(sensor.getNodeNumber());
				if(ttd!= null && ttd <= inferenceValue)
					sensor.setTriggered(true, scenario, ttd, 0.0);
			}
		} else
			configuration.getTimesToDetection().remove(scenario);
		configuration.addObjectiveValue(scenario, inferenceValue*set.getGloballyNormalizedScenarioWeight(scenario));
		configuration.addInferenceResult(scenario, new InferenceResult(inferencePass, inferenceValue));
		
		/*
		InferenceResult inferenceResult = null;
		
		TimeStep ts = null;
		InferenceResult inferenceResult = null;
		for(TimeStep timeStep: set.getNodeStructure().getTimeSteps()) {
			ts = timeStep;
			for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
				String specificType = set.getSensorSettings(sensor.getSensorType()).specificType;
				Boolean triggered = null;
				if(sensor.getSensorType().contains("Electrical Conductivity")) {
					if(this.currentIteration==-3) //A hack to trigger the calculation of the best TTD for ERT (complicated because of well pairings)
						triggered = E4DSensors.ertBestSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
					else
						triggered = E4DSensors.ertSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
				} else
					triggered = sensorTriggered(set, specificType, scenario, sensor.getNodeNumber(), timeStep);
				sensor.setTriggered(triggered, scenario, timeStep.getRealTime(), 0.0);
			}
			inferenceResult = inference(configuration, set, scenario);
			if (inferenceResult.isInferred())
				break; // Stop once we've met the inference test - use this detection time
		}
		// Now store objective and inference results in the configuration
		float timeInYears = 1000000; // Default penalty for a scenario with no detection
		if (ts != null && inferenceResult.isInferred()) { // Store value if we have hit inference
			timeInYears = ts.getRealTime();
			configuration.addTimeToDetection(scenario, timeInYears);
		} else { // If no inference, no leak was detected for this scenario
			configuration.getTimesToDetection().remove(scenario);
		}
		configuration.addObjectiveValue(scenario, timeInYears * set.getGloballyNormalizedScenarioWeight(scenario));
		configuration.addInferenceResult(scenario, inferenceResult);
		*/
	}
	
	private List<Float> listOfValidTTDs(List<ExtendedSensor> sensors, ScenarioSet set, String scenario, String testType) {
		List<Float> ttds = new ArrayList<Float>();
		for(ExtendedSensor sensor: sensors) { //Loop through sensors in configuration
			Float ttd = null;
			if(sensor.getSensorType().contains("Electrical Conductivity")) //Exception because ERT comes from a different matrix
				ttd = E4DSensors.ertGetDetection(scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
			else {
				String specificType = set.getSensorSettings(sensor.getSensorType()).specificType; //Specific Type of the sensor
				ttd = set.getDetectionMap().get(specificType).get(scenario).get(sensor.getNodeNumber());
			}
			// We only want to keep detections (not null) for a given sensor that is being tested
			if(ttd!=null && sensor.getSensorType().contains(testType)) //TODO: Doesn't work with dupliate sensors, need to key to alias or specificType
				ttds.add(ttd);
		}
		Collections.sort(ttds); //Sort the TTDs, smallest to largest
		return ttds;
	}
	
	/*// This method tells the Simulated Annealing process whether sensors have been triggered
	public static Boolean sensorTriggered(ScenarioSet set, String specificType, String scenario, Integer nodeNumber, TimeStep timestep) {
		if(set.getDetectionMap().get(specificType).get(scenario).containsKey(nodeNumber)) {
			if(set.getDetectionMap().get(specificType).get(scenario).get(nodeNumber) < timestep.getRealTime()) {
				return true;
			}
		}
		return false;
	}*/
	
}
