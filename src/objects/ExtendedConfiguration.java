package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Constants.ModelOption;
import utilities.Point3f;


public class ExtendedConfiguration extends Configuration {
	
	private List<Well> wells;

	// Weighted with penalty for scenarios that do not detect
	private Map<Scenario, Float> objectiveValues;
	
	
	private Map<Scenario, InferenceResult> inferenceResults;

	public ExtendedConfiguration() {
		this(false);
	}

	public ExtendedConfiguration(boolean copy) {

		wells = Collections.synchronizedList(new ArrayList<Well>());
		sensors = Collections.synchronizedList(new ArrayList<Sensor>());
		timesToDetection = Collections.synchronizedMap(new HashMap<Scenario, Float>());
		objectiveValues = Collections.synchronizedMap(new HashMap<Scenario, Float>());		
		inferenceResults = Collections.synchronizedMap(new HashMap<Scenario, InferenceResult>());

		if(!copy) {
			Constants.log(Level.INFO, "Sensor configuration: initialized", null);
			Constants.log(Level.CONFIG, "Sensor configuration: configuration", this);
		} else {
			// Could log a message saying we're making a copy?
		}
	}	

	public synchronized void matchConfiguration(ScenarioSet set, ExtendedConfiguration toDuplicate) {

		if(!toDuplicate.wells.isEmpty() && toDuplicate.wells.get(0) instanceof RealizedWell) {
			this.sensors.clear();
			for(Well well: toDuplicate.wells) {
				addRealizedWell((RealizedWell)well);
			}
		} else {
			this.sensors.clear();
			for(ExtendedSensor sensor: toDuplicate.getExtendedSensors()) {
				this.addSensor(set, sensor.makeCopy());
			}
		}

		timesToDetection.clear();
		objectiveValues.clear();
		inferenceResults.clear();

		if(toDuplicate.getTimesToDetection() != null) {
			for(Scenario key: toDuplicate.getTimesToDetection().keySet()) {
				addTimeToDetection(key, toDuplicate.getTimesToDetection().get(key));
			}
		}

		if(toDuplicate.getTimesToDetection() != null) {
			for(Scenario key: toDuplicate.objectiveValues.keySet()) {
				addObjectiveValue(key, toDuplicate.objectiveValues.get(key));
			}
		}

		if(toDuplicate.inferenceResults != null) {		
			for(Scenario key: toDuplicate.inferenceResults.keySet()) {
				addInferenceResult(key, toDuplicate.inferenceResults.get(key));
			}
		}
	}


	public synchronized ExtendedConfiguration makeCopy(ScenarioSet set) {

		ExtendedConfiguration copy = new ExtendedConfiguration(true);

		// Copy sensors, this will add the wells
		for(ExtendedSensor sensor: getExtendedSensors()) {
			ExtendedSensor sensorCopy = sensor.makeCopy();
			copy.addSensor(set, sensorCopy);
		}
		
		if(!wells.isEmpty() && wells.get(0) instanceof RealizedWell) {
			for(Well well: wells) {
				copy.addRealizedWell((RealizedWell)well);
			}
		} else {
			for(ExtendedSensor sensor: getExtendedSensors()) {
				ExtendedSensor sensorCopy = sensor.makeCopy();
				copy.addSensor(set, sensorCopy);
			}
		}
		
		copy.timesToDetection = new HashMap<Scenario, Float>(timesToDetection);
		copy.objectiveValues = new HashMap<Scenario, Float>(objectiveValues);
		copy.inferenceResults = new HashMap<Scenario, InferenceResult>(inferenceResults);
/*
		// Copy in ttd and objective values?
		for(Scenario key: getTimesToDetection().keySet()) {
			copy.addTimeToDetection(key, timesToDetection.get(key));
		}

		for(Scenario key: objectiveValues.keySet()) {
			copy.addObjectiveValue(key, objectiveValues.get(key));
		}

		for(Scenario key: inferenceResults.keySet()) {
			copy.addInferenceResult(key, inferenceResults.get(key));
		}
*/
		return copy;		
	}


	@Override
	public String toString() {
		StringBuilder toString = new StringBuilder();
		toString.append("Configuration: ");
		if(wells.isEmpty()) {
			toString.append("Empty\n");
			return toString.toString();
		}

		toString.append("\nWells:\n");
		for(Well well: wells) {
			toString.append("\n\tWell at " + well.toString() + "\n");
			for(ExtendedSensor sensor: well.getSensors()) {
				toString.append("\t\t" + sensor.toString() + "\n");
			}
		}

		toString.append("\n");
		
		Map<String, Scenario> sorted = new TreeMap<String, Scenario>();
		for(Scenario key: getTimesToDetection().keySet()) {
			sorted.put(key.getScenario(), key);
		}

		for(String scenario: sorted.keySet()) {
			float ttd = getTimesToDetection().get(sorted.get(scenario));
			String ttdStr = ttd > 10000 ? Constants.exponentialFormat.format(ttd) : Constants.decimalFormat.format(ttd);
			toString.append("\tTime to detection for " + sorted.get(scenario).toString() + ": " + ttdStr + "\n");
		}
		
		sorted.clear();
		for(Scenario key: objectiveValues.keySet()) {
			sorted.put(key.getScenario(), key);
		}

		for(String scenario: sorted.keySet()) {
			float obj = objectiveValues.get(sorted.get(scenario));
			String objStr = obj > 10000 ? Constants.exponentialFormat.format(obj) : Constants.decimalFormat.format(obj);			
			toString.append("\tObjective value for " + sorted.get(scenario).toString() + ": " + objStr + "\n");
		}

		sorted.clear();
		for(Scenario key: inferenceResults.keySet()) {
			sorted.put(key.getScenario(), key);
		}
		
		for(String scenario: sorted.keySet()) {

			toString.append("\tInference result for " + sorted.get(scenario).toString() + ": " + inferenceResults.get(sorted.get(scenario)).toString() + "\n");
		}

		return toString.toString();
	}

	public List<ExtendedSensor> getExtendedSensors() {
		ArrayList<ExtendedSensor> copyOfSensors = new ArrayList<ExtendedSensor>();
		for(Sensor sensor: sensors) {
			if(sensor instanceof ExtendedSensor)
			copyOfSensors.add((ExtendedSensor)sensor);
		}
		return copyOfSensors;
	}

	public List<Well> getWells() {
		List<Well> copyOfWells = new ArrayList<Well>();
		for(Well well: wells)
			copyOfWells.add(well);
		return copyOfWells;
	}

	public synchronized void Clear() {
		objectiveValues.clear();
		timesToDetection.clear();
		inferenceResults.clear();
	}

	public synchronized void addObjectiveValue(Scenario scenario, float timeInYears) {
		objectiveValues.put(scenario, timeInYears);
	}

	public synchronized void addTimeToDetection(Scenario scenario, float timeToDetection) {
		timesToDetection.put(scenario, timeToDetection);
	}

	public synchronized void addInferenceResult(Scenario scenario, InferenceResult inferenceResult) {
		inferenceResults.put(scenario, inferenceResult);
	}

	public synchronized void ClearSensors() {
		sensors.clear();
	}

	
	/**
	 * Returns the average objective value of all scenarios
	 * 
	 * A penalty has been applied for scenarios that do not detect
	 * 
	 * This value has been weighted
	 * 
	 */
	public synchronized float getObjectiveValue() {
		float sum = 0;
		for(Scenario scenario: getObjectiveValues().keySet()) {
			sum += getObjectiveValues().get(scenario); // No weights here
		}
		return sum;
	}
	
	/**
	 * Returns the absolute time to detection of the triggering scenarios
	 * 
	 * This value does not include any weights or penalties
	 * 
	 */
	public synchronized float getAbsoluteTimeToDetection() {
		float sum = 0;
		for(Scenario scenario: getTimesToDetection().keySet()) {
			sum += getTimesToDetection().get(scenario); // No weights here
		}
		return sum;
	}
		
	/**
	 * Returns the average time to detection of the triggering scenarios
	 * 
	 * This value has been normalized against all detecting scenarios
	 * 
	 */
	public synchronized float getNormalizedAverageTimeToDetection(Map<Scenario, Float> scenarioWeights) {	
		float sum = 0;
		float totalWeight = 0;
		for(Scenario scenario: getTimesToDetection().keySet()) {
			sum += getTimesToDetection().get(scenario) * scenarioWeights.get(scenario);
			totalWeight += scenarioWeights.get(scenario);
		}
		return sum / totalWeight;
	}
	
	/**
	 * Returns the average time to detection of the triggering scenarios
	 * 
	 * This value has been normalized against all detecting scenarios
	 * 
	 */
	public synchronized float getNormalizedPercentScenariosDetected(Map<Scenario, Float> scenarioWeights, float totalScenarioWeights) {
		float detectedWeight = 0;
		for(Scenario scenario: getTimesToDetection().keySet()) {
			detectedWeight += scenarioWeights.get(scenario);
		}
		return detectedWeight / totalScenarioWeights;
	}
	
	
	public synchronized List<Integer> getSensorPositions(String type) {
		List<Integer> positions = new ArrayList<Integer>();
		for(Sensor sensor: sensors) {
			if(sensor.getSensorType().equals(type))
				positions.add(sensor.getNodeNumber());
		}
		return positions;
	}
	/*
	 * Will return a list of sensor positions excluding the sensor passed in
	 */
	public synchronized List<Integer> getSensorPositions(ExtendedSensor current) {
		List<Integer> positions = new ArrayList<Integer>();
		for(Sensor sensor: sensors) {
			if(sensor.equals(current))	// Skip this one
				continue;
			if(sensor.getSensorType().equals(current.getSensorType()))
				positions.add(sensor.getNodeNumber());
		}
		return positions;
	}

	public float getCost(ScenarioSet set) {
		// Get the cost
		float totalCost = 0;
		for(Sensor sensor: sensors) {			
			totalCost += set.getCost(sensor.getSensorType());
		}
		return totalCost;
	}

	public String getSummary() {
		List<String> ijs = new ArrayList<String>();

		StringBuffer nodePositions = new StringBuffer();
		if(sensors.isEmpty())
			return "Empty";		
		for(Sensor sensor: sensors) {
			char type = sensor.getSensorType().charAt(0);
			nodePositions.append(sensor.getNodeNumber() + "" + type + ", ");
			String IJ = sensor.getIJK().getI() + "_" + sensor.getIJK().getJ();
			if(!ijs.contains(IJ))
				ijs.add(IJ);
		}
		Collections.sort(ijs);
		String nodes = "\t[" + nodePositions.toString().substring(0, nodePositions.toString().length()-2) + "]";
		return "\t" + ijs.size() + " " + ijs.toString() + "\t" + nodes;

	}

	public String getInferenceResults() {
		StringBuffer toString = new StringBuffer();
		for(Scenario key: inferenceResults.keySet()) {
			toString.append("\tInference result for " + key.toString() + ": " + inferenceResults.get(key).toString() + "\n");
		}
		return toString.toString();
	}
	
	public Map<Scenario, Float> getTimesToDetection() {
		return timesToDetection;
	}

	public Map<Scenario, Float> getObjectiveValues() {
		return objectiveValues;
	}

	public void setTimesToDetection(Map<Scenario, Float> timesToDetection) {
		this.timesToDetection = timesToDetection;
	}
	
	/*******************************************************************************************
	 * Helper Methods for Move Logic
	 * **************************************/

	public synchronized void addSensor(ScenarioSet scenarioSet, ExtendedSensor sensor) {

		if(!sensors.contains(sensor))
			sensors.add(sensor);
		updateWells(scenarioSet);

	}

	private synchronized void updateWells(ScenarioSet scenarioSet) {

		for(Sensor sensor: sensors) {
			if(sensor instanceof ExtendedSensor)
				((ExtendedSensor)sensor).setWell(null);
		}

		wells.clear();

		for(Sensor sensor: sensors)  {
			if(!(sensor instanceof ExtendedSensor))
				continue;			
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			boolean foundWell = false;
			for(Well well: wells){
				if(well.isAt(extendedSensor)) {
					extendedSensor.setWell(well);
					well.addSensor(extendedSensor);
					foundWell = true; // Found a well at the sensors new position
				}
			}
			if(!foundWell) {
				Well newWell = new Well(extendedSensor.getIJK().getI(), extendedSensor.getIJK().getJ(), scenarioSet);
				newWell.addSensor(extendedSensor);
				extendedSensor.setWell(newWell);
				wells.add(newWell);
			}
		}
		
	}

	public synchronized boolean mutateWell(ScenarioSet set) {

		Constants.log(Level.FINER, "Sensor configuration: mutating wells", null);

		Object addedWell = addRealizedWell(set);
		if(addedWell  != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, ADDED REALIZED WELL", addedWell);
			return true;
		}
		Object movedRealizedWellInBounds = moveRealizedWellInBounds(set);
		if(movedRealizedWellInBounds != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED REALIZED WELL IN BOUNDS", movedRealizedWellInBounds);
			return true;
		}
		Object movedRealizedWell = moveRealizedWell(set);
		if(movedRealizedWell != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED REALIZED WELL", movedRealizedWell);
			return true;
		}

		return false;
	}

	public synchronized boolean mutateSensor(ScenarioSet scenarioSet, ModelOption modelOption) {

		// If we can afford a new sensor add it at the add point:
		Constants.log(Level.FINER, "Sensor configuration: mutating sensors", null);
		//	boolean debug = true;

		Object addedSensor;
		if(modelOption == ModelOption.ALL_SENSORS) addedSensor = addAllSensor(scenarioSet);
		else
			addedSensor = addSensor(scenarioSet);
		if(addedSensor != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, ADDED SENSOR", addedSensor);
			return true;
		}
		//Have to skip this if we're running as one sensor, this is pretty much guaranteed to have out of bounds sensors we don't want to move (outside of their clouds)
		if(!(modelOption == ModelOption.ALL_SENSORS)){
			Object movedSensorInBounds = moveSensorInBounds(scenarioSet);
			if(movedSensorInBounds != null) {
				Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR IN BOUNDS", movedSensorInBounds);;
				return true;
			}
		}
		
		Object movedSensor;
		if(modelOption == ModelOption.ALL_SENSORS) movedSensor = moveAllSensor(scenarioSet);
		else movedSensor = moveSensor(scenarioSet);
		if(movedSensor != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR", movedSensor);
			return true;
		}
		
		// Prioritize shuffling a well
		if(ModelOption.INDIVIDUAL_SENSORS_2 == modelOption) {
			Object shuffledWell = shuffleWell(scenarioSet);
			if(shuffledWell != null) {
				Constants.log(Level.FINER, "Sensor configuration: mutated, SHUFFLED WELL", shuffledWell);
				return true;
			}
		}
		Object movedWell = moveWell(scenarioSet);
		if(movedWell != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED WELL", movedWell);
			return true;
		}
		if(ModelOption.INDIVIDUAL_SENSORS_2 != modelOption) {
			Object shuffledWell = shuffleWell(scenarioSet);
			if(shuffledWell != null) {
				Constants.log(Level.FINER, "Sensor configuration: mutated, SHUFFLED WELL", shuffledWell);
				return true;
			}
		}

		Constants.log(Level.WARNING, "Sensor configuration: couldn't mutate", null);

		return false;
	}

	public boolean mutateSensorToEdgeOnly(ScenarioSet set) {	
		set.setEdgeMovesOnly(true);
		boolean sensorMutate = mutateSensor(set, ModelOption.INDIVIDUAL_SENSORS);
		set.setEdgeMovesOnly(false);
		return sensorMutate;
	}

	private Object addRealizedWell(ScenarioSet scenarioSet) {

		// Add well at add point
		if(wells.size() < scenarioSet.getMaxWells()) {
			RealizedWell well = new RealizedWell(scenarioSet.getAddPoint().getI(), scenarioSet.getAddPoint().getJ(), scenarioSet);
			addRealizedWell(well);
			return well;
		}
		// Too many wells
		return null;
	}

	private Object moveRealizedWellInBounds(ScenarioSet set) {
		List<Well> outOfBoundsWells = new ArrayList<Well>();
		for(Well well: wells) {
			if(well instanceof RealizedWell) {
				if(((RealizedWell)well).isOutOfBounds(set)) {
					outOfBoundsWells.add(well);
				}
			}
		}
		return moveRealizedWell(outOfBoundsWells, set); // Any well can move
	}

	private Object moveRealizedWell(ScenarioSet scenarioSet) {
		return moveRealizedWell(wells, scenarioSet); // Any well can move
	}

	private Object addSensor(ScenarioSet scenarioSet) {

		// Timer
		// long startTime = System.currentTimeMillis();

		// We will try to add here first.
		int addPoint = scenarioSet.getNodeStructure().getNodeNumber(scenarioSet.getAddPoint());
		Map<String, List<Integer>> affordableSensors = new HashMap<String, List<Integer>>();
		List<String> types = new ArrayList<String>();
		boolean atAddPoint = false;
		for(String type: scenarioSet.getDataTypes()) {
			List<Integer> validNodes = scenarioSet.getValidNodes(type, this, true, true, true);
			if(!validNodes.isEmpty()) {
//				affordableSensors.put(type, scenarioSet.getValidNodes(type, this, true, true, true));
				affordableSensors.put(type, validNodes);
				types.add(type);
				if(validNodes.contains(addPoint)) {
					atAddPoint = true; // We can add a sensor at the add point
				}
			}
		}
		if(affordableSensors.isEmpty()) {
			// System.out.println("Could not add sensor, time taken: " + (System.currentTimeMillis()-startTime));
			return null;
		}

		String type = types.get(Constants.random.nextInt(types.size()));
		if(atAddPoint) {
			ExtendedSensor toAdd = new ExtendedSensor(addPoint, type, scenarioSet.getNodeStructure());
			addSensor(scenarioSet, toAdd);
			return toAdd;
		} else {
			int index = Constants.random.nextInt(affordableSensors.get(type).size());
			ExtendedSensor toAdd = new ExtendedSensor(affordableSensors.get(type).get(index), type, scenarioSet.getNodeStructure());
			addSensor(scenarioSet, toAdd);
			return toAdd;
		}

		// System.out.println("Added sensor, time taken: " + (System.currentTimeMillis()-startTime));

	}
	
	private Object addAllSensor(ScenarioSet scenarioSet) {

		// Timer
		// long startTime = System.currentTimeMillis();

		// We will try to add here first.
		int addPoint = scenarioSet.getNodeStructure().getNodeNumber(scenarioSet.getAddPoint());
		Map<String, List<Integer>> affordableSensors = new HashMap<String, List<Integer>>();
		List<String> types = new ArrayList<String>();
		boolean atAddPoint = false;
		List<Integer> validNodes = scenarioSet.getValidNodes("all", this, true, true, true);
		if(!validNodes.isEmpty()) {
			affordableSensors.put("all", validNodes);
			if(validNodes.contains(addPoint)) {
				atAddPoint = true; // We can add a sensor at the add point
			}
		}
		if(affordableSensors.isEmpty()) {
			// System.out.println("Could not add sensor, time taken: " + (System.currentTimeMillis()-startTime));
			return null;
		}
		if(atAddPoint) {
			ExtendedSensor toAdd = new ExtendedSensor(addPoint, "all", scenarioSet.getNodeStructure());
			addSensor(scenarioSet, toAdd);
			return toAdd;
		} else {
			int index = Constants.random.nextInt(affordableSensors.get("all").size());
			for(String type: scenarioSet.getSensorSettings().keySet()){
				if(type != "all"){
					ExtendedSensor toAdd = new ExtendedSensor(affordableSensors.get("all").get(index), type, scenarioSet.getNodeStructure());
					addSensor(scenarioSet, toAdd);
				}
			}
			return new Object();
		}

		// System.out.println("Added sensor, time taken: " + (System.currentTimeMillis()-startTime));

	}

	private Object moveSensorInBounds(ScenarioSet scenarioSet) {

		// First see if have any out of bounds sensors
		List<Sensor> outOfBounds = new ArrayList<Sensor>();
		for(ExtendedSensor sensor: getExtendedSensors()) {
			// The current sensor is not in the cloud, or there are multiple sensors at that location
			if(!sensor.isInCloud(scenarioSet) || getSensorPositions(sensor).contains(sensor.getNodeNumber()))
				outOfBounds.add(sensor);
		}

		return moveSensor(outOfBounds, scenarioSet);

	}

	private Object moveSensor(ScenarioSet scenarioSet) {
		return moveSensor(sensors, scenarioSet);	// Otherwise just move a random one
	}

	private Object moveAllSensor(ScenarioSet scenarioSet) {
		return moveAllSensor(sensors, scenarioSet);	// Otherwise just move a random one
	}

	/*
	 * This is the beginnings of a stand-alone sensor-swap. TODO: Remove?
	 * 
	private Object switchSensor(ScenarioSet scenarioSet) {
		//Pick a current sensor
		Sensor thisSensor = sensors.get(Constants.random.nextInt(sensors.size()));
		System.out.println("Switching the type of sensor: " + thisSensor);
		
		//Get the list of valid types to switch with, return if empty (only one allowed)
		List<String> types = scenarioSet.getDataTypes();
		if(!types.remove(thisSensor.getSensorType())) System.out.println("This sensor type wasn't in the list. That shouldn't happen.");
		if(types.isEmpty()){
			System.out.println("Cannot switch sensor type - only one is allowed");
			return null;
		}
		
		//Make sure that there's not already other sensors 
		
		
		

	}
	*/
	
	private Object moveWell(ScenarioSet scenarioSet) {
		if(wells.size() == 0)
			return null; // No wells to move
		// Otherwise randomize and try to move the well
		List<Well> wells = getWells();
		Collections.shuffle(wells, Constants.random);
		for(Well well: wells) {
			if(well.move(this, scenarioSet)) {
				updateWells(scenarioSet);
				return well;
			}
		}
		return null;
	}

	private Object shuffleWell(ScenarioSet scenarioSet) {
		if(wells.size() == 0)
			return null; // No wells to move

		// Otherwise randomize and try to move the well
		List<Well> wells = getWells();
		Collections.shuffle(wells, Constants.random);
		for(Well well: wells) {
			if(well.shuffle(this, scenarioSet)) {
				updateWells(scenarioSet);
				return well;
			}
		}


		return null;
	}

	// level 2
	private Object moveSensor(List<Sensor> sensors, ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(sensors, Constants.random);
		for(Sensor sensor: sensors) {
			if(!(sensor instanceof ExtendedSensor)) 
				continue;
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			if(extendedSensor.move(this, scenarioSet)) {
				addSensor(scenarioSet, extendedSensor);	
				return extendedSensor; // We were able to move an out of bounds sensor into the cloud
			}
		}
		return null;
	}
	
	private Object moveAllSensor(List<Sensor> sensors, ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(sensors, Constants.random);
		//Will break if not extended sensors
		ExtendedSensor sensor1 = (ExtendedSensor) sensors.get(0);
		Integer nodeToMove = sensor1.getNodeNumber();
		ArrayList<ExtendedSensor> sensorsToMove = new ArrayList<ExtendedSensor>();
		for(Sensor sensor: sensors) {
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			if(sensor.getNodeNumber().equals(nodeToMove)) sensorsToMove.add(extendedSensor);
		}
		/*
		if(extendedSensor.move(this, scenarioSet)) {
			addSensor(scenarioSet, extendedSensor);	
			return extendedSensor; // We were able to move an out of bounds sensor into the cloud
		}
		*/
		if(ExtendedSensor.move(sensorsToMove, this, scenarioSet)){
			for(ExtendedSensor sensor: sensorsToMove){
				addSensor(scenarioSet, sensor);
			}
			return new Object();
		}
		return null;
	}

	private Object moveRealizedWell(List<Well> wellsToMove, ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(wellsToMove, Constants.random);
		for(Well well: wellsToMove) {
			if(well.move(this, scenarioSet)) {
				return well;
			}
		}
		return null;
	}

	public synchronized void addRealizedWell(RealizedWell realizedWell) {
		for(ExtendedSensor sensor: realizedWell.getSensors()) {
			sensors.add(sensor);
		}
		wells.add(realizedWell);
	}

}
