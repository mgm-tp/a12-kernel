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
package com.mgmtp.a12.kernel.core.tool.a12internal.api.ado;

import java.util.Locale;
import java.util.Optional;

import com.mgmtp.a12.kernel.core.tool.a12internal.api.ado.serviceparams.ICodeGeneratorParameters.ProgrammingLanguage;

/**
 * This interface is intended to be implemented by clients.
 * Models a project specific predefined type.
 * <p>
 * For parsing, the predefined types are handled like strings.
 * For code generation the name (see {@link #getName()} is used. The runtime
 * must include the code for the validation.
 * <p>
 * See e.g. EMAIL, IBAN etc for predefined types.
 * Predefined types are project specific.
 */
public interface IPredefinedType extends IFieldType {

	enum ValidationStrategy {
		NONE,
		NOT_SUPPORTED,
		PROVIDED_VALIDATION_CODE,
		LENGTH_AND_PATTERN_CHECK
	}

	/**
	 * It is the responsibility of the caller to assure, that the predefined type is supported by
	 * the project.
	 * Should never be null.
	 *
	 * @return the name of the predefined type.
	 */
	public String getName();

	/**
	 * Returns the minimum length of the value. The minimum is always greater or equal 1.
	 * A minimum length of 0 is not allowed as empty strings are not regarded as a valid
	 * value for a string. Empty strings are handled as values, which are not specified.
	 *
	 * @return the minimum length of the string
	 */
	public int getMinLength();

	/**
	 * Returns the maximal length of the value. If the maximal length is undefined, returns a
	 * {@link IFieldType#UNDEFINED}. If the max length is set, max has to be always greater or
	 * equal 1 and greater or equal to the minimum length.
	 *
	 * @return the maximal length of the value
	 */
	public int getMaxLength();

	/**
	 * @return the pattern that must be matched by the value or an empty Optional if no pattern is
	 * 		specified.
	 */
	default Optional<String> getPattern() {
		return Optional.empty();
	}

	/**
	 * @param plang
	 * 		the {@link ProgrammingLanguage}
	 * @return The {@link ValidationStrategy} for the given programming language, an empty Optional
	 * 		if no entry has been specified.
	 */
	default Optional<ValidationStrategy> getValidationConfig(ProgrammingLanguage plang) {
		return Optional.empty();
	}

	/**
	 * Returns the error message; see {@link IStringType#getErrorMessage}
	 *
	 * @param loc
	 * 		specifies the {@link Locale} for the error message
	 * @return error message or null when not specified.
	 */
	default String getErrorMessage(Locale loc) {
		return null;
	}

	@Override
	public default boolean matchBaseType(IFieldType expectedBaseType) {
		return expectedBaseType == null || expectedBaseType instanceof IPredefinedType || expectedBaseType instanceof IStringType;
	}

	@Override
	default boolean isValueApplicable(String value) {
		if (getMaxLength() > 0 && getMaxLength() < value.length()) {
			return false;
		}
		return getMinLength() <= 0 || getMinLength() <= value.length();
	}
}
