/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.bugzilla.ui.tasklist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.bugzilla.core.BugzillaPlugin;
import org.eclipse.mylar.bugzilla.ui.BugzillaOpenStructure;
import org.eclipse.mylar.bugzilla.ui.BugzillaUITools;
import org.eclipse.mylar.bugzilla.ui.BugzillaUiPlugin;
import org.eclipse.mylar.bugzilla.ui.ViewBugzillaAction;
import org.eclipse.mylar.bugzilla.ui.actions.RefreshBugzillaAction;
import org.eclipse.mylar.bugzilla.ui.actions.RefreshBugzillaReportsAction;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.mylar.tasklist.ITaskHandler;
import org.eclipse.mylar.tasklist.ITaskCategory;
import org.eclipse.mylar.tasklist.ITaskListElement;
import org.eclipse.mylar.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.tasklist.internal.TaskCategory;
import org.eclipse.mylar.tasklist.ui.actions.CopyDescriptionAction;
import org.eclipse.mylar.tasklist.ui.actions.DeleteAction;
import org.eclipse.mylar.tasklist.ui.actions.GoIntoAction;
import org.eclipse.mylar.tasklist.ui.actions.OpenTaskEditorAction;
import org.eclipse.mylar.tasklist.ui.actions.RemoveFromCategoryAction;
import org.eclipse.mylar.tasklist.ui.actions.RenameAction;
import org.eclipse.mylar.tasklist.ui.views.TaskListView;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.internal.Workbench;

/**
 * @author Mik Kersten and Ken Sueda
 */
public class BugzillaTaskHandler implements ITaskHandler {

	public boolean deleteElement(ITaskListElement element) {
		if (element instanceof BugzillaQueryCategory) {
			boolean deleteConfirmed = MessageDialog.openQuestion(
		            Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
		            "Confirm delete", 
		            "Delete the selected query and all contained tasks?");
			if (!deleteConfirmed) 
				return false;
			BugzillaQueryCategory query = (BugzillaQueryCategory) element;
			MylarTaskListPlugin.getTaskListManager().deleteQuery(query);
		} else if (element instanceof BugzillaTask) {
			BugzillaTask task = (BugzillaTask) element;
			if (task.isActive()) {
				MessageDialog.openError(Workbench.getInstance()
						.getActiveWorkbenchWindow().getShell(), "Delete failed",
						"Task must be deactivated in order to delete.");
				return false;
			}
			
			String message = task.getDeleteConfirmationMessage();			
			boolean deleteConfirmed = MessageDialog.openQuestion(
		            Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
		            "Confirm delete", message);
			if (!deleteConfirmed)
				return false;  
									
//			task.removeReport();
			MylarTaskListPlugin.getTaskListManager().deleteTask(task);
			MylarPlugin.getContextManager().contextDeleted(task.getHandleIdentifier(), task.getContextPath());
			IWorkbenchPage page = MylarTaskListPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();

			// if we couldn't get the page, get out of here
			if (page == null)
				return true;
			try {
				TaskListView.getDefault().closeTaskEditors(task, page);
			} catch (Exception e) {
				MylarPlugin.log(e, " deletion failed");
			}
		}
		TaskListView.getDefault().getViewer().refresh();
		return true;
	}

	public void taskCompleted(ITask task) {
		// TODO can't do this	
	}

	public void itemOpened(ITaskListElement element) {

		boolean offline = MylarTaskListPlugin.getPrefs().getBoolean(MylarPlugin.WORK_OFFLINE);
		
		if (element instanceof BugzillaTask) {
			BugzillaTask t = (BugzillaTask) element;
			MylarTaskListPlugin.ReportOpenMode mode = MylarTaskListPlugin.getDefault().getReportMode();
			if (mode == MylarTaskListPlugin.ReportOpenMode.EDITOR) {
				t.openTaskInEditor(offline);
			} else if (mode == MylarTaskListPlugin.ReportOpenMode.INTERNAL_BROWSER) {
				if(offline){
					MessageDialog.openInformation(null, "Unable to open bug", "Unable to open the selected bugzilla task since you are currently offline");
	    			return;
	    		}
				String title = "Bug #" + BugzillaTask.getBugId(t.getHandleIdentifier());
				BugzillaUITools.openUrl(title, title, t.getBugUrl());	    			
			} else {
				// not supported
			}
		} else if(element instanceof BugzillaCustomQuery){
			BugzillaCustomQuery queryCategory = (BugzillaCustomQuery)element;
	       	BugzillaCustomQueryDialog sqd = new BugzillaCustomQueryDialog(Display.getCurrent().getActiveShell(), queryCategory.getQueryString(), queryCategory.getDescription(false), queryCategory.getMaxHits()+"");
        	if(sqd.open() == Dialog.OK){
	        	queryCategory.setDescription(sqd.getName());
	        	queryCategory.setQueryString(sqd.getUrl());
	        	int maxHits = -1;
	        	try{
	        		maxHits = Integer.parseInt(sqd.getMaxHits());
	        	} catch(Exception e){}
	        	queryCategory.setMaxHits(maxHits);
	        	
	        	new RefreshBugzillaAction(queryCategory).run();
        	}
		}else if (element instanceof BugzillaQueryCategory){
			BugzillaQueryCategory queryCategory = (BugzillaQueryCategory)element;
	       	BugzillaQueryDialog sqd = new BugzillaQueryDialog(Display.getCurrent().getActiveShell(), queryCategory.getQueryString(), queryCategory.getDescription(false), queryCategory.getMaxHits()+"");
        	if(sqd.open() == Dialog.OK){
	        	queryCategory.setDescription(sqd.getName());
	        	queryCategory.setQueryString(sqd.getUrl());
	        	int maxHits = -1;
	        	try{
	        		maxHits = Integer.parseInt(sqd.getMaxHits());
	        	} catch(Exception e){}
	        	queryCategory.setMaxHits(maxHits);
	        	
	        	new RefreshBugzillaAction(queryCategory).run();
        	}
	    } else if(element instanceof BugzillaHit){
	    	BugzillaHit hit = (BugzillaHit)element;
	    	MylarTaskListPlugin.ReportOpenMode mode = MylarTaskListPlugin.getDefault().getReportMode();
	    	if (mode == MylarTaskListPlugin.ReportOpenMode.EDITOR) {
	    		if(hit.hasCorrespondingActivatableTask()){
		    		hit.getAssociatedTask().openTaskInEditor(offline);
		    	} else {
		    		if(offline){
		    			MessageDialog.openInformation(null, "Unable to open bug", "Unable to open the selected bugzilla report since you are currently offline");
		    			return;
		    		}
			    	BugzillaOpenStructure open = new BugzillaOpenStructure(((BugzillaHit)element).getServerName(), ((BugzillaHit)element).getID(),-1);
			    	List<BugzillaOpenStructure> selectedBugs = new ArrayList<BugzillaOpenStructure>();
			    	selectedBugs.add(open);
			    	ViewBugzillaAction viewBugs = new ViewBugzillaAction("Display bugs in editor", selectedBugs);
					viewBugs.schedule();
		    	}
    		} else if (mode == MylarTaskListPlugin.ReportOpenMode.INTERNAL_BROWSER) {
    			if(offline){
    				MessageDialog.openInformation(null, "Unable to open bug", "Unable to open the selected bugzilla report since you are currently offline");
	    			return;
	    		}
    			String title = "Bug #" + BugzillaTask.getBugId(hit.getHandleIdentifier());
    			BugzillaUITools.openUrl(title, title, hit.getBugUrl());  			
    		} else {
    			// not supported
    		}
	    }
		
	}

	public boolean acceptsItem(ITaskListElement element) {
		return element instanceof BugzillaTask || element instanceof BugzillaHit || element instanceof BugzillaQueryCategory;
	}

	public void dropItem(ITaskListElement element, TaskCategory cat) {
		if (element instanceof BugzillaHit) {
        	BugzillaHit bh = (BugzillaHit) element;
    		if (bh.getAssociatedTask() != null) {
        		bh.getAssociatedTask().setCategory(cat);
        		cat.addTask(bh.getAssociatedTask());
        	} else {
        		BugzillaTask bt = new BugzillaTask(bh, true);
        		bh.setAssociatedTask(bt);
        		bt.setCategory(cat);
        		cat.addTask(bt);
        		BugzillaUiPlugin.getDefault().getBugzillaTaskListManager().addToBugzillaTaskRegistry(bt);
        	}
		}		
	}

	public void taskClosed(ITask element, IWorkbenchPage page) {
		try{
			IEditorInput input = null;		
			if (element instanceof BugzillaTask) {
				input = new BugzillaTaskEditorInput((BugzillaTask)element, true);
			}
			IEditorPart editor = page.findEditor(input);
	
			if (editor != null) {
				page.closeEditor(editor, false);
			}
		} catch (Exception e){
			MylarPlugin.log(e, "Error while trying to close a bugzilla task");
		}
	}

	public ITask taskAdded(ITask newTask) {
		if(newTask instanceof BugzillaTask){
			BugzillaTask bugTask = BugzillaUiPlugin.getDefault().getBugzillaTaskListManager().getFromBugzillaTaskRegistry(newTask.getHandleIdentifier());
			if(bugTask == null){
				BugzillaUiPlugin.getDefault().getBugzillaTaskListManager().addToBugzillaTaskRegistry((BugzillaTask)newTask);
				bugTask = (BugzillaTask)newTask;	
			}
			return bugTask;
		}
		return null;
	}

	public void restoreState(TaskListView taskListView) {
		if (BugzillaPlugin.getDefault().refreshOnStartUpEnabled()) {
			RefreshBugzillaReportsAction refresh = new RefreshBugzillaReportsAction();
			refresh.setShowProgress(false);
			refresh.run();
			refresh.setShowProgress(true);
		}		
	}

	public boolean enableAction(Action action, ITaskListElement element) {

		if(element instanceof BugzillaHit){
			return false;
		} else if(element instanceof BugzillaTask){
			if(action instanceof DeleteAction || action instanceof CopyDescriptionAction || action instanceof OpenTaskEditorAction || action instanceof RemoveFromCategoryAction){
				return true;
			} else {
				return false;
			}
		} else if(element instanceof BugzillaQueryCategory){
			if(action instanceof DeleteAction || action instanceof CopyDescriptionAction || action instanceof OpenTaskEditorAction || action instanceof RenameAction){
				return true;
			} else if(action instanceof GoIntoAction){
				BugzillaQueryCategory cat = (BugzillaQueryCategory) element;
				if(cat.getChildren().size() > 0){
					return true;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public void itemRemoved(ITaskListElement element, ITaskCategory category) {
		if (element instanceof BugzillaTask){
			BugzillaTask task = (BugzillaTask) element;
			if (category != null) {
				category.removeTask(task);
			} else {
				String message = task.getDeleteConfirmationMessage();			
				boolean deleteConfirmed = MessageDialog.openQuestion(
			            Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
			            "Confirm delete", message);
				if (!deleteConfirmed) 
					return;
				MylarTaskListPlugin.getTaskListManager().deleteTask(task);
			}
		}
	}

	public ITask dropItemToPlan(ITaskListElement element) {
		if(element instanceof BugzillaHit){
			BugzillaHit hit = (BugzillaHit) element;
			return hit.getOrCreateCorrespondingTask();
		} else if(element instanceof BugzillaTask){		
			return (ITask) element;
		} else {
			return null;
		}		
	} 
}
