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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.vba.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.RuleInfo;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;

/**
 * Wrapper around another implementation of {@link IEC} which filters out fields and groups which
 * are not contained in the given sets.
 */
public class VBAEC implements IEC {

	protected IEC ec;

	private Map<String, List<IField>> containedFieldsCache;

	private Set<String> fields;
	private Set<String> relevantGroups;

	private final Set<String> rulesGroups;

	public VBAEC(IEC ec, Set<String> relevantGroups, Set<String> fields, List<RuleInfo> rules) {
		this.ec = ec;
		this.relevantGroups = relevantGroups;
		this.fields = fields;
		this.rulesGroups = rules.stream().map(r -> EntityUtils.getParentPath(r.getFullName())).collect(Collectors.toSet());

		this.containedFieldsCache = new HashMap<>();
	}

	@Override
	public IEntity get(String fullName) {
		IEntity ret = ec.get(fullName);
		if (ret instanceof IField) {
			if (fields.contains(((IField) ret).getFullName())) {
				return ret;
			}
			return null;
		}

		return ret;
	}

	@Override
	public List<IField> getAllContainedFields(String pathName) {
		return containedFieldsCache.computeIfAbsent(pathName,
				k -> ec.getAllContainedFields(k).stream().filter(s -> fields.contains(s.getFullName()))
						.collect(Collectors.toList()));
	}

	@Override
	public List<IEntity> getDirectlyContainedEntities(String pathName) {
		List<IEntity> entities = ec.getDirectlyContainedEntities(pathName);
		List<IEntity> ret = new ArrayList<>();
		for (IEntity e : entities) {
			if (e.isField() && fields.contains(e.getFullName())) {
				ret.add(e);
				continue;
			}
			// Filter groups s.t. only relevant groups remain.
			if (e.isGroup()) {
				for (String group : relevantGroups) {
					if ((e.getFullName().startsWith(group) || group.startsWith(e.getFullName())) &&
							(!getAllContainedFields(e.getFullName()).isEmpty() || ruleGroupsContains(e.getFullName()))) {
						ret.add(e);
						break;
					}
				}
			}
		}
		return ret;
	}

	private boolean ruleGroupsContains(String fullName) {
		return rulesGroups.stream().anyMatch(rg -> EntityUtils.isDescendantOrEqual(fullName, rg));
	}

	@Override
	public Set<String> shortName2FullName(String shortName) {
		// If a field is referenced with its short name,
		// the full name should be acquired in the full context.
		// The get-method then will result in a null, which seems to be correct.
		return ec.shortName2FullName(shortName);
	}
}
