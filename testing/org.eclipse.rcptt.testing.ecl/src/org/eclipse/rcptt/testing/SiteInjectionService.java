/*******************************************************************************
 * Copyright (c) 2009, 2014 Xored Software Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xored Software Inc - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.rcptt.testing;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.rcptt.ecl.runtime.SingleCommandService;
import org.eclipse.rcptt.launching.injection.InjectionFactory;
import org.eclipse.rcptt.launching.injection.UpdateSite;
import org.eclipse.rcptt.testing.commands.SiteInjection;

public class SiteInjectionService extends SingleCommandService<SiteInjection> {

	public SiteInjectionService() {
		super(SiteInjection.class);
	}

	@Override
	protected Object serviceTyped(SiteInjection command) throws InterruptedException, CoreException {
		final UpdateSite rv = InjectionFactory.eINSTANCE.createUpdateSite();
		rv.setUri(command.getUri());
		if (!command.getUnit().isEmpty()) {
			rv.setAllUnits(false);
			rv.getUnits().addAll(command.getUnit());
		} else {
			rv.setAllUnits(true);
		}

		return rv;
	}

}
