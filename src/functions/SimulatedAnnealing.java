package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
	
	// This calculates an value describing how well the sensors detected for this configuration
	// A large penalty is given to sensors that do not detect
	@Override
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set, boolean runThreaded) {
		
		// Start a timer	
		long startTime = System.currentTimeMillis();		
		// Clear out previous information
		for (ExtendedSensor sensor: configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
		}
		
		if(runThreaded) { //Threaded
		    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
		    for(final String scenario: set.getScenarios()) {
		    	if(set.getScenarioWeights().get(scenario) <= 0) continue; //Skip any scenarios with a weighting of 0
		    	executor.execute(new Runnable() {
		    		@Override
		    		public void run() {
		    			try {
							innerLoopParallel(configuration, set, scenario);
						} catch (Exception e) {
							e.printStackTrace();
						}
		    		}
		    	});
		    }
		    awaitTerminationAfterShutdown(executor);
			
		} else { //Not threaded
			for(final String scenario: set.getScenarios()) {
				if(set.getScenarioWeights().get(scenario) <= 0) continue; //Skip any scenarios with a weighting of 0
				try {
					innerLoopParallel(configuration, set, scenario);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		Constants.timer.addPerConfiguration(System.currentTimeMillis() - startTime);
		return configuration.getObjectiveValue();
	}
	
	
	public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
	    threadPool.shutdown();
	    try {
	        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
	            threadPool.shutdownNow();
	        }
	    } catch (InterruptedException ex) {
	        threadPool.shutdownNow();
	        Thread.currentThread().interrupt();
	    }
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
			// Pre-check: determine the minimum sensor requirement to verify valid configuration
			int minReq = 0;
			for(int count: test.values()) minReq += count;
			if(sensors.size() < minReq) continue; //Not enough sensors to complete test
			// Primary check: determine that sensor types match up to verify valid configuration
			boolean testPass = true;
			float testValue = 0;
			for(String testKey: test.keySet()) { //Loop through sensors in a test
				
				// We already checked that there were enough technologies, so we don't need to do anything more here
				if(testKey.equals("Any Technology")) continue;
				
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
				
				// Do a final check to confirm that the "Any Technology" requirement doesn't increase the inferenceValue
				List<Float> allTTDs = listOfValidTTDs(sensors, set, scenario, "");
				if(allTTDs.size() < minReq) continue; //Not enough detecting sensors to complete test
				if(allTTDs.get(minReq-1) > testValue) //Save the largest TTD at the minimum "All Sensor" test requirement
					testValue = allTTDs.get(minReq-1);
				
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
	
}
