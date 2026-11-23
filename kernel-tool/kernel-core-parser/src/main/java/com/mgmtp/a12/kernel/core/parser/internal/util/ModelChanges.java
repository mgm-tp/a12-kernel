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

package com.mgmtp.a12.kernel.core.parser.internal.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.ado.serviceparams.IExtmpModelChanges;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IRule;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IEntityChangeSet;

/**
 * See {@link IExtmpModelChanges}
 */
public class ModelChanges implements IExtmpModelChanges {

	private Set<IRule> rulesModellingMaxValueRestrictions = new HashSet<>();
	private Map<IEntity, EntityChangeSet> entityChangeSets = new HashMap<>();
	private Long newMaxUsb;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelChanges#
	 * getRulesModellingMaxValueRestrictions(
	 * )
	 */
	@Override
	public Set<IRule> getRulesModellingMaxValueRestrictions() {
		return rulesModellingMaxValueRestrictions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IModelChanges#
	 * getEntityChangeSets()
	 */
	@Override
	public Set<IEntityChangeSet> getEntityChangeSets() {
		return new HashSet<>(entityChangeSets.values());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mgmtp.a12.mvk.extmpspecifics.api.ado.serviceparams.IExtmpModelChanges#getNewMaxUsb()
	 */
	@Override
	public Optional<Long> getNewMaxNumberOfUsb() {
		return Optional.ofNullable(newMaxUsb);
	}

	public void addRuleModellingMaxValueRestrictions(IRule rule) {
		rulesModellingMaxValueRestrictions.add(rule);
	}

	public void setNewMaxValue(IEntity entity, int index, long newMaxValue) {
		EntityChangeSet changeSet = getChangeSet(entity);
		changeSet.setNewMaxValue(index, newMaxValue);
	}

	public void setMaxUsb(long newMaxUsb) {
		this.newMaxUsb = newMaxUsb;
	}

	private EntityChangeSet getChangeSet(IEntity entity) {
		return entityChangeSets.computeIfAbsent(entity, EntityChangeSet::new);
	}
}
