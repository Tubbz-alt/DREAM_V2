package utilities;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Constants for use throughout code, the booleans at the top and random seed can be changed to alter functionality
 * @author port091
 * @author rodr144
 */

public class Constants {
	
	// These probably be eventually wrapped into the code directly, but for now give us functionality for debugging/developing extensions without breaking the working release
	public static boolean buildDev = true;
	public static boolean runThreaded = true;
	public static boolean runScripts = true;
	public static boolean useParetoOptimal = true;
	
	static File userDir = new File(System.getProperty("user.dir"));
	public static String parentDir = userDir.getParent();
	
	public enum ModelOption {
		
		INDIVIDUAL_SENSORS_2("Individual Sensors 2.0", "Prioritize moving a well over adjusting sensors in a well."),
		ALL_SENSORS("Aggregated Sensor Technology", "Place a sensor of every type at each monitoring location");
		//NO LONGER SUPPORTED: these used to be options and are either depreciated or redundant.
		//INDIVIDUAL_SENSORS("Individual Sensors", "Prioritize adjusting sensors in a well over moving a well."),
		//REALIZED__WELLS("Realized Wells", "Wells have all sensor types at each k index");
		
		String alias;
		String description;
		ModelOption(String alias, String description) {
			this.alias = alias;
		}
		
		public String getDescription() {
			return description;
		}
		
		@Override
		public String toString() {
			return alias;
		}
		
	}	

	// "Diana Bacon Runs 03"; 
	public static String RUN_TEST =  "STORM";//"Bacon Base Case with Flux 3-D";//"120517_small_domain_STOMP_runs";//"Bacon Base Case with Flux 3-D";

	public static Random random = new Random(1); //Right now this is seeded, this way we have reproducable results. Should probably un-seed for release.
	
	public static DecimalFormat decimalFormat = new DecimalFormat("###.###");
	public static DecimalFormat percentageFormat = new DecimalFormat("###.##");
	public static DecimalFormat exponentialFormat = new DecimalFormat("0.00000000E00");
	public static DecimalFormat exponentialFormatShort = new DecimalFormat("0.0E00");
	
	private static Logger LOGGER;
	private static boolean loggerOn;
	
	public static String homeDirectory = System.getProperty("user.home");

	public static boolean returnAverageTTD = true;
	public static boolean scenarioUnion = true;
	
	public static Timer timer = new Constants().new Timer();
	
	/*	
	 *  Example of node number vs. index. Each cell has: 
	 *  1) i,j,k			- each of the three dimensions are 1 <= dim <= dimMax
	 *  2) node number		- 1-indexed, used by DREAM to store which nodes are triggered and to query from nodes
	 *  3) index			- 0-indexed, used in reading values from the hdf5 files.
	 *  _________________________    _________________________    _________________________    
	 * 	| 1,1,1 | 1,2,1 | 1,3,1 |    | 1,1,2 | 1,2,2 | 1,3,2 |    | 1,1,3 | 1,2,3 | 1,3,3 |
	 * 	| 1     | 4     | 7     |    | 10    | 13    | 16    |    | 19    | 22    | 25    |
	 * 	| 0     | 3     | 6     |    | 1     | 4     | 7     |    | 2     | 5     | 8     |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 2,1,1 | 2,2,1 | 2,3,1 |    | 2,1,2 | 2,2,2 | 2,3,2 |    | 2,1,3 | 2,2,3 | 2,3,3 |
	 * 	| 2     | 5     | 8     |    | 11    | 14    | 17    |    | 20    | 23    | 26    |
	 * 	| 9     | 12    | 15    |    | 10    | 13    | 16    |    | 11    | 14    | 17    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 3,1,1 | 3,2,1 | 3,3,1 |    | 3,1,2 | 3,2,2 | 3,3,2 |    | 3,1,3 | 3,2,3 | 3,3,3 |
	 * 	| 3     | 6     | 9     |    | 12    | 15    | 18    |    | 21    | 24    | 27    |
	 * 	| 18    | 21    | 24    |    | 19    | 22    | 25    |    | 20    | 23    | 26    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|
	 * 
	 */
	
	
	// This function takes a 0-indexed index and returns a 1-indexed node number. (See above)
	public static int getNodeNumber(Point3i ijkDimensions, int index) {
		return getNodeNumber(ijkDimensions.getI(), ijkDimensions.getJ(), ijkDimensions.getK(), index);
	}
	
	public static Integer getNodeNumber(int iMax, int jMax, int kMax, int index) {
		return (index % kMax) * iMax * jMax + (index/kMax % jMax) * iMax + (index/(kMax*jMax) % iMax + 1);
	}
	
	// This function takes a 1-indexed node number and returns a 0-indexed index. (See above)
	public static int getIndex(Point3i ijkDimensions, int nodeNumber) {
		return getIndex(ijkDimensions.getI(), ijkDimensions.getJ(), ijkDimensions.getK(), nodeNumber);
	}
	
	public static Integer getIndex(int iMax, int jMax, int kMax, int nodeNumber){
		return ((nodeNumber-1)%iMax)*jMax*kMax + (((nodeNumber-1)/iMax)%jMax)*kMax + (((nodeNumber-1)/(iMax*jMax))%kMax);
	}

	public static void initializeLogger(String homeDirectory, String className) throws SecurityException, IOException {
		//getInstance().
		LOGGER = Logger.getLogger(className);
		loggerOn = true;
		//getInstance().
		LOGGER.setLevel(Level.FINER);	// Level.ALL will log everything (every inference)

		File home = new File(homeDirectory);

		if(!home.exists()) {
			home.mkdir();
		}

		File logFile = new File(home, "STORM.log");	// TODO: append a time stamp?

		if(!logFile.exists()) {
			logFile.createNewFile();
		}

		FileHandler handler = new FileHandler(logFile.getAbsolutePath(), 10485760*2, 1, true); // allow up to 1 meg files?
		handler.setFormatter(new SimpleFormatter());
		//getInstance().
		LOGGER.addHandler(handler);

		log(Level.INFO, "Initialized Logger", null);
	}

	public static void turnLoggerOn(boolean on) {
		loggerOn = on;
	}

	public static void log(Level level, String message, Object obj) {
		// Make sure only one thread modifies the log at a time
		if(!loggerOn)
			return;
		synchronized(LOGGER){//getInstance()){
			if(obj != null) {
				if(obj instanceof String)
					message += ": {0}\n";
				else
					message += "\n------------------------------------------------>\n{0}------------------------------------------------<\n";
			}
			//getInstance().
			LOGGER.log(level, message, obj);			
		}
	}

	public static void disposeLogger() {
		// TODO: want this to let go of lock
		//	LOGGER.getH
	}
	
	public class Timer {
		
		private long perConfiguration;
		private long perScenario;
		private long perTime;

		public synchronized void addPerConfiguration(long time) {
			perConfiguration += time;
		}
		

		public synchronized void addPerScenario(long time) {
			perScenario += time;
		}
		

		public synchronized void addPerTime(long time) {
			perTime += time;
		}
		
		@Override 
		public String toString() {
			return "Per configuration, scenario, time: " + perConfiguration + ", " + perScenario + ", " + perTime;
		}
	}
	
	public static ArrayList<Float> makeLines(ArrayList<Float> centers){
		ArrayList<Float> lines = new ArrayList<Float>();
		for(int x = 1; x < centers.size(); x++) {
			float half = (centers.get(x)-centers.get(x-1))/2;
			if(x == 1) 
				lines.add(new Float(centers.get(x-1)-half).floatValue());
			lines.add(new Float(centers.get(x-1)+half).floatValue());
			if(x == centers.size()-1) 
				lines.add(new Float(centers.get(x)+half).floatValue());
		}
		return lines;
	}

}
