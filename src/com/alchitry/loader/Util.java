package com.alchitry.loader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class Util {
	private static Display display;
	private static Shell shell;
	public static final boolean isWindows;
	public static final boolean isLinux;

	static {
		String os = System.getProperty("os.name");
		isWindows = os.startsWith("Windows");
		isLinux = os.startsWith("Linux");
	}

	public static void setDisplay(Display display) {
		Util.display = display;
	}

	public static void setShell(Shell shell) {
		Util.shell = shell;
	}

	public static void asyncExec(Runnable r) {
		display.asyncExec(r);
	}

	public static void syncExec(Runnable r) {
		display.syncExec(r);
	}

	public static Display getDisplay() {
		return display;
	}

	public static Shell getShell() {
		return shell;
	}

	private static class QuestionRunnable implements Runnable {
		public boolean result;
		private String title;
		private String message;

		public QuestionRunnable(String title, String message) {
			this.title = title;
			this.message = message;
		}

		@Override
		public void run() {
			MessageBox confirm = new MessageBox(shell, SWT.YES | SWT.NO | SWT.APPLICATION_MODAL);
			confirm.setText(title);
			confirm.setMessage(message);
			result = confirm.open() == SWT.YES;
		}
	}

	public static boolean askQuestion(String message) {
		return askQuestion("Question?", message);
	}

	public static boolean askQuestion(String title, String message) {
		QuestionRunnable question = new QuestionRunnable(title, message);
		display.syncExec(question);
		return question.result;
	}

	public static void showError(final String error) {
		showError("Error!", error);
	}

	public static void showError(final String title, final String error) {
		display.asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox b = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR | SWT.APPLICATION_MODAL);
				b.setText(title);
				b.setMessage(error);
				b.open();
			}

		});
	}

	public static void showInfo(final String text) {
		showInfo("Info", text);
	}

	public static void showInfo(final String title, final String text) {
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox b = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION | SWT.PRIMARY_MODAL);
				b.setText(title);
				b.setMessage(text);
				b.open();
			}

		});
	}
	

}
