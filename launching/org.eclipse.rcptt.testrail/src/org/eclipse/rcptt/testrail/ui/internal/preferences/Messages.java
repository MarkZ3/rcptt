/*******************************************************************************
 * Copyright (c) 2009, 2016 Xored Software Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xored Software Inc - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.rcptt.testrail.ui.internal.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.rcptt.testrail.ui.internal.preferences.messages"; //$NON-NLS-1$
	public static String TestRailPreferencePage_EnableIntegration;
	public static String TestRailPreferencePage_Address;
	public static String TestRailPreferencePage_Username;
	public static String TestRailPreferencePage_Password;
	public static String TestRailPreferencePage_IncorrectAddressMsg;
	public static String TestRailPreferencePage_AddressEndsWithSlash;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
