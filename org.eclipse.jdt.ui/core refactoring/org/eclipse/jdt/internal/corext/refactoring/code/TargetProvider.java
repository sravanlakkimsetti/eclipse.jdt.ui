/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

abstract class TargetProvider {

	public abstract void initialize();

	public abstract ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm)  throws JavaModelException;
	
	public abstract BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm);
	
	public abstract MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm);
	
	public abstract RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException;
	
	public abstract RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException;

	public static TargetProvider create(ICompilationUnit cu, MethodInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(ICompilationUnit cu, MethodDeclaration declaration) {
		ITypeBinding type= declaration.resolveBinding().getDeclaringClass();
		if (type.isLocal())
			return new LocalTypeTargetProvider(cu, declaration);
		else
			return new MemberTypeTargetProvider(cu, declaration);
	}

	static void checkFieldDeclaration(RefactoringStatus result, ICompilationUnit unit, MethodInvocation invocation, int severity) {
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(invocation, BodyDeclaration.class);
		if (decl instanceof FieldDeclaration) {
			result.addEntry(new RefactoringStatusEntry(
				RefactoringCoreMessages.getString("TargetProvider.field_initializer"), //$NON-NLS-1$
				severity, 
				JavaSourceContext.create(unit, invocation),
				null, RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER));
		}
	}
	
	static void fastDone(IProgressMonitor pm) {
		if (pm == null)
			return;
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.worked(1);
		pm.done();
	}
	
	static class SingleCallTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodInvocation fInvocation;
		private SourceProvider fSourceProvider;
		private boolean fIterated;
		public SingleCallTargetProvider(ICompilationUnit cu, MethodInvocation invocation) {
			Assert.isNotNull(cu);
			Assert.isNotNull(invocation);
			fCUnit= cu;
			fInvocation= invocation;
		}
		public void initialize() {
			fIterated= false;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			return new ICompilationUnit[] { fCUnit };
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			if (fIterated)
				return new BodyDeclaration[0];
			fastDone(pm);
			return new BodyDeclaration[] { 
				(BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class)
			};
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			fastDone(pm);
			if (fIterated)
				return null;
			fIterated= true;
			return new MethodInvocation[] { fInvocation };
		}
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			fastDone(pm);
			return new RefactoringStatus();
		}
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			RefactoringStatus result= new RefactoringStatus();
			checkFieldDeclaration(result, fCUnit, node, RefactoringStatus.FATAL);
			fastDone(pm);
			return result;
		}
	}

	private static class BodyData {
		public BodyDeclaration fBody;
		private List fInvocations;
		public BodyData(BodyDeclaration declaration) {
			fBody= declaration;
		}
		public void addInvocation(MethodInvocation node) {
			if (fInvocations == null)
				fInvocations= new ArrayList(2);
			fInvocations.add(node);
		}
		public MethodInvocation[] getInvocations() {
			return (MethodInvocation[])fInvocations.toArray(new MethodInvocation[fInvocations.size()]);
		}
		public boolean hasInvocations() {
			return fInvocations != null && !fInvocations.isEmpty();
		}
		public BodyDeclaration getDeclaration() {
			return fBody;
		}
	}

	private static class InvocationFinder extends ASTVisitor {
		Map result= new HashMap(2);
		Stack fBodies= new Stack();
		BodyData fCurrent;
		private IMethodBinding fBinding;
		public InvocationFinder(IMethodBinding binding) {
			Assert.isNotNull(binding);
			fBinding= binding;
		}
		public boolean visit(MethodInvocation node) {
			if (Bindings.equals(fBinding, node.getName().resolveBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= null;
			return true;
		}
		public void endVisit(TypeDeclaration node) {
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(FieldDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(FieldDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(MethodDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(MethodDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
			
		}
		public boolean visit(Initializer node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData(node);
			return true;
		}
		public void endVisit(Initializer node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
		}
	}
	
	private static class LocalTypeTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodDeclaration fDeclaration;
		private Map fBodies;
		public LocalTypeTargetProvider(ICompilationUnit unit, MethodDeclaration declaration) {
			Assert.isNotNull(unit);
			Assert.isNotNull(declaration);
			fCUnit= unit;
			fDeclaration= declaration;
		}
		public void initialize() {
			InvocationFinder finder= new InvocationFinder(fDeclaration.resolveBinding());
			ASTNode type= ASTNodes.getParent(fDeclaration, ASTNode.TYPE_DECLARATION);
			type.accept(finder);
			fBodies= finder.result;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			fastDone(pm);
			return new ICompilationUnit[] { fCUnit };
		}
	
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			Set result= fBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fBodies.get(declaration);
			Assert.isTrue(data != null);
			fastDone(pm);
			return data.getInvocations();
		}
	
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			fastDone(pm);
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			RefactoringStatus result= new RefactoringStatus();
			checkFieldDeclaration(result, fCUnit, node, RefactoringStatus.ERROR);
			fastDone(pm);
			return result;
		}
	}
	
	private static class MemberTypeTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodDeclaration fMethod;
		private Map fCurrentBodies;
		public MemberTypeTargetProvider(ICompilationUnit unit, MethodDeclaration method) {
			Assert.isNotNull(unit);
			Assert.isNotNull(method);
			fCUnit= unit;
			fMethod= method;
		}
		public void initialize() {
			// do nothing.
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm)  throws JavaModelException {
			IMethod method= Binding2JavaModel.find(fMethod.resolveBinding(), fCUnit.getJavaProject());
			Assert.isTrue(method != null);
			ICompilationUnit[] result= RefactoringSearchEngine.findAffectedCompilationUnits(	
				pm, RefactoringScopeFactory.create(method),
				SearchEngine.createSearchPattern(method, IJavaSearchConstants.REFERENCES));
			return result;
		}
	
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			ASTNode root= AST.parseCompilationUnit(unit, true);
			InvocationFinder finder= new InvocationFinder(fMethod.resolveBinding());
			root.accept(finder);
			fCurrentBodies= finder.result;
			Set result= fCurrentBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fCurrentBodies.get(declaration);
			Assert.isTrue(data != null);
			fastDone(pm);
			return data.getInvocations();
		}
	
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			fastDone(pm);
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			RefactoringStatus result= new RefactoringStatus();
			checkFieldDeclaration(result, fCUnit, node, RefactoringStatus.ERROR);
			fastDone(pm);
			return result;
		}
	}
}
