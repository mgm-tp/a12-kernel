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

import java.util.LinkedList;
import java.util.List;

import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.FeldKennungIdentifierMappingLfdNr;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.FeldKennungIdentifierMappingLfdNrBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.UXMLMapping4Reader;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.UXMLMapping4ReaderBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.XMLReader;
import com.mgmtp.a12.kernel.core.codegen.internal.generator.backingbeans.xml.reader.XMLReaderBuilder;
import com.mgmtp.a12.kernel.core.codegen.internal.modularcodegen.SourceCodeCollectorV2;
import com.mgmtp.a12.kernel.core.parser.internal.check.util.EntityUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.IField;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.Product;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * The code generator creating the XML Reader code.
 */
public class XMLReaderCodeGenerator extends ModelBasedCodeGenerator {

	private static final String XML_READER_CLASSNAME_PREFIX = "XMLReader_";
	private static final String XML_READER_UXML_PREFIX = "mapping/UXMLMapping4Reader";
	private static final String XML_READER_FELDKENNUNG_ID_MAPPING_PREFIX = "mapping/FeldKennungIdentifierMapping_";

	private static final int MAXFIELDS_MAPPING = 1000;

	private XMLReader xmlReader;
	private UXMLMapping4Reader uXmlMapping4Reader;
	private List<FeldKennungIdentifierMappingLfdNr> fkMapLfdNrList;

	public XMLReaderCodeGenerator(CodeGenData data, IProblemReporter pr, SourceCodeCollectorV2 collector) {
		super(data, pr, collector);
	}

	@Override
	protected boolean preCheck() {
		return true;
	}

	@Override
	protected boolean createTemplateParameterClasses() {

		//Needed to set the template directory correctly
		CodeGenContext.initialize(Product.XML_READER, data.getParams().getProgrammingLanguage());

		xmlReader = new XMLReaderBuilder(data, problemReporter, getPackageInfo()).build();

		List<IField> fields = EntityUtils.getAllFields(data.getEc());
		int numberOfMappingFiles = (fields.size() / MAXFIELDS_MAPPING) + 1;

		uXmlMapping4Reader = new UXMLMapping4ReaderBuilder(data, problemReporter, getPackageInfo(), numberOfMappingFiles).build();

		fkMapLfdNrList = new LinkedList<>();

		for (int i = 0; i < numberOfMappingFiles; i++) {
			int startIndex = i * MAXFIELDS_MAPPING;
			int endIndex = (i + 1) * MAXFIELDS_MAPPING;
			if (endIndex >= fields.size()) {
				endIndex = fields.size();
			}

			List<IField> fieldsSubset = fields.subList(startIndex, endIndex);
			fkMapLfdNrList.add(new FeldKennungIdentifierMappingLfdNrBuilder(data, problemReporter, getPackageInfo(), (i + 1),
					fieldsSubset).build());
		}

		return !problemReporter.hasProblemOccurred();
	}

	@Override
	protected void createEntries() {
		CodeGenContext.initialize(Product.XML_READER, data.getParams().getProgrammingLanguage());
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

		String packageName = getPackageInfo().getInternalXmlReaderPackageName();
		switch (xmlReader.getXmlReaderType()) {
			case UXML:
				String uXmlNamePrefix = "U" + XML_READER_CLASSNAME_PREFIX;
				createJavaEntry(uXmlNamePrefix, xmlReader.toCode("UXML"),
						packageName);

				final String prefix = getPrefix(packageName);
				createEntry(prefix + XML_READER_UXML_PREFIX + BaseCodeGenConstants.JAVA_SUFFIX, uXmlMapping4Reader.toCode());

				writeFeldKennungIdentifierMappingLfdNr();
				break;
			case SXML:
				createJavaEntry("S" + XML_READER_CLASSNAME_PREFIX, xmlReader.toCode("SXML"), packageName);
				break;
			case EXML:
			case EXML_MAPPED:
			case NONE:
				break;
		}

	}

	private void createCppEntries() {

		switch (xmlReader.getXmlReaderType()) {
			case SXML:
				createCppAndHEntry("S" + XML_READER_CLASSNAME_PREFIX, xmlReader.toCode("SXML"), xmlReader.toCode("SXML_h"));
				break;
			case UXML:
			case EXML:
			case EXML_MAPPED:
			case NONE:
				break;
		}

	}

	private void writeFeldKennungIdentifierMappingLfdNr() {
		final String prefix = getPrefix(getPackageInfo().getInternalXmlReaderPackageName());
		for (FeldKennungIdentifierMappingLfdNr f : fkMapLfdNrList) {
			createEntry(prefix + XML_READER_FELDKENNUNG_ID_MAPPING_PREFIX + f.getFileNumber() + BaseCodeGenConstants.JAVA_SUFFIX,
					f.toCode());
		}
	}
}
