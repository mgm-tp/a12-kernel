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

package com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams;

import java.util.Set;

/**
 * Interface specifies the mandatory information.
 * Names specify either
 * <ul>
 * <li>full path names of fields that are mandatory without any restrictions</li>
 * <li>full path names of fields that are mandatory, if any field of their root group is set</li>
 * <li>names of root groups that are mandatory.</li>
 * </ul>
 */

public interface IMandatoryInformation {

	/**
	 * @return full names of all mandatory fields.
	 */
	public Set<String> getMandatory();

	/**
	 * @return full names of all fields, which are mandatory only if
	 * 		any field of their root group is set.
	 */
	public Set<String> getMandatoryForRootGroup();

	/**
	 * @return the names of all mandatory root groups (only the names, i.e. without the leading
	 * 		slash).
	 */
	public Set<String> getMandatoryRootGroups();
}
