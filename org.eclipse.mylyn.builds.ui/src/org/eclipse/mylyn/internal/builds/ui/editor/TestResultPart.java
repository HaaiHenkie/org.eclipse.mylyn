/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui.editor;

import org.eclipse.emf.databinding.FeaturePath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.mylyn.builds.core.IBuild;
import org.eclipse.mylyn.builds.core.ITestCase;
import org.eclipse.mylyn.builds.core.ITestResult;
import org.eclipse.mylyn.builds.core.ITestSuite;
import org.eclipse.mylyn.builds.core.TestCaseResult;
import org.eclipse.mylyn.builds.internal.core.BuildPackage.Literals;
import org.eclipse.mylyn.builds.internal.core.TestResult;
import org.eclipse.mylyn.internal.builds.ui.BuildImages;
import org.eclipse.mylyn.internal.builds.ui.actions.ShowTestResultsAction;
import org.eclipse.mylyn.internal.builds.ui.util.TestResultManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Steffen Pingel
 */
public class TestResultPart extends AbstractBuildEditorPart {

	public class FilterTestFailuresAction extends Action {

		public FilterTestFailuresAction() {
			super("Show Failures Only", IAction.AS_CHECK_BOX);
			setToolTipText("Show Failures Only");
			setImageDescriptor(BuildImages.FILTER_FAILURES);
		}

		@Override
		public void run() {
			if (isChecked()) {
				viewer.addFilter(testFailureFilter);
				viewer.expandAll();
			} else {
				viewer.removeFilter(testFailureFilter);
			}
		}

	}

	private static final String ID_POPUP_MENU = "org.eclipse.mylyn.builds.ui.editor.menu.TestResult"; //$NON-NLS-1$

	static class TestResultContentProvider implements ITreeContentProvider {

		private static final Object[] NO_ELEMENTS = new Object[0];

		private TestResult input;

		public void dispose() {
			input = null;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ITestSuite) {
				return ((ITestSuite) parentElement).getCases().toArray();
			}
			return NO_ELEMENTS;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement == input) {
				return input.getSuites().toArray();
			}
			if (inputElement instanceof String) {
				return new Object[] { inputElement };
			}
			return NO_ELEMENTS;
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof ITestSuite) {
				return !((ITestSuite) element).getCases().isEmpty();
			}
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof TestResult) {
				input = (TestResult) newInput;
			} else {
				input = null;
			}
		}

	}

	/**
	 * Selects failed tests only.
	 */
	private class TestFailureFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof ITestCase) {
				return isFailure(element);
			} else if (element instanceof ITestSuite) {
				for (ITestCase testCase : ((ITestSuite) element).getCases()) {
					if (isFailure(testCase)) {
						return true;
					}
				}
			}
			return false;
		}

		boolean isFailure(Object element) {
			TestCaseResult status = ((ITestCase) element).getStatus();
			return status == TestCaseResult.FAILED || status == TestCaseResult.REGRESSION;
		}

	}

	private MenuManager menuManager;

	private ShowTestResultsAction showTestResultsAction;

	private FilterTestFailuresAction filterTestFailuresAction;

	private TreeViewer viewer;

	private TestFailureFilter testFailureFilter;

	public TestResultPart() {
		super(ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED);
		setPartName("Test Results");
	}

	@Override
	protected Control createContent(Composite parent, FormToolkit toolkit) {
		Composite composite = toolkit.createComposite(parent);
		composite.setLayout(new GridLayout(6, false));

		ITestResult testResult = getInput(IBuild.class).getTestResult();
		if (testResult != null) {
			Label label;
			Text text;

			label = createLabel(composite, toolkit, "Passed:");
			GridDataFactory.defaultsFor(label).indent(0, 0).applyTo(label);
			text = createTextReadOnly(composite, toolkit, "");
			bind(text, IBuild.class, FeaturePath
					.fromList(Literals.BUILD__TEST_RESULT, Literals.TEST_RESULT__PASS_COUNT));

			label = createLabel(composite, toolkit, "Failed:");
			GridDataFactory.defaultsFor(label).indent(0, 0).applyTo(label);
			text = createTextReadOnly(composite, toolkit, "");
			bind(text, IBuild.class, FeaturePath
					.fromList(Literals.BUILD__TEST_RESULT, Literals.TEST_RESULT__FAIL_COUNT));

			label = createLabel(composite, toolkit, "Ignored:");
			GridDataFactory.defaultsFor(label).indent(0, 0).applyTo(label);
			text = createTextReadOnly(composite, toolkit, "");
			bind(text, IBuild.class, FeaturePath.fromList(Literals.BUILD__TEST_RESULT,
					Literals.TEST_RESULT__IGNORED_COUNT));
		}

		viewer = new TreeViewer(toolkit.createTree(composite, SWT.NONE));
		GridDataFactory.fillDefaults().hint(300, 100).span(6, 1).grab(true, true).applyTo(viewer.getControl());
		viewer.setContentProvider(new TestResultContentProvider());
		viewer.setLabelProvider(new DecoratingStyledCellLabelProvider(new TestResultLabelProvider(), null, null));
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				getPage().getSite().getSelectionProvider().setSelection(event.getSelection());
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				Object item = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (item instanceof ITestSuite) {
					TestResultManager.openInEditor((ITestSuite) item);
				} else if (item instanceof ITestCase) {
					TestResultManager.openInEditor((ITestCase) item);
				}
			}
		});

		testFailureFilter = new TestFailureFilter();

		menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		getPage().getEditorSite().registerContextMenu(ID_POPUP_MENU, menuManager, viewer, true);
		Menu menu = menuManager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		if (testResult != null) {
			viewer.setInput(testResult);
		} else {
			viewer.setInput("No test results generated.");
		}

		toolkit.paintBordersFor(composite);
		return composite;
	}

	@Override
	public void initialize(BuildEditorPage page) {
		super.initialize(page);

		showTestResultsAction = new ShowTestResultsAction();
		showTestResultsAction.selectionChanged(new StructuredSelection(getInput(IBuild.class)));

		filterTestFailuresAction = new FilterTestFailuresAction();
		showTestResultsAction.selectionChanged(new StructuredSelection(getInput(IBuild.class)));
	}

	@Override
	protected void fillToolBar(ToolBarManager toolBarManager) {
		super.fillToolBar(toolBarManager);

		toolBarManager.add(filterTestFailuresAction);
	}

}
