/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasklist;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylar.internal.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.tasklist.ui.wizards.TaskDataExportWizard;
import org.eclipse.mylar.internal.tasklist.util.TaskDataExportJob;
import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * @author Rob Elves
 */
public class TaskListBackupManager implements IPropertyChangeListener {

	public static final String BACKUP_FAILURE_MESSAGE = "Could not backup task data. Check backup preferences.\n";

	private static final long SECOND = 1000;

	private static final long MINUTE = 60 * SECOND;

	private static final long HOUR = 60 * MINUTE;

	private static final long DAY = 24 * HOUR;

	private Timer timer;

	public TaskListBackupManager() {
		boolean enabled = MylarTaskListPlugin.getMylarCorePrefs().getBoolean(
				TaskListPreferenceConstants.BACKUP_AUTOMATICALLY);

		if (enabled) {
			start();
		}
	}

	public void start() {
		timer = new Timer();
		timer.schedule(new CheckBackupRequired(), MINUTE, HOUR);
	}

	public void stop() {
		timer.cancel();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TaskListPreferenceConstants.BACKUP_AUTOMATICALLY)) {
			if ((Boolean) event.getNewValue() == true) {
				start();
			} else {
				stop();
			}
		}
	}

	/**
	 * @throws InvocationTargetException
	 * @throws IOException 
	 */
	static public void backupNow() throws InvocationTargetException, IOException {
		String destination = MylarTaskListPlugin.getMylarCorePrefs().getString(
				TaskListPreferenceConstants.BACKUP_FOLDER);
		
		File backupFolder = new File(destination);
		if(!backupFolder.exists()) {
			backupFolder.mkdir();		
		}
		
		removeOldBackups(backupFolder);

//		String formatString = "yyyy-MM-dd";
//		SimpleDateFormat format = new SimpleDateFormat(formatString, Locale.ENGLISH);
//		String date = format.format(new Date());
		String fileName = TaskDataExportWizard.getZipFileName();
//			FILENAME_PREFIX + "-" + date + ZIP_FILE_EXTENSION;

		final TaskDataExportJob backupJob = new TaskDataExportJob(destination, true, fileName);

		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {			
			service.run(true, false, backupJob);
		} catch (InterruptedException e) {
			// ignore
		}

		MylarTaskListPlugin.getMylarCorePrefs()
				.setValue(TaskListPreferenceConstants.BACKUP_LAST, new Date().getTime());
	}

	static private void removeOldBackups(File folder) {

		int maxBackups = MylarTaskListPlugin.getMylarCorePrefs().getInt(TaskListPreferenceConstants.BACKUP_MAXFILES);
		
		File[] files = folder.listFiles();		
		ArrayList<File> backupFiles = new ArrayList<File>();
		for (File file : files) {
			if (file.getName().startsWith(TaskDataExportWizard.ZIP_FILE_PREFIX)) {				
				backupFiles.add(file);
			}
		}
		
		File[] backupFileArray = backupFiles.toArray(new File[1]);
		
		Arrays.sort( backupFileArray, new Comparator<File>()
		{
			public int compare(File file1, File file2) {
				return new Long((file1).lastModified()).compareTo(new Long((file2).lastModified()));
			}

		}); 
		
		int toomany = backupFileArray.length - maxBackups;
		if (toomany > 0) {
			for (int x = 0; x < toomany; x++) {
				backupFileArray[x].delete();
			}
		}
	}

	class CheckBackupRequired extends TimerTask {

		@Override
		public void run() {
			long lastBackup = MylarTaskListPlugin.getMylarCorePrefs()
					.getLong(TaskListPreferenceConstants.BACKUP_LAST);
			int days = MylarTaskListPlugin.getMylarCorePrefs().getInt(TaskListPreferenceConstants.BACKUP_SCHEDULE);
			long waitPeriod = days * DAY;
			final long now = new Date().getTime();

			if ((now - lastBackup) > waitPeriod) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							// MessageDialog.openQuestion(shell, title,
							// message);
							backupNow();
						} catch (InvocationTargetException e) {
							MylarStatusHandler.fail(e, BACKUP_FAILURE_MESSAGE + e.getCause().getMessage(), true);
							return;
						} catch (IOException e) {
							MylarStatusHandler.fail(e, BACKUP_FAILURE_MESSAGE + e.getCause().getMessage(), true);
							return;
						}
					}
				});
			}
		}
	}

}
