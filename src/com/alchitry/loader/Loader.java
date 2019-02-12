package com.alchitry.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Loader {
	private static final String AU_DESCRIPTION = "Alchitry Au";
	private static final String CU_DESCRIPTION = "Alchitry Cu";
	private static final String toolsDir = "tools";
	private static final String loaderEx = Util.isWindows ? "loader.exe" : "loader";
	private ArrayList<String> deviceList;
	private Thread worker;
	private Object lock;
	private String status;

	public Loader() {
		deviceList = new ArrayList<>();
		lock = new Object();
	}

	public boolean jobRunning() {
		if (worker != null && worker.isAlive())
			return true;
		return false;
	}

	public void stop() {
		if (jobRunning())
			worker.interrupt();
	}

	private int getDeviceNumber(boolean isAu) {
		String deviceDescription = isAu ? AU_DESCRIPTION : CU_DESCRIPTION;
		fetchDeviceList(null);

		String[] devices = getDeviceList();

		for (int i = 0; i < devices.length; i++) {
			if (devices[i].equals(deviceDescription))
				return i;
		}

		return -1;
	}

	private void fetchDeviceList(Runnable callBack) {
		ArrayList<String> cmd = new ArrayList<>();
		cmd.add(toolsDir + File.separator + loaderEx);
		cmd.add("-l");
		Process p = runCommand(cmd);

		BufferedReader inputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));

		int exitCode = -1;
		try {
			exitCode = p.waitFor();
		} catch (InterruptedException e) {
			p.destroy();
			Util.showError("Interrupted Exception!");
			synchronized (deviceList) {
				deviceList.clear();
			}
			if (callBack != null)
				Util.asyncExec(callBack);
			return;
		}

		synchronized (deviceList) {
			deviceList.clear();

			if (exitCode == 0) {
				try {
					String line = inputStream.readLine();
					if (line.startsWith("Devices")) {
						while ((line = inputStream.readLine()) != null) {
							String[] s = line.split(":");
							deviceList.add(s[1].trim());
						}
					}

				} catch (IOException e) {
					Util.showError("IO Exception!");
				}
			}
		}

		try {
			inputStream.close();
		} catch (IOException e) {
			Util.showError("Failed to close stream!");
		}

		if (callBack != null)
			Util.asyncExec(callBack);
	}

	public void updateDeviceList(Runnable callBack) {
		if (jobRunning()) {
			Util.showError("Job already running!");
			return;
		}
		worker = new Thread() {
			public void run() {
				fetchDeviceList(callBack);
			}
		};
		worker.start();
	}

	public String getStatus() {
		synchronized (lock) {
			return status;
		}
	}

	private void setStatus(String s) {
		synchronized (lock) {
			status = s;
		}
	}

	public void startFlashEEPROM(boolean isAu, Runnable progressCallBack, Runnable finishedCallBack) {
		if (jobRunning()) {
			Util.showError("Job already running!");
			return;
		}
		worker = new Thread() {
			public void run() {
				ArrayList<String> cmd = new ArrayList<>();
				cmd.add(toolsDir + File.separator + loaderEx);

				cmd.add("-u");

				if (isAu)
					cmd.add(toolsDir + File.separator + "au_ftdi.data");
				else
					cmd.add(toolsDir + File.separator + "cu_ftdi.data");

				Process p = runCommand(cmd);
				
				if (p == null) {
					setStatus("Failed to start loader!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				startStreamPrinter(p.getErrorStream(), progressCallBack);
				startStreamPrinter(p.getInputStream(), progressCallBack);

				int exitCode = -1;
				try {
					exitCode = p.waitFor();
				} catch (InterruptedException e) {
					p.destroy();
					setStatus("Interrupted Exception!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				if (exitCode != 0) {
					Util.showError("Failed to program EEPROM!");
				} else {
					Util.showInfo("Info", "Unplug and replug the device to reload the EEPROM contents!");
				}

				Util.asyncExec(progressCallBack);
				Util.asyncExec(finishedCallBack);
			}
		};
		worker.start();
	}

	public void startProgramming(String bin, boolean isAu, boolean flash, Runnable progressCallBack, Runnable finishedCallBack) {
		if (jobRunning()) {
			Util.showError("Job already running!");
			return;
		}
		worker = new Thread() {
			public void run() {
				ArrayList<String> cmd = new ArrayList<>();
				cmd.add(toolsDir + File.separator + loaderEx);
				if (isAu && flash) {
					cmd.add("-p");
					File f = new File(toolsDir + File.separator + "au_loader.bin");
					cmd.add(f.getAbsolutePath());
				}
				if (flash)
					cmd.add("-f");
				else
					cmd.add("-r");
				cmd.add(bin);

				setStatus("Looking for device...");
				Util.asyncExec(progressCallBack);

				int devNum = getDeviceNumber(isAu);

				if (devNum < 0) {
					Util.showError("Couldn't find device!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				cmd.add("-b");
				cmd.add(Integer.toString(devNum));

				Process p = runCommand(cmd);
				
				if (p == null) {
					setStatus("Failed to start loader!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				startStreamPrinter(p.getErrorStream(), progressCallBack);
				startStreamPrinter(p.getInputStream(), progressCallBack);

				int exitCode = -1;
				try {
					exitCode = p.waitFor();
				} catch (InterruptedException e) {
					p.destroy();
					setStatus("Interrupted Exception!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				if (exitCode != 0) {
					Util.showError("Failed to program device!");
				} else {
					setStatus("Done.");
				}

				Util.asyncExec(progressCallBack);
				Util.asyncExec(finishedCallBack);
			}
		};
		worker.start();
	}

	public void startErase(boolean isAu, Runnable progressCallBack, Runnable finishedCallBack) {
		if (jobRunning()) {
			Util.showError("Job already running!");
			return;
		}
		worker = new Thread() {
			public void run() {
				ArrayList<String> cmd = new ArrayList<>();
				cmd.add(toolsDir + File.separator + loaderEx);
				if (isAu) {
					cmd.add("-p");
					cmd.add(toolsDir + File.separator + "au_loader.bin");
				}
				cmd.add("-e");

				setStatus("Looking for device...");
				Util.asyncExec(progressCallBack);

				int devNum = getDeviceNumber(isAu);

				if (devNum < 0) {
					Util.showError("Couldn't find device!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				cmd.add("-b");
				cmd.add(Integer.toString(devNum));

				Process p = runCommand(cmd);
				
				if (p == null) {
					setStatus("Failed to start loader!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				startStreamPrinter(p.getErrorStream(), progressCallBack);
				startStreamPrinter(p.getInputStream(), progressCallBack);

				int exitCode = -1;
				try {
					exitCode = p.waitFor();
				} catch (InterruptedException e) {
					p.destroy();
					setStatus("Interrupted Exception!");
					Util.asyncExec(progressCallBack);
					Util.asyncExec(finishedCallBack);
					return;
				}

				if (exitCode != 0) {
					Util.showError("Failed to erase device!");
				} else {
					setStatus("Done.");
				}

				Util.asyncExec(progressCallBack);
				Util.asyncExec(finishedCallBack);
			}
		};
		worker.start();
	}

	public String[] getDeviceList() {
		synchronized (deviceList) {
			return deviceList.toArray(new String[0]);
		}
	}

	public static Process runCommand(List<String> cmd) {
		ProcessBuilder pb = new ProcessBuilder(cmd);

		Process process;

		try {
			process = pb.start();
		} catch (Exception e) {
			Util.showError("Could not start " + cmd.get(0) + "!");
			return null;
		}

		return process;
	}

	public void startStreamPrinter(final InputStream s, final Runnable progressCallBack) {
		Thread printer = new Thread() {
			public void run() {
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(s));
				String line;
				try {
					while ((line = inputStream.readLine()) != null) {
						setStatus(line);
						progressCallBack.run();
					}
				} catch (IOException e) {
				} finally {
					try {
						inputStream.close();
					} catch (IOException e) {
					}
				}
			}
		};
		printer.start();
	}
}
