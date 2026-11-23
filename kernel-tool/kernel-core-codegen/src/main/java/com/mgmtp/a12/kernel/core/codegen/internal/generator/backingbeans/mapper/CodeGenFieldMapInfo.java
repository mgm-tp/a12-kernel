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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.IntegerParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.ListParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.StringParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.NullTerminal;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.STParameterClassBase;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IFieldMapping;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.utils.MapInfoUtils;

/**
 * Corresponding class for the template with the same name.
 * With this class a single entry of field mapping information can be generated.
 */
@StringTemplateGroup("base")
public class CodeGenFieldMapInfo extends STParameterClassBase {

	private final IFieldMapping fieldMapInfo;
	private final List<StringParameter> fieldIdentifiers1;
	private List<StringParameter> fieldIdentifiers2 = new ArrayList<>();
	private final List<IntegerParameter> mziSpecs;
	private final int rootGroupNumber;

	public CodeGenFieldMapInfo(IFieldMapping fieldMapInfo, int rootGroupNumber) {
		this.fieldMapInfo = fieldMapInfo;
		// there is no method for the next initialization
		// because it should be transparent that fieldIdentifiers2 is also initialized
		this.fieldIdentifiers1 = fieldMapInfo.getFieldIdentifiers().stream()
				.map(pair -> {
					fieldIdentifiers2.add(new StringParameter(pair.getRight()));
					return new StringParameter(pair.getLeft());
				})
				.collect(Collectors.toList());
		this.mziSpecs = createMziSpecsList();
		this.rootGroupNumber = rootGroupNumber;
	}

	public String getFullNameCode() {
		return new StringParameter(fieldMapInfo.getEntity().getFullName()).toCode();
	}

	public String getFieldIdentifiers1Code() {
		return new ListParameter<>(fieldIdentifiers1).toCode();
	}

	public String getFieldIdentifiers2Code() {
		if (MapInfoUtils.isSplit(fieldMapInfo)) {
			return new ListParameter<>(fieldIdentifiers2).toCode();
		} else {
			return new NullTerminal().toCode("nullptr");
		}
	}

	private List<IntegerParameter> createMziSpecsList() {
		return fieldMapInfo.getMziSpecs().stream()
				.map(mziSpec -> MapInformation.convertIntegerSpecificationToCode(mziSpec))
				.collect(Collectors.toList());
	}

	public String getMziSpecsCode() {
		return new ListParameter<>(mziSpecs).toCode();
	}

	public String getHierarchyLevel4Mzi() {
		return MapInformation.convertIntegerSpecificationToCode(fieldMapInfo.getHierarchyLevel4Mzi()).toCode();
	}

	public String getHierarchyLevel4FieldList() {
		return MapInformation.convertIntegerSpecificationToCode(fieldMapInfo.getHierarchyLevel4FieldList()).toCode();
	}

	public String getRootGroupNumber() {
		return new IntegerParameter(rootGroupNumber).toCode();
	}

}
