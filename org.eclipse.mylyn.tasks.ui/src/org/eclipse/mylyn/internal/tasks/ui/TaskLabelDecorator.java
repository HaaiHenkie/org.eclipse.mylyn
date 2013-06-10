/*******************************************************************************
 * Copyright (c) 2004, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.mylyn.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITaskRepositoryElement;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;

/**
 * @author Mik Kersten
 */
public class TaskLabelDecorator implements ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {

		ImageDescriptor priorityOverlay = getPriorityImageDescriptor(element);
		if (priorityOverlay != null) {
			decoration.addOverlay(priorityOverlay, IDecoration.BOTTOM_LEFT);
		}

		if (element instanceof ITask) {
			ITask task = (ITask) element;
			if (!task.isCompleted()
					&& (TasksUiPlugin.getTaskActivityManager().isDueToday(task) || TasksUiPlugin.getTaskActivityManager()
							.isOverdue(task))) {
				decoration.addOverlay(CommonImages.OVERLAY_DATE_OVERDUE, IDecoration.TOP_RIGHT);
			} else if (!task.isCompleted() && task.getDueDate() != null) {
				decoration.addOverlay(CommonImages.OVERLAY_DATE_DUE, IDecoration.TOP_RIGHT);
			}
			if (hasNotes(task)) {
				decoration.addOverlay(TasksUiImages.NOTES, IDecoration.BOTTOM_RIGHT);
			}
		} else if (element instanceof ITaskRepositoryElement) {
			ITaskRepositoryElement repositoryElement = (ITaskRepositoryElement) element;
			String repositoryUrl = repositoryElement.getRepositoryUrl();
			TaskRepository taskRepository = TasksUi.getRepositoryManager().getRepository(
					repositoryElement.getConnectorKind(), repositoryUrl);
			if (taskRepository != null) {
				decoration.addSuffix("   [" + taskRepository.getRepositoryLabel() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (element instanceof TaskRepository) {
			ImageDescriptor overlay = TasksUiPlugin.getDefault().getOverlayIcon(
					((TaskRepository) element).getConnectorKind());
			if (overlay != null) {
				decoration.addOverlay(overlay, IDecoration.BOTTOM_RIGHT);
			}
		}
	}

	public void addListener(ILabelProviderListener listener) {
		// ignore
	}

	public void dispose() {
		// ignore
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// ignore
	}

	private ImageDescriptor getPriorityImageDescriptor(Object element) {
		AbstractRepositoryConnectorUi connectorUi;
		if (element instanceof ITask) {
			ITask repositoryTask = (ITask) element;
			connectorUi = TasksUiPlugin.getConnectorUi(((ITask) element).getConnectorKind());
			if (connectorUi != null) {
				return connectorUi.getTaskPriorityOverlay(repositoryTask);
			}
		}
		if (element instanceof ITask) {
			return TasksUiInternal.getPriorityImage((ITask) element);
		}
		return null;
	}

	private boolean hasNotes(ITask task) {
		if (task instanceof AbstractTask) {
			return !((AbstractTask) task).getNotes().isEmpty();
		}
		return false;
	}

}
