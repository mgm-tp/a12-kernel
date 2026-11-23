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

package com.mgmtp.a12.kernel.core.onTheFlyTesting.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.CodeProfileType;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICalculationInfo;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters;
import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.IMetaInfoCodeGenParams;

class CodeGenParams implements ICodeGeneratorParameters {

	static Builder getBuilder() {
		return new Builder();
	}

	static class Builder {
		private List<String> restrictedValidGraphemeSet = new ArrayList<>();
		private char decimalSeparator;
		private List<Locale> locales;
		private List<IMetaInfoCodeGenParams> metaInfoCodegenParams = new ArrayList<>();

		Builder setRestrictedValidGraphemeSet(List<String> restrictedValidGraphemeSet) {
			this.restrictedValidGraphemeSet = restrictedValidGraphemeSet;
			return this;
		}

		Builder setDecimalSeparator(char decimalSeparator) {
			this.decimalSeparator = decimalSeparator;
			return this;
		}

		Builder setLocales(List<Locale> locales) {
			this.locales = locales;
			return this;
		}

		Builder setMetaInfoCodegenParams(List<IMetaInfoCodeGenParams> metaInfoCodegenParams) {
			this.metaInfoCodegenParams = metaInfoCodegenParams;
			return this;
		}

		CodeGenParams build() {
			return new CodeGenParams(this);
		}
	}

	private final List<String> restrictedValidGraphemeSet;
	private final char decimalSeparator;
	private final List<Locale> locales;
	private final List<IMetaInfoCodeGenParams> metaInfoCodegenParams;

	private CodeGenParams(Builder builder) {
		this.restrictedValidGraphemeSet = builder.restrictedValidGraphemeSet;
		this.decimalSeparator = builder.decimalSeparator;
		this.locales = builder.locales;
		this.metaInfoCodegenParams = builder.metaInfoCodegenParams;
	}

	@Override
	public List<String> restrictedValidGraphemeSet() {
		return restrictedValidGraphemeSet;
	}

	@Override
	public char getDecimalSeparator() {
		return decimalSeparator;
	}

	@Override
	public ProgrammingLanguage getProgrammingLanguage() {
		return ProgrammingLanguage.GROOVY;
	}

	@Override
	public String getName() {
		return "ruleTest";
	}

	@Override
	public List<Locale> getLanguages() {
		return locales;
	}

	@Override
	public List<IMetaInfoCodeGenParams> getMetaInfoCodeGenParams() {
		return metaInfoCodegenParams;
	}

	@Override
	public CodeProfileType getCodeProfile() {
		return CodeProfileType.CP_STANDARD;
	}

	@Override
	public List<Pair<String, String>> getGeneratedFileInformation() {
		return new ArrayList<>();
	}

	@Override
	public List<ICalculationInfo> getCalculationInformation() {
		return new ArrayList<>();
	}

	@Override
	public String getProductVersion() {
		return "test rule";
	}

}
