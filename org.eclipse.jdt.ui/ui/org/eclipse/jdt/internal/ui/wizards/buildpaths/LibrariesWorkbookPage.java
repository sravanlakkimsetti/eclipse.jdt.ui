/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class LibrariesWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	
	private ListDialogField fLibrariesList;
	private IWorkspaceRoot fWorkspaceRoot;
	
	private IDialogSettings fDialogSettings;
	
	private Control fSWTControl;
		
	public LibrariesWorkbookPage(IWorkspaceRoot root, ListDialogField classPathList) {
		fClassPathList= classPathList;
		fWorkspaceRoot= root;
		fSWTControl= null;
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		String[] buttonLabels= new String[] { 
			/* 0 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addnew.button"),	//$NON-NLS-1$
			/* 1 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addexisting.button"), //$NON-NLS-1$
			/* 2 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addjar.button"),	//$NON-NLS-1$
			/* 3 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addextjar.button"), //$NON-NLS-1$
			/* 4 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.addvariable.button"), //$NON-NLS-1$
			/* 5 */ null,  
			/* 6 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.setsource.button"), //$NON-NLS-1$
			/* 7 */ null,  
			/* 8 */ NewWizardMessages.getString("LibrariesWorkbookPage.libraries.remove.button") //$NON-NLS-1$
		};		
				
		LibrariesAdapter adapter= new LibrariesAdapter();
				
		fLibrariesList= new ListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fLibrariesList.setDialogFieldListener(adapter);
		fLibrariesList.setLabelText(NewWizardMessages.getString("LibrariesWorkbookPage.libraries.label")); //$NON-NLS-1$
		fLibrariesList.setRemoveButtonIndex(8); //$NON-NLS-1$
	
		fLibrariesList.enableButton(6, false);
		
		fLibrariesList.setViewerSorter(new CPListElementSorter());

	}
		
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		updateLibrariesList();
	}
	
	private void updateLibrariesList() {
		List cpelements= fClassPathList.getElements();
		List libelements= new ArrayList(cpelements.size());
		
		int nElements= cpelements.size();
		for (int i= 0; i < nElements; i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE) {
				libelements.add(cpe);
			}
		}
		fLibrariesList.setElements(libelements);
	}		
		
	// -------- ui creation
	
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibrariesList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fLibrariesList.getListControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fLibrariesList.setButtonsMinWidth(buttonBarWidth);
		
		fLibrariesList.getTableViewer().setSorter(new CPListElementSorter());
		
		fSWTControl= composite;
				
		return composite;
	}
	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	
	private class LibrariesAdapter implements IDialogFieldListener, IListAdapter {
		
		// -------- IListAdapter --------
		public void customButtonPressed(DialogField field, int index) {
			libaryPageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(DialogField field) {
			libaryPageSelectionChanged(field);
		}
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			libaryPageDialogFieldChanged(field);
		}
	}
	
	private void libaryPageCustomButtonPressed(DialogField field, int index) {
		CPListElement[] libentries= null;
		switch (index) {
		case 0: /* add new */
			libentries= createNewClassContainer();
			break;
		case 1: /* add existing */
			libentries= chooseClassContainers();
			break;
		case 2: /* add jar */
			libentries= chooseJarFiles();
			break;
		case 3: /* add external jar */
			libentries= chooseExtJarFiles();
			break;
		case 4: /* add variable */
			libentries= chooseVariableEntries();
			break;				
		case 6: /* set source attachment */
			List selElements= fLibrariesList.getSelectedElements();
			CPListElement selElement= (CPListElement) selElements.get(0);				
			SourceAttachmentDialog dialog= new SourceAttachmentDialog(getShell(), selElement.getClasspathEntry());
			if (dialog.open() == dialog.OK) {
				selElement.setSourceAttachment(dialog.getSourceAttachmentPath(), dialog.getSourceAttachmentRootPath());
				fLibrariesList.refresh();
				fClassPathList.refresh();
			}
			break;
		}
		if (libentries != null) {
			int nElementsChosen= libentries.length;					
			// remove duplicates
			List cplist= fLibrariesList.getElements();
			List elementsToAdd= new ArrayList(nElementsChosen);
			
			for (int i= 0; i < nElementsChosen; i++) {
				CPListElement curr= libentries[i];
				if (!cplist.contains(curr) && !elementsToAdd.contains(curr)) {
					elementsToAdd.add(curr);
					addAttachmentsFromExistingLibs(curr);
				}
			}
			fLibrariesList.addElements(elementsToAdd);
			fLibrariesList.postSetSelection(new StructuredSelection(libentries));
		}
	}
	
	private void libaryPageSelectionChanged(DialogField field) {
		List selElements= fLibrariesList.getSelectedElements();
		fLibrariesList.enableButton(6, canDoSourceAttachment(selElements));
	}
	
	private void libaryPageDialogFieldChanged(DialogField field) {
		if (fCurrJProject != null) {
			// already initialized
			updateClasspathList();
		}
	}	
	
	private boolean canDoSourceAttachment(List selElements) {
		if (selElements != null && selElements.size() == 1) {
			CPListElement elem= (CPListElement) selElements.get(0);
			return (!(elem.getResource() instanceof IFolder));
		}
		return false;
	}		
	
	private void updateClasspathList() {
		List projelements= fLibrariesList.getElements();
		
		boolean remove= false;
		List cpelements= fClassPathList.getElements();
		// backwards, as entries will be deleted
		for (int i= cpelements.size() - 1; i >= 0; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					remove= true;
				}	
			}
		}
		for (int i= 0; i < projelements.size(); i++) {
			cpelements.add(projelements.get(i));
		}
		if (remove || (projelements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}
	
		
	private CPListElement[] createNewClassContainer() {
		String title= NewWizardMessages.getString("LibrariesWorkbookPage.NewClassFolderDialog.title"); //$NON-NLS-1$
		IProject currProject= fCurrJProject.getProject();
		
		NewContainerDialog dialog= new NewContainerDialog(getShell(), title, currProject, getUsedContainers());
		IPath projpath= currProject.getFullPath();
		dialog.setMessage(NewWizardMessages.getFormattedString("LibrariesWorkbookPage.NewClassFolderDialog.description", projpath.toString())); //$NON-NLS-1$
		int ret= dialog.open();
		if (ret == dialog.OK) {
			IFolder folder= dialog.getFolder();
			return new CPListElement[] { newCPLibraryElement(folder) };
		}
		return null;
	}
			
			
	private CPListElement[] chooseClassContainers() {	
		Class[] acceptedClasses= new Class[] { IFolder.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
			
		acceptedClasses= new Class[] { IProject.class, IFolder.class };

		ViewerFilter filter= new TypedViewerFilter(acceptedClasses, getUsedContainers());	
			
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("LibrariesWorkbookPage.ExistingClassFolderDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		dialog.setInitialSelection(fCurrJProject.getProject());
		
		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
			CPListElement[] res= new CPListElement[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource) elements[i];
				res[i]= newCPLibraryElement(elem);
			}
			return res;
		}
		return null;		
	}
	
	private CPListElement[] chooseJarFiles() {
		Class[] acceptedClasses= new Class[] { IFile.class };
		TypedElementSelectionValidator validator= new TypedElementSelectionValidator(acceptedClasses, true);
		ViewerFilter filter= new ArchiveFileFilter(getUsedJARFiles());
		
		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("LibrariesWorkbookPage.JARArchiveDialog.description")); //$NON-NLS-1$
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		dialog.setInitialSelection(fCurrJProject.getProject());		

		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
			CPListElement[] res= new CPListElement[elements.length];
			for (int i= 0; i < res.length; i++) {
				IResource elem= (IResource)elements[i];
				res[i]= newCPLibraryElement(elem);
			}
			return res;
		}
		return null;
	}
	
	private IContainer[] getUsedContainers() {
		ArrayList res= new ArrayList();
		if (fCurrJProject.exists()) {
			try {
				IPath outputLocation= fCurrJProject.getOutputLocation();
				if (outputLocation != null) {
					IResource resource= fWorkspaceRoot.findMember(outputLocation);
					if (resource instanceof IFolder) { // != Project
						res.add(resource);
					}
				}
			} catch (JavaModelException e) {
				// ignore it here, just log
				JavaPlugin.log(e.getStatus());
			}
		}	
			
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			IResource resource= elem.getResource();
			if (resource instanceof IContainer) {
				res.add(resource);
			}
		}
		return (IContainer[]) res.toArray(new IContainer[res.size()]);
	}
	
	private IFile[] getUsedJARFiles() {
		List res= new ArrayList();
		List cplist= fLibrariesList.getElements();
		for (int i= 0; i < cplist.size(); i++) {
			CPListElement elem= (CPListElement)cplist.get(i);
			IResource resource= elem.getResource();
			if (resource instanceof IFile) {
				res.add(resource);
			}
		}
		return (IFile[]) res.toArray(new IFile[res.size()]);
	}	
	
	private CPListElement newCPLibraryElement(IResource res) {
		return new CPListElement(IClasspathEntry.CPE_LIBRARY, res.getFullPath(), res);
	};

	
	private CPListElement[] chooseExtJarFiles() {
		String lastUsedPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
		if (lastUsedPath == null) {
			lastUsedPath= ""; //$NON-NLS-1$
		}
		FileDialog dialog= new FileDialog(getShell(), SWT.MULTI);
		dialog.setText(NewWizardMessages.getString("LibrariesWorkbookPage.ExtJARArchiveDialog.title")); //$NON-NLS-1$		
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(lastUsedPath);
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= new Path(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[nChosen];
		for (int i= 0; i < nChosen; i++) {
			IPath path= filterPath.append(fileNames[i]).makeAbsolute();	
			elems[i]= new CPListElement(IClasspathEntry.CPE_LIBRARY, path, null);
		}
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, filterPath.toOSString());
		
		return elems;
	}
	
	private CPListElement[] chooseVariableEntries() {
		NewVariableEntryDialog dialog= new NewVariableEntryDialog(getShell(), null);
		if (dialog.open() == dialog.OK) {
			List existingElements= fLibrariesList.getElements();
			
			IPath[] paths= dialog.getResult();
			ArrayList result= new ArrayList();
			for (int i = 0; i < paths.length; i++) {
				CPListElement elem= new CPListElement(IClasspathEntry.CPE_VARIABLE, paths[i], null);
				IPath resolvedPath= JavaCore.getResolvedVariablePath(paths[i]);
				elem.setIsMissing((resolvedPath == null) || !resolvedPath.toFile().exists());
				if (!existingElements.contains(elem)) {
					result.add(elem);
				}
			}
			return (CPListElement[]) result.toArray(new CPListElement[result.size()]);
		}
		return null;
	}
	
	
	private void addAttachmentsFromExistingLibs(CPListElement elem) {
		try {
			IJavaModel jmodel= fCurrJProject.getJavaModel();
			IJavaProject[] jprojects= jmodel.getJavaProjects();
			for (int i= 0; i < jprojects.length; i++) {
				IJavaProject curr= jprojects[i];
				if (!curr.equals(fCurrJProject)) {
					IClasspathEntry[] entries= curr.getRawClasspath();
					for (int k= 0; k < entries.length; k++) {
						IClasspathEntry entry= entries[k];
						if (entry.getEntryKind() == elem.getEntryKind()
							&& entry.getPath().equals(elem.getPath())) {
							IPath attachPath= entry.getSourceAttachmentPath();
							if (attachPath != null && !attachPath.isEmpty()) {
								elem.setSourceAttachment(attachPath, entry.getSourceAttachmentRootPath());
								return;
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
	}		
					
	// a dialog to set the source attachment properties
	private class SourceAttachmentDialog extends StatusDialog implements IStatusChangeListener {
		
		private SourceAttachmentBlock fSourceAttachmentBlock;
				
		public SourceAttachmentDialog(Shell parent, IClasspathEntry entry) {
			super(parent);
			setTitle(NewWizardMessages.getFormattedString("LibrariesWorkbookPage.SourceAttachmentDialog.title", entry.getPath().toString())); //$NON-NLS-1$
			fSourceAttachmentBlock= new SourceAttachmentBlock(fWorkspaceRoot, this, entry);
		}
		
		/*
		 * @see Windows#configureShell
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.SOURCE_ATTACHMENT_DIALOG);
		}		
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);
						
			Control inner= fSourceAttachmentBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
		}
		
		public IPath getSourceAttachmentPath() {
			return fSourceAttachmentBlock.getSourceAttachmentPath();
		}
		
		public IPath getSourceAttachmentRootPath() {
			return fSourceAttachmentBlock.getSourceAttachmentRootPath();
		}
				
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fLibrariesList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		for (int i= selElements.size()-1; i >= 0; i--) {
			CPListElement curr= (CPListElement) selElements.get(i);
			int kind= curr.getEntryKind();
			if (kind != IClasspathEntry.CPE_LIBRARY && kind != IClasspathEntry.CPE_VARIABLE) {
				selElements.remove(i);
			}
		}
		fLibrariesList.selectElements(new StructuredSelection(selElements));
	}	


}