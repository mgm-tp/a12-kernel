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

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.FeldInhalteMapLfdNr;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.FeldInhalteMapLfdNrBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.UXMLMapping4Writer;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.UXMLMapping4WriterBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.XMLSchemaNodesContainer;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.XMLSchemaNodesContainerBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.XMLWriter;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.writer.XMLWriterBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.modularcodegen.SourceCodeCollectorV2;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.xml.IXMLZusatzfeld;
import com.mgmtp.a12.kernel.core.extmpspecifics.a12internal.api.xml.XMLSchemaNode;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.api.IExtmpCodeGenParams;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.Product;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * The code generator creating the XML Writer code.
 */
public class XMLWriterCodeGenerator extends ModelBasedCodeGenerator {

	private static final String XML_WRITER_CLASSNAME_PREFIX = "XMLWriter_";
	private static final String XML_WRITER_SCHEMA_NODES_PREFIX = "XmlSchemaKnoten_";
	private static final String XML_WRITER_FELDINHALTE_MAP_PREFIX = "FeldInhalteMap_";
	private static final String XML_WRITER_UXML_MAPPING_PREFIX = "UXMLMapping4Writer";

	private static final int MAXFELDER_MAPPING = 200;

	private XMLWriter xmlWriter;
	private XMLSchemaNodesContainer xmlSchemaNodeContainer;
	private UXMLMapping4Writer uXmlMapping4Writer;
	private FeldInhalteMapLfdNr[] feldInhalteMap;

	public XMLWriterCodeGenerator(CodeGenData data, IProblemReporter pr, SourceCodeCollectorV2 collector) {
		super(data, pr, collector);
	}

	@Override
	protected boolean preCheck() {
		return true;
	}

	@Override
	protected boolean createTemplateParameterClasses() {

		//Needed to set the template directory correctly
		CodeGenContext.initialize(Product.XML_WRITER, data.getParams().getProgrammingLanguage());

		xmlWriter = new XMLWriterBuilder(data, problemReporter, getPackageInfo()).build();
		List<XMLSchemaNode> xmlSchemaNodes = ((IExtmpCodeGenParams) data.getParams()).getXmlSchemaNodes();
		xmlSchemaNodeContainer = new XMLSchemaNodesContainerBuilder(data, problemReporter, getPackageInfo(),
				xmlSchemaNodes).build();

		int numberOfMappingFiles = (EntityUtils.getAllFields(data.getEc()).size() / MAXFELDER_MAPPING) + 1;
		List<IXMLZusatzfeld> additionalFields = ((IExtmpCodeGenParams) data.getParams()).getXmlZusatzfelder();
		uXmlMapping4Writer = new UXMLMapping4WriterBuilder(data, problemReporter, getPackageInfo(), additionalFields,
				numberOfMappingFiles).build();

		feldInhalteMap = new FeldInhalteMapLfdNr[numberOfMappingFiles];
		List<IField> allFields = EntityUtils.getAllFields(data.getEc());
		for (int i = 0; i < numberOfMappingFiles; i++) {
			int startIndex = i * MAXFELDER_MAPPING;
			int endIndex = (i + 1) * MAXFELDER_MAPPING;
			if (endIndex >= allFields.size()) {
				endIndex = allFields.size();
			}
			List<IField> currentFields = allFields.subList(startIndex, endIndex);
			feldInhalteMap[i] = new FeldInhalteMapLfdNrBuilder(data, problemReporter, getPackageInfo(), currentFields,
					i + 1).build();
		}

		return !problemReporter.hasProblemOccurred();
	}

	@Override
	protected void createEntries() {
		CodeGenContext.initialize(Product.XML_WRITER, data.getParams().getProgrammingLanguage());
		switch (data.getParams().getProgrammingLanguage()) {
			case JAVA:
				createJavaEntries();
				break;
			case CPLUS:
				createCppEntries();
				break;
			default:
				throw new IllegalArgumentException("Unsupported programming language " + data.getParams().getProgrammingLanguage());
		}
	}

	private void createJavaEntries() {
		String packageName = getPackageInfo().getInternalXmlWriterPackageName();
		switch (xmlWriter.getXmlReaderType()) {
			case UXML:
				String uXmlNamePrefix = "U" + XML_WRITER_CLASSNAME_PREFIX;
				createJavaEntry(uXmlNamePrefix, xmlWriter.toCode("UXML"), packageName);

				final String prefix = getPrefix(packageName);
				createEntry(prefix + "mapping/" + XML_WRITER_UXML_MAPPING_PREFIX + BaseCodeGenConstants.JAVA_SUFFIX,
						uXmlMapping4Writer.toCode());
				for (FeldInhalteMapLfdNr current : feldInhalteMap) {
					String filePath = prefix + "mapping/" + XML_WRITER_FELDINHALTE_MAP_PREFIX + current.getLfdNr() + BaseCodeGenConstants.JAVA_SUFFIX;
					createEntry(filePath, current.toCode());
				}
				break;
			case SXML:
				createJavaEntry("S" + XML_WRITER_CLASSNAME_PREFIX, xmlWriter.toCode("SXML"),
						packageName);
				createJavaEntry(XML_WRITER_SCHEMA_NODES_PREFIX, xmlSchemaNodeContainer.toCode(),
						getPackageInfo().getInternalSXmlPackageName());
				break;
			case EXML_MAPPED:
				createJavaEntry("U" + XML_WRITER_CLASSNAME_PREFIX, xmlWriter.toCode("UXML4EXML"),
						packageName);
				break;
			case EXML:
			case NONE:
				break;
			default:
				break;
		}

	}

	private void createCppEntries() {

		switch (xmlWriter.getXmlReaderType()) {
			case SXML:
				createCppAndHEntry("S" + XML_WRITER_CLASSNAME_PREFIX, xmlWriter.toCode("SXML"), xmlWriter.toCode("SXML_h"));
				createCppAndHEntry(XML_WRITER_SCHEMA_NODES_PREFIX, xmlSchemaNodeContainer.toCode(),
						xmlSchemaNodeContainer.toCode("h"));
				break;
			case UXML:
			case EXML:
			case EXML_MAPPED:
			case NONE:
				break;
			default:
				break;
		}

	}
}
