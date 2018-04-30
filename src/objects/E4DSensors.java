package objects;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.core.runtime.IProgressMonitor;

import hdf5Tool.HDF5Interface;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class E4DSensors {
	
	public static Map<Float, Map<Scenario, Map<Integer, Map<Integer, Float>>>> ertDetectionTimes = new HashMap<Float, Map<Scenario, Map<Integer, Map<Integer, Float>>>>(); //detection threshold <scenario <first well <second well, detection>>>
	public static Map<Float, Map<Integer, List<Integer>>> ertPotentialWellPairings = new HashMap<Float, Map<Integer, List<Integer>>>(); //detection threshold matrix <first well, list of top well pairs>>
	public static Map<Float, Map<Integer, Integer>> ertWellPairings = new HashMap<Float, Map<Integer, Integer>>(); //detection threshold matrix <first well, second well>
	public static int wellPairs = 5;
	
	// This method determines which wells should be passed along to E4D
	public static ArrayList<Point3i> calculateE4DWells(STORMData data, String parameter, int maximumWells, IProgressMonitor monitor) throws Exception {
				
		float threshold = 0.01f; //Hard number determined by Catherine, first value to test
		NodeStructure nodeStructure = data.getSet().getNodeStructure();
		
		ArrayList<Point3i> wellList = new ArrayList<Point3i>();
		HashSet<Integer> allNodes = new HashSet<Integer>(); //All nodes that meet the above threshold
		
		
		// Successively remove a level of magnitude to the search threshold until nodes are found
		int iteration = 1;
		while (allNodes.size()==0) {
			if(iteration>1) monitor.worked(-600/iteration);
			// Loop through scenarios and add all nodes that trigger in all
			monitor.subTask("Scanning for valid nodes with threshold = " + threshold);
			for(Scenario scenario: data.getSet().getScenarios()) {
				if(monitor.isCanceled()) return null;
				try {
					HashSet<Integer> nodes = null;
					nodes = HDF5Interface.queryNodes(nodeStructure, scenario.getScenario(), parameter, threshold, threshold, Trigger.RELATIVE_DELTA, DeltaType.BOTH, monitor);
					allNodes.addAll(nodes);
				} catch (Exception e) {
					System.out.println("Unable to query nodes from files.");
					e.printStackTrace();
				}
			}
			threshold /= 10;
			monitor.worked(600/iteration); //This loop will be 60% of the progress bar
			iteration++;
		}
		
		// Count how many unique wells locations were found in the above nodes
		for(Integer node: allNodes) {
			Point3i temp = nodeStructure.getIJKFromNodeNumber(node);
			Point3i well = new Point3i(temp.getI(), temp.getJ(), 1); //set k to 1 to get the single well location
			if(!wellList.contains(well))
				wellList.add(well);
		}
		
		// If a reasonable number of wells is found, sort and return
		monitor.subTask("Valid wells = " + wellList.size() + "; starting goal seek.");
		if(wellList.size() <= maximumWells) {
			Collections.sort(wellList, Point3i.IJ_COMPARATOR);
		// If too many wells are found, goal seek to the desired number
		} else {
			wellList = new ArrayList<Point3i>(); //reset
			TreeMap<Float, Integer> changePerNode = new TreeMap<Float, Integer>();
			
			// Find the relative difference for all the nodes found above
			TimeStep firstStep = nodeStructure.getTimeSteps().get(0);
			TimeStep lastStep = nodeStructure.getTimeSteps().get(nodeStructure.getTimeSteps().size()-1);
			for(Integer node: allNodes) {
				float maxDifference = 0;
				for(Scenario scenario: data.getSet().getScenarios()) {
					if(monitor.isCanceled()) return null;
					float start = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), firstStep, parameter, node);
					float end = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), lastStep, parameter, node);
					float difference = Math.abs((end - start) / start);
					if(difference > maxDifference)
						maxDifference = difference;
				}
				changePerNode.put(maxDifference, node);
				monitor.worked(400/allNodes.size());
			}
			
			// Sort the nodes by greatest relative change
			ArrayList<Float> keys = new ArrayList<Float>(changePerNode.keySet());
			ArrayList<Integer> values = new ArrayList<Integer>(keys.size());
			Collections.sort(keys, Collections.reverseOrder());
			for(Float key: keys)
				values.add(changePerNode.get(key));
			
			// Convert to Point3i and sort
			for(Integer node: values) {
				Point3i temp = nodeStructure.getIJKFromNodeNumber(node);
				Point3i well = new Point3i(temp.getI(), temp.getJ(), 1); //set k to 1 to get the single well location
				if(!wellList.contains(well))
					wellList.add(well);
				if(wellList.size() == maximumWells)
					break;
			}
			Collections.sort(wellList, Point3i.IJ_COMPARATOR);
		}
		return wellList;
	}
	
	
	// This method is called when a results matrix is found in the correct location
	// The matrix is read and stored for all selected scenarios
	// The top 5 well pairings are also mapped to each well
	public static void addERTSensor(ScenarioSet set) {
		ertDetectionTimes.clear();
		ertPotentialWellPairings.clear();
		// May need to remove these if they were added previously
		List<String> toRemove = new ArrayList<String>();
		for(String type: set.getNodeStructure().getDataTypes()) {
			if(type.contains("Electrical Conductivity"))
				toRemove.add(type);
		}
		set.getNodeStructure().getDataTypes().removeAll(toRemove);
		
		File dir = new File(Constants.userDir, "e4d");
		if(!dir.exists())
			System.out.println("Did not find the e4d folder.");
		FileFilter fileFilter = new WildcardFileFilter("ertResultMatrix_" + set.getScenarioEnsemble() + "_" + set.getScenarios().size() + "*.csv");
		File[] files = dir.listFiles(fileFilter);
		for(File ertInput: files) {
			float threshold = Float.parseFloat(ertInput.getPath().substring(ertInput.getPath().lastIndexOf("_")+1, ertInput.getPath().length()-4));
			ertDetectionTimes.put(threshold, new HashMap<Scenario, Map<Integer, Map<Integer, Float>>>());
			ertPotentialWellPairings.put(threshold, new HashMap<Integer, List<Integer>>());
			
			set.getSensors().add("Electrical Conductivity_" + threshold);
			set.getSensorSettings().put("Electrical Conductivity_" + threshold, new SensorSetting(set.getNodeStructure(), set, "Electrical Conductivity_" + threshold, set.getScenarios(), 0, 0));
			set.getSensorSettings().get("Electrical Conductivity_" + threshold).setUserSettings(100, Color.BLUE, threshold, threshold, Trigger.RELATIVE_DELTA, false, DeltaType.BOTH, 0, 0);
			set.getNodeStructure().getDataTypes().add("Electrical Conductivity_" + threshold);
			
			// Here, we want to read sensor pairings and times from the matrix
			String line = "";
			List<Scenario> scenarios = set.getScenarios();
			List<Integer> validNodes = new ArrayList<Integer>();
			Map<Integer, Map<Integer, Float>> detectionTimesPerWell = new HashMap<Integer, Map<Integer, Float>>();
			try (BufferedReader br = new BufferedReader(new FileReader(ertInput))) {
				// Iterate through all the scenarios
				int scenarioIteration = 0;
				// Read each line, comma delimited
				while ((line = br.readLine()) != null) {
					String[] lineList = line.split(",");

					// The first line lists the valid nodes per scenario (duplicates SensorSettings --> setValidNodes())
					if (lineList.length!=0 && lineList[0].toLowerCase().equals(scenarios.get(scenarioIteration).getScenario()) && !lineList[0].equals("")) {
						validNodes.clear();
						for (int j=1; j<lineList.length; j++) {
							String[] ijList = lineList[j].split(":");
							validNodes.add(set.getNodeStructure().getNodeNumber(Integer.parseInt(ijList[0]), Integer.parseInt(ijList[1]), 1));
						}
					}

					// The following lines list ERT detection times for valid nodes per scenario
					else if (lineList.length>1) {
						Map<Integer, Float> timePerPairedWell = new HashMap<Integer, Float>();
						Integer key = null;
						String[] ijList = lineList[0].split(":");
						try { //if the scenario was removed during weighting, this will throw an error
							key = set.getNodeStructure().getNodeNumber(Integer.parseInt(ijList[0]), Integer.parseInt(ijList[1]), 1);
						} catch (NumberFormatException ne) {
							System.out.println("Unable to parse ijk coordinate of E4D well.");
							for(int j=0; j<lineList.length; j++)
								br.readLine();
							continue;
						}
						for (int j=1; j<lineList.length; j++) {
							timePerPairedWell.put(validNodes.get(j-1), Float.parseFloat(lineList[j]));
						}
						detectionTimesPerWell.put(key, timePerPairedWell);
					}
					
					// The following blank line triggers the saving of detection times for the scenario
					else {
						ertDetectionTimes.get(threshold).put(scenarios.get(scenarioIteration), detectionTimesPerWell);
						detectionTimesPerWell = new HashMap<Integer, Map<Integer, Float>>();
						scenarioIteration++;
					}
				}
				if(detectionTimesPerWell.size()!=0)
					ertDetectionTimes.get(threshold).put(scenarios.get(scenarioIteration), detectionTimesPerWell);//saves the last scenario
				
				//Final check if any potential wells had no detections across all scenarios and delete
				Map<Integer, Float> zeroDetectionWells = new HashMap<Integer, Float>();
				for(Scenario scenario: ertDetectionTimes.get(threshold).keySet()) {
					for(Integer primaryWell: ertDetectionTimes.get(threshold).get(scenario).keySet()) {
						if(!zeroDetectionWells.containsKey(primaryWell))
							zeroDetectionWells.put(primaryWell, (float)0);
						float count = zeroDetectionWells.get(primaryWell);
						for(Integer secondaryWell: ertDetectionTimes.get(threshold).get(scenario).get(primaryWell).keySet())
							count += ertDetectionTimes.get(threshold).get(scenario).get(primaryWell).get(secondaryWell);
						zeroDetectionWells.put(primaryWell, count);
					}
				}
				for(Integer primaryWell: zeroDetectionWells.keySet()) {
					if(zeroDetectionWells.get(primaryWell)==0) {
						for(Scenario scenario: ertDetectionTimes.get(threshold).keySet()) {
							ertDetectionTimes.get(threshold).get(scenario).remove(primaryWell);//TODO: Do I also need to remove from secondary well lists?
						}
					}
				}
			} catch (IOException ex) {
				System.out.println("Something went wrong trying to read the ERT matrix");
				ex.printStackTrace();
			}
			// Add top well pairings
			if(!ertDetectionTimes.get(threshold).isEmpty()) {
				Scenario firstScenario = set.getScenarios().get(0);
				for(Integer firstWellLoop: ertDetectionTimes.get(threshold).get(firstScenario).keySet()) {
					List<Float> averageTTD = new ArrayList<Float>(); // As a list, this is easier to sort and clip
					Map<Integer, Float> tempWellPairings = new HashMap<Integer, Float>(); // Unsorted list
					List<Integer> wellList = new ArrayList<Integer>();
					for(Integer secondWellLoop: ertDetectionTimes.get(threshold).get(firstScenario).get(firstWellLoop).keySet()) {
						float sumTTD = 0;
						for(Scenario scenarioLoop: ertDetectionTimes.get(threshold).keySet())
							sumTTD =+ ertDetectionTimes.get(threshold).get(scenarioLoop).get(firstWellLoop).get(secondWellLoop);
						float avgTTD = sumTTD / ertDetectionTimes.get(threshold).size();
						if(avgTTD!=0)
							averageTTD.add(avgTTD);
						tempWellPairings.put(secondWellLoop, avgTTD);
					}
					Collections.sort(averageTTD); //sort with smallest first
					if(averageTTD.size()>=wellPairs)
						averageTTD.subList(wellPairs, averageTTD.size()).clear(); //trim to top wells
					for(Float value: averageTTD) {
						for(Integer well: tempWellPairings.keySet()) {
							if(tempWellPairings.get(well).equals(value)) {
								wellList.add(well);
								tempWellPairings.remove(well);
								break;
							}
						}
					}
					if(wellList.size()==0) //to prevent later errors, add a dummy pairing
						wellList.add(firstWellLoop);
					ertPotentialWellPairings.get(threshold).put(firstWellLoop, wellList);
				}
			}
		}
	}
	
	
	// This method returns valid nodes for E4D
	public static HashSet<Integer> setValidNodesERT(IProgressMonitor monitor, float threshold) {
		HashSet<Integer> validNodes = new HashSet<Integer>();
		for(Scenario scenario: ertDetectionTimes.get(threshold).keySet()) {
			validNodes.addAll(ertDetectionTimes.get(threshold).get(scenario).keySet());
			monitor.worked(300/ertDetectionTimes.get(threshold).size()); //TODO: Monitor needs to be updated so that it adds the correct amount worked with multiple thresholds
		}
		return validNodes;
	}
	
	
	// This method tells the Simulated Annealing process whether sensors have been triggered by ERT (reads matrix)
	public static Boolean ertSensorTriggered(TimeStep timestep, Scenario scenario, Integer nodeNumber, float threshold) throws Exception{
		Boolean triggered = false;
		
		// Return as triggered only if the timestep exceeds the detection value for the well pairing
		if(ertWellPairings.get(threshold).containsKey(nodeNumber)) {
			Integer wellPair = ertWellPairings.get(threshold).get(nodeNumber);
			Float detection = ertDetectionTimes.get(threshold).get(scenario).get(nodeNumber).get(wellPair);
			if(detection!=0 && timestep.getTimeStep()>detection)
				triggered = true;
		}
		
		return triggered;
	}
	
	
	// Emulates the above function, but forces the selection of the best well pairing
	public static Boolean ertBestSensorTriggered(TimeStep timestep, Scenario scenario, Integer nodeNumber, float threshold) throws Exception{
		Boolean triggered = false;
		
		// Return as triggered only if the timestep exceeds the detection value for the well pairing
		if(ertDetectionTimes.get(threshold).get(scenario).containsKey(nodeNumber)) {
			float minDetection = Float.MAX_VALUE;
			// Use the minimum TTD from all well pairings
			for(Float detection: ertDetectionTimes.get(threshold).get(scenario).get(nodeNumber).values()) {
				if(detection < minDetection && detection!=0)
					minDetection = detection;
			}
			if(timestep.getTimeStep()>minDetection)
				triggered = true;
		}
		return triggered;
	}
	
	
	public static void ertNewPairing() {
		for(float threshold: ertPotentialWellPairings.keySet()) {
			ertWellPairings.put(threshold, new HashMap<Integer, Integer>());
			for(Integer primaryWell: ertPotentialWellPairings.get(threshold).keySet()) {
				Random rand = new Random();
				int n = rand.nextInt(ertPotentialWellPairings.get(threshold).get(primaryWell).size());
				ertWellPairings.get(threshold).put(primaryWell, ertPotentialWellPairings.get(threshold).get(primaryWell).get(n));
			}
		}
	}
	
	
	// This method looks for an ERT sensor at a new location and picks a new random pairing
	public static ExtendedConfiguration ertAddPairing(ExtendedConfiguration newConfiguration, ExtendedConfiguration currentConfiguration) {
		int i = 0;
		for(ExtendedSensor sensor: newConfiguration.getExtendedSensors()) {
			if(sensor.getSensorType().contains("Electrical Conductivity")) {
				float threshold = Float.parseFloat(sensor.getSensorType().substring(sensor.getSensorType().lastIndexOf("_")+1, sensor.getSensorType().length()));
				
				//Check if a sensor was moved by comparing against all sensors in the last configuration
				boolean moved = true;
				for(ExtendedSensor current: currentConfiguration.getExtendedSensors()) {
					if(sensor.getNodeNumber().intValue()==current.getNodeNumber().intValue() && current.getSensorType().contains(sensor.getSensorType())) {
						moved = false;
						break;
					}
				}
				
				//If moved, randomly pick one of the potential well pairings to assign
				if(moved) {
					int nodeNumber = sensor.getNodeNumber();
					int n = new Random().nextInt(ertPotentialWellPairings.get(threshold).get(nodeNumber).size());
					int nodePairNumber = ertPotentialWellPairings.get(threshold).get(nodeNumber).get(n);
					ertWellPairings.get(threshold).put(nodeNumber, ertPotentialWellPairings.get(threshold).get(nodeNumber).get(n));
					sensor.setNodePair(nodePairNumber);
					newConfiguration.getExtendedSensors().set(i, sensor);
				}
			}
			i++;
		}
		return newConfiguration;
	}
	
}
