package hdf5Tool;

import gridviz.DataGrid;
import gridviz.GridError;
import gridviz.GridParser;
import gridviz.GridParser.DataStructure;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.io.FileUtils;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5File;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileBrowser.java
 *
 * Created on Aug 12, 2013, 10:33:55 AM
 */
/**
 *
 * @author port091
 * @author whit162
 */
public class FileBrowser extends javax.swing.JFrame {

	private static final long serialVersionUID = 8513730341976192873L;

	public static final String TECPLOT = "tecplot";
	public static final String STOMP = "stomp";
	public static final String NTAB = "ntab";
	
	public static final String SCENARIO_PER_FILE = "scenario per file";
	public static final String SCENARIO_PER_FOLDER = "scenario per folder";
	
	private JComboBox jComboBox_fileType;
	private JComboBox jComboBox_folderStructure;
		
	private JTextField jTextField_inputDir;
	private JTextField jTextField_outputDir;

	private File file_inputDir;
	private File file_outputDir;

	private CheckList checkList_timesteps;
	private CheckList checkList_scenarios;
	private CheckList checkList_dataFields;	

	private boolean debug = false;

	private Map<String, Map<Integer, GridParser>> gridsByTimeAndScenario;
	private Map<String, List<Float>> statisticsByDataField;
	private ProgressMonitor monitor;
	private int processedTasks = 0;

	private JLabel statusLabel;

	public FileBrowser() {
		initComponents();
	}

	private void initComponents() {

		setTitle("DREAM HDF5 Converter");
		setBackground(new java.awt.Color(240, 240, 240));
		getContentPane().setBackground(new java.awt.Color(240, 240, 240));
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		JMenuItem openItem = new JMenuItem("Open");
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					jButton_inputDirActionPerformed(e);
				} catch (GridError e1) {
					e1.printStackTrace();
				}
			}
		});
		JMenu menu = new JMenu("File");
		menu.add(openItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
	
		JPanel jPanel_fileType = new JPanel();
		jComboBox_fileType = new JComboBox(new String[]{TECPLOT, STOMP, NTAB});
		jComboBox_folderStructure = new JComboBox(new String[]{SCENARIO_PER_FILE, SCENARIO_PER_FOLDER});
		jComboBox_fileType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				if(jComboBox_fileType.getSelectedItem().equals(TECPLOT)) {
					jComboBox_folderStructure.setEnabled(true);	
				} else if(jComboBox_fileType.getSelectedItem().equals(STOMP)) {
					jComboBox_folderStructure.setSelectedItem(SCENARIO_PER_FOLDER);
					jComboBox_folderStructure.setEnabled(false);	
				} else if(jComboBox_fileType.getSelectedItem().equals(NTAB)) {
					jComboBox_folderStructure.setSelectedItem(SCENARIO_PER_FILE);
					jComboBox_folderStructure.setEnabled(false);	
				}
			}
		});
		jPanel_fileType.setLayout(new BoxLayout(jPanel_fileType, BoxLayout.LINE_AXIS));
		jPanel_fileType.add(Box.createRigidArea(new Dimension(30,0)));
		jPanel_fileType.add(new JLabel("File type:"));
		jPanel_fileType.add(Box.createRigidArea(new Dimension(5,0)));
		jPanel_fileType.add(jComboBox_fileType);
		jPanel_fileType.add(Box.createRigidArea(new Dimension(30,0)));		
		jPanel_fileType.add(new JLabel("Folder structure:"));
		jPanel_fileType.add(Box.createRigidArea(new Dimension(5,0)));
		jPanel_fileType.add(jComboBox_folderStructure);
		jPanel_fileType.add(Box.createRigidArea(new Dimension(15,0)));
		
		checkList_dataFields = new CheckList();
		checkList_timesteps = new CheckList();
		checkList_scenarios = new CheckList();
		checkList_dataFields.setEnabled(false);
		checkList_timesteps.setEnabled(false);
		checkList_scenarios.setEnabled(false);

		JPanel jPanel_data = new JPanel();
		JLabel jLabel_scenarios = new JLabel();
		JPanel jPanel_timesteps = new JPanel();
		JLabel jLabel_timesteps = new JLabel();
		JLabel jLabel_data = new JLabel();
		JPanel jPanel_scenarios = new JPanel();
		JButton jButton_done = new JButton("Run...");
		JLabel jLabel_outputDir = new JLabel("Output directory:");
		JLabel jLabel_inputDir = new JLabel("Input directory:");
		jTextField_inputDir = new JTextField();
		jTextField_outputDir = new JTextField();
		file_outputDir = new File("C:\\");
		JButton jButton_outputDir = new JButton("Select");
		JButton jButton_inputDir = new JButton("Select");

		// Blue border
		jPanel_data.setBackground(new java.awt.Color(33,57,156));
		jPanel_timesteps.setBackground(new java.awt.Color(33,57,156));
		jPanel_scenarios.setBackground(new java.awt.Color(33,57,156));

		// Scenarios:
		jLabel_scenarios.setText("Scenarios");
		javax.swing.GroupLayout jPanel_scenariosLayout = new javax.swing.GroupLayout(jPanel_scenarios);
		jPanel_scenarios.setLayout(jPanel_scenariosLayout);
		jPanel_scenariosLayout.setHorizontalGroup(
				jPanel_scenariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_scenarios, 0, 294, Short.MAX_VALUE)
				);
		jPanel_scenariosLayout.setVerticalGroup(
				jPanel_scenariosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_scenarios, 0, 184, Short.MAX_VALUE)
				);

		// Time steps:
		jLabel_timesteps.setText("Time steps");
		javax.swing.GroupLayout jPanel_timestepsLayout = new javax.swing.GroupLayout(jPanel_timesteps);
		jPanel_timesteps.setLayout(jPanel_timestepsLayout);
		jPanel_timestepsLayout.setHorizontalGroup(
				jPanel_timestepsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_timesteps, 0, 294, Short.MAX_VALUE)
				);
		jPanel_timestepsLayout.setVerticalGroup(
				jPanel_timestepsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_timesteps, 0, 184, Short.MAX_VALUE)
				);

		// Data:
		jLabel_data.setText("Data");
		javax.swing.GroupLayout jPanel_dataLayout = new javax.swing.GroupLayout(jPanel_data);
		jPanel_data.setLayout(jPanel_dataLayout);
		jPanel_dataLayout.setHorizontalGroup(
				jPanel_dataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_dataFields, 0, 245, Short.MAX_VALUE)
				);
		jPanel_dataLayout.setVerticalGroup(
				jPanel_dataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(checkList_dataFields, 0, 394, Short.MAX_VALUE)
				);

		// Buttons
		jButton_done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					jButton_doneActionPerformed(evt);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		jButton_inputDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {
					jButton_inputDirActionPerformed(evt);
				} catch (GridError e) {
					e.printStackTrace();
				}
			}
		});
		jButton_outputDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButton_outputDirActionPerformed(evt);
			}
		});

		// Top panel
		jTextField_inputDir.setEnabled(false);
		jTextField_outputDir.setEnabled(false);

		statusLabel = new JLabel("");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addGap(8, 8, 8)
										.addComponent(jPanel_fileType)
										.addGap(69, 69, 69))
								.addGroup(layout.createSequentialGroup()
										.addGap(8, 8, 8)
										.addComponent(jLabel_inputDir)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jTextField_inputDir, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jButton_inputDir)
										.addGap(69, 69, 69))
										.addGroup(layout.createSequentialGroup()
												.addComponent(jLabel_outputDir)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(jTextField_outputDir, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(jButton_outputDir)
												.addGap(69, 69, 69))
												.addGroup(layout.createSequentialGroup()
																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																		.addComponent(jPanel_scenarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addComponent(jLabel_scenarios)
																		.addComponent(jPanel_timesteps, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addComponent(jLabel_timesteps))
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(jLabel_data)
																				.addComponent(jPanel_data, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))																																				
																				.addComponent(statusLabel, javax.swing.GroupLayout.Alignment.LEADING)			
																				.addComponent(jButton_done, javax.swing.GroupLayout.Alignment.TRAILING))
																				.addContainerGap())
				);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
							.addComponent(jPanel_fileType))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jTextField_inputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jLabel_inputDir)
								.addComponent(jButton_inputDir))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jTextField_outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(jLabel_outputDir)
										.addComponent(jButton_outputDir))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(jLabel_scenarios)
														.addComponent(jLabel_data))
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																.addGroup(layout.createSequentialGroup()
																		.addComponent(jPanel_scenarios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(jLabel_timesteps)
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(jPanel_timesteps, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																		.addComponent(jPanel_data, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(statusLabel)
																		.addComponent(jButton_done)
																		.addContainerGap())
				);

		pack();
	}

	private void jButton_doneActionPerformed(ActionEvent evt) throws Exception {

		// Create the HDF5 Files
		if(!file_outputDir.exists())
			file_outputDir.mkdir();

		if(gridsByTimeAndScenario == null) {
			System.out.println("Error: Grid is Empty");
			return;
		}

		JCheckBox[] scenarios = checkList_scenarios.getListData();
		JCheckBox[] timeSteps = checkList_timesteps.getListData();

		// Use them all.. well okay, all but one :)
		final int cores = Runtime.getRuntime().availableProcessors() - 1;
		//ExecutorService service = Executors.newFixedThreadPool(cores);
		System.out.println("Using " + (cores) + " cores...");

		final List<Thread> runningThreads = new ArrayList<Thread>();
		final List<Thread> threadsToRun = new ArrayList<Thread>();
		
		int totalScenarios = 0;
		for(JCheckBox scenario: scenarios) {
			if(!scenario.isSelected())
				continue;
			FileConverterThread runnable = new FileConverterThread(scenario.getText(), timeSteps);
			Thread thread = new Thread(runnable);
			threadsToRun.add(thread);
			//service.execute(new FileConverterThread(scenario.getText(), timeSteps));
			totalScenarios++;
		}
		
		MonitorRunnable monitorRunnable = new MonitorRunnable(null, (cores), totalScenarios);
		new Thread(monitorRunnable).start();

		Thread runningAllStuffThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(!threadsToRun.isEmpty()) {
					if(runningThreads.size() < cores - 1) { // probably not
						Thread thread = threadsToRun.remove(0);
						thread.start();
						runningThreads.add(thread);
					} else {
						List<Thread> deadThreads = new ArrayList<Thread>();
						for(Thread thread: runningThreads) {
							if(!thread.isAlive()) {
								deadThreads.add(thread);
							}
						}
						for(Thread dead: deadThreads) {
							runningThreads.remove(dead); // remove the dead...!
						}
					}
					try {
						Thread.sleep(100); // mmmm maybe?
						// Listening for a cancel
						if(monitor.isCanceled()) {
							// Kill everything
							for(Thread killable: runningThreads) {
								killable.interrupt();
							}
							runningThreads.clear();
							threadsToRun.clear(); // maybe  java can collect these???
							return;
						}
					} catch (Exception e) {
						
					}
				}
				
				while(!runningThreads.isEmpty()) {
					List<Thread> deadThreads = new ArrayList<Thread>();
					for(Thread thread: runningThreads) {
						if(!thread.isAlive()) {
							deadThreads.add(thread);
						}
					}
					for(Thread dead: deadThreads) {
						runningThreads.remove(dead); // remove the dead...!
					}
					
					try {
						Thread.sleep(1000); // mmmm maybe?
						// Listening for a cancel
						if(monitor.isCanceled()) {
							// kill everything
							for(Thread killable: runningThreads) {
								killable.interrupt();
							}
							runningThreads.clear();
							threadsToRun.clear(); // maybe  java can collect these???
							return;
						}
					} catch (Exception e) {
						
					}
				}
			}
		});
		runningAllStuffThread.start();
	}

	private class MonitorRunnable implements Runnable {

		private int totalTasks;

		MonitorRunnable(ExecutorService service, int cores, int totalTasks) {
			this.totalTasks = totalTasks;
			monitor = new ProgressMonitor(FileBrowser.this, "Converting files on " + cores + " cores", "0/" + totalTasks, 0, totalTasks);
			processedTasks = 0;
		}

		@Override
		public void run() {
			while(!monitor.isCanceled()) {
				// or maybe some done check too?
				try {
					monitor.setNote(processedTasks + "/" + totalTasks);
					monitor.setProgress(processedTasks);
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace(); // Ignore and continue
					JOptionPane.showMessageDialog(FileBrowser.this, Arrays.toString(e.getStackTrace()), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				}
			}
			monitor.setProgress(processedTasks);	
			JOptionPane.showMessageDialog(FileBrowser.this, (!monitor.isCanceled() ? "Success" : "Canceled") + ", h5 files are located here: " + file_outputDir.getAbsolutePath());
		}
	}

	private class FileConverterThread implements Runnable {

		private String scenario;
		private JCheckBox[] timeSteps;			
		public FileConverterThread(String scenario, JCheckBox[] timeSteps) {
			this.scenario = scenario;
			this.timeSteps = timeSteps;
		}
		
		@Override
		public void run() {
			H5File hdf5File = null;
			try {
				FileFormat hdf5Format = null;
				hdf5Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
				File hdf5FileLocation = new File(file_outputDir, scenario + ".h5");	
				hdf5File = (H5File)hdf5Format.createFile(hdf5FileLocation.getName(), FileFormat.FILE_CREATE_DELETE);
				System.out.println("File: " + hdf5File);
				hdf5File.open();
				
				// We will use the first file to read all the time steps...
				boolean firstFile = true;
				
				//NTAB
				if(jComboBox_fileType.getSelectedItem().toString().equals(NTAB)) {
					DataStructure ntabData = gridsByTimeAndScenario.get(scenario).values().iterator().next().extractNTABData();
					int timeStepIndex = 0;
					for(JCheckBox timeStep: timeSteps) {
						if(!timeStep.isSelected()) continue;
						addDataFromFiles(ntabData, hdf5File, "plot" + timeStep.getText(), firstFile, timeStepIndex);
						timeStepIndex++;
						firstFile = false;
					}
				//STOMP
				} else if(jComboBox_fileType.getSelectedItem().toString().equals(STOMP)) {
					for(JCheckBox timeStep: timeSteps) {
						if(!timeStep.isSelected()) continue;
						GridParser parser = gridsByTimeAndScenario.get(scenario).get(Integer.parseInt(timeStep.getText()));
						addDataFromFolders(parser, hdf5File, "plot" + timeStep.getText(), firstFile);
						firstFile = false;
					}
					computeStatistics(hdf5File, timeSteps.length);
				//TECPLOT
				} else if(jComboBox_fileType.getSelectedItem().toString().equals(TECPLOT)) {
					// Only file type with the option to be a single folder or multiple folders
					if(jComboBox_folderStructure.getSelectedItem().equals(SCENARIO_PER_FILE)) {
						DataStructure ntabData = gridsByTimeAndScenario.get(scenario).values().iterator().next().extractTecplotData();
						int timeStepIndex = 0;
						for(JCheckBox timeStep: timeSteps) {
							if(!timeStep.isSelected()) continue;
							addDataFromFiles(ntabData, hdf5File, "plot" + timeStep.getText(), firstFile, timeStepIndex);
							timeStepIndex++;
							firstFile = false;
						}
					} else if(jComboBox_folderStructure.getSelectedItem().equals(SCENARIO_PER_FOLDER)) {
						for(JCheckBox timeStep: timeSteps) {
							if(!timeStep.isSelected()) continue;
							GridParser parser = gridsByTimeAndScenario.get(scenario).get(Integer.parseInt(timeStep.getText()));
							addDataFromFolders(parser, hdf5File, "plot" + timeStep.getText(), firstFile);
							firstFile = false;
						}
					}
				}
				System.out.println("Writing the file to disk:");
				FileUtils.copyFile(hdf5File, hdf5FileLocation);
				System.out.println("Done");
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(FileBrowser.this, Arrays.toString(e.getStackTrace()), e.getMessage(), JOptionPane.ERROR_MESSAGE);
				hdf5File.delete(); // Remove the file					
			} finally {
				if(hdf5File != null)  {
					try {
						hdf5File.close();
					} catch (HDF5Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				processedTasks++; // For the monitor 
			}
		}
	}

	private void jButton_inputDirActionPerformed(ActionEvent evt) throws GridError {
		// Open a folder
		gridsByTimeAndScenario = new TreeMap<String, Map<Integer, GridParser>>();
		statisticsByDataField = new TreeMap<String, List<Float>>();

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("C:\\"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = chooser.showOpenDialog(null);

		if(returnValue == JFileChooser.APPROVE_OPTION) {

			file_inputDir = chooser.getSelectedFile();

			file_outputDir = new File(file_inputDir.getAbsolutePath() + "_hdf5");

			if(file_inputDir != null && file_inputDir.isDirectory()) {

				statusLabel.setText("Loading directory: " + file_inputDir);
				FileBrowser.this.validate();
				FileBrowser.this.repaint();

				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	
				getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				Thread readThread = new Thread(new Runnable() {

					@Override
					public void run() {					

						// Figure out what we're reading
						String folderStructure = FileBrowser.this.jComboBox_folderStructure.getSelectedItem().toString();
						
						if(folderStructure.equals(SCENARIO_PER_FOLDER)) {
							parseFolderStucture(file_inputDir);
						} else if(folderStructure.equals(SCENARIO_PER_FILE)) {
							parseSingleFolder(file_inputDir);	
						}
						
						// Sanity check on the number of time steps matching in each scenario...
						
						// Bin by time steps found, ntab files seem to have scenarios that stop short of the max number of time steps...
						final Map<Integer, List<String>> temp = new TreeMap<Integer, List<String>>(); 						
						for(String scenario: gridsByTimeAndScenario.keySet()) {
							if(!temp.containsKey(gridsByTimeAndScenario.get(scenario).keySet().size()))
								temp.put(gridsByTimeAndScenario.get(scenario).keySet().size(), new ArrayList<String>());
							temp.get(gridsByTimeAndScenario.get(scenario).keySet().size()).add(scenario);
						}
						
						
						final Integer[] variableSteps =  temp.keySet().toArray(new Integer[]{});
						
						//Exclude everything that doesn't have the max number of timesteps
						Object scenarioToUse;
						if(temp.keySet().size() > 1) {
							TimestepSelectionSlider slider = new TimestepSelectionSlider(variableSteps, temp);
							JOptionPane.showConfirmDialog(null, slider, "Timestep Selection", JOptionPane.DEFAULT_OPTION);
							int indexToUse = slider.getValue();
							String tossedScenarios = "You have selected to include " + variableSteps[indexToUse] + " time steps. " +
									"The following scenarios contained a smaller number and will be removed:\n";
							scenarioToUse = temp.get(variableSteps[indexToUse]).get(0);
							for(int i=0; i < indexToUse; i++){
								tossedScenarios+= "[" + variableSteps[i] + "] " +  temp.get(variableSteps[i]) + "\n";
								for(String scenario: temp.get(variableSteps[i])){
									gridsByTimeAndScenario.remove(scenario);
								}
							}
							if(indexToUse != 0){
								final String finalMessage = tossedScenarios;
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane.showMessageDialog(FileBrowser.this, finalMessage);									
									}								
								});	
							}
						}
						else{
							scenarioToUse = gridsByTimeAndScenario.keySet().toArray()[0];
						}
						Object[] scenarios = gridsByTimeAndScenario.keySet().toArray();

						
						// Assumes 1 scenario and 1 time step actually exists
						Object[] timeSteps = gridsByTimeAndScenario.get(scenarioToUse).keySet().toArray();
						Object[] data;
						try {							
							data = gridsByTimeAndScenario.get(scenarioToUse).get(timeSteps[0]).getDataTypes(jComboBox_fileType.getSelectedItem().toString());
						} catch (GridError e) {
							e.printStackTrace(); // Can't continue
							return;
						}

						System.out.println("Scenarios: " + Arrays.toString(scenarios));
						System.out.println("Times: " + Arrays.toString(timeSteps));
						System.out.println("Data: " + Arrays.toString(data));

						checkList_timesteps.setListData(timeSteps, true);
						checkList_scenarios.setListData(scenarios, true);
						checkList_dataFields.setListData(data, false);

						checkList_dataFields.setEnabled(true);
						checkList_timesteps.setEnabled(true);
						checkList_scenarios.setEnabled(true);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {

								jTextField_inputDir.setText(file_inputDir.getAbsolutePath());
								jTextField_outputDir.setText(file_outputDir.getAbsolutePath());				

								setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

								statusLabel.setText("");
								FileBrowser.this.repaint();		
							}							
						});
					}

				});
				readThread.start();
			}	
		} else {
			return;
		}

	}
		
	private void parseFolderStucture(File parentDirectory) {
		for(File subFile: file_inputDir.listFiles()) {
			if(subFile.isDirectory()) {		
				if(!gridsByTimeAndScenario.containsKey(subFile.getName()))
					gridsByTimeAndScenario.put(subFile.getName(), new TreeMap<Integer, GridParser>());
				// We are parsing a new set
				// Look for the plot files
				List<Integer> sortedSteps = new ArrayList<Integer>();
				Map<Integer, File> files = new HashMap<Integer, File>();
				for(File file: subFile.listFiles()) {
					if(file.getName().startsWith("plot.")) {
						int timeStep = Integer.parseInt(file.getName().replaceAll("plot.", ""));
						sortedSteps.add(timeStep);
						files.put(timeStep, file);
					}
				}
				Collections.sort(sortedSteps);
				int timeStep = 0;
				for(int sortedStep: sortedSteps) {
					gridsByTimeAndScenario.get(subFile.getName()).put(timeStep, new GridParser(files.get(sortedStep).getAbsolutePath()));
					timeStep++;
				}
			}
		}
	}
	
	private void parseSingleFolder(File directory) {
		String fileType = jComboBox_fileType.getSelectedItem().toString();
		for(File subFile: file_inputDir.listFiles()) {
			// Filter on ntab files
			if(fileType.equals(NTAB) && subFile.getName().endsWith(".ntab")) {
				// File the grid	
				String name = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
				if(!gridsByTimeAndScenario.containsKey(name))
					gridsByTimeAndScenario.put(name, new TreeMap<Integer, GridParser>());
				parseNtabFile(subFile);
				// No extension for tecplot?
			} else if(fileType.equals(TECPLOT)) {
				String name = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
				if(!gridsByTimeAndScenario.containsKey(name))
					gridsByTimeAndScenario.put(name, new TreeMap<Integer, GridParser>());
				try {
					List<Integer> years = GridParser.getTecplotTimestep(subFile);
					for(int year: years) {
						GridParser thisTimeStep = new GridParser(subFile.getAbsolutePath(), year); 
						if(gridsByTimeAndScenario.get(name).containsKey(year)) {
							// In this case we really want to merge the two files...
							gridsByTimeAndScenario.get(name).get(year).mergeFile(subFile);
							//	System.out.println("Merging time step = " + yrs + " to map at " + name);
						} else {
							gridsByTimeAndScenario.get(name).put(year, thisTimeStep);
							//	System.out.println("Adding time step = " + yrs + " to map at " + name);
						}
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}	
	

	private void parseNtabFile(File subFile) {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(subFile));
			String firstLine = "";			
			while(!(firstLine = fileReader.readLine()).startsWith("index") && firstLine != null);
			fileReader.close();
	
			// We just need the first line
			// First line should start with index, header we can ignore it
			if(firstLine.startsWith("index")) {
				// Everything else will be nodes
				String[] tokens = firstLine.split("\\s+");
				// skip these 
				// index i j k element_ref nuft_ind x y z dx dy dz
				boolean read = false;
				for(String token: tokens) {
					if(read) {
						if(token.contains("."))
							token = token.split("\\.")[0];
						String years = token.replaceAll("\\D+", "");
						try {
							int yrs = Integer.parseInt(years);
							GridParser thisTimeStep = new GridParser(subFile.getAbsolutePath(), yrs); 
							String name = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
							if(gridsByTimeAndScenario.get(name).containsKey(yrs)) {
								// In this case we really want to merge the two files...
							
								gridsByTimeAndScenario.get(name).get(yrs).mergeFile(subFile);
								//	System.out.println("Merging time step = " + yrs + " to map at " + name);
							} else {
								gridsByTimeAndScenario.get(name).put(yrs, thisTimeStep);
								//	System.out.println("Adding time step = " + yrs + " to map at " + name);
							}
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
					if(token.equalsIgnoreCase("volume")) {
						read = true;
					}
				}
			} 
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void jButton_outputDirActionPerformed(ActionEvent evt) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("C:\\"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = chooser.showOpenDialog(null);
		if(returnValue == JFileChooser.APPROVE_OPTION) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			file_outputDir = chooser.getSelectedFile();
		} else {
			return;
		}		
		jTextField_outputDir.setText(file_outputDir.getAbsolutePath());
	}
	
	private void addDataFromFiles(DataStructure ntabData, H5File hdf5File, String plotFileName, boolean firstFile, int timeStepIndex) throws Exception {

		// Sending the print statements from the grid jar to a dummy output stream to keep them off the console
		PrintStream originalStream = System.out;
		if(!debug) {
			PrintStream dummyStream    = new PrintStream(new OutputStream(){
				public void write(int b) { }
			});
			System.setOut(dummyStream);
		}
		
		// Restoring the console print stream
		System.setOut(originalStream);

		// Get the root
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		Datatype dtype = hdf5File.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, -1);

		long startTime = System.currentTimeMillis();

		if(firstFile) {
			// Times/Timesteps
			int timeStep = 0;
			List<Float> timeStepsAsInts = new ArrayList<Float>();
			List<Float> timeStepsInYears = new ArrayList<Float>();
			JCheckBox[] timeSteps = checkList_timesteps.getListData();
			
			
			// Time steps are already in years ...
			for(JCheckBox cb: timeSteps) {	    		
				if(!cb.isSelected())
					continue;	   
				try {
					Integer ts = Integer.parseInt(cb.getText());
					float tsf = ts.floatValue();
					timeStepsInYears.add(tsf);
					timeStepsAsInts.add(new Integer(timeStep).floatValue());
				} catch (Exception e) {
					// Nothing to do...
				}
				timeStep++;
			}

			float[] timeStepArray = new float[timeStepsAsInts.size()];
			float[] timesArray = new float[timeStepsAsInts.size()]; 
			for(int i = 0; i < timesArray.length; i++) {
				timeStepArray[i] = timeStepsAsInts.get(i);
				timesArray[i] = timeStepsInYears.get(i);
			}

			//  deltas_y, deltas_x deltas_z, data_types, nodes_y, nodes_x, nodes_z, timestep
			Group dataGroup = hdf5File.createGroup("data", root);
			hdf5File.createScalarDS("x", dataGroup, dtype, new long[]{ntabData.x.length}, null, null, 0, ntabData.x);
			hdf5File.createScalarDS("y", dataGroup, dtype, new long[]{ntabData.y.length}, null, null, 0, ntabData.y);
			hdf5File.createScalarDS("z", dataGroup, dtype, new long[]{ntabData.z.length}, null, null, 0, ntabData.z);	

			hdf5File.createScalarDS("steps", dataGroup, dtype, new long[]{timeStepArray.length}, null, null, 0, timeStepArray);	
			hdf5File.createScalarDS("times", dataGroup, dtype, new long[]{timesArray.length}, null, null, 0, timesArray);			
		}

		System.out.println("Time to add grid info: " + (System.currentTimeMillis() - startTime));

		// Create a group for each time in the set
		Group timeStepGroup = hdf5File.createGroup(plotFileName, root);

		// Get the dimensions of the grid
		long[] dims3D = {ntabData.i, ntabData.j, ntabData.k};

		JCheckBox[] dataFields = checkList_dataFields.getListData();

		int iMax = new Long(dims3D[0]).intValue();
		int jMax = new Long(dims3D[1]).intValue();
		int kMax = new Long(dims3D[2]).intValue();

		for(JCheckBox dataField: dataFields) {
			startTime = System.currentTimeMillis();
			String field = dataField.getText();    		
			if(!dataField.isSelected())
				continue;    	
			if(field.equals("x") || field.equals("y") || field.equals("z"))
				continue;
			// Replacing strange characters in the field name
			String fieldClean = field.split(",")[0].replaceAll("\\+", "p").replaceAll("\\-", "n").replaceAll("\\(", "_").replaceAll("\\)", "");
			if(debug)
				System.out.print("\t\t\tAdding field: " + fieldClean + "...");	

			try {

				float[] dataAsFloats = ntabData.data.get(field)[timeStepIndex];
				float[] temp = new float[dataAsFloats.length];
				
				int counter = 0;
				// Ordering is different for ntab, i's, j's, then k's?
				for(int i = 1; i <= iMax; i++) {
					for(int j = 1; j <= jMax; j++) {
						for(int k = 1; k <= kMax; k++) {
							int nodeNumber = (k-1) * iMax * jMax + (j-1) * iMax + (i);
							temp[counter] = Math.abs(dataAsFloats[nodeNumber-1]);
							
							counter++;
						}
					}	
				}


				hdf5File.createScalarDS(fieldClean, timeStepGroup, dtype, dims3D, null, null, 0, temp);
				if(debug)
					System.out.println("SUCCESS");
			} catch(Exception e) {
				if(debug)
					System.out.println("FAILED");
			}

			System.out.println("Time to add "+field+" info: " + (System.currentTimeMillis() - startTime));
		}
		if(debug)
			System.out.println("\t\tDone loading plot file: plotFileName");
	}
	
	//This function is called during a loop through time steps (each file)
	private void addDataFromFolders(GridParser parser, H5File hdf5File, String plotFileName, boolean firstFile) throws Exception {

		DataGrid grid = parser.extractStompData();
		
		// Get the root
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		Datatype dtype = hdf5File.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, -1);
		
		long startTime = System.currentTimeMillis();
		
		// Get the dimensions of the grid
		long[] dims3D = {grid.getSize().getX(), grid.getSize().getY(), grid.getSize().getZ()};
		int iMax = new Long(dims3D[0]).intValue();
		int jMax = new Long(dims3D[1]).intValue();
		int kMax = new Long(dims3D[2]).intValue();
		
		JCheckBox[] dataFields = checkList_dataFields.getListData();
		
		//Data group is filled from the first file only
		if(firstFile) {
			int timeStep = 0;
			List<Float> timeStepsAsFloats = new ArrayList<Float>();
			List<Float> timeStepsInYears = new ArrayList<Float>();
			JCheckBox[] scenarios = checkList_scenarios.getListData();
			JCheckBox[] timeSteps = checkList_timesteps.getListData();
			String firstScenario = scenarios[0].getText();
			for(JCheckBox cb: timeSteps) {
				if(!cb.isSelected()) continue;
				Integer ts = 0;
				try {
					ts = Integer.parseInt(cb.getText());
					float tsf = ts.floatValue();
					timeStepsAsFloats.add(tsf);
				} catch (Exception e) {
					timeStepsAsFloats.add(new Integer(timeStep).floatValue());
				}
				try {
					timeStepsInYears.add(new Double(gridsByTimeAndScenario.get(firstScenario).get((ts)).extractStompData().getTimestep()).floatValue());
				} catch (Exception e) {
					// hmm...
				}
				timeStep++;
			}

			float[] timeStepArray = new float[timeStepsAsFloats.size()];
			float[] timesArray = new float[timeStepsAsFloats.size()]; 
			for(int i = 0; i < timesArray.length; i++) {
				timeStepArray[i] = timeStepsAsFloats.get(i);
				timesArray[i] = timeStepsInYears.get(i);
			}
			//	System.out.println("Timesteps: " + Arrays.toString(timeStepArray));
			//	System.out.println("Times: " + Arrays.toString(timesArray));


			//  deltas_y, deltas_x deltas_z, data_types, nodes_y, nodes_x, nodes_z, timestep
			Group dataGroup = hdf5File.createGroup("data", root);
			float[] xCoordinates = new float[grid.getSize().getX()];
			float[] yCoordinates = new float[grid.getSize().getY()];
			float[] zCoordinates = new float[grid.getSize().getZ()];

			for(int i = 0; i < xCoordinates.length; i++) {
				xCoordinates[i] = new Double(grid.getFieldValues("x").getValue(i)).floatValue();
			}
			for(int i = 0; i < yCoordinates.length; i++) {
				yCoordinates[i] = new Double(grid.getFieldValues("y").getValue(i*xCoordinates.length)).floatValue();
			}
			for(int i = 0; i < zCoordinates.length; i++) {
				zCoordinates[i] = new Double(grid.getFieldValues("z").getValue(i*(xCoordinates.length*yCoordinates.length))).floatValue();
			}
			
			hdf5File.createScalarDS("x", dataGroup, dtype, new long[]{xCoordinates.length}, null, null, 0, xCoordinates);
			hdf5File.createScalarDS("y", dataGroup, dtype, new long[]{yCoordinates.length}, null, null, 0, yCoordinates);
			hdf5File.createScalarDS("z", dataGroup, dtype, new long[]{zCoordinates.length}, null, null, 0, zCoordinates);	
			
			hdf5File.createScalarDS("steps", dataGroup, dtype, new long[]{timeStepArray.length}, null, null, 0, timeStepArray);	
			hdf5File.createScalarDS("times", dataGroup, dtype, new long[]{timesArray.length}, null, null, 0, timesArray);
			
			// Make a float array to add the porosity info, should be flat
			File inputFile = new File(file_inputDir, firstScenario +"\\input");
			if(inputFile.exists()){
				BufferedReader fileReader = new BufferedReader(new FileReader(inputFile));
				String line = "";
				while(!(line = fileReader.readLine()).startsWith("~Mechanical Properties Card") && line != null);
				while((line = fileReader.readLine()).startsWith("#") && line != null);
				fileReader.close();
				System.out.println(line);
				String[] parts = line.split(",");
				parts = parts[3].split(":");
				File porosityFile;
				if(parts.length > 1) porosityFile = new File(file_inputDir, firstScenario + "\\" + parts[1]);
				else porosityFile = new File(file_inputDir, firstScenario + "\\hack.hack.hack");
				if(porosityFile.exists()){
					BufferedReader porosityReader = new BufferedReader(new FileReader(porosityFile));
					line = "";
					int index=0;
					float[] dataAsDoubles = new float[(int)(dims3D[0]*dims3D[1]*dims3D[2])];
					while((line = porosityReader.readLine()) != null){
						try{
							dataAsDoubles[index] = Float.parseFloat(line);
							index++;
						} catch(NumberFormatException ne){
							System.out.println("Error in reading the porosity input files - there seems to be something that is not a float");
						}
					}
					porosityReader.close();
					float[] dataAsFloats = new float[dataAsDoubles.length];

					int counter = 0;	
					for(int i = 1; i <= iMax; i++) {
						for(int j = 1; j <= jMax; j++) {	
							for(int k = 1; k <= kMax; k++) {
								int nodeNumber = (k-1) * iMax * jMax + (j-1) * iMax + (i);
								dataAsFloats[counter] = Math.abs(dataAsDoubles[nodeNumber-1]);
								counter++;
							}
						}	
					}
					hdf5File.createScalarDS("porosities", dataGroup, dtype, dims3D, null, null, 0, dataAsFloats);
				}
				System.out.println("Time to add porosity info: " + (System.currentTimeMillis() - startTime));
			}
		}
		
		// Create a group for each time in the set
		Group timeStepGroup = hdf5File.createGroup(plotFileName, root);
		
		
		for(JCheckBox dataField: dataFields) {
			startTime = System.currentTimeMillis();
			String field = dataField.getText();
			if(!dataField.isSelected()) continue;
			if(field.equals("x") || field.equals("y") || field.equals("z")) continue;
			// Replacing strange characters in the field name
			String fieldClean = field.split(",")[0].replaceAll("\\+", "p").replaceAll("\\-", "n").replaceAll("\\(", "_").replaceAll("\\)", "");
			System.out.print("\tAdding " + fieldClean + "... ");
			
			try {
				// Convert to float[] and reorder for hdf5
				//long getDataStartTime = System.currentTimeMillis();
				float[] dataAsDoubles = grid.getFieldValues(field).getValues();
				//System.out.println(" Time to read info: " + (System.currentTimeMillis() - getDataStartTime));
				
				float[] dataAsFloats = new float[dataAsDoubles.length];
				List<Float> stats = new ArrayList<Float>();
				
				int counter = 0;
				float sum = 0;
				float min = Float.MAX_VALUE;
				float max = Float.MIN_VALUE;
				for(int i = 1; i <= iMax; i++) {
					for(int j = 1; j <= jMax; j++) {	
						for(int k = 1; k <= kMax; k++) {				
							int nodeNumber = (k-1) * iMax * jMax + (j-1) * iMax + (i);
							dataAsFloats[counter] = Math.abs(dataAsDoubles[nodeNumber-1]);
							sum = sum + dataAsFloats[counter];
							if(dataAsFloats[counter] < min) min = dataAsFloats[counter];
							if(dataAsFloats[counter] > max) max = dataAsFloats[counter];
							counter++;
						}
					}	
				}
				if(statisticsByDataField.containsKey(fieldClean)) {
					stats.add(0, Math.min(statisticsByDataField.get(fieldClean).get(0), min));
					stats.add(1, sum/counter + statisticsByDataField.get(fieldClean).get(1));//Sum averages now, divide by time step later
					stats.add(2, Math.max(statisticsByDataField.get(fieldClean).get(2), max));
				} else {
					stats.add(0, min);
					stats.add(1, sum/counter); //Average for time step
					stats.add(2, max);
				}
				statisticsByDataField.put(fieldClean, stats);
				
				hdf5File.createScalarDS(fieldClean, timeStepGroup, dtype, dims3D, null, null, 0, dataAsFloats);
				System.out.print("SUCCESS. ");
			} catch(Exception e) {
				System.out.print("FAILED. ");
			}
			System.out.println("Time to add: " + (System.currentTimeMillis() - startTime) + " ms");
		}
		System.out.println("Done loading plot file: " + plotFileName);
	}
	
	private void computeStatistics(H5File hdf5File, int timeSteps) throws Exception {
		// Get the root
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		Datatype dtype = hdf5File.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, -1);
		
		Group statisticsGroup;
		statisticsGroup = hdf5File.createGroup("Statistics", root);
		for(String dataField: statisticsByDataField.keySet()) {
			float[] statsArray = new float[statisticsByDataField.get(dataField).size()];
			statsArray[0] = statisticsByDataField.get(dataField).get(0); //min
			statsArray[1] = statisticsByDataField.get(dataField).get(1)/timeSteps; //average
			statsArray[2] = statisticsByDataField.get(dataField).get(2); //max
			hdf5File.createScalarDS(dataField, statisticsGroup, dtype, new long[]{statsArray.length}, null, null, 0, statsArray);
		}
	}
	
	public static void main(String args[]) {
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {
				new FileBrowser().setVisible(true);
			}
		});
	}
}
