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

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;

/**
 * The interface provides an access to all changes to the model:
 * An instance of ModelChanges specifies
 * <ul>
 * <li>the rules describing the changes and
 * <li>the change set for the entities.
 * </ul>
 * An instance of ModelChanges can be used to apply the changes to the
 * {@link IEC#applyChanges(Set)}.
 */
public interface IModelChanges {

	/**
	 * @return a list of rules with error condition text, that models a restriction of a max value
	 * 		of an entity. Returns an empty list, if {@link #getEntityChangeSets()} returns an
	 * 		empty list. Never returns null.
	 */
	public Set<IRule> getRulesModellingMaxValueRestrictions();

	/**
	 * @return a list of changes for entities. The list is empty, if
	 *        {@link #getRulesModellingMaxValueRestrictions()} returns an empty list.
	 */
	public Set<IEntityChangeSet> getEntityChangeSets();

}
