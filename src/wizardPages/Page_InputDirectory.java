package wizardPages;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.MutationFunction.MUTATE;
import hdf5Tool.HDF5Interface;
import utilities.Constants;
import utilities.Constants.ModelOption;
import utilities.PorosityDialog;
import wizardPages.DREAMWizard.STORMData;

/**
 * Select the directory with the hdf5 files to be used, as well as the style of algorithm to run. Currently has 2 modes supported.
 * See line 164
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_InputDirectory extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private String simulation = "SimulatedAnnealing";
	private ModelOption modelOption = ModelOption.INDIVIDUAL_SENSORS_2;
	private Text fileDirectoryText;
	private String directory = Constants.homeDirectory;
	
	private boolean isCurrentPage = false;
	
	protected Page_InputDirectory(STORMData data) {
		super("Input Directory");
		this.data = data;
	}
	
	@Override
	public void createControl(Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		
		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
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
		
		container = new Composite(sc, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		layout.numColumns = 2;
		container.setLayout(layout);

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	public void completePage() throws Exception {	
		isCurrentPage = false;
		
		// We want to essentially reset everything at this point
		data.getSet().clearRun();
		HDF5Interface.statistics.clear();
		
		// Read in scenario and parameter information from the files
		Constants.homeDirectory = directory;
		data.setupScenarioSet(modelOption, MUTATE.SENSOR, simulation, fileDirectoryText.getText());
		data.getSet().setScenarioEnsemble(fileDirectoryText.getText().substring(fileDirectoryText.getText().lastIndexOf(File.separator)+1));
		// Ask for porosity input if it doesn't exist yet
		if(!data.getSet().getNodeStructure().porosityOfNodeIsSet()){
			PorosityDialog dialog = new PorosityDialog(container.getShell(), data);
			dialog.open();
		}
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		for(Control control: container.getChildren())
			control.dispose(); // Remove the children.
		
		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Input Directory");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLabel1.setFont(boldFont);
		
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "Select the directory containing HDF5 files for all leakage simulations to be considered. If the user has not converted ASCII simulation output data into DREAM readable HDF5 input files, the Launch Converter button will open a pop-up file converter tool. Read more about the DREAM HDF5 Converter tool in the user manual. Note: The HDF5 files must be directly available within the directory provided; they may not be in subdirectories within the root directory.");	
			}
		});
		
		Label infoLabel = new Label(container,  SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Provide the path to a single directory containing hdf5 formatted files of all subsurface simulation output at specified plot times.");
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 4));
		
		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText(" Select a directory ");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(fileDirectoryText.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					Constants.homeDirectory = dir;
					fileDirectoryText.setText(dir);
					data.reset();
				}
			}
		});
		
		//// Hack that allows Jonathan and Catherine automatic directory inputs ////
		if(directory.contains("whit162") && directory==Constants.homeDirectory && !directory.contains("Desktop"))
			directory = directory + "\\Desktop\\BCO_new";
		if(!System.getProperty("os.name").contains("Mac") && directory.contains("rupr404") && directory==Constants.homeDirectory && !directory.contains("Desktop"))
			directory = directory + "\\Desktop\\BCO_new";
		if(!System.getProperty("os.name").contains("Mac") && directory.contains("xuwe421") && directory==Constants.homeDirectory && !directory.contains("Desktop"))
			directory = "C:\\Users\\xuwe421\\eclipse-workspace\\BCO_new";
		if(directory.contains("d3x455") && directory==Constants.homeDirectory && !directory.contains("Desktop"))
			directory = directory + "C:\\Users\\D3X455\\OneDrive - PNNL\\Desktop\\DREAM-FY19\\BCO_new";
		
		//// End of hack ////
		
		fileDirectoryText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fileDirectoryText.setText(directory);
		fileDirectoryText.setForeground(Constants.black);
		fileDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		errorFound(validFileCheck(fileDirectoryText.getText()), "  Directory must contain an h5 or iam file.");
		fileDirectoryText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				File resultsFolder = new File(((Text)e.getSource()).getText());
				boolean dirError = !resultsFolder.isDirectory();
				boolean fileError = true;
				if (dirError == true) {
					((Text)e.getSource()).setForeground(Constants.red);
					fileError = false;
				} else {
					((Text)e.getSource()).setForeground(Constants.black);
					directory = ((Text)e.getSource()).getText();
					fileError = validFileCheck(resultsFolder.getPath());
				}
				errorFound(dirError, "  Invalid directory.");
				errorFound(fileError, "  Directory must contain an h5 or iam file.");
			}
		});

		Group radioButton = new Group(container, SWT.SHADOW_NONE);
		radioButton.setText("Model option");
		radioButton.setLayout(new RowLayout(SWT.VERTICAL));
		radioButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		Button button1 = new Button(radioButton, SWT.RADIO);
		button1.setText(Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString());
		button1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				modelOption = ModelOption.INDIVIDUAL_SENSORS_2;
			}
		});
		button1.setSelection(true);
		Button button2 = new Button(radioButton, SWT.RADIO);
		button2.setText(Constants.ModelOption.ALL_SENSORS.toString());
		button2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				modelOption = ModelOption.ALL_SENSORS;
			}
		});
				
		Label noteLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		noteLabel.setText("More info: The \"Launch Converter\" button will allow file format conversions from ASCII to hdf5 for common subsurface simulation output formats (currently: NUFT, STOMP). If the file converter is incompatible with the desired output file format, specific formatting requirements are given in the user manual. ");
		GridData noteGridData = new GridData(GridData.FILL_HORIZONTAL);
		noteGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		noteGridData.verticalSpan = 4;
		noteGridData.widthHint = 500;
		noteLabel.setLayoutData(noteGridData);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();		

		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(true);
	}
	
	private boolean validFileCheck(String folderDir) {
		boolean h5Error = true;
		File folder = new File(folderDir);
		File[] fList = folder.listFiles();
		for (File file : fList) {
			if(file.getName().contains(".h5") || file.getName().contains(".iam")) {
				h5Error = false;
				break;
			}
		}
		return h5Error;
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