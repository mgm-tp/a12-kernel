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

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.StringParameter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.NullTerminal;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.STParameterClassBase;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.StringTemplateGroup;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IRootGroupMapping;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.utils.MapInfoUtils;

/**
 * Corresponding class for the template with the same name.
 * With this class a single entry of root group mapping information can be generated.
 */
@StringTemplateGroup("base")
public class CodeGenRootGroupMapInfo extends STParameterClassBase {

	// here we treat this as counter for the root groups and
	// in CodeGenFieldMapInfo it is the root group number
	private final int counter;
	private final IRootGroupMapping rootGroupMapInfo;
	private final String indexFieldFullName4Usb;

	public CodeGenRootGroupMapInfo(int counter, IRootGroupMapping rootGroupMapInfo, String indexFieldFullName4Usb) {
		this.counter = counter;
		this.rootGroupMapInfo = rootGroupMapInfo;
		this.indexFieldFullName4Usb = indexFieldFullName4Usb;
	}

	public String getCounter() {
		return String.valueOf(counter);
	}

	public String getFullNameCode() {
		return new StringParameter(rootGroupMapInfo.getEntity().getFullName()).toCode();
	}

	public String getName1Code() {
		return new StringParameter(rootGroupMapInfo.getRootGroupName1()).toCode();
	}

	public String getName2Code() {
		if (MapInfoUtils.isSplit(rootGroupMapInfo)) {
			return new StringParameter(rootGroupMapInfo.getRootGroupName2()).toCode();
		} else {
			return new NullTerminal().toCode("nullptr");
		}
	}

	public String getHierarchyLevel4Splitting() {
		return MapInformation.convertIntegerSpecificationToCode(rootGroupMapInfo.getHierarchyLevel4Splitting()).toCode();
	}

	public String getHierarchyLevel4RootGroupRepetition() {
		return MapInformation.convertIntegerSpecificationToCode(rootGroupMapInfo.getHierarchyLevel4RootgroupRepetition()).toCode();
	}

	public String getHierarchyLevel4Usb() {
		return MapInformation.convertIntegerSpecificationToCode(rootGroupMapInfo.getHierarchyLevel4Usb()).toCode();
	}

	public String getIndexFieldFullName4Usb() {
		return indexFieldFullName4Usb != null ?
				new StringParameter(indexFieldFullName4Usb).toCode() :
				new NullTerminal().toCode("nullptr");
	}
}
