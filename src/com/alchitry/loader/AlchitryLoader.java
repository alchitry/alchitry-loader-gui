package com.alchitry.loader;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

public class AlchitryLoader {

	public static final String VERSION = "1.0.0";

	protected Shell shell;
	private Text binText;
	private Button btnAuButton;
	private Button btnCuButton;
	private Button btnEraseButton;
	private Button btnFlashCheckbox;
	private Button btnProgramButton;
	private Button btnOpenBinButton;
	private Label lblStatus;
	private Button btnResetButton;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			AlchitryLoader window = new AlchitryLoader();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		Util.setDisplay(display);
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void setEnabled(boolean enabled) {
		btnAuButton.setEnabled(enabled);
		btnCuButton.setEnabled(enabled);
		btnEraseButton.setEnabled(enabled);
		btnProgramButton.setEnabled(enabled);
		btnOpenBinButton.setEnabled(enabled);
		btnFlashCheckbox.setEnabled(enabled && !btnCuButton.getSelection());
		btnResetButton.setEnabled(enabled);
		binText.setEnabled(enabled);
		shell.setEnabled(enabled);
	}

	private void setStatus(String status) {
		Util.syncExec(new Runnable() {
			@Override
			public void run() {
				lblStatus.setText(status);
			}
		});
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		Util.setShell(shell);
		shell.setText("Alchitry Loader V" + VERSION);
		shell.setLayout(new GridLayout(4, false));
		shell.setImage(SWTResourceManager.getImage(AlchitryLoader.class, "/images/icon.png"));

		binText = new Text(shell, SWT.BORDER);
		binText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		btnOpenBinButton = new Button(shell, SWT.NONE);
		btnOpenBinButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnOpenBinButton.setText("Open Bin File");

		btnOpenBinButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				FileDialog fd = new FileDialog(shell);
				fd.setFilterExtensions(new String[] { "*.bin", "*.*" });
				fd.setFilterNames(new String[] { "Bin Files", "All Files" });
				String file = fd.open();
				if (file != null)
					binText.setText(file);
			}
		});

		Label lblNewLabel = new Label(shell, SWT.NONE);
		lblNewLabel.setText("Board:");

		btnAuButton = new Button(shell, SWT.RADIO);
		btnAuButton.setSelection(true);
		btnAuButton.setText("Alchitry Au");

		btnAuButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				btnFlashCheckbox.setEnabled(true);
			}
		});

		btnCuButton = new Button(shell, SWT.RADIO);
		btnCuButton.setText("Alchitry Cu");

		btnCuButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				btnFlashCheckbox.setSelection(true);
				btnFlashCheckbox.setEnabled(false);
			}
		});

		btnResetButton = new Button(shell, SWT.NONE);
		btnResetButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnResetButton.setText("Flash EEPROM");

		btnResetButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				setEnabled(false);
				setStatus("Starting...");
				Loader loader = new Loader();
				loader.startFlashEEPROM(btnAuButton.getSelection(), new Runnable() {

					@Override
					public void run() {
						setStatus(loader.getStatus());
					}
				}, new Runnable() {

					@Override
					public void run() {
						setEnabled(true);
					}
				});
			}
		});

		btnFlashCheckbox = new Button(shell, SWT.CHECK);
		btnFlashCheckbox.setSelection(true);
		btnFlashCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		btnFlashCheckbox.setText("Program Flash");

		btnEraseButton = new Button(shell, SWT.NONE);
		btnEraseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnEraseButton.setText("Erase");

		btnEraseButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				setEnabled(false);
				setStatus("Starting...");
				Loader loader = new Loader();
				loader.startErase(btnAuButton.getSelection(), new Runnable() {

					@Override
					public void run() {
						setStatus(loader.getStatus());
					}
				}, new Runnable() {

					@Override
					public void run() {
						setEnabled(true);
					}
				});

			}
		});

		Label lblNewLabel_1 = new Label(shell, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_1.setText("Status:");

		lblStatus = new Label(shell, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		btnProgramButton = new Button(shell, SWT.NONE);
		btnProgramButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnProgramButton.setText("Program");

		btnProgramButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (!checkBinFileExists())
					return;
				setEnabled(false);
				setStatus("Starting...");
				Loader loader = new Loader();
				loader.startProgramming(binText.getText(), btnAuButton.getSelection(), btnFlashCheckbox.getSelection(), new Runnable() {

					@Override
					public void run() {
						setStatus(loader.getStatus());
					}
				}, new Runnable() {

					@Override
					public void run() {
						setEnabled(true);
					}
				});

			}
		});

		shell.pack();
		Point s = shell.getSize();
		shell.setMinimumSize(s);
		s.x = Math.max(s.x, 500);
		shell.setSize(s);
	}

	private boolean checkBinFileExists() {
		File f = new File(binText.getText());
		if (!f.exists()) {
			Util.showError("Bin file could not be opened!");
			return false;
		}
		return true;
	}
}
