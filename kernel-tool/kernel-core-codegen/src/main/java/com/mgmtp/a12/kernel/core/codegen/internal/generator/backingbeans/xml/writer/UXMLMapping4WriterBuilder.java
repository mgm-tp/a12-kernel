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

package com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer;

import java.util.List;
import java.util.stream.Collectors;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.CodeGenData;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.CodeGenModelBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.utils.PackageInformation;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.xml.IXMLZusatzfeld;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

public class UXMLMapping4WriterBuilder extends CodeGenModelBuilder {

	private List<IXMLZusatzfeld> additionalFields;
	private int numberOfMappingFiles;
	private List<String> rootGroups;

	public UXMLMapping4WriterBuilder(CodeGenData data, IProblemReporter pr, PackageInformation packageInfo,
			List<IXMLZusatzfeld> additionalFields, int numberOfMappingFiles) {
		super(data, pr, packageInfo);

		this.additionalFields = additionalFields;
		this.numberOfMappingFiles = numberOfMappingFiles;
		this.rootGroups = data.getEc().getDirectlyContainedEntities("/").stream()
				.map(r -> EntityUtils.getShortName(r.getFullName())).collect(Collectors.toList());
	}

	@Override
	public UXMLMapping4Writer build() {
		return new UXMLMapping4Writer(this);
	}

	public List<IXMLZusatzfeld> getAdditionalFields() {
		return additionalFields;
	}

	public List<String> getRootgroups() {
		return rootGroups;
	}

	public int getNumberOfMappingFiles() {
		return numberOfMappingFiles;
	}
}
