package org.eclipse.php.ui.wizards;

import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.ui.util.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractUserLibraryWizard extends Wizard {

	public abstract void init(IBuildpathEntry initialEntry,
			IScriptProject project, IBuildpathEntry[] currentBuildpath);

	public abstract void init(IScriptProject project,
			IBuildpathEntry[] currentBuildpath);

	public IBuildpathEntry[] getNewEntries(Shell shell) {
		WizardDialog dialog = new WizardDialog(shell, this);
		PixelConverter converter = new PixelConverter(shell);
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70),
				converter.convertHeightInCharsToPixels(20));
		dialog.create();
		if (dialog.open() == Window.OK) {
			return getNewEntries();
		}
		return null;
	}

	protected abstract IBuildpathEntry[] getNewEntries();

}