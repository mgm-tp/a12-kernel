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
package com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.config.IProjectSpecificSettings;

/**
 * Intended to be implemented by the client.
 * Specifies the parameters for the code generation.
 */
// similar but different: IValidationCodeGeneratorConfig
// similar but different: ICodeControlParameter
// (this class: ICodeGeneratorParameters)
public interface ICodeGeneratorParameters extends IModelCodeGeneratorParameters {

	String DEFAULT_PACKAGE_PATH = "com.mgmtp.a12.kernel.generated.";

	/**
	 * Possible programming languages
	 */
	enum ProgrammingLanguage {
		JAVA(true),
		CPLUS(true),
		JAVA_SCRIPT_REDUCED(true),
		JAVA_SCRIPT_VK(false),
		VBA(true),
		GROOVY(true);
		private boolean withUsb;

		ProgrammingLanguage(boolean withUsb) {
			this.withUsb = withUsb;
		}

		public boolean isWithUsb() {
			return withUsb;
		}
	}

	/**
	 * Possible products
	 */
	enum Product {
		VALIDATION,
		XML_READER,
		XML_WRITER,
		MAPPER,
		BUILDSCRIPTS,
		DATATRANSFER,
		STRUCTURALMAPPING
	}

	/**
	 * Configuration for the javascript code generation.
	 */
	enum JsVkCodeGenConfig {
		STANDARD,
		/**
		 * See IValidationCodeGeneratorConfig.JsCodeGenConfig#WRAPPED_IN_FUNCTION
		 */
		WRAPPED_IN_FUNCTION,
		/**
		 * See IValidationCodeGeneratorConfig.JsCodeGenConfig#WRAPPED_IN_FUNCTION_EXTENDED
		 */
		WRAPPED_IN_FUNCTION_EXTENDED
	}

	/**
	 * Returns a {@link ProgrammingLanguage}, which specifies the programming language the code
	 * generator should use.
	 *
	 * @return a {@link ProgrammingLanguage}, specifying the programming language for the code
	 * 		generation.
	 */
	public ProgrammingLanguage getProgrammingLanguage();

	/**
	 * Specifies the name, which is used for naming the different generated fragments.
	 * <p>
	 * Note: in ExtMP we use variant, model and version name (e.g. {@literal variant_model_version}).
	 * <p>
	 * The string may only use [A-za-z_][A-za-z0-9_]* as characters.
	 * </p>
	 *
	 * @return the name used in the code generation to name the model.
	 */
	public String getName();

	/**
	 * In comparison to {@link #getName()}, this name won't be used in the generated java code and
	 * that's why the namespace for this name is broader. While generating javascript code with
	 * {@link JsVkCodeGenConfig#WRAPPED_IN_FUNCTION} or
	 * {@link JsVkCodeGenConfig#WRAPPED_IN_FUNCTION_EXTENDED}, this name will be used to define a
	 * module with this name in {@code root} and in this case, the raw name or id of the document
	 * model without escaping the character '-' and preserving capital letters is expected.
	 *
	 * @return the name without any change.
	 */
	public default String getRawName() {
		return getName();
	}

	/**
	 * Can be implemented to specify a package name to be used to generate java code that differs from the default package
	 * {@link #DEFAULT_PACKAGE_PATH}. If not implemented, java code will typically default to {@link #DEFAULT_PACKAGE_PATH},
	 * and some ExtMP-specific code will use other defaults (for historic reasons). If implemented, all java code, including all
	 * ExtMP-specific java code, must use the given prefix.
	 * <p/>
	 * NOTE: We use an Optional here rather than a nullable String, to emphasize the explicit representation for "default"
	 * (replacing absent values by a concrete default value eagerly wouldn't work, because different default values apply
	 * for different (ExtMP-specific) generated code files, at least when legacy behavior is to be reproduced exactly).
	 */
	default public Optional<String> getNondefaultPackagePrefix() {
		return Optional.empty();
	}

	/**
	 * Specifies the list of languages the code is generated for. Do not mix up with the programming
	 * language. And do not mix up with the language specified by the project specific settings. The
	 * ordering of the list specifies the ordering in the generated code.
	 * <p>
	 * If there are multiple languages returned here, the generated code will encompass all the
	 * selected languages, which can be selected, specifying
	 * <i>com.mgmtp.a12.kernel.core.rt.api.ValidationMode#getLanguage()</i>
	 * when
	 * using the validation runtime API.
	 * <p>
	 * For the project specific settings for the languages see
	 * {@link IProjectSpecificSettings#getProjectLanguageSettings()}
	 *
	 * @return the list of languages the code is generated for.
	 */
	public List<Locale> getLanguages();

	/**
	 * Specifies the code profile: standard or source in a mapping relationship.
	 *
	 * @return the code profile.
	 */
	public CodeProfileType getCodeProfile();

	/**
	 * Returns the information that should be put at the beginning of generated files as comments.
	 * The output depends on the programming language. They usually contain things like copyright
	 * strings and general information about the generated code.
	 * <p>
	 * The order of the output information, corresponds to the iteration order of the returned
	 * map.
	 *
	 * @return the additional informations for the generated files.
	 */
	public List<Pair<String, String>> getGeneratedFileInformation();

	/**
	 * Get the calculation information for the model. For ExtMP, only serverside calculations
	 * are stored here.
	 *
	 * @return the calculation information of the model.
	 */
	public List<ICalculationInfo> getCalculationInformation();

	/**
	 * Returns a String, containing the version of the server that is used to generate the code.
	 *
	 * @return the server version that is used to generate the code.
	 */
	public String getProductVersion();

	/**
	 * Get the information regarding dynamic enumerations.
	 *
	 * @return the list of {@link IDynamicEnumerationInfo}s to be used for code generation. Never
	 * 		<code>null</code>.
	 */
	public default List<IDynamicEnumerationInfo> getDynamicEnumerationInformation() {
		return Collections.emptyList();
	}

	/**
	 * This configuration will affect the code generation only if {@link #getProgrammingLanguage()}
	 * returns {@link ProgrammingLanguage#JAVA_SCRIPT_VK}.
	 *
	 * @return see IValidationCodeGeneratorConfig#getJsCodeGenConfig().
	 */
	public default JsVkCodeGenConfig getJsVkCodeGenConfig() {
		return JsVkCodeGenConfig.STANDARD;
	}

	/**
	 * @return {@code true} if code generation should return plain source code.
	 * 		If {@code false} (default), zipped source code will be returned.
	 */
	public default boolean isPlain() {
		return false;
	}
}
