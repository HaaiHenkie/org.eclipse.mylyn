/*******************************************************************************
 * Copyright (c) 2004, 2013 Tasktop Technologies and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     See git history
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.mylyn.commons.workbench.AdaptiveRefreshPolicy.IFilteredTreeListener;
import org.eclipse.mylyn.internal.tasks.ui.AbstractTaskListFilter;
import org.eclipse.mylyn.internal.tasks.ui.ITasksUiPreferenceConstants;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListInterestFilter;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListInterestSorter;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskScheduleContentProvider;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class FocusTaskListAction implements IFilteredTreeListener, IViewActionDelegate, IActionDelegate2 {

	private Set<AbstractTaskListFilter> previousFilters = new HashSet<>();

	private ViewerComparator previousSorter;

	private final TaskListInterestFilter taskListInterestFilter = new TaskListInterestFilter();

	private final TaskListInterestSorter taskListInterestSorter = new TaskListInterestSorter();

	private TaskListView taskListView;

	private IAction action;

	public FocusTaskListAction() {
	}

	@Override
	public void filterTextChanged(final String text) {
		if (taskListView.isFocusedMode() && (text == null || "".equals(text.trim()))) { //$NON-NLS-1$
			taskListView.getViewer().expandAll();
		}
	}

	@Override
	public void init(IAction action) {
		this.action = action;
		initAction();
	}

	@Override
	public void init(IViewPart view) {
		if (view instanceof TaskListView) {
			taskListView = (TaskListView) view;
			taskListView.getFilteredTree().getRefreshPolicy().addListener(this);
			taskListView.getFilteredTree().addDisposeListener(e -> taskListView.getFilteredTree().getRefreshPolicy().removeListener(FocusTaskListAction.this));
			if (TasksUiPlugin.getDefault()
					.getPreferenceStore()
					.getBoolean(ITasksUiPreferenceConstants.TASK_LIST_FOCUSED)) {
				installInterestFilter();
			}
			initAction();
			showProgressBar(taskListView.isFocusedMode());
		}
	}

	private void initAction() {
		if (action != null && taskListView != null) {
			action.setChecked(taskListView.isFocusedMode());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// ignore
	}

	private void showProgressBar(boolean visible) {
		taskListView.getFilteredTree().setShowProgress(visible);
	}

	protected void installInterestFilter() {
		TasksUiInternal.preservingSelection(taskListView.getViewer(), () -> {
			try {
				taskListView.getFilteredTree().setRedraw(false);
				taskListView.setFocusedMode(true);
				previousSorter = taskListView.getViewer().getSorter();
				previousFilters = taskListView.clearFilters();
				if (!taskListView.getFilters().contains(taskListInterestFilter)) {
					taskListView.addFilter(taskListInterestFilter);
				}
				// Setting sorter causes root refresh
				taskListInterestSorter.setconfiguredSorter(previousSorter);
				taskListView.getViewer().setComparator(taskListInterestSorter);
				taskListView.getViewer().expandAll();

				showProgressBar(true);
			} finally {
				taskListView.getFilteredTree().setRedraw(true);
			}
		});
	}

	protected void uninstallInterestFilter() {
		TasksUiInternal.preservingSelection(taskListView.getViewer(), () -> {
			try {
				taskListView.getViewer().getControl().setRedraw(false);
				taskListView.setFocusedMode(false);
				for (AbstractTaskListFilter filter : previousFilters) {
					taskListView.addFilter(filter);
				}
				taskListView.removeFilter(taskListInterestFilter);
				Text textControl = taskListView.getFilteredTree().getFilterControl();
				if (textControl != null && textControl.getText().length() > 0) {
					taskListView.getViewer().expandAll();
				} else {
					taskListView.getViewer().collapseAll();
					// expand first element (Today) in scheduled mode
					if (taskListView.getViewer().getContentProvider() instanceof TaskScheduleContentProvider
							&& taskListView.getViewer().getTree().getItemCount() > 0) {
						TreeItem item = taskListView.getViewer().getTree().getItem(0);
						if (item.getData() != null) {
							taskListView.getViewer().expandToLevel(item.getData(), 1);
						}
					}
				}
				taskListView.getViewer().setComparator(previousSorter);
				showProgressBar(false);
			} finally {
				taskListView.getViewer().getControl().setRedraw(true);
			}
		});
	}

	public void run() {
		if (taskListView == null) {
			return;
		}

		if (!taskListView.isFocusedMode()) {
			TasksUiPlugin.getDefault()
			.getPreferenceStore()
			.setValue(ITasksUiPreferenceConstants.TASK_LIST_FOCUSED, true);
			installInterestFilter();
		} else {
			TasksUiPlugin.getDefault()
			.getPreferenceStore()
			.setValue(ITasksUiPreferenceConstants.TASK_LIST_FOCUSED, false);
			uninstallInterestFilter();
		}
	}

	@Override
	public void run(IAction action) {
		run();
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run();
	}

	@Override
	public void dispose() {
		// ignore
	}

}
