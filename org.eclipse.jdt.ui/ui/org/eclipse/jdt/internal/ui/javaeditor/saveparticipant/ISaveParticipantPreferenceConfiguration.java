/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.preferences.IWorkingCopyManager;

/**
 * Preference UI to configure details of a save participant on the  the
 * Java &gt; Editor &gt; Save Participants preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.3
 */
public interface ISaveParticipantPreferenceConfiguration {

	/**
	 * Creates a control that will be displayed on the Java &gt; Editor &gt; Save Participants 
	 * preference page to edit the details of a save participant.
	 *
	 * @param parent the parent composite to which to add the preferences control
	 * @param manager the working copy manager used to manage the working copy of the preferences 
	 * @return the control that was added to the <code>parent</code>
	 */
	Control createControl(Composite parent, IWorkingCopyManager manager);

	/**
	 * Called after creating the control.
	 * <p>
	 * Implementations should load the preferences values and update the controls accordingly.
	 * </p>
	 * @param context the context from which to load the preference values from
	 */
	void initialize(IScopeContext context);

	/**
	 * Called when the <code>OK</code> button is pressed on the preference
	 * page.
	 * <p>
	 * Implementations should commit the configured preference settings
	 * into their form of preference storage.</p>
	 */
	void performOk();

	/**
	 * Called when the <code>Defaults</code> button is pressed on the
	 * preference page.
	 * <p>
	 * Implementation should reset any preference settings to
	 * their default values and adjust the controls accordingly.</p>
	 */
	void performDefaults();

	/**
	 * Called when the preference page is being disposed.
	 * <p>
	 * Implementations should free any resources they are holding on to.</p>
	 */
	void dispose();
	
	/**
	 * Called when project specific settings have been enabled
	 */
	void enableProjectSettings();
	
	/**
	 * Called when project specific settings have been disabled
	 */
	void disableProjectSettings();
}
