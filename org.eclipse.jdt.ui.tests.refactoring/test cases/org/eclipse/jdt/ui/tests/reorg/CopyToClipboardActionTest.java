/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.ui.reorg.MockWorkbenchSite;

import org.eclipse.jdt.internal.corext.refactoring.reorg2.CopyToClipboardAction;


public class CopyToClipboardActionTest extends RefactoringTest{

	private ILabelProvider fLabelProvider;

	private static final Class clazz= CopyToClipboardActionTest.class;

	private Clipboard fClipboard;
	
	private ICompilationUnit fCuA;
	private ICompilationUnit fCuB;	
	private IPackageFragment fPackageQ;
	private IPackageFragment fPackageQ_R;
	private IPackageFragment fDefaultPackage;
	private static final String CU_A_NAME= "A";
	private static final String CU_B_NAME= "B";
	private IFile faTxt;
	private IFolder fOlder;
	
	public CopyToClipboardActionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected void setUp() throws Exception {
		super.setUp();
		fClipboard= new Clipboard(Display.getDefault());
		fDefaultPackage= MySetup.getDefaultSourceFolder().createPackageFragment("", true, null);
		
		fCuA= createCU(getPackageP(), CU_A_NAME + ".java", 
			"package p;" +			"import java.util.List;" +			"class A{" +			"int f;" +			"{}" +			"void foo(){}" +			"class Inner{}" +			"}");
		
		fPackageQ= MySetup.getDefaultSourceFolder().createPackageFragment("q", true, null);
		fCuB= createCU(fPackageQ, CU_B_NAME + ".java", 
				"package q;" +				"import java.util.Set;" +
				"class B{" +				"int x;" +				"void bar(){}" +				"class InnerB{}" +				"}");
		
		fPackageQ_R= MySetup.getDefaultSourceFolder().createPackageFragment("q.r", true, null);
		
		faTxt= createFile((IFolder)getPackageP().getUnderlyingResource(), "a.txt");
		fOlder= createFolder(MySetup.getProject().getProject(), "fOlder");
		
		fLabelProvider= new JavaElementLabelProvider(	JavaElementLabelProvider.SHOW_VARIABLE + 
														JavaElementLabelProvider.SHOW_PARAMETERS + 
														JavaElementLabelProvider.SHOW_TYPE);
		assertTrue("A.java does not exist", fCuA.exists());
		assertTrue("B.java does not exist", fCuB.exists());
		assertTrue("q does not exist", fPackageQ.exists());
		assertTrue("q.r does not exist", fPackageQ_R.exists());
		assertTrue("a.txt does not exist", faTxt.exists());
		assertTrue("fOlder does not exist", fOlder.exists());
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		fClipboard.dispose();
		fLabelProvider.dispose();
		delete(fCuA);
		delete(fCuB);
		delete(fPackageQ_R);
		delete(fPackageQ);
		delete(faTxt);
		delete(fOlder);
	}

	private IFile createFile(IFolder folder, String fileName) throws Exception {
		IFile file= folder.getFile(fileName);
		file.create(getStream("aa"), true, null);	
		return file;
	}
	
	private IFolder createFolder(IProject project, String name) throws CoreException{
		IFolder folder= project.getFolder(name);
		folder.create(true, true, null);
		return folder;
	}
	
	private static void delete(ISourceManipulation element) {
		try {
			if (element != null && ((IJavaElement)element).exists())
				element.delete(true, null);
		} catch(JavaModelException e) {
			//ignore, we must keep going
		}		
	}
	private static void delete(IFile element) {
		try {
			element.delete(true, false, null);
		} catch(CoreException e) {
			//ignore, we must keep going
		}
	}

	private static void delete(IFolder element) {
		try {
			element.delete(true, false, null);
		} catch(CoreException e) {
			//ignore, we must keep going
		}
	}
	
	private void checkEnabled(Object[] elements){
		CopyToClipboardAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard, null);
		copyAction.update(copyAction.getSelection());
		assertTrue("action should be enabled", copyAction.isEnabled());
		copyAction.run();
		checkClipboard(elements);
	}

	private void checkDisabled(Object[] elements){
		CopyToClipboardAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard, null);
		copyAction.update(copyAction.getSelection());
		assertTrue("action should be disabled", ! copyAction.isEnabled());
	}

	private void checkClipboard(Object[] elementsCopied) {
		String[] clipboardFiles= getClipboardFiles();
		IResource[] clipboardResources= getClipboardResources();
		String clipboardText= getClipboardText();
		IJavaElement[] clipboardJavaElements= getClipboardJavaElements();
	
		IResource[] resourcesCopied= getResources(elementsCopied);
		IJavaElement[] javaElementsCopied= getJavaElements(elementsCopied);
		
		assertEquals("names", computeExpectedNames(resourcesCopied, javaElementsCopied), clipboardText);
		checkFiles(resourcesCopied, javaElementsCopied, clipboardFiles);
		checkElements(resourcesCopied, clipboardResources);
//		checkElements(javaElementsCopied, clipboardJavaElements);
	}
	
	private String computeExpectedNames(IResource[] resourcesCopied, IJavaElement[] javaElementsCopied) {
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < resourcesCopied.length; i++) {
			if (i>0)
				buff.append('\n');
			buff.append(getName(resourcesCopied[i]));
		}
		for (int i= 0; i < javaElementsCopied.length; i++) {
			if (resourcesCopied.length > 0 || i > 0)
				buff.append('\n');
			buff.append(getName(javaElementsCopied[i]));
		}
		return buff.toString();
	}

	private String getName(IResource resource){
		return fLabelProvider.getText(resource);
	}
	private String getName(IJavaElement javaElement){
		return fLabelProvider.getText(javaElement);
	}

	private static void checkElements(Object[] copied, Object[] retreivedFromClipboard) {
		assertEquals("different number of elements", copied.length, retreivedFromClipboard.length);
		sortByHashCode(copied);
		sortByHashCode(retreivedFromClipboard);
		for (int i= 0; i < retreivedFromClipboard.length; i++) {
			assertTrue("different: copied " + getName(copied[i]) + " retreived: " + getName(retreivedFromClipboard[i]) , copied[i].equals(retreivedFromClipboard[i]));
		}
	}

	private static String getName(Object object) {
		if (object instanceof IJavaElement)
			return ((IJavaElement)object).getElementName();
		if (object instanceof IResource)
			return ((IResource)object).getName();
		return object == null ? null : object.toString();
	}

	private static void sortByHashCode(Object[] copied) {
		Arrays.sort(copied, new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return arg0.hashCode() - arg1.hashCode();
			}
		});
	}

	private static void checkFiles(IResource[] resourcesCopied, IJavaElement[] javaElementsCopied, String[] clipboardFiles) {
		int expected= 0;
		expected += resourcesCopied.length;
		expected += countResources(javaElementsCopied);
		assertEquals("different number of files in clipboard", expected, clipboardFiles.length);
	}

	private static int countResources(IJavaElement[] javaElementsCopied) {
		int count= 0;
		for (int i= 0; i < javaElementsCopied.length; i++) {
			IJavaElement element= javaElementsCopied[i];
			if (element instanceof ICompilationUnit || null == element.getAncestor(IJavaElement.COMPILATION_UNIT))
				count++;
		}
		return count;
	}

	private static IResource[] getResources(Object[] elements) {
		List resources= new ArrayList(elements.length);
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			if (element instanceof IResource)
				resources.add(element);			
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	private static IJavaElement[] getJavaElements(Object[] elements) {
		List resources= new ArrayList(elements.length);
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			if (element instanceof IJavaElement)
				resources.add(element);			
		}
		return (IJavaElement[]) resources.toArray(new IJavaElement[resources.size()]);
	}

	private IJavaElement[] getClipboardJavaElements() {
		//TODO implement me
		return new IJavaElement[0];
	}

	private String[] getClipboardFiles() {
		String[] files= (String[])fClipboard.getContents(FileTransfer.getInstance());
		return files == null ? new String[0]: files;
	}
	
	private IResource[] getClipboardResources() {
		IResource[] resources= (IResource[])fClipboard.getContents(ResourceTransfer.getInstance());
		return resources == null ? new IResource[0]: resources; 
	}

	private String getClipboardText() {
		return (String)fClipboard.getContents(TextTransfer.getInstance());
	}
	
	///---------tests

	public void testDisabled0() {
		Object[] elements= {};
		checkDisabled(elements);
	}	

	public void testDisabled1() throws Exception {
		Object[] elements= {null};
		checkDisabled(elements);
	}	

	public void testDisabled2() throws Exception {
		Object[] elements= {this};
		checkDisabled(elements);
	}	

	public void testDisabled3() throws Exception {
		Object[] elements= {MySetup.getProject(), getPackageP()};
		checkDisabled(elements);
	}	

	public void testDisabled4() throws Exception{
		checkDisabled(new Object[]{getPackageP(), fCuA});
	}

	public void testDisabled5() throws Exception{
		checkDisabled(new Object[]{getRoot(), fCuA});
	}

	public void testDisabled6() throws Exception{
		checkDisabled(new Object[]{getRoot(), fPackageQ});
	}

	public void testDisabled7() throws Exception{
		checkDisabled(new Object[]{getRoot(), faTxt});
	}

	public void testDisabled8() throws Exception{
		checkDisabled(new Object[]{getRoot(), getRoot().getJavaProject()});
	}

	public void testDisabled9() throws Exception{
		checkDisabled(new Object[]{MySetup.getProject().getPackageFragmentRoots()});
	}

	public void testDisabled10() throws Exception{
		checkDisabled(new Object[]{fCuA, fCuB});
	}
	
	public void testDisabled11() throws Exception{
		checkDisabled(new Object[]{fDefaultPackage});
	}

	public void testDisabled12() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), fCuA});
	}

	public void testDisabled13() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), fPackageQ});
	}

	public void testDisabled14() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), faTxt});
	}

	public void testDisabled15() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object classA= fCuA.getType("A");
		Object[] elements= {fieldF, classA};
		checkDisabled(elements);
	}	

	public void testDisabled16() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fCuA};
		checkDisabled(elements);
	}	

	public void testDisabled17() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fDefaultPackage};
		checkDisabled(elements);
	}	

	public void testDisabled18() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, fPackageQ};
		checkDisabled(elements);
	}	

	public void testDisabled19() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, faTxt};
		checkDisabled(elements);
	}	

	public void testDisabled20() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, getRoot()};
		checkDisabled(elements);
	}	

	public void testDisabled21() throws Exception {
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF, MySetup.getProject()};
		checkDisabled(elements);
	}	

	public void testDisabled22() throws Exception {
		Object typeA= fCuA.getType("A");
		Object typeB= fCuB.getType("B");
		Object[] elements= {typeA, typeB};
		checkDisabled(elements);
	}
	
	public void testDisabled23() throws Exception{
		checkDisabled(new Object[]{faTxt, fCuB});
	}

	public void testEnabled0() throws Exception {
		Object[] elements= {MySetup.getProject()};
		checkEnabled(elements);
	}	

	public void testEnabled1() throws Exception {
		Object[] elements= {getPackageP()};
		checkEnabled(elements);
	}	

	public void testEnabled2() throws Exception {
		Object[] elements= {getRoot()};
		checkEnabled(elements);
	}	

	public void testEnabled3() throws Exception {
		Object[] elements= {MySetup.getDefaultSourceFolder()};
		checkEnabled(elements);
	}
	
	public void testEnabled4() throws Exception{
		checkEnabled(new Object[]{faTxt});
	}

	public void testEnabled5() throws Exception{
		checkEnabled(new Object[]{getRoot()});
	}

	public void testEnabled6() throws Exception{
		checkEnabled(new Object[]{fCuA});
	}

	public void testEnabled7() throws Exception{
		checkEnabled(new Object[]{getRoot().getJavaProject()});
	}
		
	public void testEnabled8() throws Exception{
		checkEnabled(new Object[]{getPackageP()});
	}
	
	public void testEnabled9() throws Exception{
		checkEnabled(new Object[]{getPackageP(), fPackageQ, fPackageQ_R});
	}
	
	public void testEnabled10() throws Exception{
		Object packDecl= fCuA.getPackageDeclarations()[0];
		Object[] elements= {packDecl};
		checkEnabled(elements);
	}

	public void testEnabled11() throws Exception{
		Object importD= fCuA.getImports()[0];
		Object[] elements= {importD};
		checkEnabled(elements);
	}

	public void testEnabled12() throws Exception{
		Object importContainer= fCuA.getImportContainer();
		Object[] elements= {importContainer};
		checkEnabled(elements);
	}

	public void testEnabled13() throws Exception{
		Object classA= fCuA.getType("A");
		Object[] elements= {classA};
		checkEnabled(elements);
	}

	public void testEnabled14() throws Exception{
		Object methodFoo= fCuA.getType("A").getMethod("foo", new String[0]);
		Object[] elements= {methodFoo};
		checkEnabled(elements);
	}

	public void testEnabled15() throws Exception{
		Object fieldF= fCuA.getType("A").getField("f");
		Object[] elements= {fieldF};
		checkEnabled(elements);
	}

	public void testEnabled16() throws Exception{
		Object initializer= fCuA.getType("A").getInitializer(1);
		Object[] elements= {initializer};
		checkEnabled(elements);
	}

	public void testEnabled17() throws Exception{
		Object innerClass= fCuA.getType("A").getType("Inner");
		Object[] elements= {innerClass};
		checkEnabled(elements);
	}

	public void testEnabled18() throws Exception{
		Object fieldF= fCuA.getType("A").getField("f");
		Object methodFoo= fCuA.getType("A").getMethod("foo", new String[0]);
		Object innerClass= fCuA.getType("A").getType("Inner");
		Object initializer= fCuA.getType("A").getInitializer(1);
		Object[] elements= {fieldF, methodFoo, initializer, innerClass};
		checkEnabled(elements);
	}
	
	public void testEnabled19() throws Exception{
		Object classA= fCuA.getType("A");
		Object importContainer= fCuA.getImportContainer();
		Object packDecl= fCuA.getPackageDeclarations()[0];
		Object[] elements= {classA, importContainer, packDecl};
		checkEnabled(elements);
	}

	public void testEnabled20() throws Exception{
		checkEnabled(new Object[]{faTxt, fCuA});
	}

	public void testEnabled21() throws Exception{
		checkEnabled(new Object[]{fOlder});
	}
}