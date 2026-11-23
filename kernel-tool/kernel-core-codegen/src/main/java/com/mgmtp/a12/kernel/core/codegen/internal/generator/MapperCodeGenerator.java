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

package com.mgmtp.a12.kernel.core.codegen.internal.generator;

import java.util.List;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.mapper.MapInformation;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.mapper.MapInformationBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.mapper.UXMLMapper;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.mapper.UXMLMapperBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.modularcodegen.SourceCodeCollectorV2;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.e2u_mapper.IMapInfo;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.api.IExtmpCodeGenParams;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.Product;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * The code generator for the mapper. With this class following things can be generated:
 * <ul>
 * <li>Mapping information from the mapped model.</li>
 * <li>XML-converter</li>
 * </ul>
 */
public class MapperCodeGenerator extends ModelBasedCodeGenerator {

	private static final String MAP_INFO_CLASSNAME_PREFIX = "MapInformation_";
	private static final String UXML_TO_EXML_CLASSNAME_PREFIX = "UXMLToEXMLMapper_";
	private static final String EXML_TO_UXML_CLASSNAME_PREFIX = "EXMLToUXMLMapper_";

	private MapInformation mapInformation;
	private UXMLMapper uXMLMapper;

	public MapperCodeGenerator(CodeGenData data, IProblemReporter pm, SourceCodeCollectorV2 collector) {
		super(data, pm, collector);
	}

	@Override
	protected boolean preCheck() {
		return true;
	}

	@Override
	protected boolean createTemplateParameterClasses() {

		//Needed to set the template directory correctly
		CodeGenContext.initialize(Product.MAPPER, data.getParams().getProgrammingLanguage());

		List<IMapInfo> mapInfos = ((IExtmpCodeGenParams) data.getParams()).getMappedModelParameter().getMappingInfos();
		mapInformation = new MapInformationBuilder(data, problemReporter, getPackageInfo(), mapInfos).build();

		uXMLMapper = new UXMLMapperBuilder(data, problemReporter, getPackageInfo()).build();

		return !problemReporter.hasProblemOccurred();
	}

	@Override
	protected void createEntries() {
		CodeGenContext.initialize(Product.MAPPER, data.getParams().getProgrammingLanguage());
		switch (data.getParams().getProgrammingLanguage()) {
			case JAVA:
				createJavaEntry(MAP_INFO_CLASSNAME_PREFIX, mapInformation.toCode(),
						getPackageInfo().getInternalUXmlMapperPackageName());
				createJavaEntry(EXML_TO_UXML_CLASSNAME_PREFIX, uXMLMapper.toCode("EXMLToUXML"),
						getPackageInfo().getInternalUXmlMapperPackageName());
				createJavaEntry(UXML_TO_EXML_CLASSNAME_PREFIX, uXMLMapper.toCode("UXMLToEXML"),
						getPackageInfo().getInternalUXmlMapperPackageName());
				if (areLegacyPublicExtmpAccessorsNeeded(true)) {
					createJavaEntry(uXMLMapper.getPublicClassNamePrefix(), uXMLMapper.toCode("Public"),
							getPackageInfo().getPublicUXmlMapperPackageName());
				}
				break;
			case CPLUS:
				// process for mapper does not reach this option, see PacketCodeGenerator#createEntries4MappedModel
				break;
			default:
				throw new IllegalArgumentException("Code for mapper may only be generated for java.");
		}
	}

}
