package wizardPages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import objects.E4DSensors;
import objects.Sensor;
import objects.SensorSetting;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import hdf5Tool.HDF5Interface;
import utilities.Constants;
import utilities.Point3i;
import utilities.E4DDialog;
import utilities.E4DRunDialog;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting detection criteria for each sensor type to be used.
 * See line 608
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_LeakageCriteria extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	protected Map<String, Integer> num_duplicates = new HashMap<String, Integer>();
	private Map<String, SensorData> sensorData;
	private Button runE4DButton;
	
	private boolean isCurrentPage = false;
	
	protected Page_LeakageCriteria(STORMData data) {
		super("Leakage Criteria");
		this.data = data;	
	}
	
	public class SensorData {
		
		private Button addButton;
		public String sensorType;
		public String sensorName;
		public String alias;
		public float cost;
		public Trigger trigger;
		public DeltaType deltaType;
		public float detectionThreshold;
		public float minZ;
		public float maxZ;
		
		private Float maxZBound;
		private Float minZBound;
		public boolean isIncluded;
		private boolean isDuplicate;
		
		private Label nodeLabel;
		private Label sensorTypeLabel;
		private Label detectionLabel;
		private Label minZLabel;
		private Label maxZLabel;
		
		private Text aliasText;
		private Text costText;
		private Text detectionText;
		private Text minZText;
		private Text maxZText;
		
		private Combo thresholdCombo;
		public String specificType;
		
		// New class for storing the data about one particular sensor type (IAM)
		public SensorData(String specificType, SensorSetting sensorSettings) {
			String[] tokens = specificType.split("_");
			sensorType = tokens[0];
			sensorName = sensorType;
			alias = sensorName;
			detectionThreshold = Float.parseFloat(tokens[2]);
			trigger = iamToTrigger(tokens[1]);
			deltaType = sensorSettings.getDeltaType();
			cost = sensorSettings.getSensorCost();
			minZ = minZBound = sensorSettings.getGlobalMinZ();
			maxZ = maxZBound = sensorSettings.getGlobalMaxZ();
			this.specificType = specificType;
			alias = tokens[0] + "_" + tokens[2];				
		}
		
		//Class for storing the data about one particular sensor type (HDF5)
		public SensorData(SensorSetting sensorSettings, String sensorName) {
			
			if(sensorSettings.getType().equals(sensorName)) isDuplicate = false;
			else isDuplicate = true;
			
			sensorType = sensorSettings.getType();
			this.sensorName = sensorName;
			alias = sensorName;
			isIncluded = false; //By default
			cost = sensorSettings.getSensorCost();
			minZ = minZBound = sensorSettings.getGlobalMinZ();
			maxZ = maxZBound = sensorSettings.getGlobalMaxZ();
			
			deltaType = sensorSettings.getDeltaType();
			detectionThreshold = sensorSettings.getDetectionThreshold();
			trigger = Trigger.MINIMUM_THRESHOLD;
			specificType = sensorName + "_min_" + detectionThreshold;
			
			// Trigger should be relative delta when pressure
			if(sensorType.toLowerCase().contains("pressure") || sensorType.trim().toLowerCase().equals("p") || sensorType.contains("Electrical Conductivity")) {
				trigger = Trigger.RELATIVE_DELTA;
				specificType = sensorName + "_rel_" + detectionThreshold;
			}
			
			// Trigger should be maximum threshold when pH
			else if(sensorType.trim().toLowerCase().equals("ph")) {
				trigger = Trigger.MAXIMUM_THRESHOLD;
				specificType = sensorName + "_max_" + detectionThreshold;
			}
			
			// Exceptions for "All_SENSORS"
			if(sensorName.contains("all")) {
				alias = "All Selected Sensors";
			}
			
			// Exceptions for ERT
			if(sensorName.contains("Electrical Conductivity")) {
				alias = "ERT_" + detectionThreshold;
			}
		}
		
		public void buildUI(String type) {
			//Add a button here
			if(isDuplicate){
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						sensorData.remove(sensorName);
						data.getSet().getSensorSettings().remove(sensorName);
						loadPage();
						fixMacBug();
					}
			    });
			    addButton.setText("-");
			}
			else{
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						if(!num_duplicates.containsKey(sensorType)) num_duplicates.put(sensorType, 1);
						addSensor(sensorType, sensorType + "_" + num_duplicates.get(sensorType));
						num_duplicates.put(sensorType, num_duplicates.get(sensorType)+1);
						loadPage();
						fixMacBug();
						if(num_duplicates.get(sensorType)==100) //rare, but prevent more than 99 duplicates so statistics doesn't throw an error
							for(SensorData temp: sensorData.values())
								if(temp.sensorName==sensorType)
									temp.addButton.setEnabled(false);
					}
			    });
			    addButton.setText("+");
			}
			
		    
			// Include button
			Button includeButton = new Button(container,  SWT.CHECK);
			includeButton.setSelection(isIncluded);
			includeButton.setText(sensorType);
			for(SensorData temp: sensorData.values()) {
				if(temp.isIncluded) {
					errorFound(false, "  Must select a monitoring parameter.");
					break;
				}
				errorFound(true, "  Must select a monitoring parameter.");
			}
			includeButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { 
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					isIncluded = ((Button)e.getSource()).getSelection();
					if(isIncluded==true)
						data.getSet().getInferenceTest().setMinimumForType(((Button)e.getSource()).getText(), 1);
					else
						data.getSet().getInferenceTest().setMinimumForType(((Button)e.getSource()).getText(), 0);
					toggleEnabled();
					
					//Special handling if errors are negated when parameters are unchecked...
					//We have to search through all possible errors to see if any are negated
					boolean checkError = true;
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					boolean costError = false;
					boolean detectionError = false;
					boolean botError = false;
					boolean botBoundError = false;
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) //Skip unchecked parameters
							continue;
						else
							checkError = false;
						//Alias
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) //Skip unchecked parameters
								continue;
							if(temp.alias.trim().equals(temp2.alias.trim()) && !temp.sensorName.equals(temp2.sensorName)) {
								duplicateError = true;
								temp.aliasText.setForeground(Constants.red);
							}
						}
						if(temp.alias.contains(",")) //Contains a comma
							commaError = true;
						if(temp.alias.isEmpty()) //No alias
							emptyError = true;
						//Cost
						if(!Constants.isValidFloat(temp.costText.getText()))
							costError = true;
						//Detection
						if(!Constants.isValidFloat(temp.detectionText.getText()))
							detectionError = true;
						//Zone bottom
						if(!Constants.isValidFloat(temp.minZText.getText()))
							botError = true;
						else {
							float minZValue = Float.parseFloat(temp.minZText.getText());
							if (minZValue < temp.minZBound || minZValue > temp.maxZBound)
								botBoundError = true;
						}
						//Zone top
						if(!Constants.isValidFloat(temp.maxZText.getText()))
							topError = true;
						else {
							float maxZValue = Float.parseFloat(temp.maxZText.getText());
							if (maxZValue < temp.minZBound || maxZValue > temp.maxZBound)
								topBoundError = true;
						}
					}
					errorFound(checkError, "  Must select a monitoring parameter.");
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					errorFound(costError, "  Cost is not a real number.");
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					
					//Special handling of red text for duplicates
					if (duplicateError==false)
						for(SensorData data: sensorData.values())
							if (data.isIncluded && !data.alias.contains(",") && !data.alias.isEmpty())
								data.aliasText.setForeground(Constants.black);
				}
			});
			
			
			//Alias Input
			aliasText = new Text(container, SWT.BORDER | SWT.SINGLE);
			aliasText.setText(sensorData.get(type).alias);
			aliasText.setForeground(Constants.black);
			aliasText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e){
					aliasText = ((Text)e.getSource());
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						//temp.aliasText.setForeground(black);
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) continue; //Skip unchecked parameters
							if(temp.aliasText.getText().trim().equals(temp2.aliasText.getText().trim()) && !temp.sensorName.equals(temp2.sensorName)) {
								temp.aliasText.setForeground(Constants.red);
								duplicateError = true;
							}
						}
						if(temp.aliasText.getText().contains(",")) { //Contains a comma
							temp.aliasText.setForeground(Constants.red);
							commaError = true;
						}
						if(temp.aliasText.getText().trim().isEmpty()) { //Empty alias
							temp.aliasText.setForeground(Constants.red);
							emptyError = true;
						}
						if (duplicateError==false && commaError==false && emptyError==false) { //No errors
							temp.aliasText.setForeground(Constants.black);
							temp.alias = temp.aliasText.getText();
						}
					}
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
				}
			});
			GridData aliasTextData = new GridData(SWT.FILL, SWT.END, false, false);
			aliasTextData.widthHint = 60;
			aliasText.setLayoutData(aliasTextData);
			
			
			//Cost Input
			costText = new Text(container, SWT.BORDER | SWT.SINGLE);
			costText.setText(String.valueOf(sensorData.get(type).cost));
			costText.setForeground(Constants.black);
			costText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					costText = ((Text)e.getSource());
					boolean costError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.costText.getText())) { //Valid number
							temp.costText.setForeground(Constants.black);
							temp.cost = Float.valueOf(temp.costText.getText());
						} else { //Not a valid number
							temp.costText.setForeground(Constants.red);
							costError = true;
						}
					}
					errorFound(costError, "  Cost is not a real number.");
				}
			});
			GridData costTextData = new GridData(SWT.FILL, SWT.END, false, false);
			costTextData.widthHint = 60;
			costText.setLayoutData(costTextData);
			
			
			//Detection Criteria
			thresholdCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			thresholdCombo.add(Trigger.MAXIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.MINIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.RELATIVE_DELTA.toString());
			thresholdCombo.add(Trigger.ABSOLUTE_DELTA.toString());
			thresholdCombo.setText(trigger.toString());
			if(trigger == Trigger.MAXIMUM_THRESHOLD) //TODO: Catherine, please review this text to verify that the tooltips are using the correct terminology
				thresholdCombo.setToolTipText("Leak when concentration is less than value");
			else if(trigger == Trigger.MINIMUM_THRESHOLD)
				thresholdCombo.setToolTipText("Leak when concentration is greater than value");
			else if(trigger == Trigger.RELATIVE_DELTA)
				thresholdCombo.setToolTipText("Leak when change from original concentration relative to the initial concentration (decimal) exceeds value");
			else if(trigger == Trigger.ABSOLUTE_DELTA)
				thresholdCombo.setToolTipText("Leak when change from original concentration exceeds value");
			thresholdCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if(((Combo)e.getSource()).getText().equals(Trigger.MINIMUM_THRESHOLD.toString())) {
						trigger = Trigger.MINIMUM_THRESHOLD;
						thresholdCombo.setToolTipText("Leak when concentration is greater than value");
					} else if(((Combo)e.getSource()).getText().equals(Trigger.MAXIMUM_THRESHOLD.toString())) {
						trigger = Trigger.MAXIMUM_THRESHOLD;
						thresholdCombo.setToolTipText("Leak when concentration is less than value");
					} else if(((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_DELTA.toString())) {
						trigger = Trigger.RELATIVE_DELTA;
						thresholdCombo.setToolTipText("Leak when change from original concentration relative to the initial concentration (decimal) exceeds value");
					} else { //(((Combo)e.getSource()).getText().equals(Trigger.ABSOLUTE_DELTA.toString()))
						trigger = Trigger.ABSOLUTE_DELTA;
						thresholdCombo.setToolTipText("Leak when change from original concentration exceeds value");
					}
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
					getSpecificType();
				}
			});
			GridData thresholdComboData = new GridData(SWT.FILL, SWT.END, false, false);
			thresholdComboData.widthHint = 105;
			thresholdCombo.setLayoutData(thresholdComboData);			
			
			//Detection Value
			detectionText = new Text(container, SWT.BORDER | SWT.SINGLE);
			detectionText.setText(String.valueOf(sensorData.get(type).detectionThreshold));
			if(HDF5Interface.getStatistic(type, 0)!=null)
				detectionText.setToolTipText("Minimum = " + HDF5Interface.getStatistic(type, 0) + "; Maximum = " + HDF5Interface.getStatistic(type, 2));
			detectionText.setForeground(Constants.black);
			detectionText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					detectionText = ((Text)e.getSource());
					boolean detectionError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.detectionText.getText())) { //Valid number
							temp.detectionText.setForeground(Constants.black);
							temp.detectionThreshold = Float.valueOf(temp.detectionText.getText());
						} else { //Not a valid number
							temp.detectionText.setForeground(Constants.red);
							detectionError = true;
						}
					}
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
					getSpecificType();
				}
			});
			GridData detectionInputData = new GridData(SWT.FILL, SWT.END, false, false);
			detectionInputData.widthHint = 60;
			detectionText.setLayoutData(detectionInputData);
			
			
			// Set minimum z
			minZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			minZText.setText(String.valueOf(sensorData.get(type).minZ));
			minZText.setForeground(Constants.black);
			minZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					minZText = ((Text)e.getSource());
					boolean botError = false;
					boolean botBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.minZText.getText())) { //Valid number
							float minZValue = Float.valueOf(temp.minZText.getText());
							if (minZValue < minZBound || minZValue > maxZBound) {
								temp.minZText.setForeground(Constants.red);
								botBoundError = true;
							} else {
								temp.minZText.setForeground(Constants.black);
								temp.minZ = minZValue;
							}
						} else { //Not a valid number
							temp.minZText.setForeground(Constants.red);
							botError = true;
						}
					}
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
				}
			});
			GridData minZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			minZTextData.widthHint = 60;
			minZText.setLayoutData(minZTextData);
			
			
			// Set maximum z
			maxZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			maxZText.setText(String.valueOf(sensorData.get(type).maxZ));
			maxZText.setForeground(Constants.black);
			maxZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					maxZText = ((Text)e.getSource());
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(Constants.isValidFloat(temp.maxZText.getText())) { //Valid number
							float maxZValue = Float.valueOf(temp.maxZText.getText());
							if (maxZValue < minZBound || maxZValue > maxZBound) {
								temp.maxZText.setForeground(Constants.red);
								topBoundError = true;
							} else {
								temp.maxZText.setForeground(Constants.black);
								temp.maxZ = maxZValue;
							}
						} else { //Not a valid number
							temp.maxZText.setForeground(Constants.red);
							topError = true;
						}
					}
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
				}
			});
			GridData maxZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			maxZTextData.widthHint = 60;
			maxZText.setLayoutData(maxZTextData);
			
			// Hide unused fields for ERT sensors
			if(type.contains("Electrical Conductivity")) {
				addButton.setVisible(false);
				aliasText.setEnabled(false);
				thresholdCombo.setEnabled(false);
				detectionText.setEnabled(false);
				minZText.setEnabled(false);
				maxZText.setEnabled(false);
			}
			
			// Hide unused fields for IAM
			if(data.fileType=="iam") {
				addButton.setVisible(false);
				aliasText.setEnabled(false);
				thresholdCombo.setEnabled(false);
				detectionText.setEnabled(false);
			}
			
			// Hide unused fields for ALL_SENSORS
			if(type.contains("all")) {
				addButton.setVisible(false);
				aliasText.setEnabled(false);
				costText.setToolTipText("This sensor will detect as a combination of all selected sensors, but will move as one sensor during optimization.");
				thresholdCombo.setVisible(false);
				detectionText.setVisible(false);
			}
			
			toggleEnabled();
		}
		
		private void toggleEnabled() {
			if(sensorTypeLabel != null && !sensorTypeLabel.isDisposed())
				sensorTypeLabel.setEnabled(isIncluded);
			if(aliasText != null && !aliasText.isDisposed() && !alias.contains("ERT") && data.fileType!="iam")
				aliasText.setEnabled(isIncluded);
			if(costText != null && !costText.isDisposed())
				costText.setEnabled(isIncluded);
			if(thresholdCombo != null && !thresholdCombo.isDisposed() && !sensorType.contains("all") && !alias.contains("ERT") && data.fileType!="iam")
				thresholdCombo.setEnabled(isIncluded);					
			if(detectionText != null && !detectionText.isDisposed() && !sensorType.contains("all") && !alias.contains("ERT") && data.fileType!="iam")
				detectionText.setEnabled(isIncluded);
			if(detectionLabel != null && !detectionLabel.isDisposed())
				detectionLabel.setEnabled(isIncluded);
			if(minZLabel != null && !minZLabel.isDisposed())
				minZLabel.setEnabled(isIncluded);
			if(minZText != null && !minZText.isDisposed() && !alias.contains("ERT"))
				minZText.setEnabled(isIncluded);
			if(maxZLabel != null && !maxZLabel.isDisposed())
				maxZLabel.setEnabled(isIncluded);
			if(maxZText != null && !maxZText.isDisposed() && !alias.contains("ERT"))
				maxZText.setEnabled(isIncluded);
		}
		
		private void getSpecificType() {
			if(trigger == Trigger.MAXIMUM_THRESHOLD)
				specificType = sensorType + "_max_" + detectionThreshold;
			else if(trigger == Trigger.MINIMUM_THRESHOLD)
				specificType = sensorType + "_min_" + detectionThreshold;
			else if(trigger == Trigger.RELATIVE_DELTA)
				specificType = sensorType + "_rel_" + detectionThreshold;
			else // if(trigger == Trigger.ABSOLUTE_DELTA)
				specificType = sensorType + "_abs_" + detectionThreshold;

		}
	}
	
	@Override
	public void loadPage() {
		isCurrentPage = true;
		if(!DREAMWizard.errorMessage.getText().contains("  No nodes were found for the provided parameters."))
			DREAMWizard.errorMessage.setText("");
		for(Control control: container.getChildren())
			control.dispose(); // Remove the children.
		
		// Before we do anything, add E4D matrices to paretoMap
		E4DSensors.addERTSensor(data.getSet());
		
		// If we need to reset sensors, this boolean will be set to true
		if(data.needToResetMonitoringParameters) {
			data.needToResetMonitoringParameters = false;
			sensorData = new TreeMap<String, SensorData>(); // New UI
			
			// If we are dealing with H5 files, add all possible data types
			if(data.fileType=="hdf5" && !E4DSensors.ertDetectionTimes.isEmpty()) {
				for(String dataType: data.getSet().getAllPossibleDataTypes()) {
					if(data.getSensorSettings(dataType) != null) // Adds all sensors from the list
						sensorData.put(dataType, new SensorData(data.getSet().getSensorSettings(dataType), dataType));
					else // If the user went back after findings nodes, some sensors were removed and saved in another map
						sensorData.put(dataType, new SensorData(data.getSet().getRemovedSensorSettings(dataType), dataType));
				}
				data.getSet().resetRemovedSensorSettings();
			}
			
			// IAM values are already stored in paretoMap, but E4D files might also be stored in paretoMap
			else {
				for(String specificType: data.getSet().getParetoMap().keySet()) {
					String dataType = specificType.substring(0, specificType.indexOf("_"));
					sensorData.put(dataType, new SensorData(specificType, data.getSet().getSensorSettings(dataType)));
				}
			}
		}
		
		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		Font boldFontSmall = new Font(container.getDisplay(), new FontData("Helvetica", 10, SWT.BOLD));
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Leakage Criteria");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 7, 2));
		infoLabel1.setFont(boldFont);
		
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "After reading through the directory of realization outputs, DREAM will generate a table of monitoring parameters that the user can select. These parameters are specific to the included realizations. The selected monitoring parameters will be used in the optimization algorithm. The user may label what technology they will use to monitor each selected parameter in the \"Alias for Monitoring Technology\" box and then provide a realistic cost per monitoring technology if it is known; if not, the costs should be set equal. The detection criteria may be specified based on the relative change from initial conditions, absolute change from initial conditions, or a maximum or minimum threshold. If relative delta, absolute delta, or maximum threshold is selected, the given value and all values above are treated as detecting a leak. If minimum threshold is selected, that value and all values below are treated as detecting a leak.");	
			}
		});
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Select the monitoring parameters of interest, include a cost per appropriate sensor type, and set the detection criteria. NOTE: The minimum and maximum values are read from the first realization read by DREAM. These are provided to give the user an idea of the values present.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
		
		// Headers
		new Label(container, SWT.NULL);	// Blank filler
		Label monitorParams = new Label(container, SWT.LEFT);
		Label aliasLabel = new Label(container, SWT.LEFT);
		Label costPerSensor = new Label(container, SWT.LEFT);
		Label detectionCriteria = new Label(container, SWT.LEFT);
		Label detectionLabel = new Label(container, SWT.LEFT);
		Label minZLabel = new Label(container, SWT.LEFT);
		Label maxZLabel = new Label(container, SWT.LEFT);
		monitorParams.setText("Monitoring Parameter");
		aliasLabel.setText("Alias for Monitoring Technology");
		costPerSensor.setText("Cost per Sensor");
		detectionCriteria.setText("Detection Criteria");
		detectionLabel.setText("Detection Value");
		minZLabel.setText("Zone Bottom");
		maxZLabel.setText("Zone Top");
		monitorParams.setFont(boldFontSmall);
		aliasLabel.setFont(boldFontSmall);
		costPerSensor.setFont(boldFontSmall);
		detectionCriteria.setFont(boldFontSmall);
		detectionLabel.setFont(boldFontSmall);
		minZLabel.setFont(boldFontSmall);
		maxZLabel.setFont(boldFontSmall);
		
		for(SensorData sensor: sensorData.values()) {
			sensor.buildUI(sensor.sensorName);
		}
		
		Group parametersGroup = new Group(container, SWT.SHADOW_NONE);
		parametersGroup.setText("Nodes Found for Each Parameter");
		parametersGroup.setFont(boldFontSmall);
		parametersGroup.setLayout(new GridLayout(4,true));
		GridData tempData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		tempData.horizontalSpan = 10;
		parametersGroup.setLayoutData(tempData);
		
		Button findTriggeringNodes = new Button(parametersGroup, SWT.BALLOON);
		findTriggeringNodes.setText("Find triggering nodes");
		findTriggeringNodes.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				fixMacBug();
				
				// Checks if there are any new sensor settings to be added to paretoMap
				// Also saves sensorSetting information (i.e. cloudNodes, validNodes, sensorAliases, etc.)
				findTriggeringNodes();
			}	       
		});
		
		for (int i=0; i<3; i++)
			new Label(parametersGroup, SWT.NULL);
		
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);
			temp.nodeLabel = new Label(parametersGroup, SWT.WRAP);
			if(data.getSet().getSensorSettings(label) == null)
				data.getSet().resetSensorSettings(label);
			if(data.getSet().getSensorSettings(label).getValidNodes(null).size() > 0)
				temp.nodeLabel.setText(label+ ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size() + "   ");
			else
				temp.nodeLabel.setText(label+ ": Not set   ");
		}
		
		// If the user has the E4D module installed, allow the E4D buttons to show up
		String e4dModuleDirectory = Constants.userDir + File.separator + "e4d";
		File e4dDirectory = new File(e4dModuleDirectory);
		if (e4dDirectory.exists()) {
			final File e4dWellList = new File(Constants.userDir, "e4d" + File.separator + "ertWellLocationsIJ_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getScenarios().size() + ".txt");
			
			Composite composite_E4D = new Composite(container, SWT.NULL);
			GridLayout gridLayout_E4D = new GridLayout();
			gridLayout_E4D.numColumns = 3;
			composite_E4D.setLayout(gridLayout_E4D);
			GridData gridData_E4D = new GridData(GridData.FILL_HORIZONTAL);
			gridData_E4D.horizontalSpan=8;
			composite_E4D.setLayoutData(gridData_E4D);			
			
			//Add an info icon to explain the E4D Buttons
			Label infoLinkE4D = new Label(composite_E4D, SWT.NULL);
		  	infoLinkE4D.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
	  		infoLinkE4D.addListener(SWT.MouseUp, new Listener(){
	  			@Override
	  			public void handleEvent(Event event) {
	  				// TODO: Catherine edit text here! This info pop-up also needs to note the specific units that E4D needs
	  				MessageDialog.openInformation(container.getShell(), "Additional information", "After finding triggering nodes, the user may write input files for the E4D model. E4D is a three-dimensional (3D) modeling and inversion code designed for subsurface imaging and monitoring using static and time-lapse 3D electrical resistivity (ER) or spectral induced polarization (SIP) data.");	
	  			}
	  		});
	  		
	  		// Save the E4D files
	  		Button writeE4DButton = new Button(composite_E4D, SWT.PUSH);
	  		writeE4DButton.setText("  Write E4D File  ");
	  		writeE4DButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					fixMacBug();
					
					// Begin by identifying the parameter to build the file from
					List<String> list = new ArrayList<String>();
					String selectedParameter = null;
					int maximumWells = 30; //default value
					for(String label: sensorData.keySet()) {
						if (label.contains("Pressure"))
							list.add(label);
					}
					if (list.size() > 0) { // If pressure parameters are detected, open dialog
						E4DDialog dialog = new E4DDialog(container.getShell(), list);
						dialog.open();
						selectedParameter = dialog.getParameter();
						maximumWells = dialog.getMaximumWells();
						if(dialog.getReturnCode() == 1) // If the dialog box is closed, do nothing
							return;
					} else if (list.isEmpty()) { // If no pressure parameters, throw error
						DREAMWizard.errorMessage.setText("No pressure parameter exists to create an E4D file.");
						return;
					}
					
					// Returns the best well that fall within the threshold (currently 30)
					ArrayList<Point3i> wells = null;
					try {
						wells = data.runWellOptimizationE4D(selectedParameter, maximumWells);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					// For this to be empty, no change was seen at any node with the selected parameter (very rare)
					if (wells==null) {
						DREAMWizard.errorMessage.setText("No change was detected with the selected pressure parameter.");
						return;
					}
					
					// Now that we have our wells, print it out
					StringBuilder ijStringBuilder = new StringBuilder();
					for(Point3i well: wells)
						ijStringBuilder.append(Point3i.toCleanString(well) + "\n");
					File e4dWellFile = new File(Constants.userDir, "e4d" + File.separator + "ertWellLocationsIJ_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getScenarios().size() + ".txt");
					try{
						e4dWellFile.createNewFile();
						FileUtils.writeStringToFile(e4dWellFile, ijStringBuilder.toString());
						MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
						dialog.setText("Write E4D File: Success");
						dialog.setMessage("An E4D file was created that provides the 30 best well locations across all scenarios based on the selected pressure parameter. "
								+ "E4D will use these well locations to reduce computatational time.\n\nDirectory: " + e4dWellFile.getAbsolutePath());
						dialog.open();
						// TODO: Catherine edit text here!
					} catch (IOException e1) {
						MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
						dialog.setText("Write E4D File: Failed");
						dialog.setMessage("The program was unable to write out the optimized E4D well locations.\n\nDirectory: " + e4dWellFile.getAbsolutePath());
						dialog.open();
						e1.printStackTrace();
					}
					runE4DButton.setEnabled(e4dWellList.exists());
				}
			});
	  		
	  		// If the user has a well list that matches the scenario ensemble and size, allow the run E4D button to show up
	  		runE4DButton = new Button(composite_E4D, SWT.PUSH);
	  		runE4DButton.setText("  Run E4D  ");
	  		runE4DButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					fixMacBug();
					
					String[] list = new String[3];
					list[0] = list[1] = list[2] = "";
					for(String label: sensorData.keySet()) {
						if(label.toLowerCase().contains("brine saturation") || label.toLowerCase().contains("aqueous saturation"))
							list[0] = label;
						if(label.toLowerCase().contains("gas saturation"))
							list[1] = label;
						if(label.toLowerCase().contains("salt") || label.toLowerCase().contains("salinity"))
							list[2] = label;
					}
					E4DRunDialog dialog = new E4DRunDialog(container.getShell(), data.getSet().getScenarioEnsemble(), list[0], list[1], list[2], sensorData);
					dialog.open();
					if(dialog.getReturnCode() == 1) // If the dialog box is closed, do nothing
						return;
					if(System.getProperty("os.name").contains("Windows")) { // TODO: Is there a different script to run the Mac version?
						try {
							data.runE4DWindows(dialog, e4dWellList);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					loadPage();
				}
	  		});
	  		runE4DButton.setEnabled(e4dWellList.exists());
		}
		
		container.layout();	
		
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		boolean enableVis  = false;
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);
			if(temp.isIncluded &&  data.getSet().getSensorSettings(label).getValidNodes(null).size() > 0)
				enableVis = true;
		}
		
		DREAMWizard.visLauncher.setEnabled(enableVis);
		DREAMWizard.convertDataButton.setEnabled(false);
	} //ends load page
	
	@Override
	public void createControl(Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		
		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.addListener(SWT.Activate, new Listener() {
	        public void handleEvent(Event e) {
	            sc.setFocus();
	        }
	    });
		sc.addListener(SWT.MouseWheel, new Listener() {
	        public void handleEvent(Event event) {
	            int wheelCount = event.count;
	            wheelCount = (int) Math.ceil(wheelCount / 3.0f);
	            while (wheelCount < 0) {
	                sc.getVerticalBar().setIncrement(4);
	                wheelCount++;
	            }
	            
	            while (wheelCount > 0) {
	                sc.getVerticalBar().setIncrement(-4);
	                wheelCount--;
	            }
	            sc.redraw();
	        }
	    });
		GridData compositeData = new GridData(GridData.FILL, GridData.FILL, true, true);
		compositeData.heightHint = 400;
		compositeData.minimumHeight = 400;
		sc.setLayoutData(compositeData);
        sc.setExpandHorizontal(true);
        sc.getVerticalBar().setIncrement(20);
        sc.setExpandVertical(true);
        
        container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		layout.numColumns = 8;
		container.setLayout(layout);
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Checks if there are any new sensor settings to be added to paretoMap
		// Also saves sensorSetting information (i.e. cloudNodes, validNodes, sensorAliases, etc.)
		findTriggeringNodes();
		
		// Count the total nodes to verify that some valid nodes were found
		HashSet<Integer> nodes = new HashSet<Integer>();
		for(String label: data.getSet().getSensorSettings().keySet())
			nodes.addAll(data.getSet().getSensorSettings().get(label).getValidNodes(null));
		if(nodes.size()==0)
			errorFound(true, "  No nodes were found for the provided parameters.");
		
		// Calculate the volume of aquifer degraded
		volumeOfAquiferDegraded(nodes);
		
		// Write out some setup information
		System.out.println("Number of sensors = " + data.getSet().getSensorSettings().size());
		System.out.println("Number of time steps = " + SensorSetting.getYears().size());
		Float firstTime = SensorSetting.getYears().get(0);
		System.out.println("Average volume of aquifer degraded at first time step (" + firstTime.toString() + ") = " + SensorSetting.getAverageVolumeDegradedAtTimesteps().get(firstTime).toString());
		Float lastTime = SensorSetting.getYears().get(SensorSetting.getYears().size()-1);
		System.out.println("Average volume of aquifer degraded at last time step (" + lastTime.toString() + ") = " + SensorSetting.getAverageVolumeDegradedAtTimesteps().get(lastTime).toString());
	}
	
	
	private void addSensor(String dataType, String newName) {
		data.getSet().addSensorSetting(newName, dataType);
		data.getSet().getInferenceTest().setMinimumForType(newName, 1);
		sensorData.put(newName, new SensorData(data.getSet().getSensorSettings(newName), newName));
	}
	
	
	// Calculates a list of timesteps and a map of volume degraded by year
	private void volumeOfAquiferDegraded(HashSet<Integer> nodes) {
		
		// Loop through scenarios and sensor types to get a list of all detection times or timesteps
		// Also create a TTD map that combines all the selected sensors (for quicker VOD calculations)
		Map<String, HashMap<Integer, Float>> earliestDetectionForAllSensors = new HashMap<String, HashMap<Integer, Float>>(); //Scenario <NodeNumber, Detection>
		for(String scenario: data.getSet().getAllScenarios())
			earliestDetectionForAllSensors.put(scenario, new HashMap<Integer, Float>()); //initialize scenarios
		HashSet<Float> years = new HashSet<Float>();
		for(SensorData sensor: sensorData.values()) {
			if(!sensor.isIncluded) continue; //Only included sensors
			if(!sensor.sensorType.contains("Electrical Conductivity")) {
				for(String scenario: data.getSet().getParetoMap().get(sensor.specificType).keySet()) {
					for(Integer nodeNumber: data.getSet().getParetoMap().get(sensor.specificType).get(scenario).keySet()) {
						float ttd = data.getSet().getParetoMap().get(sensor.specificType).get(scenario).get(nodeNumber);
						years.add(ttd); //TODO: If IAM has too many steps, round values to nearest year to reduce
						if(!earliestDetectionForAllSensors.get(scenario).containsKey(nodeNumber))
							earliestDetectionForAllSensors.get(scenario).put(nodeNumber, ttd);
						else if (ttd < earliestDetectionForAllSensors.get(scenario).get(nodeNumber))
							earliestDetectionForAllSensors.get(scenario).put(nodeNumber, ttd);
					}
				}
			} else { // ERT is currently stored in a separate matrix, handle it differently
				E4DSensors.ertNewPairing();
				for(String scenario: E4DSensors.ertDetectionTimes.get(sensor.detectionThreshold).keySet()) {
					for(Integer primaryNode: E4DSensors.ertDetectionTimes.get(sensor.detectionThreshold).get(scenario).keySet()) {
						for(Float ttd: E4DSensors.ertDetectionTimes.get(sensor.detectionThreshold).get(scenario).get(primaryNode).values()) {
							years.add(ttd);
							if(!earliestDetectionForAllSensors.get(scenario).containsKey(primaryNode))
								earliestDetectionForAllSensors.get(scenario).put(primaryNode, ttd);
							else if (ttd < earliestDetectionForAllSensors.get(scenario).get(primaryNode))
								earliestDetectionForAllSensors.get(scenario).put(primaryNode, ttd);
						}
					}
				}
			}
		}
		
		// Sort the timesteps
		ArrayList<Float> sortedYears = new ArrayList<Float>(years);
		java.util.Collections.sort(sortedYears);
		
		// Determine the volume of aquifer degraded per scenario
		Map<String, HashMap<Float, Float>> volumeDegradedByYear = new HashMap<String, HashMap<Float, Float>>(); //Scenario <Year, VolumeDegraded>
		float vod = 0;
		for(String scenario: data.getSet().getScenarios()) {
			volumeDegradedByYear.put(scenario, new HashMap<Float, Float>());
			for(Float time: sortedYears) {
				for(Integer nodeNumber: earliestDetectionForAllSensors.get(scenario).keySet()) {
					if(earliestDetectionForAllSensors.get(scenario).get(nodeNumber) <= time) {
						Point3i location = data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber); //get location of node that detected at this time
						vod =+ data.getSet().getNodeStructure().getVolumeOfNode(location); //convert the found node into a volume and add cumulatively
					}
				}
				volumeDegradedByYear.get(scenario).put(time, vod);
			}
		}
		
		// Save the VOD information
		SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, sortedYears);
	}
	
	//We want to do the same process when the page is completed or the "find triggering nodes" is selected
	private void findTriggeringNodes() {
		
		// sensorSettings needs to only have selected sensors now
		// Also create a map of sensors where we need to find nodes
		Sensor.sensorAliases = new HashMap<String, String>();
		ArrayList<SensorData> newSensors = new ArrayList<SensorData>();
		ArrayList<SensorData> activeSensors = new ArrayList<SensorData>();
		int count = 0;
		for(String label: sensorData.keySet()) {
			SensorData sensor = sensorData.get(label);
			if (!sensor.isIncluded)
				data.getSet().removeSensorSettings(sensor.sensorName);
			else {
				count += data.getSet().getInferenceTest().getMinimumForType(label);
				activeSensors.add(sensor);
				if(!data.getSet().getParetoMap().containsKey(sensor.specificType) && !sensor.sensorType.contains("Electrical Conductivity"))
					newSensors.add(sensor); //if these settings are new to the paretoMap, we need to add them
			}
			Sensor.sensorAliases.put(label, sensor.alias);
		}
		if(count>data.getSet().getInferenceTest().getOverallMinimum()) //Initially set this at the sum of sensors
			data.getSet().getInferenceTest().setOverallMinimum(count);
		data.needToResetWells = true;
		
		// Based on the list of H5 sensors above, add results to paretoMap
		// Calculate the sum of nodes that detect (cloudNodes)
		// Run pareto optimization to get the final set of valid nodes (validNodes)
		data.setupSensors(newSensors, activeSensors);
		
		// Write the number of valid nodes to the display
		for(String label: sensorData.keySet()) {
			SensorData sensor = sensorData.get(label);
			if(sensor.isIncluded)
				sensor.nodeLabel.setText(label + ": " + data.getSet().getSensorSettings(label).getNodeSize());
			else
				sensor.nodeLabel.setText(label + ": Not set");
		}
		
		// Now that we've found nodes, make vis available
		DREAMWizard.visLauncher.setEnabled(true);	
	}
	
	//Hack to fix a bug on mac that would replace the contents of whatever field was selected with the alias of the first selected monitoring parameter.
	//This gets around the problem by selecting that alias field so that it replaces itself - not a real fix to the problem.
	public void fixMacBug() {
		if(System.getProperty("os.name").contains("Mac")){
			for(String sensor : sensorData.keySet()){
				if(sensorData.get(sensor).isIncluded){
					sensorData.get(sensor).aliasText.setFocus();
					break;
				}
			}
		}
	}
		
	private Trigger iamToTrigger(String trigger) {
		Trigger temp = null;
		if(trigger.contains("min"))
			temp = Trigger.MINIMUM_THRESHOLD;
		else if(trigger.contains("max"))
			temp = Trigger.MAXIMUM_THRESHOLD;
		else if(trigger.contains("rel"))
			temp = Trigger.RELATIVE_DELTA;
		else if(trigger.contains("abs"))
			temp = Trigger.ABSOLUTE_DELTA;
		return temp;
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}

	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
	
}