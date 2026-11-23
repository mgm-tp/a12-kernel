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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.base.utils.internal.ElementPathUtils;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenModel;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.IntegerParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.meta.util.MetaSprachUnterschiedeHelper;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.codegen.internal.utils.SplitFunctionsHelper;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IFieldMapping;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IMapInfo;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IRootGroupMapping;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IEntity;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IGroup;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;

/**
 * Corresponding class to the template with the same name.
 * With this class the mapping information from the mapped model is generated.
 */
@StringTemplateGroup("base")
public class MapInformation extends CodeGenModel {

	private ProgrammingLanguage language;
	private List<IMapInfo> mapInfos;
	private Map<String, Integer> rootGroupNumberByEXMLEntityFullName;
	private List<String> codeFragments4RootGroupMapInfos;
	private List<CodeGenFieldMapInfo> fieldMapInfos;

	public MapInformation(MapInformationBuilder builder) {
		super(builder);
		this.language = data.getParams().getProgrammingLanguage();
		this.mapInfos = builder.getMapInfos();
		// since the creation of the code fragments should be called in a fixed order,
		// we initialize them in the constructor
		this.rootGroupNumberByEXMLEntityFullName = new HashMap<>();
		this.codeFragments4RootGroupMapInfos = createCodeFragments4RootGroupMapInfos();
		this.fieldMapInfos = createCodeFragments4FieldMapInfos();
	}

	private List<String> createCodeFragments4RootGroupMapInfos() {
		AtomicInteger i = new AtomicInteger(0);
		return mapInfos.stream()
				.filter(mi -> mi instanceof IRootGroupMapping)
				.map(mi -> {
					rootGroupNumberByEXMLEntityFullName.put(mi.getEntity().getFullName(), i.get());
					String indexFieldFullName4Usb = getIndexFieldFullName4Usb((IRootGroupMapping) mi);
					return new CodeGenRootGroupMapInfo(i.getAndIncrement(), (IRootGroupMapping) mi,
							indexFieldFullName4Usb).toCode();
				})
				.collect(Collectors.toList());
	}

	private String getIndexFieldFullName4Usb(IRootGroupMapping rootGroupMapping) {
		if ((rootGroupMapping.getHierarchyLevel4Usb() == IMapInfo.NOT_APPLICABLE) || rootGroupMapping.hasLevelMappedToTechnicalUsb()) {
			return null;
		}
		IGroup indexedGroup = getIndexedGroup(rootGroupMapping, rootGroupMapping.getHierarchyLevel4Usb());
		return EntityUtils.createPath(indexedGroup.getFullName(), indexedGroup.getIndexFieldNames().iterator().next());
	}

	private IGroup getIndexedGroup(IRootGroupMapping rootGroupMapping, int hierarchyLevel) {
		String indexedGroupFullName = EntityUtils.getPathUpToLevel(hierarchyLevel, rootGroupMapping.getEntity().getFullName());
		IEntity indexedGroupAsEntity = data.getEc().get(indexedGroupFullName);
		assert indexedGroupAsEntity instanceof IGroup;
		IGroup indexedGroup = (IGroup) indexedGroupAsEntity;
		assert indexedGroup.getIndexFieldNames().size() == 1;
		return indexedGroup;
	}

	private List<CodeGenFieldMapInfo> createCodeFragments4FieldMapInfos() {
		return mapInfos.stream()
				.filter(mi -> mi instanceof IFieldMapping)
				.map(mi -> {
					int rootGroupNumber = getRootGroupNumber((IFieldMapping) mi);
					return new CodeGenFieldMapInfo((IFieldMapping) mi, rootGroupNumber);
				})
				.collect(Collectors.toList());
	}

	private int getRootGroupNumber(IFieldMapping fieldMapInfo) {
		String fieldFullName = fieldMapInfo.getEntity().getFullName();
		return rootGroupNumberByEXMLEntityFullName.entrySet().stream()
				.filter(entry -> fieldFullName.startsWith(entry.getKey() + ElementPathUtils.SEPARATOR))
				.findFirst()
				.map(Entry::getValue)
				.orElseThrow(() -> new IllegalArgumentException("Cannot find root group number for field " + fieldFullName));
	}

	public List<String> getCodeFragments4RootGroupMapInfos() {
		return codeFragments4RootGroupMapInfos;
	}

	public String getFieldMapInfosSplitFunctionCode() {
		return SplitFunctionsHelper.deriveSplitFunctionCode(fieldMapInfos,
				MetaSprachUnterschiedeHelper.JAVA_INIT_FIELD_MAP_INFOS_METHOD_NAME,
				SplitFunctionsHelper.ELEMENTS_PER_SPLIT_METHOD_DEFAULT,
				language);
	}

	public String getFieldMapInfosSplitFunctionCodeAufrufe() {
		final String aufruf = MetaSprachUnterschiedeHelper.formatMethodCall(
				MetaSprachUnterschiedeHelper.METHOD_NAME_INIT_FIELD_MAP_INFOS_TEMPLATE, language);
		return SplitFunctionsHelper.deriveSplitFunctionCodeCalls(fieldMapInfos.size(),
				aufruf, SplitFunctionsHelper.ELEMENTS_PER_SPLIT_METHOD_DEFAULT, language);
	}

	/**
	 * This method is for a hierarchy level mapping or mzi specifications which are integers and not
	 * always defined.
	 *
	 * @return string representation for the given integer. The result will be inserted into the
	 * 		templates. If the given integer equals to {@link IMapInfo#NOT_APPLICABLE}, the string
	 * 		"MapInfo.NOT_APPLICABLE" will be returned.
	 */
	protected static IntegerParameter convertIntegerSpecificationToCode(Integer integerSpecification) {
		if (integerSpecification == IMapInfo.NOT_APPLICABLE) {
			return new IntegerParameter("MapInfo.NOT_APPLICABLE");
		} else {
			return new IntegerParameter(integerSpecification);
		}
	}

}
