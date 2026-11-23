/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */

package com.mgmtp.a12.kernel.core.service.internal.serviceparams;

import java.util.Collections;
import java.util.Set;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation;

/**
 * See {@link IMandatoryInformation}
 */
public class MandatoryInformation implements IMandatoryInformation {

	private final Set<String> mandatory;
	private final Set<String> mandatoryForRootGroup;
	private final Set<String> mandatoryRootGroups;

	/**
	 * Creates an empty mandatory information.
	 */
	public MandatoryInformation() {
		this.mandatory = Collections.emptySet();
		this.mandatoryForRootGroup = Collections.emptySet();
		this.mandatoryRootGroups = Collections.emptySet();
	}

	/**
	 * Creates a mandatory information containing the provided mandatory fields and root groups.
	 */
	public MandatoryInformation(Set<String> mandatory, Set<String> mandatoryForRootGroup, Set<String> mandatoryRootGroups) {
		this.mandatory = Collections.unmodifiableSet(mandatory);
		this.mandatoryForRootGroup = Collections.unmodifiableSet(mandatoryForRootGroup);
		this.mandatoryRootGroups = Collections.unmodifiableSet(mandatoryRootGroups);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation#
	 * getMandatory()
	 */
	@Override
	public Set<String> getMandatory() {
		return mandatory;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation#
	 * getMandatoryForRootGroup()
	 */
	@Override
	public Set<String> getMandatoryForRootGroup() {
		return mandatoryForRootGroup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMandatoryInformation#
	 * getMandatoryRootGroups()
	 */
	@Override
	public Set<String> getMandatoryRootGroups() {
		return mandatoryRootGroups;
	}
}
