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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mgmtp.a12.kernel.core.base.utils.internal.ElementPathUtils;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.CodeGenConstants;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.CodeGenData;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenMetaModel;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenModel;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenModelBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.GroupSortInfo;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.meta.DefaultPresentationInformation;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.meta.MetaContainerInfo;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.PackageInformation;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.RuleInfo;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.utils.ExtmpUtils;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.parsetree.a12internal.services.ParseBaumBedingungsServices;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEC;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IPredefinedType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMetaInfoCodeGenParams;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ISortField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * Builds the {@link CodeGenModel}.
 */
public class CodeGenMetaModelBuilder extends CodeGenModelBuilder {
	protected final List<IMetaInfoCodeGenParams> additionalValues;
	protected final List<RuleInfo> ruleInfos;
	protected final String productVersion;

	public CodeGenMetaModelBuilder(CodeGenData data, List<RuleInfo> rules, IProblemReporter problemReporter,
			PackageInformation packageInfo) {
		super(data, problemReporter, packageInfo);
		this.additionalValues = new ArrayList<>(data.getParams().getMetaInfoCodeGenParams());
		this.ruleInfos = rules;
		this.productVersion = data.getParams().getProductVersion();
	}

	@Override
	public CodeGenMetaModel build() {
		return new CodeGenMetaModel(this);
	}

	public SortedSet<String> getApplBedNames() {
		SortedSet<String> applNames = new TreeSet<>();
		for (RuleInfo r : ruleInfos) {
			applNames.addAll(ParseBaumBedingungsServices.getApplikationsBedingungsnamen(r.getCondition()));
		}
		return applNames;
	}

	public Set<String> getCustomFieldTypeNames() {
		return Stream.<String>concat(ruleInfos.stream() // from rules
								.flatMap(r -> ParseBaumBedingungsServices.getCustomFieldTypeReferences(r.getCondition()).stream())
								.distinct(),
						extractCustomFieldTypes(data.getEc()).stream()) // from field types
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private Set<String> extractCustomFieldTypes(IEC ec) {
		return EntityUtils.getAllFields(ec).stream()
				.map(IField::getType)
				.filter(IPredefinedType.class::isInstance)
				.map(t -> ((IPredefinedType) t).getName())
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public String getProductVersion() {
		return productVersion;
	}

	public List<IMetaInfoCodeGenParams> getAdditionalValues() {
		return additionalValues;
	}

	public int getNumFields() {
		return EntityUtils.getAllFields(data.getEc()).size();
	}

	public int getNumRules() {
		return ruleInfos.size();
	}

	public DefaultPresentationInformation getDefaultPresentationInformation() {
		return new DefaultPresentationInformation(data.getPss());
	}

	public List<GroupSortInfo> getGroupSortInfoList() {
		List<GroupSortInfo> returnList = new ArrayList<>();
		List<IEntity> topLevelGroups = data.getEc().getDirectlyContainedEntities(ElementPathUtils.SEPARATOR);

		for (IEntity rootgroup : topLevelGroups) {
			computeGroupList((IGroup) rootgroup, group -> {
				List<ISortField> sortFields = group.getSortInfo().get(CodeGenConstants.SORT_CATEGORY_DEFAULT);
				if (sortFields != null && !sortFields.isEmpty()) {
					returnList.add(new GroupSortInfo(group.getFullName(), sortFields));
				}
			});
		}
		return returnList;
	}

	public List<MetaContainerInfo> getMetaContainerInfoList() {
		List<MetaContainerInfo> returnList = new ArrayList<>();
		List<IEntity> topLevelGroups = data.getEc().getDirectlyContainedEntities(ElementPathUtils.SEPARATOR);

		for (IEntity rootgroup : topLevelGroups) {
			computeGroupList((IGroup) rootgroup, group -> {
				returnList.add(new MetaContainerInfo(
						group,
						additionalValues,
						data.getParams().getProgrammingLanguage(),
						ExtmpUtils.isHModel(data.getModelSpecificSettings())));
			});
		}
		return returnList;
	}

	private void computeGroupList(IGroup group, Consumer<IGroup> consumer) {
		consumer.accept(group);
		data.getEc().getDirectlyContainedEntities(group.getFullName()).stream()
				.filter(IEntity::isGroup)
				.forEach(k -> computeGroupList((IGroup) k, consumer));
	}
}
