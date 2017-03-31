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
import objects.Scenario;
import objects.Sensor;
import objects.SensorSetting;
import objects.TimeStep;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.*;
import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;
import utilities.Constants.ModelOption;
import utilities.Point2i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting detection criteria for each sensor type to be used.
 * See line 608
 * @author port091
 * @author rodr144
 */

public class Page_LeakageCriteria extends DreamWizardPage implements AbstractWizardPage {
		
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	protected Map<String, Integer> num_duplicates = new HashMap<String, Integer>();
	
	private STORMData data;
	private boolean isCurrentPage = false;
	private Button scenarioUnionButton;
	private Button sensorUnionButton;
	private Button e4dButton;
	private Button buttonSelectDir;
	private boolean toggling = false;
	private Text e4dFolder;
	
	private Map<String, SensorData> sensorData;
	
		
	protected Page_LeakageCriteria(STORMData data) {
		super("Leakage Criteria");
		this.data = data;	
	}
	
	public class SensorData {

		public String sensorType;
		public String sensorName;
		public String alias;
		public float cost;
		public float detection;
		public float leakage;
		public float maxZ;
		public float minZ;
		public Trigger trigger;
		public DeltaType deltaType;

		public float min;
		public float max;
		private float dataMin;
		private float dataMax;
		private Float maxZBound;
		private Float minZBound;

		public boolean asRelativeChange;
		public boolean asAbsoluteChange;
		public boolean isIncluded;
		private boolean isDuplicate;
		
		// A couple of these may need to be global
		private Label nodeLabel;
		private Label sensorTypeLabel;
		private Label detectionLabel;
		private Label leakageLabel;
		private Label minZLabel;
		private Label maxZLabel;
		
		private Text aliasText;
		private Text costText;
		private Text detectionText;
		private Text leakageText;
		private Text minZText;
		private Text maxZText;
		
		private Color aliasForeground;
		private Color costForeground;
		private Color detectionForeground;
		private Color leakageForeground;
		private Color minZForeground;
		private Color maxZForeground;
		
		private String costErrorText;
		private String detectionErrorText;
		private String leakageErrorText;
		private String minZErrorText;
		private String maxZErrorText;
		
		private Combo thresholdCombo;
		
		
		//Class for storing the data about one particular sensor type.
		public SensorData(SensorSetting sensorSettings, String sensorName) {
			
			if(sensorSettings.getType().equals(sensorName)) isDuplicate = false;
			else isDuplicate = true;
			
			sensorType = sensorSettings.getType();
			this.sensorName = sensorName;
			alias = sensorName;
			isIncluded = false; // By default
			cost = sensorSettings.getCost();
			detection = sensorSettings.getDetection();
			leakage = sensorSettings.getLeakage();
			maxZ = sensorSettings.getMaxZ();
			minZ = sensorSettings.getMinZ();
			minZBound = minZ;
			maxZBound = maxZ;
			costErrorText = Constants.decimalFormat.format(cost);
			detectionErrorText = Constants.decimalFormat.format(detection);
			leakageErrorText = Constants.decimalFormat.format(leakage);
			minZErrorText = Constants.decimalFormat.format(minZ);
			maxZErrorText = Constants.decimalFormat.format(maxZ);
			
			// These may be backwards?
			trigger = Trigger.MINIMUM_THRESHOLD;
			deltaType = sensorSettings.getDeltaType();
			
			// Try to be a little smarter about pressure
			if(sensorType.toLowerCase().contains("pressure") || sensorType.toLowerCase().equals("p"))
				trigger = Trigger.RELATIVE_DELTA;
			
			// Removing this, its buggy...
			if(sensorType.toLowerCase().equals("ph"))
				trigger = Trigger.MAXIMUM_THRESHOLD;
			
			dataMin = sensorSettings.getMin();
			dataMax = sensorSettings.getMax();
			
			// Default threshold goes from half way to max
			if(trigger == Trigger.MAXIMUM_THRESHOLD) {
				min = 0;
				max = (dataMax-dataMin)/2+dataMin;
			} else {
				min = (dataMax-dataMin)/2+dataMin;
				max = Float.MAX_VALUE;
			}
			
			asRelativeChange = true;
			asAbsoluteChange = false;
			
		}	
		
		public void buildUI() {
			
			//Add a button here
			if(isDuplicate){
				Button addButton = new Button(container, SWT.PUSH);
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
				Button addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						if(!num_duplicates.containsKey(sensorType)) num_duplicates.put(sensorType, 1);
						addSensor(sensorType, sensorType + "_" + num_duplicates.get(sensorType));
						num_duplicates.put(sensorType, num_duplicates.get(sensorType)+1);
						loadPage();
						fixMacBug();
					}
			    });
			    addButton.setText("+");
			}
			
		    
			// Include button
			Button includeButton = new Button(container,  SWT.CHECK);
			includeButton.setSelection(isIncluded);
			for(SensorData data: sensorData.values()) {
				if(data.isIncluded) {
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
					toggleEnabled();
					
					//Special handling if errors are negated when parameters are unchecked...
					//We have to search through all possible errors to see if any are negated
					boolean checkError = true;
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					boolean costError = false;
					boolean detectionError = false;
					boolean leakageError = false;
					boolean botError = false;
					boolean botBoundError = false;
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						else
							checkError = false;
						//Alias
						for(SensorData data2: sensorData.values()) {
							if(!data2.isIncluded) //Skip unchecked parameters
								continue;
							if(data.alias.equals(data2.alias) && !data.sensorName.equals(data2.sensorName)) {
								duplicateError = true;
								data.aliasText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							}
						}
						if(data.alias.contains(",")) //Contains a comma
							commaError = true;
						if(data.alias.isEmpty()) //No alias
							emptyError = true;
						//Cost
						if(!isValidNumber(data.costText.getText()))
							costError = true;
						//Detection
						if(!isValidNumber(data.detectionText.getText()) && !data.detectionText.getText().equals(""))
							detectionError = true;
						//Leakage
						if(!isValidNumber(data.leakageText.getText()) && !data.leakageText.getText().equals(""))
							leakageError = true;
						//Zone bottom
						if(!isValidNumber(data.minZText.getText()))
							botError = true;
						else {
							minZ = Float.parseFloat(data.minZText.getText());
							if (minZ < data.minZBound || minZ > data.maxZBound)
								botBoundError = true;
						}
						//Zone top
						if(!isValidNumber(data.maxZText.getText()))
							topError = true;
						else {
							maxZ = Float.parseFloat(data.maxZText.getText());
							if (maxZ < data.minZBound || maxZ > data.maxZBound)
								topBoundError = true;
						}
					}
					errorFound(checkError, "  Must select a monitoring parameter.");
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					errorFound(costError, "  Cost is not a real number.");
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(leakageError, "  Leakage is not a real number.");
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					
					//Special handling of red text for duplicates
					if (duplicateError==false)
						for(SensorData data: sensorData.values())
							if (data.isIncluded && !data.alias.contains(",") && !data.alias.isEmpty())
								data.aliasText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
				}
			});
			
			// Label [min, max]
			String minStr =  Constants.decimalFormat.format(dataMin);		
			if(dataMin < 0.001)
				minStr =  Constants.exponentialFormat.format(dataMin);
			String maxStr =  Constants.decimalFormat.format(dataMax);
			if(dataMax < 0.001)
				maxStr =  Constants.exponentialFormat.format(dataMax);
		
			if(minStr.length() > 8) minStr = Constants.exponentialFormat.format(dataMin);
			if(maxStr.length() > 8) maxStr = Constants.exponentialFormat.format(dataMax);			
			
			includeButton.setText(sensorType);
			
			aliasText = new Text(container, SWT.BORDER | SWT.SINGLE);
			aliasText.setText(alias);
			aliasText.setForeground(aliasForeground);
			aliasText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e){
					alias = ((Text)e.getSource()).getText();
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						data.aliasText.setForeground(new Color(container.getDisplay(), 0, 0, 0));
						for(SensorData data2: sensorData.values()) {
							if(!data2.isIncluded) //Skip unchecked parameters
								continue;
							if(data.alias.trim().equals(data2.alias.trim()) && !data.sensorName.equals(data2.sensorName)) {
								data.aliasText.setForeground(new Color(container.getDisplay(), 255, 0, 0));
								duplicateError = true;
							}
						}
					}
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(data.alias.contains(",")) { //Contains a comma
							data.aliasText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							commaError = true;
						} else if (duplicateError==false) //No comma
							data.aliasText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						if(data.alias.isEmpty()) { //No alias
							data.aliasText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							emptyError = true;
						} else if (duplicateError==false && commaError==false) //Has alias
							data.aliasText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					}
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					aliasForeground = aliasText.getForeground();
				}
			});
			GridData aliasTextData = new GridData(SWT.FILL, SWT.END, false, false);
			aliasTextData.widthHint = 60;
			aliasText.setLayoutData(aliasTextData);
			
			
			//Cost input
			costText = new Text(container, SWT.BORDER | SWT.SINGLE);
			costText.setText(costErrorText);
			costText.setForeground(costForeground);
			costText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					costErrorText = ((Text)e.getSource()).getText();
					boolean costError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(isValidNumber(data.costText.getText())) //Valid number
							data.costText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						else { //Not a valid number
							data.costText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							costError = true;
						}
					}
					errorFound(costError, "  Cost is not a real number.");
					costForeground = costText.getForeground();
				}
			});
			GridData costTextData = new GridData(SWT.FILL, SWT.END, false, false);
			costTextData.widthHint = 60;
			costText.setLayoutData(costTextData);
			
			// Drop down menu
			thresholdCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			thresholdCombo.add(Trigger.MAXIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.MINIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.RELATIVE_DELTA.toString());
			thresholdCombo.add(Trigger.ABSOLUTE_DELTA.toString());
			thresholdCombo.setText(trigger.toString());
			thresholdCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean isMinimumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MAXIMUM_THRESHOLD.toString());
					boolean isMaximumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MINIMUM_THRESHOLD.toString());
					boolean isRelativeDetla = ((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_DELTA.toString());
					trigger = isMinimumThreshold ? Trigger.MAXIMUM_THRESHOLD : (isMaximumThreshold ? 
							Trigger.MINIMUM_THRESHOLD : isRelativeDetla ? Trigger.RELATIVE_DELTA : Trigger.ABSOLUTE_DELTA);	
					if(trigger == Trigger.MAXIMUM_THRESHOLD) {
						min = 0;
						max = Float.parseFloat(detectionText.getText());
					} else {	
						min = Float.parseFloat(detectionText.getText());
						max = Float.MAX_VALUE;
					}
						
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;

					toggleEnabled();
				}				
			});
			GridData thresholdComboData = new GridData(SWT.FILL, SWT.END, false, false);
			thresholdComboData.widthHint = 140;
			thresholdCombo.setLayoutData(thresholdComboData);
			
			//Detection input
			detectionText = new Text(container, SWT.BORDER | SWT.SINGLE);
			detectionText.setText(detectionErrorText);
			detectionText.setForeground(detectionForeground);
			
			//TODO: Verify that this is unnecessary
			//Not sure what this is about... it was causing errors, so I commented out
			/*
			if(trigger == Trigger.MAXIMUM_THRESHOLD) {
				if(max != 0) {// What if a user wants to set this to 0? 
					detectionText.setText(Constants.decimalFormat.format(max));
					if(max < 0.001)
						detectionText.setText(Constants.exponentialFormat.format(max));
				}
			} else { 
				if(min != 0) {
					detectionText.setText(Constants.decimalFormat.format(min));
					if(min < 0.001)
						detectionText.setText(Constants.exponentialFormat.format(min));
				}
			}
			
			if(deltaType == DeltaType.INCREASE) detectionText.setText("+" + detectionText.getText());
			*/
			
			detectionText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					detectionErrorText = ((Text)e.getSource()).getText();
					boolean detectionError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(!isValidNumber(data.detectionText.getText()) && !data.detectionText.getText().equals("")) { //Valid number
							data.detectionText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							detectionError = true;
						} else
							data.detectionText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					}
					errorFound(detectionError, "  Detection is not a real number.");
					detectionForeground = detectionText.getForeground();
					
					try { // Verify that entry is a real number
						if(trigger == Trigger.MAXIMUM_THRESHOLD) {
							min = 0;
							max = Float.parseFloat(detectionText.getText());
						} else {	
							min = Float.parseFloat(detectionText.getText());
							max = Float.MAX_VALUE;
						}
						
						if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
						else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
						else deltaType = DeltaType.BOTH;
					} catch (NumberFormatException ne) {
						//only add real numbers
					}
				}
			});
			GridData detectionInputData = new GridData(SWT.FILL, SWT.END, false, false);
			detectionInputData.widthHint = 60;
			detectionText.setLayoutData(detectionInputData);

			
			//Leakage input
			leakageText = new Text(container, SWT.BORDER | SWT.SINGLE);
			leakageText.setText(leakageErrorText);
			leakageText.setForeground(leakageForeground);
			leakageText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					leakageErrorText = ((Text)e.getSource()).getText();
					boolean leakageError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(!isValidNumber(data.leakageText.getText()) && !data.leakageText.getText().equals("")) { //Valid number
							data.leakageText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							leakageError = true;
						} else
							data.leakageText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					}
					errorFound(leakageError, "  Leakage is not a real number.");
					leakageForeground = leakageText.getForeground();
				}
			});
			GridData leakageInputData = new GridData(SWT.FILL, SWT.END, false, false);
			leakageInputData.widthHint = 60;
			leakageText.setLayoutData(leakageInputData);
			
			
			// Set minimum z
			minZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			minZText.setText(minZErrorText);
			minZText.setForeground(minZForeground);
			minZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					minZErrorText = ((Text)e.getSource()).getText();
					boolean botError = false;
					boolean botBoundError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(isValidNumber(data.minZText.getText())) { //Valid number
							data.minZText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
							minZ = Float.parseFloat(data.maxZText.getText());
							if (minZ < minZBound || minZ > maxZBound) {
								data.minZText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
								botBoundError = true;
							}
						} else { //Not a valid number
							data.minZText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							botError = true;
						}
					}
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					minZForeground = minZText.getForeground();
				}
			});
			GridData minZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			minZTextData.widthHint = 60;
			minZText.setLayoutData(minZTextData);
			
			
			
			// Set maximum z
			maxZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			maxZText.setText(maxZErrorText);
			maxZText.setForeground(maxZForeground);
			maxZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					maxZErrorText = ((Text)e.getSource()).getText();
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData data: sensorData.values()) {
						if(!data.isIncluded) //Skip unchecked parameters
							continue;
						if(isValidNumber(data.maxZText.getText())) { //Valid number
							data.maxZText.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
							maxZ = Float.parseFloat(data.maxZText.getText());
							if (maxZ < minZBound || maxZ > maxZBound) {
								data.maxZText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
								topBoundError = true;
							}
						} else { //Not a valid number
							data.maxZText.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							topError = true;
						}
					}
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					maxZForeground = maxZText.getForeground();
				}
			});
			GridData maxZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			maxZTextData.widthHint = 60;
			maxZText.setLayoutData(maxZTextData);
			
			toggleEnabled();
		}		
		
		private void toggleEnabled() {
			if(sensorTypeLabel != null && !sensorTypeLabel.isDisposed())
				sensorTypeLabel.setEnabled(isIncluded);
			if(aliasText != null && !aliasText.isDisposed())
				aliasText.setEnabled(isIncluded);
			if(costText != null && !costText.isDisposed())
				costText.setEnabled(isIncluded);
			if(thresholdCombo != null && !thresholdCombo.isDisposed())
				thresholdCombo.setEnabled(isIncluded);					
			if(detectionText != null && !detectionText.isDisposed())
				detectionText.setEnabled(isIncluded);
			if(detectionLabel != null && !detectionLabel.isDisposed())
				detectionLabel.setEnabled(isIncluded);
			if(leakageText != null && !leakageText.isDisposed())
				leakageText.setEnabled(isIncluded);
			if(leakageLabel != null && !leakageLabel.isDisposed())
				leakageLabel.setEnabled(isIncluded);
			if(minZLabel != null && !minZLabel.isDisposed())
				minZLabel.setEnabled(isIncluded);
			if(minZText != null && !minZText.isDisposed())
				minZText.setEnabled(isIncluded);
			if(maxZLabel != null && !maxZLabel.isDisposed())
				maxZLabel.setEnabled(isIncluded);
			if(maxZText != null && !maxZText.isDisposed())
				maxZText.setEnabled(isIncluded);
		}		
	}

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
	//	sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(720, 400).create());
        sc.setExpandHorizontal(true);
        sc.getVerticalBar().setIncrement(20);
        sc.setExpandVertical(true);
                
        container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 9;
//		layout.makeColumnsEqualWidth = false;
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();
		Map<String, String> sensorAliases = new HashMap<String, String>();
		if(data.modelOption == ModelOption.ALL_SENSORS) sensorAliases.put("all", "all");
		SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
		int count = 0;
		for(String label: sensorData.keySet()) {
			SensorData data = sensorData.get(label);
			if (data.isIncluded==true) {
				data.cost = Float.valueOf(data.costErrorText);
				data.detection = Float.valueOf(data.detectionErrorText);
				data.leakage = Float.valueOf(data.leakageErrorText);
				sensorData.put(label, data);
				count++;
			}
			sensorSettings.put(label, data);
			sensorAliases.put(label, data.alias);
			SensorSetting.sensorTypeToDataType.put(label, sensorData.get(label).sensorType);
		}
		Sensor.sensorAliases = sensorAliases;
		data.getSet().getInferenceTest().setMinimum(count);
		data.setupSensors(false, sensorSettings);
		data.needToResetWells = true;
		if(!Constants.buildDev) volumeOfAquiferDegraded(); // we only need to do this if we're not going to have the whole separate page
		
		DREAMWizard.visLauncher.setEnabled(true);
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		if(sensorData == null || data.needToResetMonitoringParameters) {
			data.needToResetMonitoringParameters = false;
			// New UI
			sensorData = new TreeMap<String, SensorData>();
		
			for(String dataType: data.getSet().getAllPossibleDataTypes()) {
								
				sensorData.put(dataType, new SensorData(data.getSet().getSensorSettings(dataType), dataType));

			}
		}
		
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}		
	
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Leakage Criteria");
		GridData infoGridData1 = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		infoGridData1.verticalSpan = 2;
		infoLabel1.setLayoutData(infoGridData1);
		infoLabel1.setFont(boldFont);

		GridData infoLinkData = new GridData(GridData.FILL_HORIZONTAL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 2;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "After reading through the directory of realization outputs, DREAM will generate a table of monitoring parameters that the user can select. These parameters are specific to the included realizations. The selected monitoring parameters will be used in the optimization algorithm. The user may label what technology they will use to monitor each selected parameter in the \"Alias for Monitoring Technology\" box and then provide a realistic cost per monitoring technology if it is known; if not, the costs should be set equal. The detection criteria may be specified based on the relative change from initial conditions, absolute change from initial conditions, or a maximum or minimum threshold. If relative delta, absolute delta, or maximum threshold is selected, the given value and all values above are treated as detecting a leak. If minimum threshold is selected, that value and all values below are treated as detecting a leak.");	
			}
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Select the monitoring parameters of interest, include a cost per appropriate sensor type, and set the detection criteria. NOTE: The minimum and maximum values are read from the first realization read by DREAM. These are provided to give the user an idea of the values present.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
				
		// Headers
		
		Font boldFont1 = new Font( container.getDisplay(), new FontData( "Helvetica", 10, SWT.BOLD ) );
		
		Label blankFiller = new Label(container, SWT.LEFT);	
		Label monitorParams = new Label(container, SWT.LEFT);
		Label aliasLabel = new Label(container, SWT.LEFT);
		Label costPerSensor = new Label(container, SWT.LEFT);
		Label detectionCriteria = new Label(container, SWT.LEFT);
		Label detectionLabel = new Label(container, SWT.LEFT);
		Label leakageLabel = new Label(container, SWT.LEFT);
		Label minZLabel = new Label(container, SWT.LEFT);
		Label maxZLabel = new Label(container, SWT.LEFT);
		
		blankFiller.setText("");
		monitorParams.setText("Monitoring Parameter");
		aliasLabel.setText("Alias for Monitoring Technology");
		costPerSensor.setText("Cost per Sensor");
		detectionCriteria.setText("Detection Criteria");
		detectionLabel.setText("Detection Value");
		leakageLabel.setText("Leakage Criteria");
		minZLabel.setText("Zone Bottom");
		maxZLabel.setText("Zone Top");
			
		monitorParams.setFont(boldFont1);
		aliasLabel.setFont(boldFont1);
		costPerSensor.setFont(boldFont1);
		detectionCriteria.setFont(boldFont1);
		detectionLabel.setFont(boldFont1);
		leakageLabel.setFont(boldFont1);
		minZLabel.setFont(boldFont1);
		maxZLabel.setFont(boldFont1);
				
		for(SensorData data: sensorData.values()) {
			data.buildUI();
		}

		Composite composite_scale = new Composite(container, SWT.BORDER);
		GridLayout gridLayout_scale = new GridLayout();
		gridLayout_scale.numColumns = 8;
		composite_scale.setLayout(gridLayout_scale);
		
		GridData gridData = new GridData();
		gridData.horizontalSpan=8;
		composite_scale.setLayoutData(gridData);

		Button queryButton = new Button(composite_scale, SWT.BALLOON);
		queryButton.setText("Find triggering nodes");

		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		
		int count = 0;
		
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);

			// Let these fill two spots
			GridData tempData = new GridData(GridData.FILL_HORIZONTAL);
			tempData.horizontalSpan = 2;
			temp.nodeLabel = new Label(composite_scale, SWT.WRAP);
			temp.nodeLabel.setLayoutData(tempData);
			if(data.getSet().getSensorSettings(label) == null)
				data.getSet().resetSensorSettings(label, temp.min, temp.max);
			if( data.getSet().getSensorSettings(label).isSet())
				temp.nodeLabel.setText(label+ ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
			else 
				temp.nodeLabel.setText(label+ ": Not set");
			count+=2;
		}
		
		if(count % 8 != 0) {
			for(int i = 0; i < 8-(count % 8); i++)
				new Label(composite_scale, SWT.NULL);
		}
		
		queryButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				fixMacBug();
				Constants.hdf5CloudData.clear();
				Constants.scenarioUnion = scenarioUnionButton.getSelection();
				boolean reset = true;							
				Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();	


				SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
				Map<String, String> sensorAliases = new HashMap<String, String>();
				if(data.modelOption == ModelOption.ALL_SENSORS) sensorAliases.put("all", "all");
				for(String label: sensorData.keySet()){
					SensorData temp = sensorData.get(label);
					sensorSettings.put(label, temp);
					String alias = temp.alias;
					sensorAliases.put(label, alias.equals("") ? label: alias);
					SensorSetting.sensorTypeToDataType.put(label, sensorData.get(label).sensorType);
				}
				Sensor.sensorAliases = sensorAliases;
				
				try {
					if(reset) {
						for(String dataType: data.getSet().getDataTypes()) {
							data.getSet().getSensorSettings(dataType).setNodesReady(false);
						}
					}
					DREAMWizard.visLauncher.setEnabled(false);
					e4dButton.setEnabled(false);
					e4dFolder.setEnabled(false);
					buttonSelectDir.setEnabled(false);
					data.setupSensors(reset, sensorSettings);
					DREAMWizard.visLauncher.setEnabled(true);
					e4dButton.setEnabled(true);
					e4dFolder.setEnabled(true);
					buttonSelectDir.setEnabled(true);
					for(String label: sensorData.keySet()){
						SensorData temp = sensorData.get(label);
						if(temp.isIncluded &&  data.getSet().getSensorSettings(label).isSet())
							temp.nodeLabel.setText(label + ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
						else
							temp.nodeLabel.setText(label + ": Not set");
					}			
					
					// Then we really only want the intersection
					if(!sensorUnionButton.getSelection()) {
						List<Integer> intersection = new ArrayList<Integer>();
						for(String dataType: data.getSet().getDataTypes()) {
							if(!data.getSet().getSensorSettings(dataType).isSet())
								continue;
							for(Integer nodeNumber: data.getSet().getSensorSettings(dataType).getValidNodes(null)) {
								boolean shared = true;
								for(String otherDataType: data.getSet().getDataTypes()) {
									if(dataType.equals(otherDataType))
										continue; // Don't need to check this one
									if(!data.getSet().getSensorSettings(otherDataType).isSet())
										continue;
									if(!data.getSet().getSensorSettings(otherDataType).getValidNodes(null).contains(nodeNumber)) {
										shared = false;
									}
									if(!shared)
										break;
								}		
								if(shared)
									intersection.add(nodeNumber);
							}
						}
						System.out.println("Intersection: (" + intersection.size() + ") " + intersection);
						for(String label: sensorData.keySet()){
							SensorData temp = sensorData.get(label);
							if(data.getSet().getSensorSettings(label) != null && data.getSet().getSensorSettings(label).isSet()) {
								data.getSet().getSensorSettings(label).setValidNodes(intersection);
								temp.nodeLabel.setText(temp.alias + ": " + data.getSet().getSensorSettings(temp.alias).getValidNodes(null).size());
							} else
								temp.nodeLabel.setText(temp.alias + ": Not set");
						}
					}

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}	       
		});	
				

		Label col = new Label(composite_scale, SWT.NULL);
		col.setText("Set up the solution space using ...");
		col.setFont(boldFont1);
		new Label(composite_scale, SWT.NULL);
		
	    scenarioUnionButton = new Button(composite_scale, SWT.CHECK);
	    final Button scenarioIntersectionButton = new Button(composite_scale, SWT.CHECK);
	    scenarioIntersectionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					scenarioUnionButton.setSelection(!scenarioIntersectionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    scenarioUnionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					scenarioIntersectionButton.setSelection(!scenarioUnionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    scenarioUnionButton.setSelection(true);
	    scenarioUnionButton.setText("union of scenarios");
	    scenarioIntersectionButton.setText("intersection of scenarios");
	    
	    sensorUnionButton = new Button(composite_scale, SWT.CHECK);
	    final Button sensorIntersectionButton = new Button(composite_scale, SWT.CHECK);
	    sensorIntersectionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					sensorUnionButton.setSelection(!sensorIntersectionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    sensorUnionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					sensorIntersectionButton.setSelection(!sensorUnionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    
	    sensorUnionButton.setSelection(true);
	    sensorUnionButton.setText("union of sensors");
	    sensorIntersectionButton.setText("intersection of sensors");

	    
		////The following code writes outputs for the e4d model
		//Layout the buttons
	    Composite composite_E4D = new Composite(container, SWT.NULL);
		GridLayout gridLayout_E4D = new GridLayout();
		gridLayout_E4D.numColumns = 8;
		composite_E4D.setLayout(gridLayout_E4D);
		GridData gridData_E4D = new GridData(GridData.FILL_HORIZONTAL);
		gridData_E4D.horizontalSpan=8;
		composite_E4D.setLayoutData(gridData_E4D);
	    
	  	Label infoLinkE4D = new Label(composite_E4D, SWT.NULL);
	  	infoLinkE4D.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
	    
	    final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
	    e4dButton = new Button(composite_E4D, SWT.PUSH);
	    e4dButton.setText("  Write E4D Files  ");
	    
		e4dFolder = new Text(composite_E4D, SWT.BORDER | SWT.SINGLE);
		e4dFolder.setText(System.getProperty("user.dir"));
		GridData e4dTextData = new GridData(GridData.FILL_HORIZONTAL);
		e4dTextData.horizontalSpan = 3;
		e4dFolder.setLayoutData(e4dTextData);
		
		buttonSelectDir = new Button(composite_E4D, SWT.PUSH);
		buttonSelectDir.setText("...");
		
		Label e4dFiller = new Label(composite_E4D, SWT.NULL);
		e4dFiller.setText("                                                                 ");
		
		//Change text red when directory is invalid
		e4dFolder.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				File resultsFolder = new File(e4dFolder.getText());
				boolean dir = resultsFolder.isDirectory();
				if (dir == true) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
					e4dButton.setEnabled(true);
				}
				else {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					e4dButton.setEnabled(false);
				}
			}				
		});
		
	    //Select the save directory
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(e4dFolder.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					e4dFolder.setText(dir);
				}
			}
		});	
		
		//Add an info icon to explain the E4D Buttons
  		infoLinkE4D.addListener(SWT.MouseUp, new Listener(){
  			@Override
  			public void handleEvent(Event event) {
  				// TODO: Catherine edit text here!
  				MessageDialog.openInformation(container.getShell(), "Additional information", "After finding triggering nodes, the user may write input files for the E4D model. E4D is a three-dimensional (3D) modeling and inversion code designed for subsurface imaging and monitoring using static and time-lapse 3D electrical resistivity (ER) or spectral induced polarization (SIP) data.");	
  			}			
  		});

		//Save the E4D files
		e4dButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				fixMacBug();
				
				//Write out the x-y and i-j well locations
				HashMap<Integer, HashMap<Integer,Integer>> ijs = new HashMap<Integer, HashMap<Integer, Integer>>();
				HashMap<Float, HashMap<Float,Float>> xys = new HashMap<Float, HashMap<Float, Float>>();
				HashSet<Integer> validNodes = new HashSet<Integer>();
				for(String label: data.getSet().getSensorSettings().keySet()){
					validNodes.addAll(data.getSet().getSensorSettings(label).getValidNodes(null));
				}
				for(Integer node: validNodes){
					Point3i ijk = data.getSet().getNodeStructure().getIJKFromNodeNumber(node);
					Point3f xyz = data.getSet().getNodeStructure().getXYZCenterFromIJK(ijk);
					if(!ijs.containsKey(ijk.getI())) ijs.put(ijk.getI(), new HashMap<Integer,Integer>());
					if(!ijs.get(ijk.getI()).containsKey(ijk.getJ())) ijs.get(ijk.getI()).put(ijk.getJ(), ijk.getK());
					else ijs.get(ijk.getI()).put(ijk.getJ(), Math.min(ijk.getK(), ijs.get(ijk.getI()).get(ijk.getJ())));
					if(!xys.containsKey(xyz.getX())) xys.put(xyz.getX(), new HashMap<Float,Float>());
					if(!xys.get(xyz.getX()).containsKey(xyz.getZ())) xys.get(xyz.getX()).put(xyz.getY(), xyz.getZ());
					else xys.get(xyz.getX()).put(xyz.getY(), Math.min(xyz.getZ(), xys.get(xyz.getX()).get(xyz.getZ())));
				}
				StringBuilder ijStringBuilder = new StringBuilder();
				//LUKE EDIT - HACKED IN TO TRY TO PLAY WITH E4D CLASS ->
				HashSet<Point2i> wellLocations = new HashSet<Point2i>();
				// <-
				for(Integer i: ijs.keySet()){
					for(Integer j: ijs.get(i).keySet()){
						//LUKE EDIT - HACKED IN TO TRY TO PLAY WITH E4D CLASS ->
						wellLocations.add(new Point2i(i,j));
						// <-
						ijStringBuilder.append(i.toString() + "," + j.toString() + "," + ijs.get(i).get(j).toString() + "\n");
					}
				}
				//LUKE EDIT - HACKED IN TO TRY TO PLAY WITH E4D CLASS ->
				data.getSet().e4dInterface = new E4DSensors(data.getSet(), wellLocations);
				File tempout = new File(e4dFolder.getText(), "mapValues.txt");
				try {
					if(!tempout.exists()) tempout.createNewFile();
					FileUtils.writeStringToFile(tempout, data.getSet().e4dInterface.printDetectionTimes());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				// <-
				StringBuilder xyStringBuilder1 = new StringBuilder();
				for(Float x: xys.keySet()){
					for(Float y: xys.get(x).keySet()){
						xyStringBuilder1.append(x.toString() + "," + y.toString() + "," + String.valueOf((data.getSet().getNodeStructure().getZ().get(data.getSet().getNodeStructure().getZ().size()-1) - xys.get(x).get(y))) + "\n");
					}
				}
				StringBuilder xyStringBuilder2 = new StringBuilder();
				for(Float x: xys.keySet()){
					for(Float y: xys.get(x).keySet()){
						xyStringBuilder2.append(x.toString() + "," + y.toString() + "," + String.valueOf(xys.get(x).get(y)) + "\n");
					}
				}
				
				try{
					File xyLocationFile1 = new File(e4dFolder.getText(), "wellLocationsXY_v1.txt");
					if(!xyLocationFile1.exists())
						xyLocationFile1.createNewFile();
					FileUtils.writeStringToFile(xyLocationFile1, xyStringBuilder1.toString());
					File xyLocationFile2 = new File(e4dFolder.getText(), "wellLocationsXY_v2.txt");
					if(!xyLocationFile2.exists())
						xyLocationFile2.createNewFile();
					FileUtils.writeStringToFile(xyLocationFile2, xyStringBuilder2.toString());	
					File ijLocationFile = new File(e4dFolder.getText(), "wellLocationsIJ.txt");
					if(!ijLocationFile.exists())
						ijLocationFile.createNewFile();
					FileUtils.writeStringToFile(ijLocationFile, ijStringBuilder.toString());
					System.out.println("Writing E4D Files to " + e4dFolder.getText());
				}
				catch(Exception e){
					System.err.println("Couldn't write to well files");
				}
			}
		});
		
		container.layout();	
		
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		boolean enableVis  = false;
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);
			if(temp.isIncluded &&  data.getSet().getSensorSettings(label).isSet())
				enableVis = true;
		}
		
		DREAMWizard.visLauncher.setEnabled(enableVis);
		e4dButton.setEnabled(enableVis);
		e4dFolder.setEnabled(enableVis);
		buttonSelectDir.setEnabled(enableVis);
		DREAMWizard.convertDataButton.setEnabled(false);
	} //ends load page
	
	private void addSensor(String dataType, String newName){
		data.getSet().addSensorSetting(newName, dataType);
		data.getSet().getInferenceTest().setMinimumRequiredForType(newName, 1);
		sensorData.put(newName, new SensorData(data.getSet().getSensorSettings(newName), newName));
	}
	
	//Right now we assume that leakage criteria are degradation criteria if we're not in dev mode, and need this function for that. Essentially cloned code from Page_DegradationCriteria.
	private void volumeOfAquiferDegraded(){	
		long current = System.currentTimeMillis();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			System.out.println(sensorType);
			nodes.addAll(data.getSet().getSensorSettings().get(sensorType).getValidNodes(null)); //TODO: might be a bad fix here
			System.out.println(nodes.size());
		}
		
		Map<Scenario, HashMap<Integer, Float>> timeToDegradationPerNode = new HashMap<Scenario, HashMap<Integer, Float>>();

		for(Scenario scenario: data.getSet().getScenarios()){
			timeToDegradationPerNode.put(scenario, new HashMap<Integer, Float>());
			for(Integer nodeNumber: nodes){	
				Float timeToDegredation = null;
				for (TimeStep timeStep: data.getSet().getNodeStructure().getTimeSteps()){
					for(String sensorType: data.getSet().getSensorSettings().keySet()){
						try {
							if(CCS9_1.sensorTriggered(data.getSet(), timeStep, scenario, sensorType, nodeNumber)) timeToDegredation = timeStep.getRealTime();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(timeToDegredation != null) break;
					}
					if(timeToDegredation != null) break;
				}
				if(timeToDegredation != null) timeToDegradationPerNode.get(scenario).put(nodeNumber, timeToDegredation);
			}
		}
		
		System.out.println(timeToDegradationPerNode.size());
		
		Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear = new HashMap<Scenario, HashMap<Float, Float>>();
		HashSet<Float> years = new HashSet<Float>();
		for(Scenario scenario: timeToDegradationPerNode.keySet()){
			volumeDegradedByYear.put(scenario, new HashMap<Float, Float>());
			for(Integer nodeNumber: timeToDegradationPerNode.get(scenario).keySet()){
				Float year = timeToDegradationPerNode.get(scenario).get(nodeNumber);
				years.add(year);
				Point3i location = data.getScenarioSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
				if(!volumeDegradedByYear.get(scenario).containsKey(year)) volumeDegradedByYear.get(scenario).put(year, data.getSet().getNodeStructure().getVolumeOfNode(location));
				else volumeDegradedByYear.get(scenario).put(year, volumeDegradedByYear.get(scenario).get(year) + data.getSet().getNodeStructure().getVolumeOfNode(location));
			}
		}
		
		System.out.println(years.size());
		
		ArrayList<Float> sortedYears = new ArrayList<Float>(years);
		java.util.Collections.sort(sortedYears);
		for(Scenario scenario: volumeDegradedByYear.keySet()){
			if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(0))) volumeDegradedByYear.get(scenario).put(sortedYears.get(0), 0f);
			for(int i=1; i<sortedYears.size(); ++i){
				if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(i))) volumeDegradedByYear.get(scenario).put(sortedYears.get(i), 0f);
				volumeDegradedByYear.get(scenario).put(sortedYears.get(i), volumeDegradedByYear.get(scenario).get(sortedYears.get(i)) + volumeDegradedByYear.get(scenario).get(sortedYears.get(i-1)));
			}
		}
		SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, sortedYears);

		long total = System.currentTimeMillis() - current;
		System.out.println("New volume of aquifer degraded time:\t" + total/1000 + "." + total%1000);
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
	
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}

	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
	
}
