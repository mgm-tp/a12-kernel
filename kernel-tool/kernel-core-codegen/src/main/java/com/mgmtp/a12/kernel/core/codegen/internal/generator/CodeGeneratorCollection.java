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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.mgmtp.a12.kernel.core.codegen.internal.modularcodegen.SourceCodeCollectorV2;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.api.IExtmpCodeGenParams.XML_RW_Type;
import com.mgmtp.a12.kernel.core.extmpspecifics.internal.utils.ExtmpUtils;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.error.IProblemReporter;

/**
 * Collection of code generators for a model.
 * Can contain
 * <ul>
 * <li>a {@link ValidationCodeGenerator},</li>
 * <li>an {@link XMLReaderCodeGenerator}, and</li>
 * <li>an {@link XMLWriterCodeGenerator}.</li>
 * <ul>
 * <p>
 * The actual list is determined by the given data.
 * <p>
 * The class to {@link #generateCode()} follows the same steps as in {@link ModelBasedCodeGenerator}:
 * <ol>
 * <li>{@link #preCheck()} is called for all contained code generators</li>
 * <li>{@link #createTemplateParameterClasses()} is called for all contained code generators</li>
 * <li>{@link #createEntries()} is called for all contained code generators</li>.
 * </ol>
 */
public class CodeGeneratorCollection extends ModelBasedCodeGenerator {

	private final List<BaseCodeGenerator> codeGenerators;

	public CodeGeneratorCollection(CodeGenData data, IProblemReporter pr, SourceCodeCollectorV2 collector,
			boolean isPublicAccessorNeeded) {
		this(data, pr, collector, "", isPublicAccessorNeeded);
	}

	public CodeGeneratorCollection(CodeGenData data, IProblemReporter pr, SourceCodeCollectorV2 collector, String relativePath,
			boolean isPublicAccessorNeeded) {
		super(data, pr, collector);
		setRelativePath(relativePath);

		List<ModelBasedCodeGenerator> codeGeneratorList = new LinkedList<>();
		if (ProgrammingLanguage.VBA.equals(data.getParams().getProgrammingLanguage())) {
			VBAValidationCodeGenerator codeGenerator = new VBAValidationCodeGenerator(data, pr, collector);
			codeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(codeGenerator);
		} else {
			ValidationCodeGenerator codeGenerator = new ValidationCodeGenerator(data, pr, collector, isPublicAccessorNeeded);
			codeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(codeGenerator);
		}

		XML_RW_Type type = ExtmpUtils.getXmlType(data.getParams());
		if (type != XML_RW_Type.NONE) {
			//If we can generate the xml reader/writer we do it

			XMLReaderCodeGenerator xmlReaderCodeGenerator = new XMLReaderCodeGenerator(data, pr, collector);
			xmlReaderCodeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(xmlReaderCodeGenerator);
			XMLWriterCodeGenerator xmlWriterCodeGenerator = new XMLWriterCodeGenerator(data, pr, collector);
			xmlWriterCodeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(xmlWriterCodeGenerator);
		}
		if (type == XML_RW_Type.EXML_MAPPED) {
			MapperCodeGenerator mapperCodeGenerator = new MapperCodeGenerator(data, pr, collector);
			mapperCodeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(mapperCodeGenerator);
		}

		if (ProgrammingLanguage.JAVA.equals(data.getParams().getProgrammingLanguage())) {
			GradleConfigGenerator codeGenerator = new GradleConfigGenerator(data, pr, collector);
			codeGenerator.setRelativePath(relativePath);
			codeGeneratorList.add(codeGenerator);
		}

		this.codeGenerators = Collections.unmodifiableList(codeGeneratorList);
	}

	@Override
	protected String deriveRelativePath() {
		return "";
	}

	ValidationCodeGenerator getValidationCodeGenerator() {
		Optional<BaseCodeGenerator> bcg = codeGenerators.stream()
				.filter(cg -> cg instanceof ValidationCodeGenerator)
				.findFirst();
		return (ValidationCodeGenerator) bcg.orElseThrow(
				() -> new IllegalStateException("No ValidationCodeGenerator in CodeGeneratorCollection"));
	}

	List<BaseCodeGenerator> getCodeGenerators() {
		return codeGenerators;
	}

	@Override
	protected boolean preCheck() {
		return codeGenerators.stream()
				.map(BaseCodeGenerator::preCheck)
				.reduce(true, (a, b) -> a && b);
	}

	@Override
	protected boolean createTemplateParameterClasses() {
		return codeGenerators.stream()
				.map(BaseCodeGenerator::createTemplateParameterClasses)
				.reduce(true, (a, b) -> a && b);
	}

	@Override
	protected void createEntries() {
		codeGenerators.stream()
				.forEach(BaseCodeGenerator::createEntries);
	}

}
